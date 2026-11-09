package com.trackit.app.ui.nfc

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.nfc.manager.NfcReaderManager
import com.trackit.app.nfc.model.EmoneyCardResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NfcUiState {
    data object Idle : NfcUiState
    data object Scanning : NfcUiState
    data class Success(val card: EmoneyCardResult) : NfcUiState
    data class Error(val message: String) : NfcUiState
    data object Disabled : NfcUiState
    data object NotSupported : NfcUiState
}

@HiltViewModel
class NfcViewModel @Inject constructor(
    private val nfcReaderManager: NfcReaderManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<NfcUiState>(NfcUiState.Idle)
    val uiState: StateFlow<NfcUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            nfcReaderManager.cardEvents.collect { result ->
                result.fold(
                    onSuccess = { cardResult ->
                        _uiState.value = NfcUiState.Success(cardResult)
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
    }

    fun retryScanning(context: Context) {
        checkAndStartScanning(context)
    }
}

