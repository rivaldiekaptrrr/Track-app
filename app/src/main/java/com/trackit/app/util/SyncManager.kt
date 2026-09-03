package com.trackit.app.util

import android.content.Context
import android.util.Log
import com.trackit.app.data.local.dao.BudgetSettingDao
import com.trackit.app.data.local.dao.CategoryBudgetDao
import com.trackit.app.data.local.dao.CategoryDao
import com.trackit.app.data.local.dao.ProfileDao
import com.trackit.app.data.local.dao.TransactionDao
import com.trackit.app.data.local.dao.WeddingCommitteeDao
import com.trackit.app.data.local.dao.WeddingDocumentDao
import com.trackit.app.data.local.dao.WeddingEventDao
import com.trackit.app.data.local.dao.WeddingExpenseDao
import com.trackit.app.data.local.dao.WeddingGuestDao
import com.trackit.app.data.local.dao.WeddingPaymentTermDao
import com.trackit.app.data.local.dao.WeddingProfileDao
import com.trackit.app.data.local.dao.WeddingRundownItemDao
import com.trackit.app.data.local.dao.WeddingSeserahanDao
import com.trackit.app.data.local.dao.WeddingTaskDao
import com.trackit.app.data.local.dao.WeddingVendorDao
import com.trackit.app.data.local.entity.BudgetSettingEntity
import com.trackit.app.data.local.entity.CategoryBudgetEntity
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.ProfileEntity
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.local.entity.WeddingCommitteeEntity
import com.trackit.app.data.local.entity.WeddingDocumentEntity
import com.trackit.app.data.local.entity.WeddingEventEntity
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import com.trackit.app.data.local.entity.WeddingGuestEntity
import com.trackit.app.data.local.entity.WeddingPaymentTermEntity
import com.trackit.app.data.local.entity.WeddingProfileEntity
import com.trackit.app.data.local.entity.WeddingRundownItemEntity
import com.trackit.app.data.local.entity.WeddingSeserahanEntity
import com.trackit.app.data.local.entity.WeddingTaskEntity
import com.trackit.app.data.local.entity.WeddingVendorEntity
import com.trackit.app.data.repository.AuthRepository
import com.trackit.app.util.FirestoreMapper.toCategoryBudgetEntity
import com.trackit.app.util.FirestoreMapper.toCategoryEntity
import com.trackit.app.util.FirestoreMapper.toBudgetSettingEntity
import com.trackit.app.util.FirestoreMapper.toFirestoreJson
import com.trackit.app.util.FirestoreMapper.toProfileEntity
import com.trackit.app.util.FirestoreMapper.toTransactionEntity
import com.trackit.app.util.FirestoreMapper.toWeddingCommitteeEntity
import com.trackit.app.util.FirestoreMapper.toWeddingDocumentEntity
import com.trackit.app.util.FirestoreMapper.toWeddingEventEntity
import com.trackit.app.util.FirestoreMapper.toWeddingExpenseEntity
import com.trackit.app.util.FirestoreMapper.toWeddingGuestEntity
import com.trackit.app.util.FirestoreMapper.toWeddingPaymentTermEntity
import com.trackit.app.util.FirestoreMapper.toWeddingProfileEntity
import com.trackit.app.util.FirestoreMapper.toWeddingRundownItemEntity
import com.trackit.app.util.FirestoreMapper.toWeddingSeserahanEntity
import com.trackit.app.util.FirestoreMapper.toWeddingTaskEntity
import com.trackit.app.util.FirestoreMapper.toWeddingVendorEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

