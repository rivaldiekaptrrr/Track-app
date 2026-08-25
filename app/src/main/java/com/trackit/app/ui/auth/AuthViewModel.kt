package com.trackit.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.repository.AuthRepository
import com.trackit.app.data.repository.AuthResult
import com.trackit.app.util.SyncPreferences
import com.trackit.app.util.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncPreferences: SyncPreferences,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser get() = authRepository.currentUser

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.signInWithEmail(email, password)) {
                is AuthResult.Success -> {
                    val uid = result.user.uid
                    syncPreferences.setUserId(uid)
                    syncPreferences.setOnlineMode(true)
                    syncManager.startSync()
                    // Login on a second device: pull remote data, do NOT push local data
                    // (isNewRegistration = false) to avoid overwriting the first device's cloud data.
                    syncManager.performInitialSync(uid, isNewRegistration = false)
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState(errorMessage = result.message)
                }
            }
        }
    }

    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.registerWithEmail(email, password)) {
                is AuthResult.Success -> {
                    val uid = result.user.uid
                    syncPreferences.setUserId(uid)
                    syncPreferences.setOnlineMode(true)
                    syncManager.startSync()
                    // New account: push all existing local data up to Firestore for the first time.
                    syncManager.performInitialSync(uid, isNewRegistration = true)
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState(errorMessage = result.message)
                }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    val uid = result.user.uid
                    syncPreferences.setUserId(uid)
                    syncPreferences.setOnlineMode(true)
                    syncManager.startSync()
                    // Google Sign-In could be a new or existing account.
                    // Treat as existing account (isNewRegistration = false) to avoid
                    // overwriting remote data. The user's data will be pulled via startSync().
                    syncManager.performInitialSync(uid, isNewRegistration = false)
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState(errorMessage = result.message)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            syncPreferences.setOnlineMode(false)
            syncPreferences.setUserId(null)
            syncManager.stopSync()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = message)
    }
}
