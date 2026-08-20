package com.trackit.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.ThemeMode
import com.trackit.app.data.local.entity.ProfileEntity
import com.trackit.app.data.repository.AuthRepository
import com.trackit.app.data.repository.BudgetRepository
import com.trackit.app.data.repository.ProfileRepository
import com.trackit.app.util.SyncPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val monthlyBudget: String = "",
    val isTtsEnabled: Boolean = true,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null,
    val activeProfileId: Long = 1L,
    val isDailyReminderEnabled: Boolean = false,
    val dailyReminderTime: String = "20:00",
    val isExpenseOnlyMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isBiometricEnabled: Boolean = true,
    val isOnlineMode: Boolean = false,
    val currentUserEmail: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val preferencesManager: PreferencesManager,
    private val authRepository: AuthRepository,
    private val syncPreferences: SyncPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadTtsPreference()
        loadBudgetForActiveProfile()
        loadDailyReminderPreferences()
        loadExpenseOnlyMode()
        loadThemeMode()
        loadBiometricPreference()
        loadCloudSyncState()
    }

    private fun loadTtsPreference() {
        viewModelScope.launch {
            preferencesManager.isTtsEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isTtsEnabled = isEnabled) }
            }
        }
    }

    private fun loadBiometricPreference() {
        viewModelScope.launch {
            preferencesManager.isBiometricEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isBiometricEnabled = isEnabled) }
            }
        }
    }

    private fun loadDailyReminderPreferences() {
        viewModelScope.launch {
            preferencesManager.isDailyReminderEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isDailyReminderEnabled = isEnabled) }
            }
        }
        viewModelScope.launch {
            preferencesManager.dailyReminderTime.collect { time ->
                _uiState.update { it.copy(dailyReminderTime = time) }
            }
        }
    }

    private fun loadBudgetForActiveProfile() {
        viewModelScope.launch {
            preferencesManager.activeProfileId.flatMapLatest { profileId ->
                _uiState.update { it.copy(activeProfileId = profileId) }
                budgetRepository.getBudgetSetting(profileId)
            }.collect { setting ->
                _uiState.update {
                    it.copy(monthlyBudget = setting?.monthlyBudget?.toLong()?.toString() ?: "")
                }
            }
        }
    }

    fun updateBudget(budget: String) {
        _uiState.update { it.copy(monthlyBudget = budget.filter { c -> c.isDigit() }) }
    }

    fun saveBudget() {
        val amount = _uiState.value.monthlyBudget.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(errorMessage = "Masukkan anggaran yang valid") }
            return
        }

        viewModelScope.launch {
            try {
                budgetRepository.saveBudget(_uiState.value.activeProfileId, amount)
                _uiState.update { it.copy(savedSuccessfully = true, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(savedSuccessfully = false) }
    }

    fun toggleTts(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setTtsEnabled(enabled)
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBiometricEnabled(enabled)
        }
    }

    fun toggleDailyReminder(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDailyReminderEnabled(enabled)
        }
    }

    fun updateDailyReminderTime(time: String) {
        viewModelScope.launch {
            preferencesManager.setDailyReminderTime(time)
        }
    }

    private fun loadExpenseOnlyMode() {
        viewModelScope.launch {
            preferencesManager.isExpenseOnlyMode.collect { enabled ->
                _uiState.update { it.copy(isExpenseOnlyMode = enabled) }
            }
        }
    }

    fun toggleExpenseOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setExpenseOnlyMode(enabled)
        }
    }

    private fun loadThemeMode() {
        viewModelScope.launch {
            preferencesManager.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
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
            syncPreferences.setUserId(null)
            _uiState.update { it.copy(isOnlineMode = false, currentUserEmail = null) }
        }
    }
}
