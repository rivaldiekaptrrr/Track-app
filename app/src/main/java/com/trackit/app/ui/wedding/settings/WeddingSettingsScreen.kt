package com.trackit.app.ui.wedding.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.ThemeMode
import com.trackit.app.ui.settings.ThemeModeOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMainProfile: () -> Unit,
    onExportPdf: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit,
    onExportCsv: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit,
    onExportWeddingPdf: (profileId: String, profileName: String) -> Unit = { _, _ -> },
    onExportWeddingCsv: (profileId: String, profileName: String) -> Unit = { _, _ -> },
    onNavigateToLogin: () -> Unit = {},
    weddingProfileId: String = "",
    viewModel: WeddingSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(weddingProfileId) {
        if (weddingProfileId.isNotBlank()) viewModel.loadForProfile(weddingProfileId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Wedding", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 0. Header Profile & Cloud Sync Card (Top Priority)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (uiState.isOnlineMode) Icons.Default.CloudDone else Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (uiState.isOnlineMode && uiState.currentUserEmail != null) "Akses Pasangan Cloud" else "Mode Wedding Lokal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (uiState.isOnlineMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (uiState.isOnlineMode) "Synced" else "Lokal",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (uiState.isOnlineMode) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.isOnlineMode) (uiState.currentUserEmail ?: "Terhubung bersama pasangan")
                                       else "Data tersimpan di perangkat lokal HP Anda",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.isOnlineMode && uiState.currentUserEmail != null) {
                        OutlinedButton(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Keluar & Matikan Akses Pasangan")
                        }
                    } else {
                        Button(
                            onClick = { onNavigateToLogin() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Masuk / Buat Akun Pasangan")
                        }
                    }
                }
            }

            // 1. Profil & Rencana Wedding
            Text(
                text = "Profil & Rencana Wedding",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Tanggal Pernikahan Pemilih
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tanggal Pernikahan (H-Day)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            val dateString = if (uiState.weddingDate > 0) {
                                java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("id", "ID"))
                                    .format(java.util.Date(uiState.weddingDate))
                            } else "Belum diatur (Klik untuk atur tanggal)"

                            Text(
                                text = dateString,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Quotes & Motivasi Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tampilkan Quote Inspirasi",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tampilkan teks motivasi di Dashboard Wedding",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.quoteEnabled,
                            onCheckedChange = { viewModel.toggleQuoteEnabled(it) }
                        )
                    }

                    if (uiState.quoteEnabled) {
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = uiState.quoteText,
                            onValueChange = { viewModel.updateQuoteText(it) },
                            placeholder = { Text("Tulis kata-kata indah atau motivasi pernikahan...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.saveQuoteText() }) {
                                    Icon(
                                        Icons.Default.Save,
                                        contentDescription = "Simpan",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        Spacer(Modifier.height(14.dp))

                        // Font Size Picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Ukuran Font",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier.background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("KECIL" to "Kecil", "SEDANG" to "Sedang", "BESAR" to "Besar").forEach { (key, label) ->
                                    ThemeModeOption(
                                        label = label,
                                        isSelected = uiState.quoteFontSize == key,
                                        onClick = { viewModel.setQuoteFontSize(key) }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Font Style Picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Gaya Font",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier.background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    "NORMAL" to "Normal",
                                    "BOLD" to "Bold",
                                    "ITALIC" to "Italic",
                                    "BOLD_ITALIC" to "B+I"
                                ).forEach { (key, label) ->
                                    ThemeModeOption(
                                        label = label,
                                        isSelected = uiState.quoteFontStyle == key,
                                        onClick = { viewModel.setQuoteFontStyle(key) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Notifikasi, Tampilan & Keamanan
            Text(
                text = "Tampilan, Notifikasi & Keamanan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Pengingat Harian Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Pengingat Harian Wedding",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Mengingatkan progres checklist & tugas pernikahan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isDailyReminderEnabled,
                            onCheckedChange = { viewModel.toggleDailyReminder(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                    // Tema Aplikasi Segmented Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Tema Aplikasi",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ThemeModeOption(
                                label = "Terang",
                                isSelected = uiState.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                            )
                            ThemeModeOption(
                                label = "Gelap",
                                isSelected = uiState.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                            )
                            ThemeModeOption(
                                label = "Sistem",
                                isSelected = uiState.themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                    // Kunci Biometrik Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Kunci Keamanan Biometrik",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Gunakan sidik jari untuk mengunci aplikasi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isBiometricEnabled,
                            onCheckedChange = { viewModel.toggleBiometric(it) }
                        )
                    }
                }
            }

            // 3. Ekspor Laporan Wedding
            Text(
                text = "Laporan & Ekspor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showExportDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ekspor Laporan Wedding (PDF / Excel)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cetak rincian anggaran vendor & pembayaran DP/pelunasan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // 4. Informasi Aplikasi & Pembaruan
            Text(
                text = "Informasi Aplikasi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "TrackIt v${com.trackit.app.BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Wedding Planner & Smart Expense Tracker",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val updateViewModel: com.trackit.app.updater.UpdateViewModel = hiltViewModel()
                    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()

                    Button(
                        onClick = { updateViewModel.checkForUpdate(silent = false) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (updateUiState.isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mengecek Pembaruan...")
                        } else {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cek Pembaruan Aplikasi")
                        }
                    }

                    updateUiState.checkError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (updateUiState.updateInfo != null) {
                        com.trackit.app.ui.components.UpdateDialog(
                            updateInfo = updateUiState.updateInfo!!,
                            downloadState = updateUiState.downloadState,
                            onStartDownload = { updateViewModel.startDownload() },
                            onInstall = { updateViewModel.installUpdate(it) },
                            onDismiss = { updateViewModel.dismissUpdateDialog() },
                            onResetDownload = { updateViewModel.resetDownloadState() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (uiState.weddingDate > 0) uiState.weddingDate else System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateWeddingDate(it) }
                    showDatePicker = false
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Ekspor Laporan Anggaran Wedding", fontWeight = FontWeight.Bold) },
            text = {
                Text("Pilih format file untuk mengekspor seluruh data anggaran & vendor pernikahan Anda.")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onExportWeddingPdf(weddingProfileId, uiState.profileName.ifBlank { "Anggaran Pernikahan" })
                            showExportDialog = false
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PDF")
                    }
                    Button(
                        onClick = {
                            onExportWeddingCsv(weddingProfileId, uiState.profileName.ifBlank { "Anggaran Pernikahan" })
                            showExportDialog = false
                        }
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Excel")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Batal") }
            }
        )
    }
}
