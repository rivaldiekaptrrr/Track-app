package com.trackit.app.ui.wedding.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.ThemeMode
import com.trackit.app.data.local.entity.WeddingProfileEntity
import com.trackit.app.data.repository.WeddingProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.trackit.app.data.repository.AuthRepository
import com.trackit.app.util.SyncPreferences

data class WeddingSettingsUiState(
    val isDailyReminderEnabled: Boolean = false,
    val dailyReminderTime: String = "20:00",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isBiometricEnabled: Boolean = true,
    val quoteText: String = "",
    val quoteEnabled: Boolean = true,
    val quoteFontSize: String = "SEDANG",
    val quoteFontStyle: String = "ITALIC",
    val weddingDate: Long = 0,
    val isOnlineMode: Boolean = false,
    val currentUserEmail: String? = null
)

@HiltViewModel
class WeddingSettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val weddingProfileRepository: WeddingProfileRepository,
    private val authRepository: AuthRepository,
    private val syncPreferences: SyncPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeddingSettingsUiState())
    val uiState: StateFlow<WeddingSettingsUiState> = _uiState.asStateFlow()

    private var currentProfile: WeddingProfileEntity? = null

    init {
        viewModelScope.launch {
            preferencesManager.isDailyReminderEnabled.collect { enabled ->
                _uiState.update { it.copy(isDailyReminderEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesManager.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            preferencesManager.isBiometricEnabled.collect { enabled ->
                _uiState.update { it.copy(isBiometricEnabled = enabled) }
            }
        }
        loadCloudSyncState()
    }

    private fun loadCloudSyncState() {
        viewModelScope.launch {
            syncPreferences.isOnlineMode.collect { isOnline ->
                _uiState.update {
                    it.copy(
                        isOnlineMode = isOnline,
                        currentUserEmail = if (isOnline) authRepository.currentUser?.email else null
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            syncPreferences.setOnlineMode(false)
            _uiState.update { it.copy(isOnlineMode = false, currentUserEmail = null) }
        }
    }

    fun loadForProfile(weddingProfileId: String) {
        viewModelScope.launch {
            weddingProfileRepository.getById(weddingProfileId).collect { profile ->
                currentProfile = profile
                if (profile != null) {
                    _uiState.update { state ->
                        state.copy(
                            quoteText = profile.quote ?: "",
                            quoteEnabled = profile.quoteEnabled,
                            quoteFontSize = profile.quoteFontSize,
                            quoteFontStyle = profile.quoteFontStyle,
                            weddingDate = profile.weddingDate
                        )
                    }
                }
            }
        }
    }

    fun updateWeddingDate(dateMillis: Long) {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            weddingProfileRepository.update(profile.copy(weddingDate = dateMillis))
            _uiState.update { it.copy(weddingDate = dateMillis) }
        }
    }

    fun toggleDailyReminder(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDailyReminderEnabled(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesManager.setThemeMode(mode) }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setBiometricEnabled(enabled) }
    }

    fun updateQuoteText(text: String) {
        _uiState.update { it.copy(quoteText = text) }
    }

    fun saveQuoteText() {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            weddingProfileRepository.update(
                profile.copy(quote = _uiState.value.quoteText.trim().ifBlank { null })
            )
        }
    }

    fun toggleQuoteEnabled(enabled: Boolean) {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            weddingProfileRepository.update(profile.copy(quoteEnabled = enabled))
        }
        _uiState.update { it.copy(quoteEnabled = enabled) }
    }

    fun setQuoteFontSize(size: String) {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            weddingProfileRepository.update(profile.copy(quoteFontSize = size))
        }
        _uiState.update { it.copy(quoteFontSize = size) }
    }

    fun setQuoteFontStyle(style: String) {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            weddingProfileRepository.update(profile.copy(quoteFontStyle = style))
        }
        _uiState.update { it.copy(quoteFontStyle = style) }
    }
}
