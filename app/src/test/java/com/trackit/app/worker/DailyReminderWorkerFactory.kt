package com.trackit.app.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.repository.TransactionRepository

/**
 * Custom WorkerFactory for unit testing DailyReminderWorker without Hilt.
 * Allows injecting mock dependencies via MockK.
 */
class DailyReminderWorkerFactory(
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: PreferencesManager
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return if (workerClassName == DailyReminderWorker::class.java.name) {
            DailyReminderWorker(appContext, workerParameters, transactionRepository, preferencesManager)
        } else {
            null
        }
    }
}
