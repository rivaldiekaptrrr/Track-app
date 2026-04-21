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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val isLoading: Boolean = true,
    val activeProfile: ProfileEntity? = null,
    val allProfiles: List<ProfileEntity> = emptyList()
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
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val startOfMonth = DateUtils.getStartOfMonth()
    private val endOfMonth = DateUtils.getEndOfMonth()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            // Observe active profile ID from DataStore
            preferencesManager.activeProfileId.flatMapLatest { profileId ->
                // Concurrently observe all reactive streams for this profile
                combine(
                    transactionRepository.getTotalSpentInMonth(startOfMonth, endOfMonth, profileId),
                    transactionRepository.getTotalIncomeInMonth(startOfMonth, endOfMonth, profileId),
                    budgetRepository.getBudgetSetting(profileId),
                    transactionRepository.getRecentTransactions(profileId, 10),
                    categoryRepository.getAllCategories(profileId),
                    profileRepository.getAllProfiles()
                ) { params ->
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

                    DashboardUiState(
                        totalSpent = totalSpent,
                        totalIncome = totalIncome,
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
                        allProfiles = profiles
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteTransaction(transactionId: Long) {
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
