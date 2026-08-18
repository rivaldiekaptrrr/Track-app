package com.trackit.app.ui.wedding.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.WeddingDocumentEntity
import com.trackit.app.data.repository.WeddingDocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeddingDocumentsUiState(
    val documents: List<WeddingDocumentEntity> = emptyList(),
    val filterOwner: String = "ALL", // ALL, GROOM, BRIDE, BOTH
    val isLoading: Boolean = true
) {
    val filtered get() = if (filterOwner == "ALL") documents
                         else documents.filter { it.ownerType == filterOwner || it.ownerType == "BOTH" }
    val totalCount get() = documents.size
    val completedCount get() = documents.count { it.isCompleted }
    val totalAdminCost get() = documents.sumOf { it.adminCost }
}

@HiltViewModel
class WeddingDocumentsViewModel @Inject constructor(
    private val repo: WeddingDocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeddingDocumentsUiState())
    val uiState: StateFlow<WeddingDocumentsUiState> = _uiState.asStateFlow()

    fun loadForProfile(weddingProfileId: String) {
        viewModelScope.launch {
            repo.getAllByProfile(weddingProfileId).collect { docs ->
                _uiState.update { it.copy(documents = docs, isLoading = false) }
            }
        }
    }

    fun toggleCompleted(doc: WeddingDocumentEntity) {
        viewModelScope.launch {
            repo.update(doc.copy(isCompleted = !doc.isCompleted))
        }
    }

    fun setFilter(owner: String) {
        _uiState.update { it.copy(filterOwner = owner) }
    }

    fun deleteDocument(doc: WeddingDocumentEntity) {
        viewModelScope.launch { repo.delete(doc) }
    }

    fun addDocument(weddingProfileId: String, name: String, owner: String, cost: Double) {
        viewModelScope.launch {
            repo.insert(
                WeddingDocumentEntity(
                    weddingProfileId = weddingProfileId,
                    docName = name,
                    ownerType = owner,
                    adminCost = cost,
                    sortOrder = _uiState.value.documents.size
                )
            )
        }
    }
}
