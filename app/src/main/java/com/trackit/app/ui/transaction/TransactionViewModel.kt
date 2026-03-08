package com.trackit.app.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionFormState(
    val id: Long? = null,
    val amount: String = "",
    val description: String = "",
    val selectedCategoryId: Long? = null,
    val date: Long = DateUtils.todayMillis(),
    val isRecurring: Boolean = false,
    val recurringType: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Long? = savedStateHandle.get<Long>("transactionId")

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    init {
        loadCategories()
        transactionId?.let { loadTransaction(it) }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _formState.update { it.copy(categories = categories) }
            }
        }
    }

    private fun loadTransaction(id: Long) {
        viewModelScope.launch {
            val transaction = transactionRepository.getById(id)
            transaction?.let { tx ->
                _formState.update {
                    it.copy(
                        id = tx.id,
                        amount = tx.amount.toLong().toString(),
                        description = tx.description,
                        selectedCategoryId = tx.categoryId,
                        date = tx.date,
                        isRecurring = tx.isRecurring,
                        recurringType = tx.recurringType,
                        isEditing = true
                    )
                }
            }
        }
    }

    fun updateAmount(amount: String) {
        _formState.update { it.copy(amount = amount.filter { c -> c.isDigit() || c == '.' }) }
    }

    fun updateDescription(description: String) {
        _formState.update { it.copy(description = description) }
    }

    fun updateCategory(categoryId: Long) {
        _formState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun updateDate(dateMillis: Long) {
        _formState.update { it.copy(date = dateMillis) }
    }

    fun updateRecurring(isRecurring: Boolean) {
        _formState.update { it.copy(isRecurring = isRecurring, recurringType = if (isRecurring) "MONTHLY" else null) }
    }

    fun updateRecurringType(type: String) {
        _formState.update { it.copy(recurringType = type) }
    }

    fun setAmountFromOcr(amount: Double) {
        _formState.update { it.copy(amount = amount.toLong().toString()) }
    }

    fun saveTransaction() {
        val state = _formState.value
        val amount = state.amount.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            _formState.update { it.copy(errorMessage = "Masukkan nominal yang valid") }
            return
        }

        if (state.selectedCategoryId == null) {
            _formState.update { it.copy(errorMessage = "Pilih kategori") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val transaction = TransactionEntity(
                    id = state.id ?: 0,
                    amount = amount,
                    description = state.description,
                    categoryId = state.selectedCategoryId,
                    date = state.date,
                    isRecurring = state.isRecurring,
                    recurringType = state.recurringType
                )

                if (state.isEditing) {
                    transactionRepository.update(transaction)
                } else {
                    transactionRepository.insert(transaction)
                }

                _formState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _formState.update { it.copy(errorMessage = null) }
    }
}
