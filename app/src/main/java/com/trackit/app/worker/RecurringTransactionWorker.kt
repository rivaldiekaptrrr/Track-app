package com.trackit.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "recurring_transaction_worker"
    }

    override suspend fun doWork(): Result {
        return try {
            val recurringTransactions = transactionRepository.getRecurringTransactions()
            val today = Calendar.getInstance()
            val todayMillis = DateUtils.todayMillis()

            for (template in recurringTransactions) {
                val shouldCreate = when (template.recurringType) {
                    "DAILY" -> true
                    "WEEKLY" -> today.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
                    "MONTHLY" -> {
                        val dayOfMonth = template.recurringDayOfMonth ?: 1
                        today.get(Calendar.DAY_OF_MONTH) == dayOfMonth
                    }
                    else -> false
                }

                if (shouldCreate) {
                    // Create a new non-recurring transaction based on the template
                    val newTransaction = TransactionEntity(
                        amount = template.amount,
                        description = template.description,
                        categoryId = template.categoryId,
                        date = todayMillis,
                        isRecurring = false, // The created instance is not recurring itself
                        recurringType = null
                    )
                    transactionRepository.insert(newTransaction)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
