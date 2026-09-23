package com.trackit.app.ui.chart.category_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CategoryItemBreakdown(
    val transaction: TransactionEntity,
    val amount: Double,
    val percentage: Float
)

data class CategoryDetailUiState(
    val category: CategoryEntity? = null,
    val categoryName: String = "Lainnya",
    val categoryIcon: String = "more_horiz",
    val categoryColor: String = "#9E9E9E",
    val monthMillis: Long = System.currentTimeMillis(),
    val transactionType: String = "EXPENSE",
    val totalCategoryAmount: Double = 0.0,
    val totalOverallMonthAmount: Double = 0.0,
    val categoryMonthPercentage: Float = 0f,
    val items: List<CategoryItemBreakdown> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val rawCategoryId: String? = savedStateHandle.get<String>("categoryId")?.takeIf {
        it != "uncategorized" && it != "null" && it.isNotBlank()
    }

    val monthMillis: Long = savedStateHandle.get<Long>("month")?.takeIf { it > 0L }
        ?: savedStateHandle.get<String>("month")?.toLongOrNull()?.takeIf { it > 0L }
        ?: System.currentTimeMillis()

    val transactionType: String = savedStateHandle.get<String>("type") ?: "EXPENSE"

    private val _uiState = MutableStateFlow(
        CategoryDetailUiState(
            monthMillis = monthMillis,
            transactionType = transactionType
        )
    )
    val uiState: StateFlow<CategoryDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetailData()
    }

    private fun loadDetailData() {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply { timeInMillis = monthMillis }
            val startOfMonth = DateUtils.getStartOfMonth(cal)
            val endOfMonth = DateUtils.getEndOfMonth(cal)

            preferencesManager.activeProfileId.flatMapLatest { profileId ->
                val categoryFlow: Flow<CategoryEntity?> = if (rawCategoryId != null) {
                    categoryRepository.getCategoryByIdFlow(rawCategoryId)
                } else {
                    flowOf<CategoryEntity?>(null)
                }

                val transactionsFlow = transactionRepository.getTransactionsByCategoryAndMonth(
                    categoryId = rawCategoryId,
                    startOfMonth = startOfMonth,
                    endOfMonth = endOfMonth,
                    type = transactionType,
                    profileId = profileId
                )

                val spendingByCategoryFlow = transactionRepository.getSpendingByCategoryAndType(
                    startOfMonth = startOfMonth,
                    endOfMonth = endOfMonth,
                    type = transactionType,
                    profileId = profileId
                )

                combine(categoryFlow, transactionsFlow, spendingByCategoryFlow) { category, txList, allSpending ->
                    val totalCategory = txList.sumOf { it.amount }
                    val totalOverall = allSpending.sumOf { it.total }
                    val monthPct = if (totalOverall > 0) (totalCategory / totalOverall * 100).toFloat() else 0f

                    val items = txList.map { tx ->
                        CategoryItemBreakdown(
                            transaction = tx,
                            amount = tx.amount,
                            percentage = if (totalCategory > 0) (tx.amount / totalCategory * 100).toFloat() else 0f
                        )
                    }

                    CategoryDetailUiState(
                        category = category,
                        categoryName = category?.name ?: "Lainnya",
                        categoryIcon = category?.iconName ?: "more_horiz",
                        categoryColor = category?.colorHex ?: "#9E9E9E",
                        monthMillis = monthMillis,
                        transactionType = transactionType,
                        totalCategoryAmount = totalCategory,
                        totalOverallMonthAmount = totalOverall,
                        categoryMonthPercentage = monthPct,
                        items = items,
                        isLoading = false
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
