package com.trackit.app.ui.wedding.vendor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.WeddingVendorEntity
import com.trackit.app.ui.wedding.common.DeleteConfirmDialog
import com.trackit.app.ui.wedding.common.WeddingScreenGuideDialog
import com.trackit.app.ui.wedding.common.WeddingGuideFeature
import com.trackit.app.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingVendorScreen(
    weddingProfileId: String,
    onNavigateBack: () -> Unit,
    viewModel: WeddingVendorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var editingVendor by remember { mutableStateOf<WeddingVendorEntity?>(null) }
    var vendorForStatusChange by remember { mutableStateOf<WeddingVendorEntity?>(null) }
    var vendorToDelete by remember { mutableStateOf<WeddingVendorEntity?>(null) }

    LaunchedEffect(weddingProfileId) { viewModel.loadForProfile(weddingProfileId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vendor Hub", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.dealVendorsCount}/${uiState.vendors.size} deal · Total ${CurrencyUtils.formatRupiah(uiState.totalContractValue)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showGuideDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Panduan Fitur")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Vendor")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // 1. Hero Overview Card
                item {
                    VendorHeroCard(uiState = uiState)
                }

                // 2. Category Filter Chips
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.filterCategory == "ALL",
                                onClick = { viewModel.setFilter("ALL") },
                                label = { Text("Semua (${uiState.vendors.size})") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        items(uiState.availableCategories) { (key, label) ->
                            val count = uiState.vendors.count { it.category == key }
                            FilterChip(
                                selected = uiState.filterCategory == key,
                                onClick = { viewModel.setFilter(key) },
                                label = { Text("$label ($count)") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // 3. Items List / Empty State
                if (uiState.filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = if (uiState.filterCategory == "ALL") "Belum ada vendor terdaftar"
                                           else "Tidak ada vendor untuk kategori ini",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                FilledTonalButton(onClick = { showAddDialog = true }) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Tambah Vendor Baru")
                                }
                            }
                        }
                    }
                } else {
                    items(uiState.filtered, key = { it.vendorId }) { vendor ->
                        VendorItemCard(
                            vendor = vendor,
                            onStatusClick = { vendorForStatusChange = vendor },
                            onEdit = { editingVendor = vendor },
                            onDelete = { vendorToDelete = vendor }
                        )
                    }
                }
            }
        }
    }

    if (showGuideDialog) {
        WeddingScreenGuideDialog(
            title = "Manajemen Vendor Hub",
            screenPurpose = "Menyimpan seluruh kontak vendor pernikahan, rincian nilai kontrak, nomor PIC, dan memantau status kesepakatan (deal).",
            features = listOf(
                WeddingGuideFeature("Kategori Vendor Terstruktur", "Kelompokkan vendor ke Venue, Katering, Fotografi, Dekorasi, MUA, Busana, Hiburan, dll."),
                WeddingGuideFeature("Kontak PIC Cepat (WA & Telp)", "Hubungi PIC vendor langsung melalui WhatsApp atau panggilan telepon dalam 1 klik."),
                WeddingGuideFeature("Pantau DP & Nilai Kontrak", "Catat nominal kontrak, uang muka (DP), dan sisa pembayaran yang harus dilunasi."),
                WeddingGuideFeature("Status Kerja Sama", "Pantau alur status vendor dari 'Riset/Hunting', 'Negosiasi', 'Deal (DP)', hingga 'Lunas Selesai'.")
            ),
            proTip = "Simpan nomor kontak PIC cadangan dan link dokumen perjanjian kerja sama di catatan vendor agar mudah diakses saat hari H.",
            onDismiss = { showGuideDialog = false }
        )
    }

    // Status Selection Dialog
    vendorForStatusChange?.let { vendor ->
        VendorStatusDialog(
            currentStatus = vendor.status,
            onDismiss = { vendorForStatusChange = null },
            onSelect = { newStatus ->
                viewModel.updateStatus(vendor, newStatus)
                vendorForStatusChange = null
            }
        )
    }

    // Add Vendor Dialog
    if (showAddDialog) {
        AddEditVendorDialog(
            vendor = null,
            availableCategories = uiState.availableCategories,
            onDismiss = { showAddDialog = false },
            onSave = { cat, name, pic, phone, ig, value, notes ->
                viewModel.addVendor(weddingProfileId, cat, name, pic, phone, ig, value, notes)
                showAddDialog = false
            }
        )
    }

    // Edit Vendor Dialog
    editingVendor?.let { vendor ->
        AddEditVendorDialog(
            vendor = vendor,
            availableCategories = uiState.availableCategories,
            onDismiss = { editingVendor = null },
            onSave = { cat, name, pic, phone, ig, value, notes ->
                viewModel.updateVendor(vendor, cat, name, pic, phone, ig, value, notes)
                editingVendor = null
            }
        )
    }

    // Delete Confirmation Dialog
    vendorToDelete?.let { vendor ->
        DeleteConfirmDialog(
            title = "Hapus Vendor?",
            message = "Apakah Anda yakin ingin menghapus vendor '${vendor.name}'? Tindakan ini tidak dapat dibatalkan.",
            onDismiss = { vendorToDelete = null },
            onConfirm = {
                viewModel.deleteVendor(vendor)
                vendorToDelete = null
            }
        )
    }
}

