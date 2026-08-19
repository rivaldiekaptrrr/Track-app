package com.trackit.app.ui.wedding.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.WeddingVendorEntity
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

    LaunchedEffect(weddingProfileId) { viewModel.loadForProfile(weddingProfileId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vendor Hub", fontWeight = FontWeight.Bold)
                        Text("${uiState.vendors.size} vendor terdaftar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                // Category filter
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(selected = uiState.filterCategory == "ALL",
                                onClick = { viewModel.setFilter("ALL") }, label = { Text("Semua") })
                        }
                        items(VENDOR_CATEGORIES) { (key, label) ->
                            FilterChip(selected = uiState.filterCategory == key,
                                onClick = { viewModel.setFilter(key) }, label = { Text(label.substring(2)) })
                        }
                    }
                }

                if (uiState.filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Store, null, Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada vendor", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { showAddDialog = true }) { Text("Tambah Vendor") }
                            }
                        }
                    }
                } else {
                    items(uiState.filtered, key = { it.vendorId }) { vendor ->
                        VendorItem(
                            vendor = vendor,
                            onStatusChange = { viewModel.updateStatus(vendor, it) },
                            onDelete = { viewModel.deleteVendor(vendor) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddVendorDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { cat, name, pic, phone, ig, value, notes ->
                viewModel.addVendor(weddingProfileId, cat, name, pic, phone, ig, value, notes)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VendorItem(
    vendor: WeddingVendorEntity,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var statusExpanded by remember { mutableStateOf(false) }
    val catLabel = VENDOR_CATEGORIES.find { it.first == vendor.category }?.second ?: vendor.category
    val statusLabel = VENDOR_STATUSES.find { it.first == vendor.status }?.second ?: vendor.status
    val statusColor = when (vendor.status) {
        "SELESAI" -> Color(0xFF2E7D32)
        "KONTRAK" -> Color(0xFF1565C0)
        "TANDA_JADI" -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(vendor.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(catLabel, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!vendor.picName.isNullOrBlank()) {
                        Text("CP: ${vendor.picName}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!vendor.phoneNumber.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(vendor.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (!vendor.instagramHandle.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("@${vendor.instagramHandle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (vendor.contractValue > 0) {
                        Text(CurrencyUtils.formatRupiah(vendor.contractValue),
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                        Surface(color = statusColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.menuAnchor()) {
                            Text(statusLabel, style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                        ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                            VENDOR_STATUSES.forEach { (key, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { onStatusChange(key); statusExpanded = false })
                            }
                        }
                    }
                }
            }
            if (!vendor.notes.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Notes, null, modifier = Modifier.size(14.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(vendor.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVendorDialog(
    onDismiss: () -> Unit,
    onAdd: (cat: String, name: String, pic: String?, phone: String?, ig: String?, value: Double, notes: String?) -> Unit
) {
    var selectedCat by remember { mutableStateOf("VENUE") }
    var name by remember { mutableStateOf("") }
    var pic by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var ig by remember { mutableStateOf("") }
    var contractValue by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Vendor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = VENDOR_CATEGORIES.find { it.first == selectedCat }?.second ?: "",
                        onValueChange = {}, label = { Text("Kategori") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        VENDOR_CATEGORIES.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedCat = key; catExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nama Vendor") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = pic, onValueChange = { pic = it },
                    label = { Text("Contact Person") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it },
                    label = { Text("No. HP / WhatsApp") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = ig, onValueChange = { ig = it },
                    label = { Text("Instagram (@username)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = contractValue, onValueChange = { contractValue = it.filter { c -> c.isDigit() } },
                    label = { Text("Nilai Kontrak (Rp)") }, prefix = { Text("Rp") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Catatan") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) {
                    onAdd(selectedCat, name.trim(),
                        pic.ifBlank { null }, phone.ifBlank { null }, ig.ifBlank { null },
                        contractValue.toDoubleOrNull() ?: 0.0, notes.ifBlank { null })
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
