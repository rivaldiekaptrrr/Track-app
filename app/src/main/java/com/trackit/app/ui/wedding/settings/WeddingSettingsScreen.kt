package com.trackit.app.ui.wedding.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trackit.app.ui.settings.ExportDialog
import com.trackit.app.ui.settings.GDriveRestoreDialog
import com.trackit.app.util.BackupManager
import com.trackit.app.util.GDriveBackupManager
import kotlinx.coroutines.launch
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMainProfile: () -> Unit,
    onExportPdf: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit,
    onExportCsv: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }
    var showGDriveRestoreDialog by remember { mutableStateOf(false) }

    val gDriveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                scope.launch {
                    Toast.makeText(context, "Mencadangkan ke Google Drive...", Toast.LENGTH_SHORT).show()
                    val backupResult = GDriveBackupManager.backupDatabase(context)
                    if (backupResult.isSuccess) {
                        Toast.makeText(context, "Backup berhasil disimpan di Google Drive", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Gagal backup: ${backupResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "Login Google gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { 
            BackupManager.isRestoring = true
            BackupManager.restoreDatabase(context, it)
            Toast.makeText(context, "Silakan tutup aplikasi secara manual dari Recent Apps dan buka kembali.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Wedding", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Akun & Profil",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            SettingsItem(
                icon = Icons.Default.SwitchAccount,
                title = "Ganti Profil / Mode",
                subtitle = "Pindah ke Tracker Pengeluaran atau Profil Lainnya",
                onClick = onNavigateToMainProfile
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Ekspor & Cadangan",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
            SettingsItem(
                icon = Icons.Default.PictureAsPdf,
                title = "Ekspor Laporan (PDF/CSV)",
                subtitle = "Simpan laporan anggaran pernikahan",
                onClick = { showExportDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.CloudUpload,
                title = "Cadangkan ke Google Drive",
                subtitle = "Amankan data ke penyimpanan awan",
                onClick = {
                    val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                        context,
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                        ).requestEmail().requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file")).build()
                    )
                    gDriveSignInLauncher.launch(client.signInIntent)
                }
            )
            SettingsItem(
                icon = Icons.Default.Restore,
                title = "Pulihkan dari Google Drive",
                subtitle = "Kembalikan data yang telah dicadangkan",
                onClick = { showGDriveRestoreDialog = true }
            )
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExportPdf = { title, startDate, endDate, typeFilter ->
                onExportPdf(title, startDate, endDate, typeFilter)
                showExportDialog = false
            },
            onExportCsv = { title, startDate, endDate, typeFilter ->
                onExportCsv(title, startDate, endDate, typeFilter)
                showExportDialog = false
            }
        )
    }

    if (showGDriveRestoreDialog) {
        GDriveRestoreDialog(
            onDismiss = { showGDriveRestoreDialog = false },
            onRestoreLocal = {
                showGDriveRestoreDialog = false
                restoreLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
