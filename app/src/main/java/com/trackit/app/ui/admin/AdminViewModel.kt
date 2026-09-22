package com.trackit.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.repository.AuthRepository
import com.trackit.app.data.repository.UserInfo
import com.trackit.app.data.repository.AccessLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AdminFilter(val label: String, val levelKey: String?) {
    ALL("Semua", null),
    PENDING("⏳ Menunggu", AccessLevel.NONE),
    EXPENSE("💰 Expense", AccessLevel.EXPENSE),
    WEDDING("💍 Wedding", AccessLevel.WEDDING),
    BOTH("⭐ Full Access", AccessLevel.BOTH),
    ADMIN("👑 Admin", AccessLevel.ADMIN)
}

data class AdminUiState(
    val isLoading: Boolean = false,
    val users: List<UserInfo> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: AdminFilter = AdminFilter.ALL,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val updatingUid: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val users = authRepository.getAllUsers()
                    .sortedWith(compareByDescending<UserInfo> { it.accessLevel == AccessLevel.ADMIN }.thenBy { it.email })
                _uiState.value = _uiState.value.copy(isLoading = false, users = users)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Gagal memuat daftar pengguna: ${e.message}"
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setFilter(filter: AdminFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    val filteredUsers: List<UserInfo>
        get() {
            val q = _uiState.value.searchQuery.trim().lowercase()
            val filter = _uiState.value.selectedFilter
            return _uiState.value.users.filter { user ->
                val matchesFilter = when (filter) {
                    AdminFilter.ALL -> true
                    else -> user.accessLevel == filter.levelKey
                }
                val matchesQuery = if (q.isEmpty()) true else {
                    user.email.lowercase().contains(q) || user.displayName.lowercase().contains(q)
                }
                matchesFilter && matchesQuery
            }
        }

    fun updateUserAccess(targetUid: String, newAccessLevel: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingUid = targetUid, errorMessage = null, successMessage = null)
            val success = authRepository.updateUserAccessLevel(targetUid, newAccessLevel)
            if (success) {
                // Update local list optimistically
                val updatedUsers = _uiState.value.users.map { user ->
                    if (user.uid == targetUid) user.copy(accessLevel = newAccessLevel) else user
                }
                _uiState.value = _uiState.value.copy(
                    updatingUid = null,
                    users = updatedUsers,
                    successMessage = "Akses berhasil diubah menjadi $newAccessLevel"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    updatingUid = null,
                    errorMessage = "Gagal memperbarui akses. Silakan coba lagi."
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}
