package com.trackit.app.ui.wedding.guests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.WeddingGuestEntity
import com.trackit.app.data.repository.WeddingGuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class CateringCalcResult(
    val totalGuests: Int,
    val totalPax: Int,
    val bufferPct: Float,
    val effectivePax: Int,
    val buffetPortions: Int,
    val gubukPortions: Int,
    val activeStalls: Int
)

data class WeddingGuestsUiState(
    val guests: List<WeddingGuestEntity> = emptyList(),
    val filterGroup: String = "ALL",
    val filterSession: String = "ALL",
    val filterRsvp: String = "ALL",
    val bufferPct: Float = 0.10f, // 10% default
    val activeStalls: Int = 4,
    val isLoading: Boolean = true
) {
    val filtered get() = guests.filter { g ->
        (filterGroup == "ALL" || g.groupAllocation == filterGroup) &&
        (filterSession == "ALL" || g.sessionTarget == filterSession) &&
        (filterRsvp == "ALL" || g.rsvpStatus == filterRsvp)
    }

    val totalGuests get() = guests.size
    val totalPax get() = guests.sumOf { it.estimatedPax }
    val attendingCount get() = guests.count { it.rsvpStatus == "ATTENDING" }

    // Breakdown by group
    val byGroup: Map<String, Int> get() = guests
        .groupBy { it.groupAllocation }
        .mapValues { (_, list) -> list.sumOf { it.estimatedPax } }

    // Kalkulator katering
    val cateringCalc: CateringCalcResult get() {
        val total = totalPax.toDouble()
        val effective = (total * (1.0 - bufferPct)).roundToInt()
        val buffet = (0.60 * effective).roundToInt()
        val gubukPerStall = if (activeStalls > 0) (0.40 * effective / activeStalls).roundToInt() else 0
        return CateringCalcResult(
            totalGuests = totalGuests,
            totalPax = totalPax,
            bufferPct = bufferPct,
            effectivePax = effective,
            buffetPortions = buffet,
            gubukPortions = gubukPerStall,
            activeStalls = activeStalls
        )
    }
}

val GUEST_GROUPS = listOf(
    "KELUARGA_CPP" to "Keluarga CPP",
    "KELUARGA_CPW" to "Keluarga CPW",
    "TEMAN_CPP" to "Teman CPP",
    "TEMAN_CPW" to "Teman CPW",
    "VIP" to "VIP"
)

val GUEST_SESSIONS = listOf(
    "AKAD" to "Akad",
    "RESEPSI" to "Resepsi",
    "KEDUANYA" to "Akad + Resepsi"
)

@HiltViewModel
class WeddingGuestsViewModel @Inject constructor(
    private val repo: WeddingGuestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeddingGuestsUiState())
    val uiState: StateFlow<WeddingGuestsUiState> = _uiState.asStateFlow()

    fun loadForProfile(weddingProfileId: String) {
        viewModelScope.launch {
            repo.getAllByProfile(weddingProfileId).collect { guests ->
                _uiState.update { it.copy(guests = guests, isLoading = false) }
            }
        }
    }

    fun setGroupFilter(group: String) { _uiState.update { it.copy(filterGroup = group) } }
    fun setSessionFilter(session: String) { _uiState.update { it.copy(filterSession = session) } }
    fun setRsvpFilter(rsvp: String) { _uiState.update { it.copy(filterRsvp = rsvp) } }
    fun setBufferPct(pct: Float) { _uiState.update { it.copy(bufferPct = pct) } }
    fun setActiveStalls(stalls: Int) { _uiState.update { it.copy(activeStalls = stalls.coerceIn(1, 20)) } }

    fun addGuest(
        weddingProfileId: String,
        name: String,
        phone: String?,
        group: String,
        session: String,
        pax: Int
    ) {
        viewModelScope.launch {
            repo.insert(
                WeddingGuestEntity(
                    weddingProfileId = weddingProfileId,
                    guestName = name,
                    phoneNumber = phone?.ifBlank { null },
                    groupAllocation = group,
                    sessionTarget = session,
                    estimatedPax = pax
                )
            )
        }
    }

    fun updateRsvp(guest: WeddingGuestEntity, status: String) {
        viewModelScope.launch { repo.update(guest.copy(rsvpStatus = status)) }
    }

    fun updateGuest(
        guest: WeddingGuestEntity,
        name: String,
        phone: String?,
        group: String,
        session: String,
        pax: Int
    ) {
        viewModelScope.launch {
            repo.update(
                guest.copy(
                    guestName = name,
                    phoneNumber = phone?.ifBlank { null },
                    groupAllocation = group,
                    sessionTarget = session,
                    estimatedPax = pax
                )
            )
        }
    }

    fun deleteGuest(guest: WeddingGuestEntity) {
        viewModelScope.launch { repo.delete(guest) }
    }

    fun addMultipleGuests(
        weddingProfileId: String,
        contacts: List<com.trackit.app.util.DeviceContact>
    ) {
        viewModelScope.launch {
            contacts.forEach { contact ->
                repo.insert(
                    WeddingGuestEntity(
                        weddingProfileId = weddingProfileId,
                        guestName = contact.name,
                        phoneNumber = contact.phoneNumber.ifBlank { null },
                        groupAllocation = "LAINNYA",
                        sessionTarget = "KEDUANYA",
                        estimatedPax = 2,
                        rsvpStatus = "PENDING"
                    )
                )
            }
        }
    }
}
