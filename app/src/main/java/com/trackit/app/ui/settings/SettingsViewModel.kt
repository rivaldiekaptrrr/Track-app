package com.trackit.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.local.entity.ProfileEntity
import com.trackit.app.data.repository.BudgetRepository
import com.trackit.app.data.repository.ProfileRepository
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
    val activeProfileId: Long = 1L
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadTtsPreference()
        loadBudgetForActiveProfile()
    }

    private fun loadTtsPreference() {
        viewModelScope.launch {
            preferencesManager.isTtsEnabled.collect { isEnabled ->
                _uiState.update { it.copy(isTtsEnabled = isEnabled) }
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
}
