package com.trackit.app.util

import android.content.Context
import com.trackit.app.data.local.dao.TransactionDao
import com.trackit.app.data.local.dao.WeddingExpenseDao
import com.trackit.app.data.local.dao.WeddingTaskDao
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import com.trackit.app.data.local.entity.WeddingTaskEntity
import com.trackit.app.data.repository.AuthRepository
import com.trackit.app.util.FirestoreMapper.toFirestoreJson
import com.trackit.app.util.FirestoreMapper.toTransactionEntity
import com.trackit.app.util.FirestoreMapper.toWeddingExpenseEntity
import com.trackit.app.util.FirestoreMapper.toWeddingTaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * Manages synchronization between the local Room database and Firestore.
 * Uses FirestoreRestClient (HTTP/REST) instead of the gRPC-based Firebase SDK
 * to bypass network-level gRPC blocks.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val restClient: FirestoreRestClient,
    private val transactionDao: TransactionDao,
    private val weddingExpenseDao: WeddingExpenseDao,
    private val weddingTaskDao: WeddingTaskDao,
    private val authRepository: AuthRepository,
    private val syncPreferences: SyncPreferences
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    /**
     * Called when app starts or online mode is enabled.
     * Pulls all data from Firestore via REST and merges into local Room DB.
     */
    fun startSync() {
        if (syncJob?.isActive == true) return

        syncJob = syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch

            Log.d("SyncManager", "Starting pull sync for user ${userId.take(5)}...")

            // Pull Transactions
            val remoteTxDocs = restClient.listDocuments("users/$userId/transactions")
            Log.d("SyncManager", "Fetched ${remoteTxDocs.size} transactions from Firestore.")
            for (doc in remoteTxDocs) {
                val remote = doc.toTransactionEntity() ?: continue
                val existing = transactionDao.getByCreatedAt(remote.createdAt)
                if (existing != null) {
                    transactionDao.update(remote.copy(id = existing.id))
                } else {
                    transactionDao.insert(remote.copy(id = 0))
                }
            }

            // Pull Wedding Expenses
            val remoteExpenseDocs = restClient.listDocuments("users/$userId/wedding_expenses")
            Log.d("SyncManager", "Fetched ${remoteExpenseDocs.size} wedding expenses from Firestore.")
            for (doc in remoteExpenseDocs) {
                val remote = doc.toWeddingExpenseEntity() ?: continue
                weddingExpenseDao.insert(remote)
            }

            // Pull Wedding Tasks
            val remoteTaskDocs = restClient.listDocuments("users/$userId/wedding_tasks")
            Log.d("SyncManager", "Fetched ${remoteTaskDocs.size} wedding tasks from Firestore.")
            for (doc in remoteTaskDocs) {
                val remote = doc.toWeddingTaskEntity() ?: continue
                weddingTaskDao.insert(remote)
            }

            Log.d("SyncManager", "Pull sync complete.")
        }
    }

    fun stopSync() {
        syncJob?.cancel()
    }

    // ======================= TRANSACTIONS =======================

    fun pushTransaction(transaction: TransactionEntity) {
        syncScope.launch {
            val isOnline = syncPreferences.isOnlineMode.first()
            val userId = authRepository.currentUser?.uid

            Log.d("SyncManager", "Sync Start: online=$isOnline, uid=${userId?.take(5)}")

            if (!isOnline) { Log.d("SyncManager", "Sync Aborted: isOnlineMode is false"); return@launch }
            if (userId == null) { Log.d("SyncManager", "Sync Aborted: userId is null"); return@launch }

            val docId = "${transaction.createdAt}_${transaction.profileId}"
            Log.d("SyncManager", "Attempting to write doc: $docId")

            val result = withTimeoutOrNull(15_000L) {
                restClient.put("users/$userId/transactions/$docId", transaction.toFirestoreJson())
            }

            when {
                result == null -> Log.e("SyncManager", "Sync TIMEOUT: REST tidak merespon dalam 15 detik.")
                result -> Log.d("SyncManager", "Sync SUCCESS (REST) for doc: $docId")
                else -> Log.e("SyncManager", "Sync FAILED (REST) for doc: $docId")
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val docId = "${transaction.createdAt}_${transaction.profileId}"
            restClient.delete("users/$userId/transactions/$docId")
        }
    }

    // ======================= WEDDING EXPENSES =======================

    fun pushWeddingExpense(expense: WeddingExpenseEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/wedding_expenses/${expense.expenseId}", expense.toFirestoreJson())
        }
    }

    fun deleteWeddingExpense(expense: WeddingExpenseEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/wedding_expenses/${expense.expenseId}")
        }
    }

    // ======================= WEDDING TASKS =======================

    fun pushWeddingTask(task: WeddingTaskEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/wedding_tasks/${task.taskId}", task.toFirestoreJson())
        }
    }

    fun deleteWeddingTask(task: WeddingTaskEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/wedding_tasks/${task.taskId}")
        }
    }

    // ======================= INITIAL SYNC (PUSH LOCAL → REMOTE) =======================

    /**
     * Called after login. Pushes all local data up to Firestore.
     * Uses explicit userId to avoid race condition with AuthRepository state.
     */
    fun performInitialSync(userId: String) {
        syncScope.launch {
            Log.d("SyncManager", "Starting initial push for user ${userId.take(5)}...")
            val allTransactions = transactionDao.getAllTransactionsAllProfiles().first()
            var successCount = 0
            for (transaction in allTransactions) {
                val docId = "${transaction.createdAt}_${transaction.profileId}"
                val ok = restClient.put("users/$userId/transactions/$docId", transaction.toFirestoreJson())
                if (ok) successCount++
            }
            Log.d("SyncManager", "Initial push done. $successCount/${allTransactions.size} transactions pushed.")
        }
    }
}
