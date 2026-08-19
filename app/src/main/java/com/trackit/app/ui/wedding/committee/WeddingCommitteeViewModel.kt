package com.trackit.app.ui.wedding.committee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.WeddingCommitteeEntity
import com.trackit.app.data.repository.WeddingCommitteeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val COMMITTEE_SIDES = listOf(
    "KELUARGA_CPP" to "Keluarga CPP",
    "KELUARGA_CPW" to "Keluarga CPW",
    "TEMAN_CPP" to "Teman CPP",
    "TEMAN_CPW" to "Teman CPW"
)

val UNIFORM_STATUSES = listOf(
    "BELUM_DIBAGI" to "Belum Dibagi",
    "SEDANG_JAHIT" to "Sedang Dijahit",
    "SIAP_PAKAI" to "Siap Pakai"
)

data class WeddingCommitteeUiState(
    val members: List<WeddingCommitteeEntity> = emptyList(),
    val filterSide: String = "ALL",
    val isLoading: Boolean = true
) {
    val filtered get() = if (filterSide == "ALL") members
                         else members.filter { it.side == filterSide }
    val totalFabric get() = members.sumOf { it.fabricMeters }
    val readyCount get() = members.count { it.uniformStatus == "SIAP_PAKAI" }
}

@HiltViewModel
class WeddingCommitteeViewModel @Inject constructor(
    private val repo: WeddingCommitteeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeddingCommitteeUiState())
    val uiState: StateFlow<WeddingCommitteeUiState> = _uiState.asStateFlow()

    fun loadForProfile(weddingProfileId: String) {
        viewModelScope.launch {
            repo.getAllByProfile(weddingProfileId).collect { members ->
                _uiState.update { it.copy(members = members, isLoading = false) }
            }
        }
    }

    fun setFilter(side: String) { _uiState.update { it.copy(filterSide = side) } }

    fun addMember(
        weddingProfileId: String,
        name: String, role: String, side: String, phone: String?,
        uniformDesc: String?, fabricMeters: Double
    ) {
        viewModelScope.launch {
            repo.insert(WeddingCommitteeEntity(
                weddingProfileId = weddingProfileId,
                memberName = name, role = role, side = side,
                phoneNumber = phone?.ifBlank { null },
                uniformDescription = uniformDesc?.ifBlank { null },
                fabricMeters = fabricMeters,
                sortOrder = _uiState.value.members.size
            ))
        }
    }

    fun updateUniformStatus(member: WeddingCommitteeEntity, status: String) {
        viewModelScope.launch { repo.update(member.copy(uniformStatus = status)) }
    }

    fun deleteMember(member: WeddingCommitteeEntity) {
        viewModelScope.launch { repo.delete(member) }
    }
}
