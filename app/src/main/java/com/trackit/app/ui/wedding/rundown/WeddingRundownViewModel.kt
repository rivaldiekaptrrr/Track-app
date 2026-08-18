package com.trackit.app.ui.wedding.rundown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.WeddingEventEntity
import com.trackit.app.data.local.entity.WeddingRundownItemEntity
import com.trackit.app.data.repository.WeddingEventRepository
import com.trackit.app.data.repository.WeddingRundownRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeddingRundownUiState(
    val events: List<WeddingEventEntity> = emptyList(),
    val rundownByEvent: Map<String, List<WeddingRundownItemEntity>> = emptyMap(),
    val selectedEventId: String? = null,
    val isLoading: Boolean = true
) {
    val selectedEvent get() = events.find { it.eventId == selectedEventId } ?: events.firstOrNull()
    val currentRundown get() = rundownByEvent[selectedEvent?.eventId] ?: emptyList()
}

@HiltViewModel
class WeddingRundownViewModel @Inject constructor(
    private val eventRepo: WeddingEventRepository,
    private val rundownRepo: WeddingRundownRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeddingRundownUiState())
    val uiState: StateFlow<WeddingRundownUiState> = _uiState.asStateFlow()

    private var currentProfileId: String = ""

    fun loadForProfile(weddingProfileId: String) {
        currentProfileId = weddingProfileId
        viewModelScope.launch {
            eventRepo.getAllByProfile(weddingProfileId).collectLatest { events ->
                val rundownMap = mutableMapOf<String, List<WeddingRundownItemEntity>>()
                // Collect each event's rundown once (snapshot)
                events.forEach { event ->
                    rundownRepo.getByEvent(event.eventId).first().let { items ->
                        rundownMap[event.eventId] = items
                    }
                }
                _uiState.update { state ->
                    state.copy(
                        events = events,
                        rundownByEvent = rundownMap,
                        selectedEventId = state.selectedEventId ?: events.firstOrNull()?.eventId,
                        isLoading = false
                    )
                }
                // Live-update rundown for selected event
                val selId = _uiState.value.selectedEventId ?: return@collectLatest
                rundownRepo.getByEvent(selId).collect { items ->
                    _uiState.update { it.copy(rundownByEvent = it.rundownByEvent + (selId to items)) }
                }
            }
        }
    }

    fun selectEvent(eventId: String) {
        _uiState.update { it.copy(selectedEventId = eventId) }
        // Subscribe to rundown of this event
        viewModelScope.launch {
            rundownRepo.getByEvent(eventId).collect { items ->
                _uiState.update { it.copy(rundownByEvent = it.rundownByEvent + (eventId to items)) }
            }
        }
    }

    // ── Events ─────────────────────────────────────────────────────────────
    fun addEvent(name: String, eventDate: Long, location: String?) {
        viewModelScope.launch {
            val newEvent = WeddingEventEntity(
                weddingProfileId = currentProfileId,
                eventName = name,
                eventDate = eventDate,
                eventLocation = location?.ifBlank { null },
                sortOrder = _uiState.value.events.size
            )
            eventRepo.insert(newEvent)
        }
    }

    fun deleteEvent(event: WeddingEventEntity) {
        viewModelScope.launch { eventRepo.delete(event) }
    }

    fun renameEvent(event: WeddingEventEntity, newName: String) {
        viewModelScope.launch { eventRepo.update(event.copy(eventName = newName)) }
    }

    // ── Rundown Items ──────────────────────────────────────────────────────
    fun addRundownItem(eventId: String, time: String, duration: Int, title: String, pic: String?, script: String?) {
        viewModelScope.launch {
            val current = _uiState.value.rundownByEvent[eventId] ?: emptyList()
            rundownRepo.insert(WeddingRundownItemEntity(
                eventId = eventId,
                timeStart = time,
                durationMinutes = duration,
                sessionTitle = title,
                pic = pic?.ifBlank { null },
                mcScript = script?.ifBlank { null },
                sortOrder = current.size
            ))
        }
    }

    fun deleteRundownItem(item: WeddingRundownItemEntity) {
        viewModelScope.launch { rundownRepo.delete(item) }
    }

    fun updateRundownItem(item: WeddingRundownItemEntity) {
        viewModelScope.launch { rundownRepo.update(item) }
    }
}
