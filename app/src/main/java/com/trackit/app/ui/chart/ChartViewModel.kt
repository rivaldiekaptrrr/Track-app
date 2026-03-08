package com.trackit.app.ui.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.dao.CategorySpending
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChartUiState(
    val spendingByCategory: List<CategoryChartData> = emptyList(),
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = true
)

data class CategoryChartData(
    val category: CategoryEntity?,
    val amount: Double,
    val percentage: Float
)

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    private val startOfMonth = DateUtils.getStartOfMonth()
    private val endOfMonth = DateUtils.getEndOfMonth()

    init {
        loadChartData()
    }

    private fun loadChartData() {
        viewModelScope.launch {
            combine(
                transactionRepository.getSpendingByCategory(startOfMonth, endOfMonth),
                categoryRepository.getAllCategories()
            ) { spending, categories ->
                val categoryMap = categories.associateBy { it.id }
                val total = spending.sumOf { it.total }

                ChartUiState(
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
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
