package com.trackit.app.ui.wedding.seserahan

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.WeddingSeserahanEntity
import com.trackit.app.ui.wedding.common.DeleteConfirmDialog
import com.trackit.app.ui.wedding.common.WeddingScreenGuideDialog
import com.trackit.app.ui.wedding.common.WeddingGuideFeature
import com.trackit.app.util.CurrencyUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingSeserahanScreen(
    weddingProfileId: String,
    onNavigateBack: () -> Unit,
    viewModel: WeddingSeserahanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<WeddingSeserahanEntity?>(null) }
    var itemForStatusChange by remember { mutableStateOf<WeddingSeserahanEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<WeddingSeserahanEntity?>(null) }

    LaunchedEffect(weddingProfileId) { viewModel.loadForProfile(weddingProfileId) }

    val filters = listOf(
        "ALL" to "Semua",
        "SESERAHAN_CPP" to "Seserahan (Pria)",
        "BALASAN_CPW" to "Balasan (Wanita)",
        "MAHAR" to "Mahar"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Seserahan & Mahar", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.readyCount}/${uiState.allItems.size} siap · Total ${CurrencyUtils.formatRupiah(uiState.totalEstimated)}",
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
                Icon(Icons.Default.Add, contentDescription = "Tambah Item")
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
                    SeserahanHeroCard(uiState = uiState)
                }

                // 2. Filter Chips
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(filters) { (key, label) ->
                            FilterChip(
                                selected = uiState.filterDirection == key,
                                onClick = { viewModel.setFilter(key) },
                                label = { Text(label, fontWeight = if (uiState.filterDirection == key) FontWeight.Bold else FontWeight.Normal) },
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
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = if (uiState.filterDirection == "ALL") "Belum ada item seserahan atau mahar"
                                           else "Tidak ada item untuk filter ini",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                FilledTonalButton(onClick = { showAddDialog = true }) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Tambah Item Pertama")
                                }
                            }
                        }
                    }
                } else {
                    items(uiState.filtered, key = { it.itemId }) { item ->
                        SeserahanItemCard(
                            item = item,
                            onStatusClick = { itemForStatusChange = item },
                            onEdit = { editingItem = item },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    if (showGuideDialog) {
        WeddingScreenGuideDialog(
            title = "Seserahan & Mahar",
            screenPurpose = "Mencatat dan memantau daftar barang hantaran seserahan, status pembelian & pengemasan kotak hias, serta rincian mahar pernikahan.",
            features = listOf(
                WeddingGuideFeature("Kategori Terpisah", "Kelompokkan item ke Seserahan Pria (CPP), Balasan Wanita (CPW), atau Mahar."),
                WeddingGuideFeature("Tahapan Status Kotak", "Pantau alur barang dari 'Direncanakan', 'Sudah Dibeli', 'Sedang Dihas/Dikotak', hingga 'Siap Diserahkan'."),
                WeddingGuideFeature("Pelacak Estimasi Biaya", "Kalkulasi total anggaran yang dialokasikan khusus untuk belanja seserahan & mahar."),
                WeddingGuideFeature("Rincian Mahar Lengkap", "Catat jenis mahar (emas logam mulia, uang tunai, perlengkapan ibadah) beserta catatan khususnya.")
            ),
            proTip = "Beri label penanggung jawab (CPP atau CPW) pada setiap kotak seserahan agar tidak tertukar saat prosesi serah terima hari H.",
            onDismiss = { showGuideDialog = false }
        )
    }

    // Status Selection Dialog
    itemForStatusChange?.let { item ->
        StatusSelectionDialog(
            currentStatus = item.status,
            onDismiss = { itemForStatusChange = null },
            onSelect = { newStatus ->
                viewModel.updateStatus(item, newStatus)
                itemForStatusChange = null
            }
        )
    }

    // Add Item Dialog
    if (showAddDialog) {
        AddEditSeserahanDialog(
            item = null,
            onDismiss = { showAddDialog = false },
            onSave = { dir, name, qty, price, notes ->
                viewModel.addItem(weddingProfileId, dir, name, qty, price, notes)
                showAddDialog = false
            }
        )
    }

    // Edit Item Dialog
    editingItem?.let { item ->
        AddEditSeserahanDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { dir, name, qty, price, notes ->
                viewModel.updateItem(item, dir, name, qty, price, notes)
                editingItem = null
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        DeleteConfirmDialog(
            title = "Hapus Item?",
            message = "Apakah Anda yakin ingin menghapus '${item.itemName}'? Tindakan ini tidak dapat dibatalkan.",
            onDismiss = { itemToDelete = null },
            onConfirm = {
                viewModel.deleteItem(item)
                itemToDelete = null
            }
        )
    }
}

@Composable
private fun SeserahanHeroCard(uiState: WeddingSeserahanUiState) {
    val totalItems = uiState.allItems.size
    val readyItems = uiState.readyCount
    val progress = if (totalItems > 0) readyItems.toFloat() / totalItems else 0f
    val percent = (progress * 100).roundToInt()

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
            // Row 1: Total Value & Readiness Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Nilai Seserahan & Mahar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = CurrencyUtils.formatRupiah(uiState.totalEstimated),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (percent == 100 && totalItems > 0) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "$readyItems/$totalItems Siap ($percent%)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (percent == 100 && totalItems > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
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
                color = if (percent == 100 && totalItems > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Row 2: 3 Sub-pills (Seserahan, Balasan, Mahar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricSubPill(
                    modifier = Modifier.weight(1f),
                    title = "Seserahan",
                    count = uiState.seserahanItems.size,
                    amount = uiState.seserahanEstimated,
                    badgeColor = Color(0xFFE91E63)
                )
                MetricSubPill(
                    modifier = Modifier.weight(1f),
                    title = "Balasan",
                    count = uiState.balasanItems.size,
                    amount = uiState.balasanEstimated,
                    badgeColor = Color(0xFF9C27B0)
                )
                MetricSubPill(
                    modifier = Modifier.weight(1f),
                    title = "Mahar",
                    count = uiState.maharItems.size,
                    amount = uiState.maharItems.sumOf { it.estimatedPrice * it.quantity },
                    badgeColor = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun MetricSubPill(
    modifier: Modifier,
    title: String,
    count: Int,
    amount: Double,
    badgeColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$count item",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyUtils.formatRupiahShort(amount),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SeserahanItemCard(
    item: WeddingSeserahanEntity,
    onStatusClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusInfo = SESERAHAN_ITEM_STATUSES.find { it.first == item.status }
    val (statusColor, statusBg) = when (item.status) {
        "SIAP" -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
        "WRAPPING" -> Pair(Color(0xFFE65100), Color(0xFFFFF3E0))
        "DIBELI" -> Pair(Color(0xFF1565C0), Color(0xFFE3F2FD))
        else -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    }

    val (directionLabel, directionColor) = when (item.direction) {
        "SESERAHAN_CPP" -> Pair("Seserahan", Color(0xFFE91E63))
        "BALASAN_CPW" -> Pair("Balasan", Color(0xFF9C27B0))
        else -> Pair("Mahar", Color(0xFFFF9800))
    }

    val totalPrice = item.estimatedPrice * item.quantity

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Title + Direction Badge + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = directionColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = directionLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = directionColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (item.quantity > 1) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${item.quantity} pcs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

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
                            text = statusInfo?.second ?: item.status,
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
            }

            // Row 2: Price Calculation + Notes
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Price details
                if (totalPrice > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = CurrencyUtils.formatRupiah(totalPrice),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.quantity > 1) {
                            Text(
                                text = " (${item.quantity}x @ ${CurrencyUtils.formatRupiahShort(item.estimatedPrice)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Harga belum diatur",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Right: Action buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Notes if any
            if (!item.notes.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusSelectionDialog(
    currentStatus: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Status Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SESERAHAN_ITEM_STATUSES.forEach { (key, label) ->
                    val isSelected = currentStatus == key
                    val (statusColor, statusBg) = when (key) {
                        "SIAP" -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
                        "WRAPPING" -> Pair(Color(0xFFE65100), Color(0xFFFFF3E0))
                        "DIBELI" -> Pair(Color(0xFF1565C0), Color(0xFFE3F2FD))
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
private fun AddEditSeserahanDialog(
    item: WeddingSeserahanEntity? = null,
    onDismiss: () -> Unit,
    onSave: (direction: String, name: String, qty: Int, price: Double, notes: String?) -> Unit
) {
    val isEditing = item != null
    var selectedDir by remember { mutableStateOf(item?.direction ?: "SESERAHAN_CPP") }
    var name by remember { mutableStateOf(item?.itemName ?: "") }
    var qty by remember { mutableIntStateOf(item?.quantity ?: 1) }
    var price by remember { mutableStateOf(if (item != null && item.estimatedPrice > 0) item.estimatedPrice.toLong().toString() else "") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    var submitted by remember { mutableStateOf(false) }

    val dirOptions = listOf(
        "SESERAHAN_CPP" to "Seserahan",
        "BALASAN_CPW" to "Balasan",
        "MAHAR" to "Mahar"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Item" else "Tambah Item",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Direction Choice Chips
                Text("Jenis Item", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dirOptions.forEach { (key, label) ->
                        val selected = selectedDir == key
                        FilterChip(
                            selected = selected,
                            onClick = { selectedDir = key },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Item Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; submitted = false },
                    label = { Text("Nama Item") },
                    placeholder = { Text("Contoh: Sepatu, Tas, Parfum") },
                    isError = submitted && name.isBlank(),
                    supportingText = { if (submitted && name.isBlank()) Text("Nama item wajib diisi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Quantity Stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Jumlah (Qty)", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { qty = (qty - 1).coerceAtLeast(1) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, null, Modifier.size(16.dp))
                        }
                        Text(
                            text = "$qty",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        FilledTonalIconButton(
                            onClick = { qty++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        }
                    }
                }

                // Estimated Price
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() } },
                    label = { Text("Estimasi Harga (Rp/item)") },
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
                    label = { Text("Catatan (Opsional)") },
                    placeholder = { Text("Merk, warna, atau toko...") },
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
                            selectedDir,
                            name.trim(),
                            qty,
                            price.toDoubleOrNull() ?: 0.0,
                            notes.ifBlank { null }
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isEditing) "Simpan Perubahan" else "Tambah Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
