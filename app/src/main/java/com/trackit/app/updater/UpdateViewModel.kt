package com.trackit.app.updater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class UpdateUiState(
    val isChecking: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val downloadState: DownloadState = DownloadState.Idle,
    val checkError: String? = null
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val appUpdateChecker: AppUpdateChecker,
    private val appUpdateDownloader: AppUpdateDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    // Configuration for the repository
    private val repoOwner = "rivaldiekaptrrr"
    private val repoName = "Track-app"

    fun checkForUpdate(silent: Boolean = true) {
        _uiState.update { it.copy(isChecking = true, checkError = null) }

        viewModelScope.launch {
            try {
                val currentVersion = BuildConfig.VERSION_NAME
                val updateInfo = appUpdateChecker.checkForUpdate(repoOwner, repoName, currentVersion)
                
                _uiState.update { 
                    it.copy(
                        isChecking = false,
                        updateInfo = updateInfo,
                        checkError = if (!silent && updateInfo == null) "Tidak dapat terhubung atau tidak ada pembaruan." else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isChecking = false, 
                        checkError = if (!silent) e.localizedMessage else null
                    )
                }
            }
        }
    }

    fun startDownload() {
        val url = _uiState.value.updateInfo?.downloadUrl ?: return
        
        viewModelScope.launch {
            appUpdateDownloader.downloadApk(url).collect { state ->
                _uiState.update { it.copy(downloadState = state) }
            }
        }
    }

    fun installUpdate(apkFile: File) {
        appUpdateDownloader.installApk(apkFile)
    }

    fun resetDownloadState() {
        _uiState.update { it.copy(downloadState = DownloadState.Idle) }
    }
    
    fun dismissUpdateDialog() {
        _uiState.update { it.copy(updateInfo = null) }
    }
}
