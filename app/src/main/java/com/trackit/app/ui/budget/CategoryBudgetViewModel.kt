package com.trackit.app.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.entity.CategoryBudgetEntity
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.repository.CategoryBudgetRepository
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryBudgetItem(
    val category: CategoryEntity,
    val budgetAmount: Double,       // 0.0 = belum diset
    val alertPercentage: Float,     // default 0.9
    val spent: Double,              // total pengeluaran bulan ini
    val isEditing: Boolean = false,
    val inputAmount: String = "",
    val inputAlertPct: Float = 0.9f
)

data class CategoryBudgetUiState(
    val items: List<CategoryBudgetItem> = emptyList(),
    val isLoading: Boolean = true,
    val savedMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryBudgetViewModel @Inject constructor(
    private val categoryBudgetRepository: CategoryBudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryBudgetUiState())
    val uiState: StateFlow<CategoryBudgetUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            preferencesManager.activeProfileId.flatMapLatest { profileId ->
                combine(
                    categoryRepository.getAllCategories(profileId),
                    categoryBudgetRepository.getAllBudgets(profileId)
                ) { categories, budgets ->
                    Triple(profileId, categories, budgets)
                }
            }.collect { (profileId, categories, budgets) ->
                val startOfMonth = DateUtils.getStartOfMonth()
                val endOfMonth = DateUtils.getEndOfMonth()
                val budgetMap = budgets.associateBy { it.categoryId }

                val items = categories
                    .filter { it.type == "EXPENSE" && !it.isHidden }
                    .map { category ->
                        val budget = budgetMap[category.id]
                        val spent = if (budget != null && budget.amount > 0) {
                            transactionRepository.getTotalSpentByCategoryInMonthSync(
                                category.id, startOfMonth, endOfMonth, profileId
                            )
                        } else 0.0

                        CategoryBudgetItem(
                            category = category,
                            budgetAmount = budget?.amount ?: 0.0,
                            alertPercentage = budget?.alertPercentage ?: 0.9f,
                            spent = spent
                        )
                    }

                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }

    fun startEditing(categoryId: Long) {
        _uiState.update { state ->
            state.copy(items = state.items.map { item ->
                if (item.category.id == categoryId) {
                    item.copy(
                        isEditing = true,
                        inputAmount = if (item.budgetAmount > 0) item.budgetAmount.toLong().toString() else "",
                        inputAlertPct = item.alertPercentage
                    )
                } else item
            })
        }
    }

    fun cancelEditing(categoryId: Long) {
        _uiState.update { state ->
            state.copy(items = state.items.map { item ->
                if (item.category.id == categoryId) item.copy(isEditing = false) else item
            })
        }
    }

    fun updateInputAmount(categoryId: Long, amount: String) {
        _uiState.update { state ->
            state.copy(items = state.items.map { item ->
                if (item.category.id == categoryId) {
                    item.copy(inputAmount = amount.filter { it.isDigit() })
                } else item
            })
        }
    }

    fun updateInputAlertPct(categoryId: Long, pct: Float) {
        _uiState.update { state ->
            state.copy(items = state.items.map { item ->
                if (item.category.id == categoryId) item.copy(inputAlertPct = pct) else item
            })
        }
    }

    fun saveBudget(categoryId: Long) {
        viewModelScope.launch {
            val activeProfileId = preferencesManager.activeProfileId.first()
            val item = _uiState.value.items.find { it.category.id == categoryId } ?: return@launch
            val amount = item.inputAmount.toDoubleOrNull() ?: 0.0

            if (amount < 0) return@launch

            categoryBudgetRepository.saveBudget(
                CategoryBudgetEntity(
                    categoryId = categoryId,
                    amount = amount,
                    alertPercentage = item.inputAlertPct,
                    profileId = activeProfileId
                )
            )
            _uiState.update { state ->
                state.copy(
                    savedMessage = "Budget ${item.category.name} berhasil disimpan",
                    items = state.items.map { i ->
                        if (i.category.id == categoryId) i.copy(isEditing = false) else i
                    }
                )
            }
        }
    }

    fun deleteBudget(categoryId: Long) {
        viewModelScope.launch {
            val activeProfileId = preferencesManager.activeProfileId.first()
            val budget = categoryBudgetRepository.getBudgetByCategorySync(categoryId, activeProfileId)
            budget?.let { categoryBudgetRepository.deleteBudget(it) }
            _uiState.update { state ->
                state.copy(
                    savedMessage = "Budget dihapus",
                    items = state.items.map { item ->
                        if (item.category.id == categoryId) {
                            item.copy(budgetAmount = 0.0, isEditing = false)
                        } else item
                    }
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(savedMessage = null) }
    }
}
