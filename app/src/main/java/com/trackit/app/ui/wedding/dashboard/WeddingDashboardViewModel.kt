package com.trackit.app.ui.wedding.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.WeddingProfileEntity
import com.trackit.app.data.local.entity.WeddingTaskEntity
import com.trackit.app.data.local.entity.ProfileEntity
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.repository.ProfileRepository
import com.trackit.app.data.repository.WeddingCommitteeRepository
import com.trackit.app.data.repository.WeddingDocumentRepository
import com.trackit.app.data.repository.WeddingExpenseRepository
import com.trackit.app.data.repository.WeddingGuestRepository
import com.trackit.app.data.repository.WeddingProfileRepository
import com.trackit.app.data.repository.WeddingSeserahanRepository
import com.trackit.app.data.repository.WeddingTaskRepository
import com.trackit.app.data.repository.WeddingVendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeddingDashboardUiState(
    val activeProfile: ProfileEntity? = null,
    val allProfiles: List<ProfileEntity> = emptyList(),
    val weddingProfile: WeddingProfileEntity? = null,
    val daysUntilWedding: Long = 0,
    val taskProgress: Float = 0f,       // selesai / total
    val docProgress: Float = 0f,        // selesai / total
    val vendorProgress: Float = 0f,     // terbayar / estimasi total
    val totalBudgetCap: Double = 0.0,
    val totalEstimated: Double = 0.0,
    val totalPaid: Double = 0.0,
    val upcomingTasks: List<WeddingTaskEntity> = emptyList(),
    // Sprint 4 counters
    val totalGuests: Int = 0,
    val totalPax: Int = 0,
    val totalVendors: Int = 0,
    val contractedVendors: Int = 0,     // status KONTRAK atau SELESAI
    val totalSeserahanItems: Int = 0,
    val readySeserahanItems: Int = 0,
    val totalCommitteeMembers: Int = 0,
    val uniformReadyCount: Int = 0,
    val isLoading: Boolean = true
) {
    val allModulesReady: Boolean get() =
        taskProgress >= 1f && docProgress >= 1f &&
        readySeserahanItems == totalSeserahanItems && totalSeserahanItems > 0
}

@HiltViewModel
class WeddingDashboardViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val profileRepository: ProfileRepository,
    private val weddingProfileRepository: WeddingProfileRepository,
    private val taskRepository: WeddingTaskRepository,
    private val documentRepository: WeddingDocumentRepository,
    private val expenseRepository: WeddingExpenseRepository,
    private val guestRepository: WeddingGuestRepository,
    private val vendorRepository: WeddingVendorRepository,
    private val seserahanRepository: WeddingSeserahanRepository,
    private val committeeRepository: WeddingCommitteeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeddingDashboardUiState())
    val uiState: StateFlow<WeddingDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                profileRepository.getAllProfiles(),
                preferencesManager.activeProfileId
            ) { profiles, activeId ->
                val activeProfile = profiles.find { it.id == activeId }
                _uiState.update { it.copy(allProfiles = profiles, activeProfile = activeProfile) }
                loadDashboard()
            }.collect()
        }
    }

    fun loadForProfile(weddingProfileId: String) {
        viewModelScope.launch {
            // Core flows (8 max untuk combine)
            combine(
                weddingProfileRepository.getById(weddingProfileId),
                taskRepository.getTotalCount(weddingProfileId),
                taskRepository.getCompletedCount(weddingProfileId),
                documentRepository.getTotalCount(weddingProfileId),
                documentRepository.getCompletedCount(weddingProfileId),
                expenseRepository.getTotalEstimated(weddingProfileId),
                expenseRepository.getTotalPaid(weddingProfileId),
                taskRepository.getUpcomingTasks(weddingProfileId)
            ) { values ->
                val profile    = values[0] as? WeddingProfileEntity
                val totalTasks = (values[1] as? Int) ?: 0
                val doneTasks  = (values[2] as? Int) ?: 0
                val totalDocs  = (values[3] as? Int) ?: 0
                val doneDocs   = (values[4] as? Int) ?: 0
                val totalEst   = (values[5] as? Double) ?: 0.0
                val totalPaid  = (values[6] as? Double) ?: 0.0
                @Suppress("UNCHECKED_CAST")
                val upcoming   = values[7] as? List<WeddingTaskEntity> ?: emptyList()

                val now = System.currentTimeMillis()
                val days = ((( profile?.weddingDate ?: now) - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(0)

                Triple(
                    profile, days,
                    WeddingDashboardUiState(
                        weddingProfile = profile,
                        daysUntilWedding = days,
                        taskProgress = if (totalTasks > 0) doneTasks.toFloat() / totalTasks else 0f,
                        docProgress = if (totalDocs > 0) doneDocs.toFloat() / totalDocs else 0f,
                        vendorProgress = if (totalEst > 0) (totalPaid / totalEst).toFloat().coerceIn(0f, 1f) else 0f,
                        totalBudgetCap = profile?.totalBudgetCap ?: 0.0,
                        totalEstimated = totalEst,
                        totalPaid = totalPaid,
                        upcomingTasks = upcoming,
                        isLoading = false
                    )
                )
            }.collectLatest { (_, _, coreState) ->
                // Layer 2: Sprint 4 counters
                combine(
                    guestRepository.getTotalCount(weddingProfileId),
                    guestRepository.getTotalPax(weddingProfileId),
                    vendorRepository.getAllByProfile(weddingProfileId),
                    seserahanRepository.getAllByProfile(weddingProfileId),
                    committeeRepository.getAllByProfile(weddingProfileId)
                ) { guestCount, guestPax, vendors, seserahanItems, committeeMembers ->
                    coreState.copy(
                        totalGuests = guestCount,
                        totalPax = guestPax ?: 0,
                        totalVendors = vendors.size,
                        contractedVendors = vendors.count { it.status in listOf("KONTRAK", "SELESAI") },
                        totalSeserahanItems = seserahanItems.size,
                        readySeserahanItems = seserahanItems.count { it.status == "SIAP" },
                        totalCommitteeMembers = committeeMembers.size,
                        uniformReadyCount = committeeMembers.count { it.uniformStatus == "SIAP_PAKAI" }
                    )
                }.collect { fullState ->
                    _uiState.value = fullState
                }
            }
        }
    }

    private fun loadDashboard() {
        _uiState.update { it.copy(isLoading = false) }
    }

    fun switchProfile(profileId: Long) {
        viewModelScope.launch {
            preferencesManager.setActiveProfileId(profileId)
        }
    }
}
