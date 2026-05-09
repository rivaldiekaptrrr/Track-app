package com.trackit.app.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trackit.app.updater.DownloadState
import com.trackit.app.updater.UpdateInfo

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    downloadState: DownloadState,
    onStartDownload: () -> Unit,
    onInstall: (java.io.File) -> Unit,
    onDismiss: () -> Unit,
    onResetDownload: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            if (downloadState !is DownloadState.Downloading) {
                onDismiss()
            }
        },
        title = {
            Text("Pembaruan Tersedia", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Versi ${updateInfo.latestVersion} tersedia untuk diunduh.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (updateInfo.releaseNotes.isNotEmpty()) {
                    Text(
                        "Catatan Rilis:\n${updateInfo.releaseNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when (downloadState) {
                    is DownloadState.Idle -> {
                        Text("Apakah Anda ingin memperbarui sekarang?", style = MaterialTheme.typography.bodyMedium)
                    }
                    is DownloadState.Downloading -> {
                        Text("Mengunduh... ${downloadState.progress}%", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is DownloadState.Done -> {
                        Text("Unduhan selesai. Siap dipasang.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    is DownloadState.Failed -> {
                        Text("Gagal: ${downloadState.error}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is DownloadState.Idle, is DownloadState.Failed -> {
                    Button(onClick = onStartDownload) {
                        Text("Unduh Sekarang")
                    }
                }
                is DownloadState.Done -> {
                    Button(onClick = {
                        // Check if we can install packages
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            if (!context.packageManager.canRequestPackageInstalls()) {
                                // Request permission
                                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                                return@Button
                            }
                        }
                        onInstall(downloadState.apkFile)
                    }) {
                        Text("Pasang")
                    }
                }
                is DownloadState.Downloading -> {
                    // Disabled button while downloading
                    Button(onClick = { }, enabled = false) {
                        Text("Mengunduh...")
                    }
                }
            }
        },
        dismissButton = {
            if (downloadState !is DownloadState.Downloading) {
                TextButton(onClick = {
                    onResetDownload()
                    onDismiss()
                }) {
                    Text("Nanti")
                }
            }
        }
    )
}
