package com.trackit.app.ui.wedding.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.WeddingTaskEntity
import com.trackit.app.data.repository.WeddingTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeddingTasksUiState(
    val allTasks: List<WeddingTaskEntity> = emptyList(),
    val filterPic: String = "ALL", // ALL, GROOM, BRIDE, BOTH, FAMILY, WO
    val isLoading: Boolean = true
) {
    val filtered get() = if (filterPic == "ALL") allTasks
                         else allTasks.filter { it.pic == filterPic }

    // Kelompok per fase
    val grouped: Map<Int, List<WeddingTaskEntity>> get() =
        filtered.groupBy { it.phaseMonth }.toSortedMap(compareByDescending { it })

    val totalCount get() = allTasks.size
    val completedCount get() = allTasks.count { it.isCompleted }
    val progressPct get() = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
}

fun Int.phaseLabel(): String = when (this) {
    12 -> "H-12 Bulan — Perencanaan Awal"
    6 -> "H-6 Bulan — Persiapan Detail"
    3 -> "H-3 Bulan — Finalisasi"
    1 -> "H-1 Bulan — Menjelang Hari-H"
    0 -> "Hari-H"
    else -> "H-$this Bulan"
}

@HiltViewModel
class WeddingTasksViewModel @Inject constructor(
    private val repo: WeddingTaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeddingTasksUiState())
    val uiState: StateFlow<WeddingTasksUiState> = _uiState.asStateFlow()

    fun loadForProfile(weddingProfileId: String) {
        viewModelScope.launch {
            repo.getAllByProfile(weddingProfileId).collect { tasks ->
                _uiState.update { it.copy(allTasks = tasks, isLoading = false) }
            }
        }
    }

    fun toggleCompleted(task: WeddingTaskEntity) {
        viewModelScope.launch { repo.update(task.copy(isCompleted = !task.isCompleted)) }
    }

    fun setFilter(pic: String) { _uiState.update { it.copy(filterPic = pic) } }

    fun addTask(
        weddingProfileId: String,
        title: String,
        desc: String?,
        phaseMonth: Int,
        pic: String
    ) {
        viewModelScope.launch {
            repo.insert(
                WeddingTaskEntity(
                    weddingProfileId = weddingProfileId,
                    phaseMonth = phaseMonth,
                    title = title,
                    description = desc,
                    pic = pic,
                    sortOrder = _uiState.value.allTasks.size
                )
            )
        }
    }

    fun updateTask(task: WeddingTaskEntity, title: String, desc: String?, phaseMonth: Int, pic: String) {
        viewModelScope.launch {
            repo.update(task.copy(title = title, description = desc, phaseMonth = phaseMonth, pic = pic))
        }
    }

    fun deleteTask(task: WeddingTaskEntity) {
        viewModelScope.launch { repo.delete(task) }
    }
}
