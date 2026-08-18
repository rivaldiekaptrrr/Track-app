package com.trackit.app.ui.wedding.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.WeddingProfileEntity
import com.trackit.app.data.local.entity.WeddingTaskEntity
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.repository.WeddingDocumentRepository
import com.trackit.app.data.repository.WeddingExpenseRepository
import com.trackit.app.data.repository.WeddingGuestRepository
import com.trackit.app.data.repository.WeddingProfileRepository
import com.trackit.app.data.repository.WeddingTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class WeddingDashboardUiState(
    val weddingProfile: WeddingProfileEntity? = null,
    val daysUntilWedding: Long = 0,
    val taskProgress: Float = 0f,     // 0.0 – 1.0
    val docProgress: Float = 0f,
    val vendorProgress: Float = 0f,   // paid/total estimated
    val totalBudgetCap: Double = 0.0,
    val totalEstimated: Double = 0.0,
    val totalPaid: Double = 0.0,
    val upcomingTasks: List<WeddingTaskEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class WeddingDashboardViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val weddingProfileRepository: WeddingProfileRepository,
    private val taskRepository: WeddingTaskRepository,
    private val documentRepository: WeddingDocumentRepository,
    private val expenseRepository: WeddingExpenseRepository,
    private val guestRepository: WeddingGuestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeddingDashboardUiState())
    val uiState: StateFlow<WeddingDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Listen to active profile to get weddingProfileId
            preferencesManager.activeProfileId.collectLatest { _ ->
                // Reload if profile changes
                loadDashboard()
            }
        }
    }

    fun loadForProfile(weddingProfileId: String) {
        viewModelScope.launch {
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
                val profile = values[0] as? WeddingProfileEntity
                val totalTasks = (values[1] as? Int) ?: 0
                val completedTasks = (values[2] as? Int) ?: 0
                val totalDocs = (values[3] as? Int) ?: 0
                val completedDocs = (values[4] as? Int) ?: 0
                val totalEst = (values[5] as? Double) ?: 0.0
                val totalPaid = (values[6] as? Double) ?: 0.0
                @Suppress("UNCHECKED_CAST")
                val upcoming = values[7] as? List<WeddingTaskEntity> ?: emptyList()

                val now = System.currentTimeMillis()
                val weddingDate = profile?.weddingDate ?: now
                val days = ((weddingDate - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(0)

                WeddingDashboardUiState(
                    weddingProfile = profile,
                    daysUntilWedding = days,
                    taskProgress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f,
                    docProgress = if (totalDocs > 0) completedDocs.toFloat() / totalDocs else 0f,
                    vendorProgress = if (totalEst > 0) (totalPaid / totalEst).toFloat().coerceIn(0f, 1f) else 0f,
                    totalBudgetCap = profile?.totalBudgetCap ?: 0.0,
                    totalEstimated = totalEst,
                    totalPaid = totalPaid,
                    upcomingTasks = upcoming,
                    isLoading = false
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    private fun loadDashboard() {
        // Placeholder — actual loading triggered externally via loadForProfile()
        _uiState.update { it.copy(isLoading = false) }
    }
}
