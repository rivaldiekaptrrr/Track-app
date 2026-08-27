package com.trackit.app.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.ThemeMode
import com.trackit.app.util.BackupManager
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onExportPdf: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit,
    onExportCsv: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit,
    onNavigateToCustomKeywords: () -> Unit,
    onNavigateToCategoryBudget: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { 
            BackupManager.isRestoring = true
            BackupManager.restoreDatabase(context, it)
            Toast.makeText(context, "Silakan tutup aplikasi secara manual dari Recent Apps dan buka kembali.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            Toast.makeText(context, "Anggaran berhasil disimpan", Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        topBar = {
            TopAppBar(
                title = {
                    Text("Pengaturan", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                                    imageVector = if (uiState.isOnlineMode) Icons.Default.CloudDone else Icons.Default.AccountCircle,
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
                                    text = if (uiState.isOnlineMode && uiState.currentUserEmail != null) "Akun Terhubung" else "Mode Lokal / Offline",
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
                                        text = if (uiState.isOnlineMode) "Cloud Active" else "Offline",
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
                                text = if (uiState.isOnlineMode) (uiState.currentUserEmail ?: "Terhubung ke Firebase")
                                       else "Data tersimpan secara lokal di perangkat ini",
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
                            Text("Keluar & Matikan Sinkronisasi")
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
                            Text("Masuk / Buat Akun Cloud")
                        }
                    }
                }
            }

            // 1. Fitur & Anggaran (Core Features)
            Text(
                text = "Fitur & Anggaran",
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
                    // Batas Anggaran Bulanan
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Batas Anggaran Bulanan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tentukan target batas pengeluaran bulanan. Anda akan menerima notifikasi jika pengeluaran mencapai 80%.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Rp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = uiState.monthlyBudget,
                            onValueChange = { viewModel.updateBudget(it) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Button(
                            onClick = { viewModel.saveBudget() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan")
                        }
                    }

                    uiState.errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    // Budget per Kategori Navigation
                    SettingsRowItem(
                        icon = Icons.Default.Category,
                        title = "Budget per Kategori",
                        subtitle = "Atur batas pengeluaran spesifik per kategori",
                        onClick = onNavigateToCategoryBudget
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Manajemen Kategori Navigation
                    SettingsRowItem(
                        icon = Icons.Default.List,
                        title = "Manajemen Kategori",
                        subtitle = "Tambah, edit, dan atur kategori transaksi",
                        onClick = onNavigateToCustomKeywords
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Mode Khusus Pengeluaran Toggle
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
                                Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Mode Khusus Pengeluaran",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Sembunyikan fitur pemasukan dan fokus pada pengeluaran",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isExpenseOnlyMode,
                            onCheckedChange = { viewModel.toggleExpenseOnlyMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.error,
                                checkedTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            // 2. Tampilan, Suara & Keamanan (Preferences)
            Text(
                text = "Tampilan, Suara & Keamanan",
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
                    // Tema Aplikasi Option
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

                    // Biometric Toggle
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
                                    "Kunci Keamanan Biometrik",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Gunakan sidik jari / PIN untuk mengunci aplikasi",
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                    // TTS Toggle
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
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Respon Suara (TTS)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Konfirmasi suara otomatis saat simpan transaksi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isTtsEnabled,
                            onCheckedChange = { viewModel.toggleTts(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                    // Daily Reminder Toggle
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
                                    "Pengingat Harian",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Notifikasi rutin untuk mencatat transaksi harian",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isDailyReminderEnabled,
                            onCheckedChange = { enabled -> 
                                viewModel.toggleDailyReminder(enabled)
                                if (enabled) {
                                    com.trackit.app.worker.ReminderScheduler.scheduleReminder(context, uiState.dailyReminderTime)
                                } else {
                                    com.trackit.app.worker.ReminderScheduler.cancelReminder(context)
                                }
                            }
                        )
                    }

                    if (uiState.isDailyReminderEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    val timeParts = uiState.dailyReminderTime.split(":")
                                    val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 20
                                    val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                                    android.app.TimePickerDialog(
                                        context,
                                        { _, selectedHour, selectedMinute ->
                                            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                                            viewModel.updateDailyReminderTime(formattedTime)
                                            com.trackit.app.worker.ReminderScheduler.scheduleReminder(context, formattedTime)
                                        },
                                        hour,
                                        minute,
                                        true
                                    ).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Waktu Pengingat",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = uiState.dailyReminderTime,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 3. Data & Ekspor (Data Management)
            Text(
                text = "Data & Ekspor",
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
                    // Export PDF/Excel
                    SettingsRowItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Ekspor Laporan (PDF / Excel)",
                        subtitle = "Cetak & unduh laporan keuangan terstruktur",
                        onClick = { showExportDialog = true },
                        iconTint = MaterialTheme.colorScheme.error
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Restore Manual
                    SettingsRowItem(
                        icon = Icons.Default.SettingsBackupRestore,
                        title = "Pemulihan Data Manual (.db)",
                        subtitle = "Muat ulang data transaksi dari file cadangan lokal",
                        onClick = { restoreLauncher.launch(arrayOf("application/octet-stream", "*/*")) }
                    )
                }
            }

            // 4. Informasi & Pembaruan Aplikasi (App Info)
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
                                "Smart Expense Tracker & Wedding Planner 2-in-1",
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

    if (showExportDialog) {
        ExportReportDialog(
            onDismiss = { showExportDialog = false },
            isExpenseOnlyMode = uiState.isExpenseOnlyMode,
            onExportPdf = { title, startDate, endDate, typeFilter ->
                showExportDialog = false
                onExportPdf(title, startDate, endDate, typeFilter)
            },
            onExportCsv = { title, startDate, endDate, typeFilter ->
                showExportDialog = false
                onExportCsv(title, startDate, endDate, typeFilter)
            }
        )
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
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

@Composable
fun ThemeModeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