@Composable
private fun VendorHeroCard(uiState: WeddingVendorUiState) {
    val totalCount = uiState.vendors.size
    val dealCount = uiState.dealVendorsCount
    val completedCount = uiState.completedCount
    val progress = if (totalCount > 0) dealCount.toFloat() / totalCount else 0f

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Row 1: Total Kontrak & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Kontrak Seluruh Vendor",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = CurrencyUtils.formatRupiah(uiState.totalContractValue),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "$dealCount/$totalCount Deal",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Row 2: Quick Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                    Text("Selesai: $completedCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF1565C0)))
                    Text("Kontrak: ${uiState.vendors.count { it.status == "KONTRAK" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE65100)))
                    Text("Tanda Jadi: ${uiState.vendors.count { it.status == "TANDA_JADI" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun VendorItemCard(
    vendor: WeddingVendorEntity,
    onStatusClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val catLabel = VENDOR_CATEGORIES.find { it.first == vendor.category }?.second ?: vendor.category
    val statusInfo = VENDOR_STATUSES.find { it.first == vendor.status }
    val (statusColor, statusBg) = when (vendor.status) {
        "SELESAI" -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
        "KONTRAK" -> Pair(Color(0xFF1565C0), Color(0xFFE3F2FD))
        "TANDA_JADI" -> Pair(Color(0xFFE65100), Color(0xFFFFF3E0))
        else -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name + Category badge (Left) & Contract Value (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vendor.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = catLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Contract Value
                if (vendor.contractValue > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyUtils.formatRupiah(vendor.contractValue),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Nilai Kontrak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Contact Info
            if (!vendor.picName.isNullOrBlank() || !vendor.phoneNumber.isNullOrBlank() || !vendor.instagramHandle.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!vendor.picName.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(vendor.picName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (!vendor.phoneNumber.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(vendor.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Notes
            if (!vendor.notes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Notes, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = vendor.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom Row: Status Pill (Left) & Quick Action Buttons (Right)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Interactive Status Pill
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable(onClick = onStatusClick)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = statusInfo?.second ?: vendor.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Ganti Status",
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Quick Contact & Management Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // WhatsApp Button
                    if (!vendor.phoneNumber.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                val cleanPhone = vendor.phoneNumber.replace(Regex("[^0-9]"), "")
                                val formattedPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedPhone"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = "Chat WhatsApp",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Instagram Button
                    if (!vendor.instagramHandle.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                val cleanIg = vendor.instagramHandle.removePrefix("@").trim()
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$cleanIg"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = "Buka Instagram",
                                tint = Color(0xFFE1306C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorStatusDialog(
    currentStatus: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Status Vendor", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VENDOR_STATUSES.forEach { (key, label) ->
                    val isSelected = currentStatus == key
                    val (statusColor, statusBg) = when (key) {
                        "SELESAI" -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
                        "KONTRAK" -> Pair(Color(0xFF1565C0), Color(0xFFE3F2FD))
                        "TANDA_JADI" -> Pair(Color(0xFFE65100), Color(0xFFFFF3E0))
                        else -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) statusBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSelected) BorderStroke(1.5.dp, statusColor) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(key) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) statusColor else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditVendorDialog(
    vendor: WeddingVendorEntity? = null,
    availableCategories: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (cat: String, name: String, pic: String?, phone: String?, ig: String?, value: Double, notes: String?) -> Unit
) {
    val isEditing = vendor != null
    var selectedCat by remember { mutableStateOf(vendor?.category ?: availableCategories.firstOrNull()?.first ?: "VENUE") }
    var name by remember { mutableStateOf(vendor?.name ?: "") }
    var pic by remember { mutableStateOf(vendor?.picName ?: "") }
    var phone by remember { mutableStateOf(vendor?.phoneNumber ?: "") }
    var ig by remember { mutableStateOf(vendor?.instagramHandle ?: "") }
    var contractValue by remember {
        mutableStateOf(if (vendor != null && vendor.contractValue > 0) vendor.contractValue.toLong().toString() else "")
    }
    var notes by remember { mutableStateOf(vendor?.notes ?: "") }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Vendor" else "Tambah Vendor",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category Choice Chips (Scrollable)
                Text("Kategori Vendor", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableCategories) { (key, label) ->
                        FilterChip(
                            selected = selectedCat == key,
                            onClick = { selectedCat = key },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Vendor Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; submitted = false },
                    label = { Text("Nama Vendor") },
                    placeholder = { Text("Contoh: Diamond Ballroom, MUA By Ayu") },
                    isError = submitted && name.isBlank(),
                    supportingText = { if (submitted && name.isBlank()) Text("Nama vendor wajib diisi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // PIC Name
                OutlinedTextField(
                    value = pic,
                    onValueChange = { pic = it },
                    label = { Text("Nama Contact Person (PIC)") },
                    placeholder = { Text("Contoh: Mbak Rina") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Phone / WhatsApp
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. HP / WhatsApp") },
                    placeholder = { Text("081234567890") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Instagram
                OutlinedTextField(
                    value = ig,
                    onValueChange = { ig = it },
                    label = { Text("Instagram (Opsional)") },
                    placeholder = { Text("username_vendor") },
                    prefix = { Text("@") },
                    leadingIcon = { Icon(Icons.Default.PhotoCamera, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Contract Value
                OutlinedTextField(
                    value = contractValue,
                    onValueChange = { contractValue = it.filter { c -> c.isDigit() } },
                    label = { Text("Nilai Kontrak / Biaya (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    visualTransformation = com.trackit.app.ui.transaction.ThousandSeparatorVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Fasilitas Termasuk") },
                    placeholder = { Text("Paket full dekor, free photobooth...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitted = true
                    if (name.isNotBlank()) {
                        onSave(
                            selectedCat,
                            name.trim(),
                            pic.ifBlank { null },
                            phone.ifBlank { null },
                            ig.ifBlank { null },
                            contractValue.toDoubleOrNull() ?: 0.0,
                            notes.ifBlank { null }
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isEditing) "Simpan Perubahan" else "Tambah Vendor")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
