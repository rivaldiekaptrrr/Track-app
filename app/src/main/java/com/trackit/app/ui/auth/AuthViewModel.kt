package com.trackit.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.repository.AuthRepository
import com.trackit.app.data.repository.AuthResult
import com.trackit.app.data.repository.AccessLevel
import com.trackit.app.data.repository.PaymentRepository
import com.trackit.app.data.repository.PaymentResult
import com.trackit.app.util.SyncPreferences
import com.trackit.app.util.SyncManager
import com.trackit.app.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isPaymentLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val accessLevel: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val paymentRepository: PaymentRepository,
    private val syncPreferences: SyncPreferences,
    private val syncManager: SyncManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser get() = authRepository.currentUser
    val availablePackages get() = paymentRepository.availablePackages

    private var pollingJob: Job? = null

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.signInWithEmail(email, password)) {
                is AuthResult.Success -> {
                    val uid = result.user.uid
                    // Fetch or create user doc in Firestore and cache access level locally
                    val accessLevel = authRepository.fetchOrCreateUserDoc(result.user)
                    preferencesManager.setAccessLevel(accessLevel)
                    syncPreferences.setUserId(uid)
                    syncPreferences.setOnlineMode(true)
                    // Only start full sync for non-admin content users
                    if (accessLevel != AccessLevel.NONE && accessLevel != AccessLevel.ADMIN) {
                        syncManager.startSync()
                        try {
                            syncManager.performInitialSync(uid, isNewRegistration = false)
                        } catch (e: Exception) {
                            android.util.Log.e("AuthViewModel", "Initial sync error: ${e.message}")
                        }
                    }
                    _uiState.value = AuthUiState(isSuccess = true, accessLevel = accessLevel)
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
                    // New account: create user doc with NONE access by default
                    val accessLevel = authRepository.fetchOrCreateUserDoc(result.user)
                    preferencesManager.setAccessLevel(accessLevel)
                    syncPreferences.setUserId(uid)
                    syncPreferences.setOnlineMode(true)
                    // NONE users get no sync (nothing to sync yet)
                    if (accessLevel != AccessLevel.NONE && accessLevel != AccessLevel.ADMIN) {
                        syncManager.startSync()
                        try {
                            syncManager.performInitialSync(uid, isNewRegistration = true)
                        } catch (e: Exception) {
                            android.util.Log.e("AuthViewModel", "Initial sync error: ${e.message}")
                        }
                    }
                    _uiState.value = AuthUiState(isSuccess = true, accessLevel = accessLevel)
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
                    val accessLevel = authRepository.fetchOrCreateUserDoc(result.user)
                    preferencesManager.setAccessLevel(accessLevel)
                    syncPreferences.setUserId(uid)
                    syncPreferences.setOnlineMode(true)
                    if (accessLevel != AccessLevel.NONE && accessLevel != AccessLevel.ADMIN) {
                        syncManager.startSync()
                        try {
                            syncManager.performInitialSync(uid, isNewRegistration = false)
                        } catch (e: Exception) {
                            android.util.Log.e("AuthViewModel", "Initial sync error: ${e.message}")
                        }
                    }
                    _uiState.value = AuthUiState(isSuccess = true, accessLevel = accessLevel)
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState(errorMessage = result.message)
                }
            }
        }
    }

    /**
     * Meminta Midtrans Snap Token & URL Pembayaran ke Backend Vercel
     */
    fun createPaymentTransaction(
        accessLevel: String,
        onSuccess: (redirectUrl: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = currentUser ?: run {
            onError("Sesi login tidak ditemukan. Silakan login kembali.")
            return
        }

        val email = user.email ?: run {
            onError("Email akun tidak ditemukan.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPaymentLoading = true, errorMessage = null)
            when (val result = paymentRepository.createSnapToken(user.uid, email, accessLevel)) {
                is PaymentResult.Success -> {
                    _uiState.value = _uiState.value.copy(isPaymentLoading = false)
                    onSuccess(result.redirectUrl)
                }
                is PaymentResult.Error -> {
                    _uiState.value = _uiState.value.copy(isPaymentLoading = false, errorMessage = result.message)
                    onError(result.message)
                }
            }
        }
    }

    /**
     * Memulai polling berkala ke Firestore untuk mendeteksi auto-upgrade lisensi secara real-time
     */
    fun startAutoPollingAccess(onGranted: (String) -> Unit) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                val user = currentUser ?: break
                try {
                    val level = authRepository.fetchOrCreateUserDoc(user)
                    if (level != AccessLevel.NONE) {
                        preferencesManager.setAccessLevel(level)
                        if (level != AccessLevel.ADMIN) {
                            syncPreferences.setUserId(user.uid)
                            syncPreferences.setOnlineMode(true)
                            syncManager.startSync()
                            try {
                                syncManager.performInitialSync(user.uid, isNewRegistration = false)
                            } catch (e: Exception) {
                                android.util.Log.e("AuthViewModel", "Sync error: ${e.message}")
                            }
                        }
                        onGranted(level)
                        break
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AuthViewModel", "Polling access error: ${e.message}")
                }
                delay(3000) // Cek setiap 3 detik
            }
        }
    }

    fun stopAutoPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun checkAccessStatus(
        onGranted: (String) -> Unit,
        onNotGranted: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = authRepository.currentUser ?: run {
                    onError("Pengguna tidak ditemukan.")
                    return@launch
                }
                val level = authRepository.fetchOrCreateUserDoc(user)
                preferencesManager.setAccessLevel(level)
                if (level != AccessLevel.NONE) {
                    if (level != AccessLevel.ADMIN) {
                        syncPreferences.setUserId(user.uid)
                        syncPreferences.setOnlineMode(true)
                        syncManager.startSync()
                        try {
                            syncManager.performInitialSync(user.uid, isNewRegistration = false)
                        } catch (e: Exception) {
                            android.util.Log.e("AuthViewModel", "Sync error: ${e.message}")
                        }
                    }
                    onGranted(level)
                } else {
                    onNotGranted()
                }
            } catch (e: Exception) {
                onError(e.message ?: "Gagal memeriksa status.")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            stopAutoPolling()
            authRepository.signOut()
            syncPreferences.setOnlineMode(false)
            syncPreferences.setUserId(null)
            preferencesManager.setAccessLevel(AccessLevel.NONE)
            preferencesManager.setHasSkippedLogin(false)
            syncManager.stopSync()
            syncManager.clearLocalData()
        }
    }

    fun setSeenWelcome() {
        viewModelScope.launch {
            preferencesManager.setHasSeenWelcome(true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, isPaymentLoading = false, errorMessage = message)
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoPolling()
    }
}
