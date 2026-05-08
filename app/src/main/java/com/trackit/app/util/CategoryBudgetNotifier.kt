package com.trackit.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.trackit.app.data.repository.CategoryBudgetRepository
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case bertanggung jawab mengecek budget per kategori dan memunculkan notifikasi
 * lokal saat pengeluaran mendekati atau melebihi batas budget.
 *
 * Dipanggil setelah setiap transaksi EXPENSE disimpan atau diubah.
 *
 * Anti-spam: notifikasi peringatan (< 100%) hanya dikirim SATU KALI per bulan per kategori,
 * dilacak via kolom `lastWarningMonth` di tabel category_budgets.
 */
@Singleton
class CategoryBudgetNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryBudgetRepository: CategoryBudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {

    companion object {
        const val CHANNEL_ID = "category_budget_alerts"
        const val CHANNEL_NAME = "Peringatan Budget Kategori"
        private const val NOTIFICATION_ID_BASE = 2000 // ID base; final = 2000 + categoryId
        private val monthFormat = SimpleDateFormat("yyyy-MM", Locale("id", "ID"))
    }

    /**
     * Cek budget untuk satu kategori tertentu setelah transaksi disimpan.
     * @param categoryId ID kategori yang baru saja digunakan.
     * @param profileId  ID profil aktif.
     */
    suspend fun checkAfterTransaction(categoryId: Long, profileId: Long) {
        // Hanya cek jika ada data budget untuk kategori ini
        val budget = categoryBudgetRepository.getBudgetByCategorySync(categoryId, profileId) ?: return
        if (budget.amount <= 0.0) return

        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()
        val totalSpent = transactionRepository.getTotalSpentByCategoryInMonthSync(
            categoryId, startOfMonth, endOfMonth, profileId
        )

        val category = categoryRepository.getCategoryByIdSync(categoryId) ?: return
        val percentage = totalSpent / budget.amount
        val currentMonth = monthFormat.format(Calendar.getInstance().time)

        ensureChannel()

        when {
            // Over budget — selalu tampilkan (tidak dibatasi per bulan)
            percentage >= 1.0 -> {
                showNotification(
                    notifId = NOTIFICATION_ID_BASE + categoryId.toInt(),
                    title = "🚨 Budget ${category.name} Habis!",
                    body = "Pengeluaran ${category.name} bulan ini sudah mencapai " +
                            "${CurrencyUtils.formatRupiah(totalSpent)} dan melebihi budget " +
                            "${CurrencyUtils.formatRupiah(budget.amount)}."
                )
            }
            // Hampir habis (mencapai alert threshold) — hanya 1x per bulan
            percentage >= budget.alertPercentage && budget.lastWarningMonth != currentMonth -> {
                showNotification(
                    notifId = NOTIFICATION_ID_BASE + categoryId.toInt(),
                    title = "⚠️ Budget ${category.name} Hampir Habis",
                    body = "Pengeluaran ${category.name} sudah ${(percentage * 100).toInt()}% " +
                            "(${CurrencyUtils.formatRupiah(totalSpent)} dari " +
                            "${CurrencyUtils.formatRupiah(budget.amount)}). Ayo berhemat!"
                )
                // Tandai bulan ini sudah kirim peringatan agar tidak spam
                categoryBudgetRepository.updateLastWarningMonth(categoryId, profileId, currentMonth)
            }
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Peringatan ketika pengeluaran kategori mendekati atau melebihi budget yang ditentukan."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun showNotification(notifId: Int, title: String, body: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(notifId, notification)
    }
}
