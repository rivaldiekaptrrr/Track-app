package com.trackit.app.ui.nfc

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.nfc.categorization.NfcKeywordMatcher
import com.trackit.app.nfc.location.NfcLocationRepository
import com.trackit.app.nfc.manager.NfcReaderManager
import com.trackit.app.nfc.model.EmoneyCardResult
import com.trackit.app.nfc.model.NfcCardTransaction
import com.trackit.app.util.CategoryBudgetNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ResolvedLastTransaction(
    val transaction: NfcCardTransaction,
    val locationName: String? = null,
    val isResolvingLocation: Boolean = false,
    val matchedCategory: CategoryEntity? = null,
    val suggestedKeyword: String = "",
    val isRecorded: Boolean = false,
    val isRecording: Boolean = false
)

sealed interface NfcUiState {
    data object Idle : NfcUiState
    data object Scanning : NfcUiState
    data class Success(
        val card: EmoneyCardResult,
        val lastTransactionState: ResolvedLastTransaction? = null
    ) : NfcUiState
    data class Error(val message: String) : NfcUiState
    data object Disabled : NfcUiState
    data object NotSupported : NfcUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NfcViewModel @Inject constructor(
    private val nfcReaderManager: NfcReaderManager,
    private val nfcLocationRepository: NfcLocationRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryBudgetNotifier: CategoryBudgetNotifier,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<NfcUiState>(NfcUiState.Idle)
    val uiState: StateFlow<NfcUiState> = _uiState.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _recordSuccessMessage = MutableStateFlow<String?>(null)
    val recordSuccessMessage: StateFlow<String?> = _recordSuccessMessage.asStateFlow()

    init {
        // Observe categories for current active profile
        viewModelScope.launch {
            preferencesManager.activeProfileId.flatMapLatest { profileId ->
                categoryRepository.getAllCategories(profileId)
            }.collect { list ->
                _categories.value = list
            }
        }

        // Observe NFC reader card events
        viewModelScope.launch {
            nfcReaderManager.cardEvents.collect { result ->
                result.fold(
                    onSuccess = { cardResult ->
                        handleCardSuccess(cardResult)
                    },
                    onFailure = { error ->
                        val friendlyMessage = when {
                            error.message?.contains("Tag was lost", ignoreCase = true) == true ||
                            error.message?.contains("Tag lost", ignoreCase = true) == true ||
                            error.message?.contains("closed", ignoreCase = true) == true ->
                                "Kartu terlepas terlalu cepat. Tahan posisi kartu sebentar hingga ponsel bergetar."
                            error.message?.contains("tidak mendukung", ignoreCase = true) == true ->
                                "Kartu ini tidak menggunakan chip standar ISO 14443-4."
                            else ->
                                error.message ?: "Gagal membaca kartu. Pastikan posisi kartu menempel tepat di sensor NFC."
                        }
                        _uiState.value = NfcUiState.Error(friendlyMessage)
                    }
                )
            }
        }
    }

    private fun handleCardSuccess(cardResult: EmoneyCardResult) {
        val lastTx = cardResult.lastTransaction
        if (lastTx == null) {
            _uiState.value = NfcUiState.Success(card = cardResult)
            return
        }

        // Set initial success state with location resolving indicator
        val initialTxState = ResolvedLastTransaction(
            transaction = lastTx,
            isResolvingLocation = true
        )
        _uiState.value = NfcUiState.Success(
            card = cardResult,
            lastTransactionState = initialTxState
        )

        // Resolve location name asynchronously
        viewModelScope.launch {
            val resolvedName = nfcLocationRepository.resolveLocation(lastTx.locationId)
            val currentCategories = _categories.value
            val keyword = if (!resolvedName.isNullOrBlank()) {
                NfcKeywordMatcher.extractSmartKeyword(resolvedName)
            } else ""

            val matchedCat = if (!resolvedName.isNullOrBlank()) {
                NfcKeywordMatcher.matchCategory(resolvedName, currentCategories)
            } else null

            _uiState.update { currentState ->
                if (currentState is NfcUiState.Success && currentState.card == cardResult) {
                    currentState.copy(
                        lastTransactionState = ResolvedLastTransaction(
                            transaction = lastTx,
                            locationName = resolvedName,
                            isResolvingLocation = false,
                            matchedCategory = matchedCat,
                            suggestedKeyword = keyword
                        )
                    )
                } else {
                    currentState
                }
            }
        }
    }

    /**
     * Records the last NFC transaction into Room database.
     * Optionally learns custom keyword into the category, and updates custom location mapping.
     */
    fun recordLastTransaction(
        category: CategoryEntity,
        keywordToLearn: String?,
        customLocationName: String? = null
    ) {
        val currentState = _uiState.value as? NfcUiState.Success ?: return
        val txState = currentState.lastTransactionState ?: return
        if (txState.isRecorded || txState.isRecording) return

        viewModelScope.launch {
            _uiState.update { state ->
                if (state is NfcUiState.Success && state.lastTransactionState != null) {
                    state.copy(lastTransactionState = state.lastTransactionState.copy(isRecording = true))
                } else state
            }

            try {
                val profileId = preferencesManager.activeProfileId.first()
                val card = currentState.card
                val locationId = txState.transaction.locationId

                // 1. If user provided custom location name, persist mapping in Room
                if (!customLocationName.isNullOrBlank() && locationId.isNotBlank()) {
                    nfcLocationRepository.saveCustomLocation(locationId, customLocationName)
                }

                // 2. If keyword should be learned, append to category customKeywords
                if (!keywordToLearn.isNullOrBlank()) {
                    val updatedCategory = NfcKeywordMatcher.appendKeywordToCategory(category, keywordToLearn)
                    if (updatedCategory.customKeywords != category.customKeywords) {
                        categoryRepository.update(updatedCategory)
                    }
                }

                // 3. Determine description
                val locDisplay = customLocationName?.takeIf { it.isNotBlank() }
                    ?: txState.locationName
                    ?: if (locationId.isNotBlank()) "Terminal $locationId" else "Transaksi ${card.cardType.displayName}"

                val finalDescription = "$locDisplay [Tap ${card.cardType.displayName}]"

                // 4. Insert transaction entity
                val transactionEntity = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    amount = txState.transaction.amount.toDouble(),
                    description = finalDescription,
                    categoryId = category.id,
                    date = txState.transaction.timestamp,
                    type = "EXPENSE",
                    profileId = profileId
                )
                transactionRepository.insert(transactionEntity)

                // 5. Trigger budget notification check
                categoryBudgetNotifier.checkAfterTransaction(category.id, profileId)

                _recordSuccessMessage.value = "Transaksi berhasil dicatat ke kategori ${category.name}"

                _uiState.update { state ->
                    if (state is NfcUiState.Success && state.lastTransactionState != null) {
                        state.copy(
                            lastTransactionState = state.lastTransactionState.copy(
                                isRecording = false,
                                isRecorded = true,
                                locationName = locDisplay
                            )
                        )
                    } else state
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    if (state is NfcUiState.Success && state.lastTransactionState != null) {
                        state.copy(lastTransactionState = state.lastTransactionState.copy(isRecording = false))
                    } else state
                }
            }
        }
    }

    fun clearRecordSuccessMessage() {
        _recordSuccessMessage.value = null
    }

    fun checkAndStartScanning(context: Context) {
        if (!nfcReaderManager.isNfcSupported(context)) {
            _uiState.value = NfcUiState.NotSupported
            return
        }
        if (!nfcReaderManager.isNfcEnabled(context)) {
            _uiState.value = NfcUiState.Disabled
            return
        }
        _uiState.value = NfcUiState.Scanning
    }

    fun isNfcSupported(context: Context): Boolean = nfcReaderManager.isNfcSupported(context)

    fun resetState() {
        _uiState.value = NfcUiState.Idle
        _recordSuccessMessage.value = null
    }

    fun retryScanning(context: Context) {
        checkAndStartScanning(context)
    }
}
