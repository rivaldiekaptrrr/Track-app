package com.trackit.app.ui.wedding.seserahan

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
import com.trackit.app.data.local.entity.WeddingSeserahanEntity
import com.trackit.app.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingSeserahanScreen(
    weddingProfileId: String,
    onNavigateBack: () -> Unit,
    viewModel: WeddingSeserahanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(weddingProfileId) { viewModel.loadForProfile(weddingProfileId) }

    val filters = listOf(
        "ALL" to "Semua",
        "SESERAHAN_CPP" to "Seserahan (CPP→CPW)",
        "BALASAN_CPW" to "Balasan (CPW→CPP)",
        "MAHAR" to "Mahar"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Seserahan & Mahar", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.readyCount}/${uiState.allItems.size} siap · ${CurrencyUtils.formatRupiah(uiState.totalEstimated)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Summary cards
                item {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeserahanSummaryCard(
                            modifier = Modifier.weight(1f),
                            label = "Seserahan",
                            count = uiState.seserahanItems.size,
                            amount = uiState.seserahanEstimated,
                            color = Color(0xFFE91E63)
                        )
                        SeserahanSummaryCard(
                            modifier = Modifier.weight(1f),
                            label = "Balasan",
                            count = uiState.balasanItems.size,
                            amount = uiState.balasanEstimated,
                            color = Color(0xFF9C27B0)
                        )
                        SeserahanSummaryCard(
                            modifier = Modifier.weight(1f),
                            label = "Mahar",
                            count = uiState.maharItems.size,
                            amount = uiState.maharItems.sumOf { it.estimatedPrice },
                            color = Color(0xFFFF9800)
                        )
                    }
                }

                // Direction filter
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(filters) { (key, label) ->
                            FilterChip(selected = uiState.filterDirection == key,
                                onClick = { viewModel.setFilter(key) }, label = { Text(label) })
                        }
                    }
                }

                if (uiState.filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada item", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { showAddDialog = true }) { Text("Tambah Item") }
                            }
                        }
                    }
                } else {
                    items(uiState.filtered, key = { it.itemId }) { item ->
                        SeserahanItem(
                            item = item,
                            onStatusChange = { viewModel.updateStatus(item, it) },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSeserahanItemDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { dir, name, qty, price, notes ->
                viewModel.addItem(weddingProfileId, dir, name, qty, price, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SeserahanSummaryCard(
    modifier: Modifier, label: String, count: Int, amount: Double, color: Color
) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                Text("$count item", style = MaterialTheme.typography.labelSmall, color = color,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(CurrencyUtils.formatRupiahShort(amount),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeserahanItem(
    item: WeddingSeserahanEntity,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var statusExpanded by remember { mutableStateOf(false) }
    val statusInfo = SESERAHAN_ITEM_STATUSES.find { it.first == item.status }
    val statusColor = when (item.status) {
        "SIAP" -> Color(0xFF2E7D32)
        "WRAPPING" -> Color(0xFFE65100)
        "DIBELI" -> Color(0xFF1565C0)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val directionLabel = when (item.direction) {
        "SESERAHAN_CPP" -> "Seserahan"
        "BALASAN_CPW" -> "Balasan"
        else -> "Mahar"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.itemName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                        Text(directionLabel, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text("${item.quantity}x", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.estimatedPrice > 0) {
                        Text(CurrencyUtils.formatRupiah(item.estimatedPrice * item.quantity),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                if (!item.notes.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                    Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.menuAnchor()) {
                        Text(statusInfo?.second ?: item.status, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        SESERAHAN_ITEM_STATUSES.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { onStatusChange(key); statusExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSeserahanItemDialog(
    onDismiss: () -> Unit,
    onAdd: (direction: String, name: String, qty: Int, price: Double, notes: String?) -> Unit
) {
    var selectedDir by remember { mutableStateOf("SESERAHAN_CPP") }
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf(1) }
    var price by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val dirOptions = listOf(
        "SESERAHAN_CPP" to "Seserahan (CPP→CPW)",
        "BALASAN_CPW" to "Balasan (CPW→CPP)",
        "MAHAR" to "Mahar"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Direction chips
                Text("Jenis", style = MaterialTheme.typography.labelMedium)
                dirOptions.forEach { (key, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedDir == key, onClick = { selectedDir = key })
                        Text(label, style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it; submitted = false },
                    label = { Text("Nama Item") }, 
                    isError = submitted && name.isBlank(),
                    supportingText = { if (submitted && name.isBlank()) Text("Nama item wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Qty", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(30.dp))
                    IconButton(onClick = { qty = (qty - 1).coerceAtLeast(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, null, Modifier.size(16.dp))
                    }
                    Text("$qty", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    IconButton(onClick = { qty++ }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    }
                }
                OutlinedTextField(value = price, onValueChange = { price = it.filter { c -> c.isDigit() } },
                    label = { Text("Estimasi Harga (Rp/item)") }, prefix = { Text("Rp") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    visualTransformation = com.trackit.app.ui.transaction.ThousandSeparatorVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Catatan (opsional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = {
                submitted = true
                if (name.isNotBlank()) {
                    onAdd(selectedDir, name.trim(), qty, price.toDoubleOrNull() ?: 0.0, notes.ifBlank { null })
                }
            }) { Text("Tambah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
