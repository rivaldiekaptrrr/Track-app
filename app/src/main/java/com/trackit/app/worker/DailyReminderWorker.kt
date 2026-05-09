package com.trackit.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trackit.app.MainActivity
import com.trackit.app.R
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "daily_reminders"
        const val CHANNEL_NAME = "Pengingat Harian"
        private const val NOTIFICATION_ID = 5000
    }

    override suspend fun doWork(): Result {
        try {
            val isEnabled = preferencesManager.isDailyReminderEnabled.first()
            if (!isEnabled) {
                return Result.success()
            }

            val profileId = preferencesManager.activeProfileId.first()
            
            val startOfDay = DateUtils.getStartOfDay()
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1 // 23:59:59.999

            val expensesCount = transactionRepository.countExpensesForDaySync(startOfDay, endOfDay, profileId)

            if (expensesCount == 0) {
                showNotification()
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun showNotification() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Pengingat untuk mencatat pengeluaran harian."
                }
                manager.createNotificationChannel(channel)
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Belum mencatat pengeluaran?")
            .setContentText("Ayo catat pengeluaran hari ini agar keuanganmu tetap terpantau!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}
