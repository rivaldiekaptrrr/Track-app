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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChartViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    init {
        loadChartData()
    }

    private fun loadChartData() {
        viewModelScope.launch {
            preferencesManager.activeProfileId.flatMapLatest { profileId ->
                // Hitung range bulan saat ini secara dinamis setiap kali dipanggil
                val startOfMonth = DateUtils.getStartOfMonth()
                val endOfMonth = DateUtils.getEndOfMonth()

                combine(
                    transactionRepository.getSpendingByCategory(startOfMonth, endOfMonth, profileId),
                    categoryRepository.getAllCategories(profileId)
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
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