import com.trackit.app.data.local.TrackItDatabase
import com.trackit.app.data.local.PreferencesManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Manages synchronization between the local Room database and Firestore.
 * Uses FirestoreRestClient (HTTP/REST) instead of the gRPC-based Firebase SDK
 * to bypass network-level gRPC blocks.
 *
 * Domain note: WeddingExpense and Transaction are intentionally isolated.
 * Wedding Planner tracks project payables (tagihan), NOT cashflow (arus kas).
 * Users manage their personal cashflow separately in Expense Tracker mode.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: TrackItDatabase,
    private val preferencesManager: PreferencesManager,
    private val restClient: FirestoreRestClient,
    private val transactionDao: TransactionDao,
    private val weddingProfileDao: WeddingProfileDao,
    private val weddingExpenseDao: WeddingExpenseDao,
    private val weddingTaskDao: WeddingTaskDao,
    private val weddingVendorDao: WeddingVendorDao,
    private val weddingGuestDao: WeddingGuestDao,
    private val weddingCommitteeDao: WeddingCommitteeDao,
    private val weddingPaymentTermDao: WeddingPaymentTermDao,
    private val weddingSeserahanDao: WeddingSeserahanDao,
    private val weddingDocumentDao: WeddingDocumentDao,
    private val weddingEventDao: WeddingEventDao,
    private val weddingRundownItemDao: WeddingRundownItemDao,
    private val categoryDao: CategoryDao,
    private val profileDao: ProfileDao,
    private val budgetSettingDao: BudgetSettingDao,
    private val categoryBudgetDao: CategoryBudgetDao,
    private val authRepository: AuthRepository,
    private val syncPreferences: SyncPreferences
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    companion object {
        private const val TAG = "SyncManager"
    }

    /**
     * Clears all local Room database tables and seeds fresh default profile & categories.
     * Prevents user data leak when switching/logging out accounts.
     */
    suspend fun clearLocalData() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Clearing local database tables on logout...")
                database.clearAllTables()
                
                // Re-seed default profile and categories
                val profileId = profileDao.insert(
                    ProfileEntity(
                        name = "Pribadi",
                        iconName = "person",
                        colorHex = "#1565C0"
                    )
                )
                preferencesManager.setActiveProfileId(profileId)
                val defaultCategories = TrackItDatabase.getDefaultCategories().map { it.copy(profileId = profileId) }
                categoryDao.insertAll(defaultCategories)
                budgetSettingDao.insert(
                    BudgetSettingEntity(profileId = profileId, monthlyBudget = 0.0)
                )
                Log.d(TAG, "Local database reset & seeded with defaults successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing local data on logout: ${e.message}", e)
            }
        }
    }

    /**
     * Called when app starts or online mode is enabled.
     * Pulls ALL data from Firestore via REST and merges into local Room DB.
     * Covers Transactions and all Wedding entities.
     */
    fun startSync() {
        if (syncJob?.isActive == true) return

        syncJob = syncScope.launch {
            try {
                _isSyncing.value = true
                if (!syncPreferences.isOnlineMode.first()) return@launch
                val userId = authRepository.currentUser?.uid ?: return@launch

                Log.d(TAG, "Starting full parallel pull sync for user ${userId.take(5)}...")
                val startTime = System.currentTimeMillis()

                // Phase 1: Parent / Independent entities in parallel
                val profilesRestored = coroutineScope {
                    val deferredProfiles = async { pullProfilesAndUpdateActive(userId) }
                    val deferredCategories = async { pullCategories(userId) }
                    val deferredWeddingProfiles = async { pullWeddingProfiles(userId) }

                    val restored = deferredProfiles.await()
                    deferredCategories.await()
                    deferredWeddingProfiles.await()
                    restored
                }

                // If Firestore had no profiles (never pushed), push local defaults now
                // so the next login can restore them properly.
                if (!profilesRestored) {
                    Log.d(TAG, "No profiles in Firestore — pushing local defaults for backup.")
                    val localProfiles = profileDao.getAllProfilesSync()
                    for (profile in localProfiles) {
                        restClient.put("users/$userId/profiles/${profile.id}", profile.toFirestoreJson())
                    }
                    val localCategories = categoryDao.getAllCategoriesSync()
                    for (cat in localCategories) {
                        restClient.put("users/$userId/categories/${cat.id}", cat.toFirestoreJson())
                    }
                }

                // Phase 2: All Dependent entities in parallel
                coroutineScope {
                    awaitAll(
                        async { pullTransactions(userId) },
                        async { pullBudgetSettings(userId) },
                        async { pullCategoryBudgets(userId) },
                        async { pullWeddingExpenses(userId) },
                        async { pullWeddingTasks(userId) },
                        async { pullWeddingVendors(userId) },
                        async { pullWeddingGuests(userId) },
                        async { pullWeddingCommittee(userId) },
                        async { pullWeddingPaymentTerms(userId) },
                        async { pullWeddingSeserahan(userId) },
                        async { pullWeddingDocuments(userId) },
                        async { pullWeddingEvents(userId) },
                        async { pullWeddingRundownItems(userId) }
                    )
                }

                val elapsed = System.currentTimeMillis() - startTime
                syncPreferences.updateLastSyncTime()
                Log.d(TAG, "Full parallel pull sync complete in ${elapsed}ms.")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
    }

    // ── Private pull helpers ────────────────────────────────────────────────────

    private suspend fun pullTransactions(userId: String) {
        val docs = restClient.listDocuments("users/$userId/transactions")
        Log.d(TAG, "Fetched ${docs.size} transactions from Firestore.")

        // Load locally available categoryIds for FK safety check
        val localCategoryIds: Set<String> = try {
            categoryDao.getAllCategoriesSync().map { it.id }.toSet()
        } catch (e: Exception) {
            emptySet()
        }

        for (doc in docs) {
            val remote = doc.toTransactionEntity() ?: continue

            // Defensive: if referenced categoryId doesn't exist locally yet,
            // set it to null to avoid FOREIGN KEY constraint crash.
            // Room has onDelete = SET_NULL for this FK anyway.
            val safeRemote = if (remote.categoryId != null && !localCategoryIds.contains(remote.categoryId)) {
                Log.w(TAG, "Category ${remote.categoryId} not found locally. Setting null for tx ${remote.id.take(6)}.")
                remote.copy(categoryId = null)
            } else {
                remote
            }

            try {
                val existing = transactionDao.getByCreatedAt(safeRemote.createdAt)
                if (existing != null) {
                    transactionDao.update(safeRemote.copy(id = existing.id))
                } else {
                    transactionDao.insert(safeRemote)
                }
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                // FK violation still happened — insert without category as fallback
                Log.e(TAG, "FK constraint for tx ${safeRemote.id.take(6)}, retrying with null categoryId. Error: ${e.message}")
                try {
                    val fallback = safeRemote.copy(categoryId = null)
                    val existing = transactionDao.getByCreatedAt(fallback.createdAt)
                    if (existing != null) {
                        transactionDao.update(fallback.copy(id = existing.id))
                    } else {
                        transactionDao.insert(fallback)
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "Skipping transaction ${safeRemote.id.take(6)} after double failure: ${e2.message}")
                }
            }
        }
    }


    private suspend fun pullWeddingProfiles(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_profiles")
        Log.d(TAG, "Fetched ${docs.size} wedding profiles from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingProfileEntity() ?: continue
            // SAFE MERGE: Preserve local profileId (FK to ProfileEntity).
            // A plain INSERT REPLACE would CASCADE-delete all child entities
            // (tasks, expenses, events, etc.) and reset profileId to 0,
            // breaking the link between WeddingProfile and its parent Profile.
            val existing = weddingProfileDao.getByIdSync(remote.id)
            if (existing != null) {
                // Update remote fields but keep the local-only profileId intact
                weddingProfileDao.update(remote.copy(profileId = existing.profileId))
            } else {
                // New profile from remote — insert as-is (profileId = 0 until user links it)
                weddingProfileDao.insert(remote)
            }
        }
    }

    private suspend fun pullWeddingExpenses(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_expenses")
        Log.d(TAG, "Fetched ${docs.size} wedding expenses from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingExpenseEntity() ?: continue
            try {
                weddingExpenseDao.insert(remote)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Log.w(TAG, "Skipping wedding expense ${remote.expenseId}: parent profile not yet synced. ${e.message}")
            }
        }
    }

    private suspend fun pullWeddingTasks(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_tasks")
        Log.d(TAG, "Fetched ${docs.size} wedding tasks from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingTaskEntity() ?: continue
            try {
                weddingTaskDao.insert(remote)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Log.w(TAG, "Skipping wedding task ${remote.taskId}: FK violation. ${e.message}")
            }
        }
    }

    private suspend fun pullWeddingVendors(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_vendors")
        Log.d(TAG, "Fetched ${docs.size} wedding vendors from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingVendorEntity() ?: continue
            try {
                weddingVendorDao.insert(remote)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Log.w(TAG, "Skipping wedding vendor ${remote.vendorId}: FK violation. ${e.message}")
            }
        }
    }

    private suspend fun pullWeddingGuests(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_guests")
        Log.d(TAG, "Fetched ${docs.size} wedding guests from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingGuestEntity() ?: continue
            try {
                weddingGuestDao.insert(remote)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Log.w(TAG, "Skipping wedding guest ${remote.guestId}: FK violation. ${e.message}")
            }
        }
    }

    private suspend fun pullWeddingCommittee(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_committee")
        Log.d(TAG, "Fetched ${docs.size} committee members from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingCommitteeEntity() ?: continue
            try {
                weddingCommitteeDao.insert(remote)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Log.w(TAG, "Skipping committee member ${remote.memberId}: FK violation. ${e.message}")
            }
        }
    }

    private suspend fun pullWeddingPaymentTerms(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_payment_terms")
        Log.d(TAG, "Fetched ${docs.size} payment terms from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingPaymentTermEntity() ?: continue
            try {
                weddingPaymentTermDao.insert(remote)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Log.w(TAG, "Skipping payment term ${remote.termId}: FK violation. ${e.message}")
            }
        }
    }

    private suspend fun pullWeddingSeserahan(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_seserahan")
        Log.d(TAG, "Fetched ${docs.size} seserahan items from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingSeserahanEntity() ?: continue
            try {
                weddingSeserahanDao.insert(remote)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Log.w(TAG, "Skipping seserahan ${remote.itemId}: FK violation. ${e.message}")
            }
        }
    }

    private suspend fun pullWeddingDocuments(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_documents")
        Log.d(TAG, "Fetched ${docs.size} wedding documents from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingDocumentEntity() ?: continue
            try {
                weddingDocumentDao.insert(remote)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Log.w(TAG, "Skipping wedding document ${remote.docId}: FK violation. ${e.message}")
            }
        }
    }

    private suspend fun pullWeddingEvents(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_events")
        Log.d(TAG, "Fetched ${docs.size} wedding events from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingEventEntity() ?: continue
            try {
                weddingEventDao.insert(remote)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Log.w(TAG, "Skipping wedding event ${remote.eventId}: FK violation. ${e.message}")
            }
        }
    }

    private suspend fun pullWeddingRundownItems(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_rundown_items")
        Log.d(TAG, "Fetched ${docs.size} rundown items from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingRundownItemEntity() ?: continue
            try {
                weddingRundownItemDao.insert(remote)
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                Log.w(TAG, "Skipping rundown item ${remote.itemId}: FK violation. ${e.message}")
            }
        }
    }

    // ======================= PUSH: TRANSACTIONS =======================

    fun pushTransaction(transaction: TransactionEntity) {
        syncScope.launch {
            val isOnline = syncPreferences.isOnlineMode.first()
            val userId = authRepository.currentUser?.uid
            Log.d(TAG, "Sync Start: online=$isOnline, uid=${userId?.take(5)}")
            if (!isOnline) { Log.d(TAG, "Sync Aborted: isOnlineMode is false"); return@launch }
            if (userId == null) { Log.d(TAG, "Sync Aborted: userId is null"); return@launch }

            val docId = "${transaction.createdAt}_${transaction.profileId}"
            val result = withTimeoutOrNull(15_000L) {
                val syncResult = restClient.put("users/$userId/transactions/$docId", transaction.toFirestoreJson())
                notifySyncSuccess(syncResult)
                syncResult
            }
            when {
                result == null -> Log.e(TAG, "Sync TIMEOUT for transaction: $docId")
                result -> Log.d(TAG, "Sync SUCCESS for transaction: $docId")
                else -> Log.e(TAG, "Sync FAILED for transaction: $docId")
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val docId = "${transaction.createdAt}_${transaction.profileId}"
            val syncResult = restClient.delete("users/$userId/transactions/$docId")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: WEDDING PROFILE =======================

    fun pushWeddingProfile(profile: WeddingProfileEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/wedding_profiles/${profile.id}", profile.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteWeddingProfile(profile: WeddingProfileEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/wedding_profiles/${profile.id}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: WEDDING EXPENSES =======================

    fun pushWeddingExpense(expense: WeddingExpenseEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/wedding_expenses/${expense.expenseId}", expense.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteWeddingExpense(expense: WeddingExpenseEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/wedding_expenses/${expense.expenseId}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: WEDDING TASKS =======================

    fun pushWeddingTask(task: WeddingTaskEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/wedding_tasks/${task.taskId}", task.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteWeddingTask(task: WeddingTaskEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/wedding_tasks/${task.taskId}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: WEDDING VENDORS =======================

    fun pushWeddingVendor(vendor: WeddingVendorEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/wedding_vendors/${vendor.vendorId}", vendor.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteWeddingVendor(vendor: WeddingVendorEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/wedding_vendors/${vendor.vendorId}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: WEDDING GUESTS =======================

    fun pushWeddingGuest(guest: WeddingGuestEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/wedding_guests/${guest.guestId}", guest.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteWeddingGuest(guest: WeddingGuestEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/wedding_guests/${guest.guestId}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: WEDDING COMMITTEE =======================

    fun pushWeddingCommitteeMember(member: WeddingCommitteeEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/wedding_committee/${member.memberId}", member.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteWeddingCommitteeMember(member: WeddingCommitteeEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/wedding_committee/${member.memberId}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: WEDDING PAYMENT TERMS =======================

    fun pushWeddingPaymentTerm(term: WeddingPaymentTermEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/wedding_payment_terms/${term.termId}", term.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteWeddingPaymentTerm(term: WeddingPaymentTermEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/wedding_payment_terms/${term.termId}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: WEDDING SESERAHAN =======================

    fun pushWeddingSeserahan(item: WeddingSeserahanEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/wedding_seserahan/${item.itemId}", item.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteWeddingSeserahan(item: WeddingSeserahanEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/wedding_seserahan/${item.itemId}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: WEDDING DOCUMENTS =======================

    fun pushWeddingDocument(doc: WeddingDocumentEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/wedding_documents/${doc.docId}", doc.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteWeddingDocument(doc: WeddingDocumentEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/wedding_documents/${doc.docId}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: WEDDING EVENTS & RUNDOWN =======================

    fun pushWeddingEvent(event: WeddingEventEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/wedding_events/${event.eventId}", event.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteWeddingEvent(event: WeddingEventEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/wedding_events/${event.eventId}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun pushWeddingRundownItem(item: WeddingRundownItemEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/wedding_rundown_items/${item.itemId}", item.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteWeddingRundownItem(item: WeddingRundownItemEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/wedding_rundown_items/${item.itemId}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= INITIAL SYNC (PUSH LOCAL → REMOTE) =======================

    /**
     * Called ONLY after a NEW REGISTRATION (not on subsequent logins).
     * Pushes all local transactions to Firestore for the first time.
     *
     * On login from a second device, this must NOT be called to avoid
     * overwriting cloud data from the first device.
     *
     * Uses explicit userId to avoid race condition with AuthRepository state.
     *
     * @param userId  The Firebase UID of the authenticated user.
     * @param isNewRegistration  Set to true only when the account was just created.
     *                           Pass false for existing-account logins.
     */
    fun performInitialSync(userId: String, isNewRegistration: Boolean) {
        syncScope.launch {
            if (isNewRegistration) {
                Log.d(TAG, "Starting initial push for new user ${userId.take(5)}...")
                // New account: push all local data up to Firestore for the first time.
                val allProfiles = profileDao.getAllProfilesSync()
                for (profile in allProfiles) {
                    val syncResult = restClient.put("users/$userId/profiles/${profile.id}", profile.toFirestoreJson())
                    notifySyncSuccess(syncResult)
                    syncResult
                }
                val allCategories = categoryDao.getAllCategoriesSync()
                for (cat in allCategories) {
                    val syncResult = restClient.put("users/$userId/categories/${cat.id}", cat.toFirestoreJson())
                    notifySyncSuccess(syncResult)
                    syncResult
                }
                val allTransactions = transactionDao.getAllTransactionsAllProfiles().first()
                var successCount = 0
                for (transaction in allTransactions) {
                    val docId = "${transaction.createdAt}_${transaction.profileId}"
                    val ok = restClient.put("users/$userId/transactions/$docId", transaction.toFirestoreJson())
                    if (ok) successCount++
                }
                Log.d(TAG, "Initial push done. $successCount/${allTransactions.size} transactions pushed.")
            } else {
                // Existing account login: only push profiles & categories if they're
                // missing in Firestore (first-ever online login for this user).
                // This ensures next logout+login can restore them.
                val remoteProfiles = restClient.listDocuments("users/$userId/profiles")
                if (remoteProfiles.isEmpty()) {
                    Log.d(TAG, "Existing account has no Firestore profiles — pushing local data as initial backup.")
                    val allProfiles = profileDao.getAllProfilesSync()
                    for (profile in allProfiles) {
                        val syncResult = restClient.put("users/$userId/profiles/${profile.id}", profile.toFirestoreJson())
                        notifySyncSuccess(syncResult)
                        syncResult
                    }
                    val allCategories = categoryDao.getAllCategoriesSync()
                    for (cat in allCategories) {
                        val syncResult = restClient.put("users/$userId/categories/${cat.id}", cat.toFirestoreJson())
                        notifySyncSuccess(syncResult)
                        syncResult
                    }
                    Log.d(TAG, "Initial backup of profiles & categories done.")
                } else {
                    Log.d(TAG, "performInitialSync skipped — profiles already in Firestore.")
                }
            }
        }
    }

    // ======================= PULL: PROFILES =======================

    /**
     * Pulls profiles from Firestore. If remote profiles exist, updates activeProfileId
     * in DataStore to match the first remote profile (fixes mismatch after account switch).
     * Returns true if at least one profile was restored from Firestore.
     */
    private suspend fun pullProfilesAndUpdateActive(userId: String): Boolean {
        val docs = restClient.listDocuments("users/$userId/profiles")
        Log.d(TAG, "Fetched ${docs.size} profiles from Firestore.")
        if (docs.isEmpty()) return false

        for (doc in docs) {
            val remote = doc.toProfileEntity() ?: continue
            val existing = profileDao.getProfileById(remote.id)
            if (existing != null) {
                profileDao.update(remote)
            } else {
                profileDao.insert(remote)
            }
        }

        // After restoring profiles, set the activeProfileId to the first remote profile.
        // This fixes the case where clearLocalData() seeded a new default profile with
        // a different autoincrement ID, causing transactions to appear empty.
        val firstRemote = docs.firstOrNull()?.toProfileEntity()
        if (firstRemote != null) {
            Log.d(TAG, "Setting activeProfileId to ${firstRemote.id} from Firestore restore.")
            preferencesManager.setActiveProfileId(firstRemote.id)
        }
        return true
    }

    // ======================= PULL: CATEGORIES =======================

    private suspend fun pullCategories(userId: String) {
        val docs = restClient.listDocuments("users/$userId/categories")
        Log.d(TAG, "Fetched ${docs.size} categories from Firestore.")
        for (doc in docs) {
            val docName = doc.optString("name", "")
            val docId = docName.substringAfterLast("/")
            
            // Self-heal: Delete malformed duplicates from Firestore
            if (docId.contains("_")) {
                Log.d(TAG, "Cleaning up legacy duplicate category doc: $docId")
                val syncResult = restClient.delete("users/$userId/categories/$docId")
                notifySyncSuccess(syncResult)
                syncResult
                continue
            }

            val remote = doc.toCategoryEntity() ?: continue
            
            // Delete matching local default to preserve the remote UUID
            val existingByName = categoryDao.getByNameAndProfile(remote.name, remote.profileId)
            if (existingByName != null && existingByName.id != remote.id) {
                categoryDao.delete(existingByName)
            }
            
            // Insert or replace based on the correct UUID
            categoryDao.insert(remote)
        }
    }

    // ======================= PULL: BUDGET SETTINGS =======================

    private suspend fun pullBudgetSettings(userId: String) {
        val docs = restClient.listDocuments("users/$userId/budget_settings")
        Log.d(TAG, "Fetched ${docs.size} budget settings from Firestore.")
        for (doc in docs) {
            val remote = doc.toBudgetSettingEntity() ?: continue
            budgetSettingDao.insert(remote)
        }
    }

    // ======================= PULL: CATEGORY BUDGETS =======================

    private suspend fun pullCategoryBudgets(userId: String) {
        val docs = restClient.listDocuments("users/$userId/category_budgets")
        Log.d(TAG, "Fetched ${docs.size} category budgets from Firestore.")
        for (doc in docs) {
            val docName = doc.optString("name", "")
            val docId = docName.substringAfterLast("/")
            
            if (docId.contains("_")) {
                Log.d(TAG, "Cleaning up legacy duplicate budget doc: $docId")
                val syncResult = restClient.delete("users/$userId/category_budgets/$docId")
                notifySyncSuccess(syncResult)
                syncResult
                continue
            }

            val remote = doc.toCategoryBudgetEntity() ?: continue
            categoryBudgetDao.insert(remote)
        }
    }


    // ======================= PUSH: PROFILE =======================

    fun pushProfile(profile: ProfileEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/profiles/${profile.id}", profile.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/profiles/${profile.id}")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: CATEGORY =======================

    fun pushCategory(category: CategoryEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val docId = category.id
            val syncResult = restClient.put("users/$userId/categories/$docId", category.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val docId = category.id
            val syncResult = restClient.delete("users/$userId/categories/$docId")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: BUDGET SETTING =======================

    fun pushBudgetSetting(budgetSetting: BudgetSettingEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.put("users/$userId/budget_settings/${budgetSetting.profileId}", budgetSetting.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteBudgetSetting(profileId: Long) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val syncResult = restClient.delete("users/$userId/budget_settings/$profileId")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    // ======================= PUSH: CATEGORY BUDGET =======================

    fun pushCategoryBudget(budget: CategoryBudgetEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val docId = budget.categoryId
            val syncResult = restClient.put("users/$userId/category_budgets/$docId", budget.toFirestoreJson())
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    fun deleteCategoryBudget(budget: CategoryBudgetEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val docId = budget.categoryId
            val syncResult = restClient.delete("users/$userId/category_budgets/$docId")
            notifySyncSuccess(syncResult)
            syncResult
        }
    }

    private suspend fun notifySyncSuccess(result: Boolean?) {
        if (result == true) {
            syncPreferences.updateLastSyncTime()
        }
    }
}
