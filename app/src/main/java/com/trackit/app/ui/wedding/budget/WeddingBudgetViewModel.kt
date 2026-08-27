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
import com.trackit.app.ui.wedding.dashboard.WeddingCategoryBudgetProgress
import javax.inject.Inject

data class ExpenseWithTerms(
    val expense: WeddingExpenseEntity,
    val terms: List<WeddingPaymentTermEntity> = emptyList()
)

data class WeddingBudgetUiState(
    val expenses: List<WeddingExpenseEntity> = emptyList(),
    val filteredExpenses: List<WeddingExpenseEntity> = emptyList(),
    val totalBudgetCap: Double = 0.0,
    val totalEstimated: Double = 0.0,
    val totalPaid: Double = 0.0,
    val filterSource: String = "ALL",
    val isLoading: Boolean = true,
    val showAddSheet: Boolean = false,
    val availableCategories: List<Pair<String, String>> = emptyList(),
    val availableSources: List<Pair<String, String>> = emptyList(),
    val categoryBudgets: List<WeddingCategoryBudgetProgress> = emptyList()
) {
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
    "MUA" to "MUA",
    "BUSANA" to "Busana",
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

    private val _filterSource = MutableStateFlow("ALL")
    val filterSource: StateFlow<String> = _filterSource.asStateFlow()

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
                _filterSource
            ) { expenses, filterSrc ->
                val defaultCatMap = EXPENSE_CATEGORIES.toMap()
                val existingCatKeys = expenses.map { it.category }.distinct()
                val allCatKeys = (defaultCatMap.keys + existingCatKeys).distinct()
                val availableCats = allCatKeys.map { key ->
                    val display = if (key.startsWith("CUSTOM:")) {
                        key.split(":").getOrNull(1) ?: key
                    } else {
                        defaultCatMap[key] ?: key
                    }
                    key to display
                }

                // Filter expenses by source of funds
                val filtered = if (filterSrc == "ALL") expenses
                               else expenses.filter { it.paidBySource == filterSrc }

                // Calculate totals based on filtered expenses
                val totalEst = filtered.sumOf { it.totalEstimated }
                val totalPaid = filtered.sumOf { it.totalPaid }

                val defaultCatWithIcons = mapOf(
                    "VENUE" to ("Venue & Gedung" to "apartment"),
                    "CATERING" to ("Katering" to "restaurant"),
                    "DECOR" to ("Dekorasi" to "brush"),
                    "MUA" to ("MUA" to "face"),
                    "BUSANA" to ("Busana" to "checkroom"),
                    "DOKUMENTASI" to ("Foto & Video" to "camera"),
                    "UNDANGAN" to ("Undangan" to "email"),
                    "SESERAHAN" to ("Seserahan" to "giftcard"),
                    "SOUVENIR" to ("Souvenir" to "redeem"),
                    "TRANSPORTASI" to ("Transportasi" to "car"),
                    "LAINNYA" to ("Lainnya" to "more")
                )

                val grouped = filtered.groupBy { it.category }
                val catBudgets = grouped.map { (catKey, list) ->
                    val totalPaidCat = list.sumOf { it.totalPaid }
                    val totalEstCat = list.sumOf { it.totalEstimated }
                    val progressCat = if (totalEstCat > 0) (totalPaidCat / totalEstCat).toFloat().coerceIn(0f, 1f) else 0f
                    
                    val (name, icon) = if (catKey.startsWith("CUSTOM:")) {
                        val parts = catKey.split(":")
                        val customName = parts.getOrNull(1) ?: catKey
                        val customIcon = parts.getOrNull(2) ?: "more"
                        customName to customIcon
                    } else {
                        val mapped = defaultCatWithIcons[catKey]
                        if (mapped != null) {
                            mapped.first to mapped.second
                        } else {
                            catKey to "more"
                        }
                    }

                    WeddingCategoryBudgetProgress(
                        categoryKey = catKey,
                        categoryName = name,
                        totalPaid = totalPaidCat,
                        totalEstimated = totalEstCat,
                        progress = progressCat,
                        iconName = icon
                    )
                }.sortedByDescending { it.totalEstimated }

                val defaultSrcMap = FUND_SOURCES.toMap()
                val existingSrcKeys = expenses.map { it.paidBySource }.distinct()
                val allSrcKeys = (defaultSrcMap.keys + existingSrcKeys).distinct()
                val availableSrcs = listOf("ALL" to "Semua") + allSrcKeys.map { key -> key to (defaultSrcMap[key] ?: key) }

                _uiState.update { it.copy(
                    expenses = expenses,
                    filteredExpenses = filtered,
                    totalEstimated = totalEst,
                    totalPaid = totalPaid,
                    filterSource = filterSrc,
                    availableCategories = availableCats,
                    availableSources = availableSrcs,
                    categoryBudgets = catBudgets,
                    isLoading = false
                )}
            }.collect()
        }
    }

    fun setSourceFilter(source: String) {
        _filterSource.value = source
    }

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

    fun editExpense(
        expense: WeddingExpenseEntity,
        category: String,
        title: String,
        totalEstimated: Double,
        paidBySource: String,
        notes: String?
    ) {
        viewModelScope.launch {
            expenseRepo.update(expense.copy(
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
                dueDate = dueDate,
                isPaid = true,
                paidDate = System.currentTimeMillis()
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
