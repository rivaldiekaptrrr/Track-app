package com.trackit.app.ui.wedding.vendor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.WeddingVendorEntity
import com.trackit.app.data.repository.WeddingVendorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val VENDOR_CATEGORIES = listOf(
    "VENUE" to "Venue & Gedung",
    "CATERING" to "Katering",
    "DECOR" to "Dekorasi",
    "MUA" to "MUA & Busana",
    "DOKUMENTASI" to "Foto & Video",
    "MUSIK" to "Musik & Hiburan",
    "WO" to "Wedding Organizer",
    "SOUVENIR" to "Souvenir",
    "LAINNYA" to "Lainnya"
)

val VENDOR_STATUSES = listOf(
    "PROSPEK" to "Prospek",
    "TANDA_JADI" to "Tanda Jadi",
    "KONTRAK" to "Kontrak",
    "SELESAI" to "Selesai"
)

data class WeddingVendorUiState(
    val vendors: List<WeddingVendorEntity> = emptyList(),
    val filterCategory: String = "ALL",
    val isLoading: Boolean = true
) {
    val filtered get() = if (filterCategory == "ALL") vendors
                         else vendors.filter { it.category == filterCategory }
    val byCategory: Map<String, List<WeddingVendorEntity>> get() =
        vendors.groupBy { it.category }
}

@HiltViewModel
class WeddingVendorViewModel @Inject constructor(
    private val repo: WeddingVendorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeddingVendorUiState())
    val uiState: StateFlow<WeddingVendorUiState> = _uiState.asStateFlow()

    fun loadForProfile(weddingProfileId: String) {
        viewModelScope.launch {
            repo.getAllByProfile(weddingProfileId).collect { vendors ->
                _uiState.update { it.copy(vendors = vendors, isLoading = false) }
            }
        }
    }

    fun setFilter(category: String) { _uiState.update { it.copy(filterCategory = category) } }

    fun addVendor(
        weddingProfileId: String, category: String, name: String,
        picName: String?, phone: String?, ig: String?,
        contractValue: Double, notes: String?
    ) {
        viewModelScope.launch {
            repo.insert(WeddingVendorEntity(
                weddingProfileId = weddingProfileId,
                category = category, name = name,
                picName = picName, phoneNumber = phone,
                instagramHandle = ig, contractValue = contractValue, notes = notes
            ))
        }
    }

    fun updateStatus(vendor: WeddingVendorEntity, status: String) {
        viewModelScope.launch { repo.update(vendor.copy(status = status)) }
    }

    fun updateVendor(
        vendor: WeddingVendorEntity,
        category: String, name: String,
        picName: String?, phone: String?, ig: String?,
        contractValue: Double, notes: String?
    ) {
        viewModelScope.launch {
            repo.update(vendor.copy(
                category = category, name = name,
                picName = picName, phoneNumber = phone,
                instagramHandle = ig, contractValue = contractValue, notes = notes
            ))
        }
    }

    fun deleteVendor(vendor: WeddingVendorEntity) {
        viewModelScope.launch { repo.delete(vendor) }
    }
}
