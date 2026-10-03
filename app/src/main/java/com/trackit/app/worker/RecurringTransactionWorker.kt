package com.trackit.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "recurring_transaction_worker"
    }

    override suspend fun doWork(): Result {
        return try {
            val recurringTransactions = transactionRepository.getAllRecurringTransactionsAllProfiles()
            val today = Calendar.getInstance()
            val todayMillis = DateUtils.todayMillis()

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 86_400_000L - 1L

            // Group by root parent ID so only 1 generator runs per chain
            val templatesByRoot = recurringTransactions.groupBy { it.parentRecurringId ?: it.id }

            for ((rootId, chain) in templatesByRoot) {
                // Find root template or fallback to the earliest item
                val template = chain.find { it.id == rootId } ?: chain.minByOrNull { it.date } ?: continue
                if (!template.isRecurring) continue

                // If template was created today, don't generate duplicate for today
                if (template.date in startOfDay..endOfDay) continue

                // Check idempotency: already generated for today?
                val existingToday = transactionRepository.findGeneratedTransactionForDate(rootId, startOfDay, endOfDay)
                if (existingToday != null) continue

                val shouldCreate = when (template.recurringType) {
                    "DAILY" -> true
                    "WEEKLY" -> {
                        val templateCal = Calendar.getInstance().apply { timeInMillis = template.date }
                        today.get(Calendar.DAY_OF_WEEK) == templateCal.get(Calendar.DAY_OF_WEEK)
                    }
                    "MONTHLY" -> {
                        val templateCal = Calendar.getInstance().apply { timeInMillis = template.date }
                        val dayOfMonth = template.recurringDayOfMonth ?: templateCal.get(Calendar.DAY_OF_MONTH)
                        val maxDay = today.getActualMaximum(Calendar.DAY_OF_MONTH)
                        val targetDay = dayOfMonth.coerceAtMost(maxDay)
                        today.get(Calendar.DAY_OF_MONTH) == targetDay
                    }
                    else -> false
                }

                if (shouldCreate) {
                    val newTransaction = TransactionEntity(
                        amount = template.amount,
                        description = template.description,
                        categoryId = template.categoryId,
                        date = todayMillis,
                        isRecurring = true, // Remains marked as recurring child so UI toggle stays ON
                        recurringType = template.recurringType,
                        recurringDayOfMonth = template.recurringDayOfMonth,
                        parentRecurringId = rootId,
                        lastGeneratedDate = todayMillis,
                        type = template.type, // Preserves EXPENSE or INCOME
                        profileId = template.profileId
                    )
                    transactionRepository.insert(newTransaction)
                    transactionRepository.update(template.copy(lastGeneratedDate = todayMillis))
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
