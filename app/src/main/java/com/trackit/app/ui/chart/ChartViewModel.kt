package com.trackit.app.ui.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.dao.CategorySpending
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.ui.dashboard.TransactionWithCategory
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
    val isExpenseOnlyMode: Boolean = false,
    val monthlyTrendData: List<MonthlyTrendData> = emptyList(),
    val searchResults: List<TransactionWithCategory> = emptyList()
)

data class MonthlyTrendData(
    val monthIndex: Int,
    val amount: Double
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

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMonth = MutableStateFlow(0)
    val searchMonth: StateFlow<Int> = _searchMonth.asStateFlow()

    private val _searchYear = MutableStateFlow(0)
    val searchYear: StateFlow<Int> = _searchYear.asStateFlow()

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    init {
        loadExpenseOnlyMode()
        loadChartData()
        loadTrendData()
        loadSearchResults()
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

    private fun loadTrendData() {
        viewModelScope.launch {
            combine(
                preferencesManager.activeProfileId,
                _selectedYear,
                _uiState.map { it.selectedTransactionType }.distinctUntilChanged()
            ) { profileId, year, type -> Triple(profileId, year, type) }
            .flatMapLatest { (profileId, year, type) ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfYear = cal.timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val endOfYear = cal.timeInMillis

                transactionRepository.getTransactionsByDateRangeFlow(startOfYear, endOfYear, type, profileId)
            }.collect { transactions ->
                val trend = MutableList(12) { MonthlyTrendData(it, 0.0) }
                transactions.forEach { tx ->
                    val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    val month = cal.get(Calendar.MONTH)
                    trend[month] = trend[month].copy(amount = trend[month].amount + tx.amount)
                }
                _uiState.update { it.copy(monthlyTrendData = trend.toList()) }
            }
        }
    }

    private fun loadSearchResults() {
        viewModelScope.launch {
            combine(
                preferencesManager.activeProfileId,
                _searchQuery,
                _searchMonth,
                _searchYear,
                _uiState.map { it.selectedTransactionType }.distinctUntilChanged()
            ) { profileId, query, month, year, type ->
                var startDate = 0L
                var endDate = 0L
                if (year != 0 || month != 0) {
                    val cal = Calendar.getInstance()
                    if (year != 0) cal.set(Calendar.YEAR, year)
                    if (month != 0) {
                        cal.set(Calendar.MONTH, month - 1)
                        startDate = DateUtils.getStartOfMonth(cal)
                        endDate = DateUtils.getEndOfMonth(cal)
                    } else {
                        cal.set(Calendar.MONTH, Calendar.JANUARY)
                        startDate = DateUtils.getStartOfMonth(cal)
                        cal.set(Calendar.MONTH, Calendar.DECEMBER)
                        endDate = DateUtils.getEndOfMonth(cal)
                    }
                }
                
                transactionRepository.searchTransactions(query, startDate, endDate, type, profileId)
                    .combine(categoryRepository.getAllCategories(profileId)) { txs, categories ->
                        val catMap = categories.associateBy { it.id }
                        txs.map { tx ->
                            TransactionWithCategory(tx, tx.categoryId?.let { catMap[it] })
                        }
                    }
            }.flatMapLatest { it }.collect { results ->
                _uiState.update { it.copy(searchResults = results) }
            }
        }
    }

    fun navigateToPreviousYear() {
        _selectedYear.value -= 1
    }

    fun navigateToNextYear() {
        _selectedYear.value += 1
    }

    fun isCurrentYear(): Boolean {
        return _selectedYear.value == Calendar.getInstance().get(Calendar.YEAR)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchFilter(month: Int, year: Int) {
        _searchMonth.value = month
        _searchYear.value = year
    }
}
