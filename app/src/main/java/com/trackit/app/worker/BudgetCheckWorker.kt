package com.trackit.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trackit.app.R
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.repository.BudgetRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.CurrencyUtils
import com.trackit.app.util.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class BudgetCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "budget_check_worker"
        const val CHANNEL_ID = "budget_alerts"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        return try {
            val activeProfileId = preferencesManager.activeProfileId.first()
            val budgetSetting = budgetRepository.getBudgetSettingSync(activeProfileId)
            val budget = budgetSetting?.monthlyBudget ?: return Result.success()

            if (budget <= 0) return Result.success()

            val startOfMonth = DateUtils.getStartOfMonth()
            val endOfMonth = DateUtils.getEndOfMonth()
            val totalSpent = transactionRepository.getTotalSpentInMonthSync(startOfMonth, endOfMonth, activeProfileId)

            val percentage = totalSpent / budget

            if (percentage >= 0.8) {
                showNotification(totalSpent, budget, percentage)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(spent: Double, budget: Double, percentage: Double) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Peringatan Anggaran",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi peringatan ketika pengeluaran mendekati batas anggaran"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val percentText = "${(percentage * 100).toInt()}%"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Peringatan Anggaran!")
            .setContentText("Pengeluaran Anda sudah $percentText dari anggaran bulan ini.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Pengeluaran Anda sudah mencapai $percentText dari anggaran bulan ini.\n\n" +
                        "Terpakai: ${CurrencyUtils.formatRupiah(spent)}\n" +
                        "Anggaran: ${CurrencyUtils.formatRupiah(budget)}\n\n" +
                        "Pertimbangkan untuk mengurangi pengeluaran agar tetap dalam batas anggaran."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
