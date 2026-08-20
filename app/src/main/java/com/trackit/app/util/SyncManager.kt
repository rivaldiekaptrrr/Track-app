package com.trackit.app.util

import com.google.firebase.firestore.FirebaseFirestore
import com.trackit.app.data.local.dao.TransactionDao
import com.trackit.app.data.local.dao.WeddingExpenseDao
import com.trackit.app.data.local.dao.WeddingTaskDao
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import com.trackit.app.data.local.entity.WeddingTaskEntity
import com.trackit.app.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val transactionDao: TransactionDao,
    private val weddingExpenseDao: WeddingExpenseDao,
    private val weddingTaskDao: WeddingTaskDao,
    private val authRepository: AuthRepository,
    private val syncPreferences: SyncPreferences
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null
    
    // Call this from MainActivity when app starts or when toggle is flipped
    fun startSync() {
        if (syncJob?.isActive == true) return
        
        syncJob = syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            
            // Listen to Transactions
            firestore.collection("users").document(userId)
                .collection("transactions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    
                    syncScope.launch {
                        for (doc in snapshot.documentChanges) {
                            val transaction = try {
                                doc.document.toObject(TransactionEntity::class.java)
                            } catch (e: Exception) {
                                null
                            } ?: continue
                            
                            when (doc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    // Check if we already have it by createdAt
                                    val existing = transactionDao.getByCreatedAt(transaction.createdAt)
                                    if (existing != null) {
                                        // Update existing local record to preserve local ID
                                        transactionDao.update(transaction.copy(id = existing.id))
                                    } else {
                                        // Insert as new (local ID will be auto-generated because it's 0 in the default constructor but Firestore might have saved the old local ID. 
                                        // Wait, Firestore saves the 'id' field too. If 'id' conflicts, we might overwrite.
                                        // Let's force id = 0 so Room auto-generates a safe local ID
                                        transactionDao.insert(transaction.copy(id = 0))
                                    }
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    val existing = transactionDao.getByCreatedAt(transaction.createdAt)
                                    if (existing != null) {
                                        transactionDao.deleteById(existing.id)
                                    }
                                }
                            }
                        }
                    }
                }
                
            // Listen to Wedding Expenses
            firestore.collection("users").document(userId)
                .collection("wedding_expenses")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    syncScope.launch {
                        for (doc in snapshot.documentChanges) {
                            val expense = try { doc.document.toObject(WeddingExpenseEntity::class.java) } catch (e: Exception) { null } ?: continue
                            when (doc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> weddingExpenseDao.insert(expense)
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> weddingExpenseDao.delete(expense)
                            }
                        }
                    }
                }

            // Listen to Wedding Tasks
            firestore.collection("users").document(userId)
                .collection("wedding_tasks")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    syncScope.launch {
                        for (doc in snapshot.documentChanges) {
                            val task = try { doc.document.toObject(WeddingTaskEntity::class.java) } catch (e: Exception) { null } ?: continue
                            when (doc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> weddingTaskDao.insert(task)
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> weddingTaskDao.delete(task)
                            }
                        }
                    }
                }
        }
    }
    
    fun stopSync() {
        syncJob?.cancel()
    }
    
    suspend fun pushTransaction(transaction: TransactionEntity) {
        if (!syncPreferences.isOnlineMode.first()) return
        val userId = authRepository.currentUser?.uid ?: return
        
        // Use createdAt as unique firestore document ID to avoid local SQLite ID collisions between phones
        val docId = "${transaction.createdAt}_${transaction.profileId}"
        firestore.collection("users").document(userId)
            .collection("transactions").document(docId)
            .set(transaction)
            .await()
    }
    
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        if (!syncPreferences.isOnlineMode.first()) return
        val userId = authRepository.currentUser?.uid ?: return
        
        val docId = "${transaction.createdAt}_${transaction.profileId}"
        firestore.collection("users").document(userId)
            .collection("transactions").document(docId)
            .delete()
            .await()
    }

    suspend fun pushWeddingExpense(expense: WeddingExpenseEntity) {
        if (!syncPreferences.isOnlineMode.first()) return
        val userId = authRepository.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("wedding_expenses").document(expense.expenseId).set(expense)
    }

    suspend fun deleteWeddingExpense(expense: WeddingExpenseEntity) {
        if (!syncPreferences.isOnlineMode.first()) return
        val userId = authRepository.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("wedding_expenses").document(expense.expenseId).delete()
    }

    suspend fun pushWeddingTask(task: WeddingTaskEntity) {
        if (!syncPreferences.isOnlineMode.first()) return
        val userId = authRepository.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("wedding_tasks").document(task.taskId).set(task)
    }

    suspend fun deleteWeddingTask(task: WeddingTaskEntity) {
        if (!syncPreferences.isOnlineMode.first()) return
        val userId = authRepository.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("wedding_tasks").document(task.taskId).delete()
    }

    suspend fun performInitialSync() {

        if (!syncPreferences.isOnlineMode.first()) return
        val userId = authRepository.currentUser?.uid ?: return
        
        // Push all local transactions to cloud
        val localTransactions = transactionDao.getAllTransactions(1).first() // Profile 1 default
        for (transaction in localTransactions) {
            val docId = "${transaction.createdAt}_${transaction.profileId}"
            firestore.collection("users").document(userId)
                .collection("transactions").document(docId)
                .set(transaction)
        }
    }
}
