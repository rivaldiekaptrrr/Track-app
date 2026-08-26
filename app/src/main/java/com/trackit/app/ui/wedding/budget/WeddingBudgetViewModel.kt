package com.trackit.app.ui.wedding.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import com.trackit.app.data.local.entity.WeddingPaymentTermEntity
import com.trackit.app.data.repository.WeddingExpenseRepository
import com.trackit.app.data.repository.WeddingProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ExpenseWithTerms(
    val expense: WeddingExpenseEntity,
    val terms: List<WeddingPaymentTermEntity> = emptyList()
)

data class WeddingBudgetUiState(
    val expenses: List<WeddingExpenseEntity> = emptyList(),
    val totalBudgetCap: Double = 0.0,
    val totalEstimated: Double = 0.0,
    val totalPaid: Double = 0.0,
    val filterCategory: String = "ALL",
    val isLoading: Boolean = true,
    val showAddSheet: Boolean = false,
    val availableCategories: List<Pair<String, String>> = emptyList(),
    val availableSources: List<Pair<String, String>> = emptyList()
) {
    val filtered get() = if (filterCategory == "ALL") expenses
                         else expenses.filter { it.category == filterCategory }
    val remaining get() = totalBudgetCap - totalEstimated
    val isOverBudget get() = totalBudgetCap > 0 && totalEstimated > totalBudgetCap

    // Breakdown per sumber dana
    val bySource: Map<String, Double> get() = expenses
        .groupBy { it.paidBySource }
        .mapValues { (_, list) -> list.sumOf { it.totalPaid } }

    // Breakdown per kategori
    val byCategory: Map<String, Double> get() = expenses
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.totalEstimated } }
}

val EXPENSE_CATEGORIES = listOf(
    "VENUE" to "Venue & Gedung",
    "CATERING" to "Katering",
    "DECOR" to "Dekorasi",
    "MUA" to "MUA & Busana",
    "DOKUMENTASI" to "Foto & Video",
    "UNDANGAN" to "Undangan",
    "SESERAHAN" to "Seserahan",
    "SOUVENIR" to "Souvenir",
    "TRANSPORTASI" to "Transportasi",
    "LAINNYA" to "Lainnya"
)

val FUND_SOURCES = listOf(
    "TABUNGAN_CPP" to "Tabungan CPP",
    "TABUNGAN_CPW" to "Tabungan CPW",
    "ORTU_CPP" to "Ortu CPP",
    "ORTU_CPW" to "Ortu CPW",
    "BERSAMA" to "Dana Bersama"
)

@HiltViewModel
class WeddingBudgetViewModel @Inject constructor(
    private val expenseRepo: WeddingExpenseRepository,
    private val profileRepo: WeddingProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeddingBudgetUiState())
    val uiState: StateFlow<WeddingBudgetUiState> = _uiState.asStateFlow()

    fun loadForProfile(weddingProfileId: String) {
        viewModelScope.launch {
            // Load budget cap
            profileRepo.getById(weddingProfileId).collect { profile ->
                _uiState.update { it.copy(totalBudgetCap = profile?.totalBudgetCap ?: 0.0) }
            }
        }
        viewModelScope.launch {
            combine(
                expenseRepo.getAllByProfile(weddingProfileId),
                expenseRepo.getTotalEstimated(weddingProfileId),
                expenseRepo.getTotalPaid(weddingProfileId)
            ) { expenses, totalEst, totalPaid ->
                Triple(expenses, totalEst ?: 0.0, totalPaid ?: 0.0)
            }.collect { (expenses, totalEst, totalPaid) ->
                val defaultCatMap = EXPENSE_CATEGORIES.toMap()
                val existingCatKeys = expenses.map { it.category }.distinct()
                val allCatKeys = (defaultCatMap.keys + existingCatKeys).distinct()
                val availableCats = allCatKeys.map { key -> key to (defaultCatMap[key] ?: key) }

                val defaultSrcMap = FUND_SOURCES.toMap()
                val existingSrcKeys = expenses.map { it.paidBySource }.distinct()
                val allSrcKeys = (defaultSrcMap.keys + existingSrcKeys).distinct()
                val availableSrcs = allSrcKeys.map { key -> key to (defaultSrcMap[key] ?: key) }

                _uiState.update { it.copy(
                    expenses = expenses,
                    totalEstimated = totalEst,
                    totalPaid = totalPaid,
                    availableCategories = availableCats,
                    availableSources = availableSrcs,
                    isLoading = false
                )}
            }
        }
    }

    fun setFilter(category: String) { _uiState.update { it.copy(filterCategory = category) } }

    fun addExpense(
        weddingProfileId: String,
        category: String,
        title: String,
        totalEstimated: Double,
        paidBySource: String,
        notes: String?
    ) {
        viewModelScope.launch {
            expenseRepo.insert(WeddingExpenseEntity(
                weddingProfileId = weddingProfileId,
                category = category,
                title = title,
                totalEstimated = totalEstimated,
                paidBySource = paidBySource,
                notes = notes
            ))
        }
    }

    fun addPayment(expense: WeddingExpenseEntity, termName: String, amount: Double, dueDate: Long) {
        viewModelScope.launch {
            val term = WeddingPaymentTermEntity(
                expenseId = expense.expenseId,
                termName = termName,
                amount = amount,
                dueDate = dueDate
            )
            expenseRepo.insertTerm(term)
            // Update totalPaid & status on the expense
            val newPaid = expense.totalPaid + amount
            val status = when {
                newPaid >= expense.totalEstimated -> "FULLY_PAID"
                newPaid > 0 -> "PARTIAL_DP"
                else -> "UNPAID"
            }
            expenseRepo.update(expense.copy(totalPaid = newPaid, paymentStatus = status))
        }
    }

    fun deleteExpense(expense: WeddingExpenseEntity) {
        viewModelScope.launch { expenseRepo.delete(expense) }
    }
}
