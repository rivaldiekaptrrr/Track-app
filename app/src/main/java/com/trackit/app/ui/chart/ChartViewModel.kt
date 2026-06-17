package com.trackit.app.ui.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.dao.CategorySpending
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ChartUiState(
    val spendingByCategory: List<CategoryChartData> = emptyList(),
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = true,
    val selectedTransactionType: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val isExpenseOnlyMode: Boolean = false
)

data class CategoryChartData(
    val category: CategoryEntity?,
    val amount: Double,
    val percentage: Float
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChartViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(System.currentTimeMillis())
    val selectedMonth: StateFlow<Long> = _selectedMonth.asStateFlow()

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    init {
        loadExpenseOnlyMode()
        loadChartData()
    }

    private fun loadExpenseOnlyMode() {
        viewModelScope.launch {
            preferencesManager.isExpenseOnlyMode.collect { enabled ->
                _uiState.update { it.copy(isExpenseOnlyMode = enabled) }
                // jika mode pengeluaran saja aktif, paksa ke EXPENSE
                if (enabled) {
                    _uiState.update { it.copy(selectedTransactionType = "EXPENSE") }
                }
            }
        }
    }

    private fun loadChartData() {
        viewModelScope.launch {
            combine(
                preferencesManager.activeProfileId,
                _selectedMonth,
                _uiState.map { it.selectedTransactionType }.distinctUntilChanged()
            ) { profileId, monthMillis, type -> Triple(profileId, monthMillis, type) }
            .flatMapLatest { (profileId, monthMillis, type) ->
                val cal = Calendar.getInstance().apply { timeInMillis = monthMillis }
                val startOfMonth = DateUtils.getStartOfMonth(cal)
                val endOfMonth = DateUtils.getEndOfMonth(cal)

                combine(
                    transactionRepository.getSpendingByCategoryAndType(startOfMonth, endOfMonth, type, profileId),
                    categoryRepository.getAllCategories(profileId)
                ) { spending, categories ->
                    val categoryMap = categories.associateBy { it.id }
                    val total = spending.sumOf { it.total }

                    _uiState.value.copy(
                        spendingByCategory = spending.map { cs ->
                            CategoryChartData(
                                category = cs.categoryId?.let { categoryMap[it] },
                                amount = cs.total,
                                percentage = if (total > 0) (cs.total / total * 100).toFloat() else 0f
                            )
                        }.sortedByDescending { it.amount },
                        totalSpent = total,
                        isLoading = false
                    )
                }
            }.collect { state ->
                _uiState.value = state
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

    fun setTransactionType(type: String) {
        _uiState.update { it.copy(selectedTransactionType = type) }
    }
}
