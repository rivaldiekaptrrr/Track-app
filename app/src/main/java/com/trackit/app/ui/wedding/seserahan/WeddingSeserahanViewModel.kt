package com.trackit.app.ui.wedding.seserahan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.WeddingSeserahanEntity
import com.trackit.app.data.repository.WeddingSeserahanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val SESERAHAN_ITEM_STATUSES = listOf(
    "BELUM_BELI" to "Belum Beli",
    "DIBELI" to "Sudah Beli",
    "WRAPPING" to "Sedang Wrapping",
    "SIAP" to "Siap"
)

data class WeddingSeserahanUiState(
    val allItems: List<WeddingSeserahanEntity> = emptyList(),
    val filterDirection: String = "ALL",
    val isLoading: Boolean = true
) {
    val filtered get() = if (filterDirection == "ALL") allItems
                         else allItems.filter { it.direction == filterDirection }

    val seserahanItems get() = allItems.filter { it.direction == "SESERAHAN_CPP" }
    val balasanItems get() = allItems.filter { it.direction == "BALASAN_CPW" }
    val maharItems get() = allItems.filter { it.direction == "MAHAR" }

    val totalEstimated get() = allItems.sumOf { it.estimatedPrice * it.quantity }
    val seserahanEstimated get() = seserahanItems.sumOf { it.estimatedPrice * it.quantity }
    val balasanEstimated get() = balasanItems.sumOf { it.estimatedPrice * it.quantity }
    val readyCount get() = allItems.count { it.status == "SIAP" }
}

@HiltViewModel
class WeddingSeserahanViewModel @Inject constructor(
    private val repo: WeddingSeserahanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeddingSeserahanUiState())
    val uiState: StateFlow<WeddingSeserahanUiState> = _uiState.asStateFlow()

    fun loadForProfile(weddingProfileId: String) {
        viewModelScope.launch {
            repo.getAllByProfile(weddingProfileId).collect { items ->
                _uiState.update { it.copy(allItems = items, isLoading = false) }
            }
        }
    }

    fun setFilter(direction: String) { _uiState.update { it.copy(filterDirection = direction) } }

    fun addItem(
        weddingProfileId: String,
        direction: String,
        itemName: String,
        quantity: Int,
        estimatedPrice: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            repo.insert(WeddingSeserahanEntity(
                weddingProfileId = weddingProfileId,
                direction = direction,
                itemName = itemName,
                quantity = quantity,
                estimatedPrice = estimatedPrice,
                notes = notes,
                sortOrder = _uiState.value.allItems.size
            ))
        }
    }

    fun updateStatus(item: WeddingSeserahanEntity, status: String) {
        viewModelScope.launch { repo.update(item.copy(status = status)) }
    }

    fun deleteItem(item: WeddingSeserahanEntity) {
        viewModelScope.launch { repo.delete(item) }
    }
}
