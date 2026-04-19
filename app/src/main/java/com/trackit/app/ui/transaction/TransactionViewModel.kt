package com.trackit.app.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import com.trackit.app.util.NumberUtils
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
    val type: String = "EXPENSE",
    val categories: List<CategoryEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null,
    val unrecognizedVoiceText: String? = null
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
                        type = tx.type,
                        isEditing = true
                    )
                }
            }
        }
    }

    fun updateAmount(amount: String) {
        val rawDigits = amount.filter { it.isDigit() }
        _formState.update { it.copy(amount = rawDigits) }
    }

    fun getFormattedAmount(): String {
        return NumberUtils.formatWithThousandSeparators(_formState.value.amount)
    }

    fun updateDescription(description: String) {
        _formState.update { it.copy(description = description) }
    }

    fun updateCategory(categoryId: Long) {
        val category = _formState.value.categories.find { it.id == categoryId }
        val type = category?.type ?: _formState.value.type
        _formState.update { it.copy(selectedCategoryId = categoryId, type = type) }
    }

    fun updateType(type: String) {
        _formState.update { it.copy(type = type, selectedCategoryId = null) }
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

    fun setFromVoice(amount: Long, description: String?, categoryName: String?, dateMillis: Long?, type: String = "EXPENSE") {
        _formState.update { state ->
            val matchedCategoryId = categoryName?.let { name ->
                state.categories.find {
                    it.name.equals(name, ignoreCase = true)
                }?.id
            }
            
            // Simpan teks suara asli jika kategori tidak terdeteksi
            val unrecognized = if (matchedCategoryId == null && description != null) description else null

            state.copy(
                amount = amount.toString(),
                description = description ?: state.description,
                selectedCategoryId = matchedCategoryId ?: state.selectedCategoryId,
                date = dateMillis ?: state.date,
                type = type,
                unrecognizedVoiceText = unrecognized
            )
        }
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
                    recurringType = state.recurringType,
                    type = state.type
                )

                if (state.isEditing) {
                    transactionRepository.update(transaction)
                } else {
                    transactionRepository.insert(transaction)
                }

                // Machine Learning Natural: Pelajari kata kunci baru dari ucapan
                if (state.unrecognizedVoiceText != null) {
                    val selectedCategory = state.categories.find { it.id == state.selectedCategoryId }
                    if (selectedCategory != null) {
                        // Bersihkan teks: hapus angka dan satuan uang (ribu, juta, rp, dll) untuk mengambil kata bendanya saja
                        val keywordToLearn = state.unrecognizedVoiceText
                            .replace(Regex("\\d+"), "")
                            .replace(Regex("(?i)\\b(ribu|rb|rebu|juta|jt|ratus|belas|puluh|rp|rupiah|gocap|cepek|seceng|noban|goban)\\b"), "")
                            .replace(Regex("[.,]"), "")
                            .trim()

                        if (keywordToLearn.isNotBlank()) {
                            val currentKeywords = selectedCategory.customKeywords.split(",").map { it.trim().lowercase() }
                            if (!currentKeywords.contains(keywordToLearn.lowercase())) {
                                val newKeywords = if (selectedCategory.customKeywords.isBlank()) {
                                    keywordToLearn
                                } else {
                                    "${selectedCategory.customKeywords}, $keywordToLearn"
                                }
                                categoryRepository.update(selectedCategory.copy(customKeywords = newKeywords))
                            }
                        }
                    }
                }

                _formState.update { it.copy(isSaving = false, savedSuccessfully = true, unrecognizedVoiceText = null) }
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _formState.update { it.copy(errorMessage = null) }
    }
}
