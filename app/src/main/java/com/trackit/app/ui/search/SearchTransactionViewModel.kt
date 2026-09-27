package com.trackit.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.ui.dashboard.TransactionWithCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val selectedType: String = "ALL", // "ALL", "EXPENSE", "INCOME"
    val searchResults: List<TransactionWithCategory> = emptyList(),
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val isSearching: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedType = MutableStateFlow("ALL")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    val uiState: StateFlow<SearchUiState> = combine(
        preferencesManager.activeProfileId,
        _query,
        _selectedType
    ) { profileId, query, type ->
        Triple(profileId, query.trim(), type)
    }.flatMapLatest { (profileId, query, type) ->
        if (query.isEmpty()) {
            flowOf(
                SearchUiState(
                    query = query,
                    selectedType = type,
                    searchResults = emptyList(),
                    isSearching = false
                )
            )
        } else {
            combine(
                transactionRepository.searchTransactions(
                    query = query,
                    startDate = 0L,
                    endDate = 0L,
                    type = type,
                    profileId = profileId
                ),
                categoryRepository.getAllCategories(profileId)
            ) { txList, categories ->
                val catMap = categories.associateBy { it.id }
                val results = txList.map { tx ->
                    TransactionWithCategory(tx, tx.categoryId?.let { catMap[it] })
                }
                val expenseSum = results.filter { it.transaction.type == "EXPENSE" }
                    .sumOf { it.transaction.amount }
                val incomeSum = results.filter { it.transaction.type == "INCOME" }
                    .sumOf { it.transaction.amount }

                SearchUiState(
                    query = query,
                    selectedType = type,
                    searchResults = results,
                    totalExpense = expenseSum,
                    totalIncome = incomeSum,
                    isSearching = false
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onTypeChange(newType: String) {
        _selectedType.value = newType
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            transactionRepository.deleteById(transactionId)
        }
    }
}
