package com.trackit.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.ProfileEntity
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.repository.BudgetRepository
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.ProfileRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import com.trackit.app.util.SyncPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val allTimeBalance: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val isLoading: Boolean = true,
    val activeProfile: ProfileEntity? = null,
    val allProfiles: List<ProfileEntity> = emptyList(),
    val lastSyncTime: Long = System.currentTimeMillis(),
    val selectedMonthMillis: Long = System.currentTimeMillis(),
    val isExpenseOnlyMode: Boolean = false
)

data class TransactionWithCategory(
    val transaction: TransactionEntity,
    val category: CategoryEntity?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val profileRepository: ProfileRepository,
    private val preferencesManager: PreferencesManager,
    private val syncPreferences: SyncPreferences
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(System.currentTimeMillis())
    val selectedMonth: StateFlow<Long> = _selectedMonth.asStateFlow()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        loadExpenseOnlyMode()
    }

    private fun loadExpenseOnlyMode() {
        viewModelScope.launch {
            preferencesManager.isExpenseOnlyMode.collect { enabled ->
                _uiState.update { it.copy(isExpenseOnlyMode = enabled) }
            }
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                preferencesManager.activeProfileId,
                _selectedMonth
            ) { profileId, monthMillis -> Pair(profileId, monthMillis) }
            .flatMapLatest { (profileId, monthMillis) ->
                val cal = Calendar.getInstance().apply { timeInMillis = monthMillis }
                val startOfMonth = DateUtils.getStartOfMonth(cal)
                val endOfMonth = DateUtils.getEndOfMonth(cal)

                val monthlyStream = combine(
                    transactionRepository.getTotalSpentInMonth(startOfMonth, endOfMonth, profileId),
                    transactionRepository.getTotalIncomeInMonth(startOfMonth, endOfMonth, profileId),
                    budgetRepository.getBudgetSetting(profileId),
                    transactionRepository.getTransactionsByMonth(startOfMonth, endOfMonth, profileId),
                    categoryRepository.getAllCategories(profileId),
                    profileRepository.getAllProfiles()
                ) { params -> params }

                val allTimeStream = combine(
                    transactionRepository.getAllTimeIncome(profileId),
                    transactionRepository.getAllTimeExpense(profileId),
                    syncPreferences.lastSyncTime
                ) { income, expense, syncTime -> Triple(income, expense, syncTime) }

                combine(monthlyStream, allTimeStream) { params, allTimeData ->
                    val totalSpent   = params[0] as Double
                    val totalIncome  = params[1] as Double
                    @Suppress("UNCHECKED_CAST")
                    val budgetSetting = params[2] as? com.trackit.app.data.local.entity.BudgetSettingEntity
                    @Suppress("UNCHECKED_CAST")
                    val transactions = params[3] as List<TransactionEntity>
                    @Suppress("UNCHECKED_CAST")
                    val categories   = params[4] as List<CategoryEntity>
                    @Suppress("UNCHECKED_CAST")
                    val profiles     = params[5] as List<ProfileEntity>

                    val categoryMap = categories.associateBy { it.id }
                    val budget = budgetSetting?.monthlyBudget ?: 0.0
                    val activeProfile = profiles.find { it.id == profileId }
                    
                    val allTimeBalance = allTimeData.first - allTimeData.second
                    val storedSyncTime = allTimeData.third
                    val syncTime = if (storedSyncTime > 0) storedSyncTime else (transactions.maxOfOrNull { it.createdAt } ?: System.currentTimeMillis())

                    DashboardUiState(
                        totalSpent = totalSpent,
                        totalIncome = totalIncome,
                        allTimeBalance = allTimeBalance,
                        monthlyBudget = budget,
                        budgetRemaining = if (budget > 0) budget - totalSpent else 0.0,
                        recentTransactions = transactions.map { tx ->
                            TransactionWithCategory(
                                transaction = tx,
                                category = tx.categoryId?.let { categoryMap[it] }
                            )
                        },
                        isLoading = false,
                        activeProfile = activeProfile,
                        allProfiles = profiles,
                        lastSyncTime = syncTime,
                        selectedMonthMillis = _selectedMonth.value,
                        isExpenseOnlyMode = _uiState.value.isExpenseOnlyMode
                    )
                }
            }.collect { state ->
                _uiState.value = state.copy(isExpenseOnlyMode = _uiState.value.isExpenseOnlyMode)
            }
        }
    }

    fun navigateToPreviousMonth() {
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedMonth.value }
        cal.add(Calendar.MONTH, -1)
        _selectedMonth.value = cal.timeInMillis
    }

    fun navigateToNextMonth() {
        val cal = Calendar.getInstance().apply { timeInMillis = _selectedMonth.value }
        cal.add(Calendar.MONTH, 1)
        _selectedMonth.value = cal.timeInMillis
    }

    fun isCurrentMonth(): Boolean {
        val now = Calendar.getInstance()
        val sel = Calendar.getInstance().apply { timeInMillis = _selectedMonth.value }
        return now.get(Calendar.YEAR) == sel.get(Calendar.YEAR) &&
               now.get(Calendar.MONTH) == sel.get(Calendar.MONTH)
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            transactionRepository.deleteById(transactionId)
        }
    }

    fun switchProfile(profileId: Long) {
        viewModelScope.launch {
            preferencesManager.setActiveProfileId(profileId)
        }
    }
}
