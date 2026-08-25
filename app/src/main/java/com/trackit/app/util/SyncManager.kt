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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

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

    companion object {
        private const val TAG = "SyncManager"
    }

    /**
     * Called when app starts or online mode is enabled.
     * Pulls ALL data from Firestore via REST and merges into local Room DB.
     * Covers Transactions and all Wedding entities.
     */
    fun startSync() {
        if (syncJob?.isActive == true) return

        syncJob = syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch

            Log.d(TAG, "Starting full pull sync for user ${userId.take(5)}...")

            pullTransactions(userId)
            pullWeddingProfiles(userId)
            pullWeddingExpenses(userId)
            pullWeddingTasks(userId)
            pullWeddingVendors(userId)
            pullWeddingGuests(userId)
            pullWeddingCommittee(userId)
            pullWeddingPaymentTerms(userId)
            pullWeddingSeserahan(userId)
            pullWeddingDocuments(userId)
            pullWeddingEvents(userId)
            pullWeddingRundownItems(userId)
            pullProfiles(userId)
            pullCategories(userId)
            pullBudgetSettings(userId)
            pullCategoryBudgets(userId)

            Log.d(TAG, "Full pull sync complete.")
        }
    }

    fun stopSync() {
        syncJob?.cancel()
    }

    // ── Private pull helpers ────────────────────────────────────────────────────

    private suspend fun pullTransactions(userId: String) {
        val docs = restClient.listDocuments("users/$userId/transactions")
        Log.d(TAG, "Fetched ${docs.size} transactions from Firestore.")
        for (doc in docs) {
            val remote = doc.toTransactionEntity() ?: continue
            val existing = transactionDao.getByCreatedAt(remote.createdAt)
            if (existing != null) {
                transactionDao.update(remote.copy(id = existing.id))
            } else {
                transactionDao.insert(remote.copy(id = 0))
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
            weddingExpenseDao.insert(remote)
        }
    }

    private suspend fun pullWeddingTasks(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_tasks")
        Log.d(TAG, "Fetched ${docs.size} wedding tasks from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingTaskEntity() ?: continue
            weddingTaskDao.insert(remote)
        }
    }

    private suspend fun pullWeddingVendors(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_vendors")
        Log.d(TAG, "Fetched ${docs.size} wedding vendors from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingVendorEntity() ?: continue
            weddingVendorDao.insert(remote)
        }
    }

    private suspend fun pullWeddingGuests(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_guests")
        Log.d(TAG, "Fetched ${docs.size} wedding guests from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingGuestEntity() ?: continue
            weddingGuestDao.insert(remote)
        }
    }

    private suspend fun pullWeddingCommittee(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_committee")
        Log.d(TAG, "Fetched ${docs.size} committee members from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingCommitteeEntity() ?: continue
            weddingCommitteeDao.insert(remote)
        }
    }

    private suspend fun pullWeddingPaymentTerms(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_payment_terms")
        Log.d(TAG, "Fetched ${docs.size} payment terms from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingPaymentTermEntity() ?: continue
            weddingPaymentTermDao.insert(remote)
        }
    }

    private suspend fun pullWeddingSeserahan(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_seserahan")
        Log.d(TAG, "Fetched ${docs.size} seserahan items from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingSeserahanEntity() ?: continue
            weddingSeserahanDao.insert(remote)
        }
    }

    private suspend fun pullWeddingDocuments(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_documents")
        Log.d(TAG, "Fetched ${docs.size} wedding documents from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingDocumentEntity() ?: continue
            weddingDocumentDao.insert(remote)
        }
    }

    private suspend fun pullWeddingEvents(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_events")
        Log.d(TAG, "Fetched ${docs.size} wedding events from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingEventEntity() ?: continue
            weddingEventDao.insert(remote)
        }
    }

    private suspend fun pullWeddingRundownItems(userId: String) {
        val docs = restClient.listDocuments("users/$userId/wedding_rundown_items")
        Log.d(TAG, "Fetched ${docs.size} rundown items from Firestore.")
        for (doc in docs) {
            val remote = doc.toWeddingRundownItemEntity() ?: continue
            weddingRundownItemDao.insert(remote)
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
                restClient.put("users/$userId/transactions/$docId", transaction.toFirestoreJson())
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
            restClient.delete("users/$userId/transactions/$docId")
        }
    }

    // ======================= PUSH: WEDDING PROFILE =======================

    fun pushWeddingProfile(profile: WeddingProfileEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/wedding_profiles/${profile.id}", profile.toFirestoreJson())
        }
    }

    fun deleteWeddingProfile(profile: WeddingProfileEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/wedding_profiles/${profile.id}")
        }
    }

    // ======================= PUSH: WEDDING EXPENSES =======================

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

    // ======================= PUSH: WEDDING TASKS =======================

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

    // ======================= PUSH: WEDDING VENDORS =======================

    fun pushWeddingVendor(vendor: WeddingVendorEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/wedding_vendors/${vendor.vendorId}", vendor.toFirestoreJson())
        }
    }

    fun deleteWeddingVendor(vendor: WeddingVendorEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/wedding_vendors/${vendor.vendorId}")
        }
    }

    // ======================= PUSH: WEDDING GUESTS =======================

    fun pushWeddingGuest(guest: WeddingGuestEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/wedding_guests/${guest.guestId}", guest.toFirestoreJson())
        }
    }

    fun deleteWeddingGuest(guest: WeddingGuestEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/wedding_guests/${guest.guestId}")
        }
    }

    // ======================= PUSH: WEDDING COMMITTEE =======================

    fun pushWeddingCommitteeMember(member: WeddingCommitteeEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/wedding_committee/${member.memberId}", member.toFirestoreJson())
        }
    }

    fun deleteWeddingCommitteeMember(member: WeddingCommitteeEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/wedding_committee/${member.memberId}")
        }
    }

    // ======================= PUSH: WEDDING PAYMENT TERMS =======================

    fun pushWeddingPaymentTerm(term: WeddingPaymentTermEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/wedding_payment_terms/${term.termId}", term.toFirestoreJson())
        }
    }

    fun deleteWeddingPaymentTerm(term: WeddingPaymentTermEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/wedding_payment_terms/${term.termId}")
        }
    }

    // ======================= PUSH: WEDDING SESERAHAN =======================

    fun pushWeddingSeserahan(item: WeddingSeserahanEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/wedding_seserahan/${item.itemId}", item.toFirestoreJson())
        }
    }

    fun deleteWeddingSeserahan(item: WeddingSeserahanEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/wedding_seserahan/${item.itemId}")
        }
    }

    // ======================= PUSH: WEDDING DOCUMENTS =======================

    fun pushWeddingDocument(doc: WeddingDocumentEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/wedding_documents/${doc.docId}", doc.toFirestoreJson())
        }
    }

    fun deleteWeddingDocument(doc: WeddingDocumentEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/wedding_documents/${doc.docId}")
        }
    }

    // ======================= PUSH: WEDDING EVENTS & RUNDOWN =======================

    fun pushWeddingEvent(event: WeddingEventEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/wedding_events/${event.eventId}", event.toFirestoreJson())
        }
    }

    fun deleteWeddingEvent(event: WeddingEventEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/wedding_events/${event.eventId}")
        }
    }

    fun pushWeddingRundownItem(item: WeddingRundownItemEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/wedding_rundown_items/${item.itemId}", item.toFirestoreJson())
        }
    }

    fun deleteWeddingRundownItem(item: WeddingRundownItemEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/wedding_rundown_items/${item.itemId}")
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
        if (!isNewRegistration) {
            Log.d(TAG, "performInitialSync skipped — existing account login, not a new registration.")
            return
        }
        syncScope.launch {
            Log.d(TAG, "Starting initial push for new user ${userId.take(5)}...")
            val allTransactions = transactionDao.getAllTransactionsAllProfiles().first()
            var successCount = 0
            for (transaction in allTransactions) {
                val docId = "${transaction.createdAt}_${transaction.profileId}"
                val ok = restClient.put("users/$userId/transactions/$docId", transaction.toFirestoreJson())
                if (ok) successCount++
            }
            Log.d(TAG, "Initial push done. $successCount/${allTransactions.size} transactions pushed.")
        }
    }

    // ======================= PULL: PROFILES =======================

    private suspend fun pullProfiles(userId: String) {
        val docs = restClient.listDocuments("users/$userId/profiles")
        Log.d(TAG, "Fetched ${docs.size} profiles from Firestore.")
        for (doc in docs) {
            val remote = doc.toProfileEntity() ?: continue
            val existing = profileDao.getProfileById(remote.id)
            if (existing != null) {
                profileDao.update(remote)
            } else {
                profileDao.insert(remote)
            }
        }
    }

    // ======================= PULL: CATEGORIES =======================

    private suspend fun pullCategories(userId: String) {
        val docs = restClient.listDocuments("users/$userId/categories")
        Log.d(TAG, "Fetched ${docs.size} categories from Firestore.")
        for (doc in docs) {
            val remote = doc.toCategoryEntity() ?: continue
            // Use name+profileId to identify existing category (avoid Long ID collision)
            val existing = categoryDao.getByNameAndProfile(remote.name, remote.profileId)
            if (existing != null) {
                categoryDao.update(remote.copy(id = existing.id))
            } else {
                categoryDao.insert(remote.copy(id = 0))
            }
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
            val remote = doc.toCategoryBudgetEntity() ?: continue
            categoryBudgetDao.insert(remote)
        }
    }

    // ======================= PUSH: PROFILE =======================

    fun pushProfile(profile: ProfileEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/profiles/${profile.id}", profile.toFirestoreJson())
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/profiles/${profile.id}")
        }
    }

    // ======================= PUSH: CATEGORY =======================

    fun pushCategory(category: CategoryEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val docId = "${category.profileId}_${category.id}"
            restClient.put("users/$userId/categories/$docId", category.toFirestoreJson())
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val docId = "${category.profileId}_${category.id}"
            restClient.delete("users/$userId/categories/$docId")
        }
    }

    // ======================= PUSH: BUDGET SETTING =======================

    fun pushBudgetSetting(budgetSetting: BudgetSettingEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.put("users/$userId/budget_settings/${budgetSetting.profileId}", budgetSetting.toFirestoreJson())
        }
    }

    fun deleteBudgetSetting(profileId: Long) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            restClient.delete("users/$userId/budget_settings/$profileId")
        }
    }

    // ======================= PUSH: CATEGORY BUDGET =======================

    fun pushCategoryBudget(budget: CategoryBudgetEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val docId = "${budget.profileId}_${budget.categoryId}"
            restClient.put("users/$userId/category_budgets/$docId", budget.toFirestoreJson())
        }
    }

    fun deleteCategoryBudget(budget: CategoryBudgetEntity) {
        syncScope.launch {
            if (!syncPreferences.isOnlineMode.first()) return@launch
            val userId = authRepository.currentUser?.uid ?: return@launch
            val docId = "${budget.profileId}_${budget.categoryId}"
            restClient.delete("users/$userId/category_budgets/$docId")
        }
    }
}
