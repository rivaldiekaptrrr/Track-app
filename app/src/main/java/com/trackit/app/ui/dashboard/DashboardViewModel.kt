package com.trackit.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.repository.BudgetRepository
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val isLoading: Boolean = true
)

data class TransactionWithCategory(
    val transaction: TransactionEntity,
    val category: CategoryEntity?
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository
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
            combine(
                transactionRepository.getTotalSpentInMonth(startOfMonth, endOfMonth),
                transactionRepository.getTotalIncomeInMonth(startOfMonth, endOfMonth),
                budgetRepository.getBudgetSetting(),
                transactionRepository.getRecentTransactions(10),
                categoryRepository.getAllCategories()
            ) { totalSpent, totalIncome, budgetSetting, transactions, categories ->
                val categoryMap = categories.associateBy { it.id }
                val budget = budgetSetting?.monthlyBudget ?: 0.0

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
                    isLoading = false
                )
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
}
