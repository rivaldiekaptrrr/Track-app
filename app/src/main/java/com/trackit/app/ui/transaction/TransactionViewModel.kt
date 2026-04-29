package com.trackit.app.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.DateUtils
import com.trackit.app.util.NumberUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val unrecognizedVoiceText: String? = null,
    val activeProfileId: Long = 1L,
    val pendingBatchTransactions: List<com.trackit.app.util.VoiceParseResult> = emptyList(),
    val savedBatchSize: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Long? = savedStateHandle.get<Long>("transactionId")

    private val _formState = MutableStateFlow(TransactionFormState())
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    init {
        loadActiveProfileAndCategories()
        transactionId?.let { loadTransaction(it) }
    }

    private fun loadActiveProfileAndCategories() {
        viewModelScope.launch {
            preferencesManager.activeProfileId.flatMapLatest { profileId ->
                _formState.update { it.copy(activeProfileId = profileId) }
                categoryRepository.getAllCategories(profileId)
            }.collect { categories ->
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

    /**
     * Menerima input dari keyboard (atau paste).
     * Compose mengirimkan teks secara penuh (termasuk operator jika ada).
     */
    fun updateAmount(input: String) {
        // Cukup ambil karakter angka dan simbol matematika saja
        val sanitized = input.filter { it.isDigit() || it in listOf('+', '-', '*', '/') }
        _formState.update { it.copy(amount = sanitized) }
    }

    /**
     * Dipanggil saat user menekan tombol operator di Calculator Toolbar (+, -, *, /).
     */
    fun appendOperator(operator: Char) {
        val current = _formState.value.amount
        if (current.isEmpty()) return
        // Jika karakter terakhir adalah operator, ganti operator-nya
        val newAmount = if (current.lastOrNull() in listOf('+', '-', '*', '/')) {
            current.dropLast(1) + operator
        } else {
            current + operator
        }
        _formState.update { it.copy(amount = newAmount) }
    }

    /**
     * Mengevaluasi ekspresi matematika seperti "45000+12000" menjadi "57000".
     * Dipanggil saat user menekan tombol "=" di Calculator Toolbar.
     */
    fun evaluateExpression() {
        val expression = _formState.value.amount
        val result = evaluateMathExpression(expression)
        if (result != null) {
            _formState.update { it.copy(amount = result.toLong().toString()) }
        }
    }

    /**
     * Menghapus satu karakter terakhir dari ekspresi (backspace).
     */
    fun backspaceAmount() {
        val current = _formState.value.amount
        if (current.isNotEmpty()) {
            _formState.update { it.copy(amount = current.dropLast(1)) }
        }
    }

    /**
     * Mengembalikan preview hasil kalkulasi real-time jika ekspresi valid.
     * Contoh: "45000+12000" → "57.000"
     */
    fun getCalcPreview(): String? {
        val expression = _formState.value.amount
        if (!expression.any { it in listOf('+', '-', '*', '/') }) return null
        val result = evaluateMathExpression(expression) ?: return null
        return NumberUtils.formatWithThousandSeparators(result.toLong().toString())
    }

    private fun evaluateMathExpression(expression: String): Double? {
        return try {
            // Parse ekspresi sederhana: hanya mendukung +, -, *, /
            val tokens = mutableListOf<String>()
            var current = StringBuilder()
            for (i in expression.indices) {
                val c = expression[i]
                if (c in listOf('+', '-', '*', '/') && i > 0 && expression[i-1].isDigit()) {
                    tokens.add(current.toString())
                    tokens.add(c.toString())
                    current = StringBuilder()
                } else {
                    current.append(c)
                }
            }
            if (current.isNotEmpty()) tokens.add(current.toString())

            // Evaluasi dengan preseden operator: kali & bagi dulu, lalu tambah & kurang
            var i = 0
            val values = mutableListOf<Double>()
            val ops = mutableListOf<Char>()
            while (i < tokens.size) {
                val token = tokens[i]
                if (token.toDoubleOrNull() != null) {
                    values.add(token.toDouble())
                } else if (token.length == 1 && token[0] in listOf('+', '-', '*', '/')) {
                    val op = token[0]
                    while (ops.isNotEmpty() && (ops.last() == '*' || ops.last() == '/')) {
                        val right = values.removeLast()
                        val left = values.removeLast()
                        values.add(applyOp(ops.removeLast(), left, right))
                    }
                    ops.add(op)
                }
                i++
            }
            while (ops.isNotEmpty()) {
                val right = values.removeLast()
                val left = values.removeLast()
                values.add(applyOp(ops.removeLast(), left, right))
            }
            values.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun applyOp(op: Char, left: Double, right: Double): Double = when (op) {
        '+' -> left + right
        '-' -> left - right
        '*' -> left * right
        '/' -> if (right != 0.0) left / right else left
        else -> left
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

    fun setFromVoiceBatch(results: List<com.trackit.app.util.VoiceParseResult>) {
        if (results.size <= 1) {
            // Fallback to single if only one
            results.firstOrNull()?.let {
                setFromVoice(it.amount ?: 0L, it.description, it.categoryName, it.dateMillis, it.type)
            }
        } else {
            _formState.update { it.copy(pendingBatchTransactions = results) }
        }
    }

    fun dismissBatch() {
        _formState.update { it.copy(pendingBatchTransactions = emptyList()) }
    }

    fun updateBatchTransactionCategory(index: Int, categoryName: String) {
        _formState.update { state ->
            val updatedBatch = state.pendingBatchTransactions.toMutableList()
            if (index in updatedBatch.indices) {
                updatedBatch[index] = updatedBatch[index].copy(categoryName = categoryName)
            }
            state.copy(pendingBatchTransactions = updatedBatch)
        }
    }

    fun saveBatchTransactions(selectedTransactions: List<com.trackit.app.util.VoiceParseResult>) {
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            try {
                selectedTransactions.forEach { result ->
                    val matchedCategoryId = result.categoryName?.let { name ->
                        _formState.value.categories.find { it.name.equals(name, ignoreCase = true) }?.id
                    } ?: _formState.value.categories.find { it.name.equals("Lainnya", ignoreCase = true) }?.id

                    val transaction = TransactionEntity(
                        amount = (result.amount ?: 0L).toDouble(),
                        description = result.description,
                        categoryId = matchedCategoryId,
                        date = result.dateMillis ?: DateUtils.todayMillis(),
                        type = result.type,
                        profileId = _formState.value.activeProfileId
                    )
                    transactionRepository.insert(transaction)
                }
                _formState.update { 
                    it.copy(
                        isSaving = false, 
                        savedSuccessfully = true, 
                        pendingBatchTransactions = emptyList(),
                        savedBatchSize = selectedTransactions.size
                    ) 
                }
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun saveTransaction() {
        // Evaluasi ekspresi terlebih dahulu jika ada operator
        if (_formState.value.amount.any { it in listOf('+', '-', '*', '/') }) {
            evaluateExpression()
        }
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
                    type = state.type,
                    profileId = state.activeProfileId
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
                        val keywordToLearn = state.unrecognizedVoiceText
                            .replace(Regex("\\d+"), "")
                            .replace(Regex("(?i)\\b(ribu|rb|rebu|juta|jt|ratus|belas|puluh|rp|rupiah|gocap|cepek|seceng|noban|goban)\\b"), "")
                            .replace(Regex("(?i)\\b(beli|membeli|bayar|membayar|dapat|mendapatkan|terima|menerima|buat|untuk)\\b"), "")
                            .replace(Regex("[.,]"), "")
                            .replace(Regex("\\s+"), " ")
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

                _formState.update { it.copy(isSaving = false, savedSuccessfully = true, unrecognizedVoiceText = null, savedBatchSize = 0) }
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _formState.update { it.copy(errorMessage = null) }
    }
}
