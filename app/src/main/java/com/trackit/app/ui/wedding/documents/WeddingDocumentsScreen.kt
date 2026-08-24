package com.trackit.app.ui.wedding.documents

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.WeddingDocumentEntity
import com.trackit.app.util.CurrencyUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingDocumentsScreen(
    weddingProfileId: String,
    onNavigateBack: () -> Unit,
    viewModel: WeddingDocumentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(weddingProfileId) {
        viewModel.loadForProfile(weddingProfileId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Berkas Legalitas", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.completedCount}/${uiState.totalCount} selesai",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Progress bar
                item {
                    val progress = if (uiState.totalCount > 0)
                        uiState.completedCount.toFloat() / uiState.totalCount else 0f
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kelengkapan Berkas", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${(progress * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (progress >= 1f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                        if (uiState.totalAdminCost > 0) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Est. biaya administrasi: ${CurrencyUtils.formatRupiah(uiState.totalAdminCost)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Filter chips
                item {
                    val filters = listOf("ALL" to "Semua", "GROOM" to "CPP", "BRIDE" to "CPW", "BOTH" to "Bersama")
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filters.forEach { (key, label) ->
                            FilterChip(
                                selected = uiState.filterOwner == key,
                                onClick = { viewModel.setFilter(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (uiState.filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.FolderOpen, null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada berkas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { showAddDialog = true }) { Text("Tambah Berkas") }
                            }
                        }
                    }
                } else {
                    items(uiState.filtered, key = { it.docId }) { doc ->
                        DocumentItem(
                            doc = doc,
                            onToggle = { viewModel.toggleCompleted(doc) },
                            onDelete = { viewModel.deleteDocument(doc) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDocumentDialog(
            weddingProfileId = weddingProfileId,
            onDismiss = { showAddDialog = false },
            onAdd = { name, owner, cost ->
                viewModel.addDocument(weddingProfileId, name, owner, cost)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun DocumentItem(
    doc: WeddingDocumentEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (doc.isCompleted) Color(0xFF1B5E20).copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surface,
        label = "doc_bg"
    )
    val ownerColor = when (doc.ownerType) {
        "GROOM" -> Color(0xFF1565C0)
        "BRIDE" -> Color(0xFFC62828)
        else -> Color(0xFF6A1B9A)
    }
    val ownerLabel = when (doc.ownerType) {
        "GROOM" -> "CPP"
        "BRIDE" -> "CPW"
        else -> "Bersama"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bgColor)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = doc.isCompleted, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    doc.docName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (doc.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (doc.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = ownerColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            ownerLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = ownerColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (doc.adminCost > 0) {
                        Text(
                            CurrencyUtils.formatRupiah(doc.adminCost),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AddDocumentDialog(
    weddingProfileId: String,
    onDismiss: () -> Unit,
    onAdd: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedOwner by remember { mutableStateOf("BOTH") }
    var cost by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Berkas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; submitted = false },
                    label = { Text("Nama Berkas") },
                    isError = submitted && name.isBlank(),
                    supportingText = { if (submitted && name.isBlank()) Text("Nama berkas wajib diisi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Pemilik", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("GROOM" to "CPP", "BRIDE" to "CPW", "BOTH" to "Bersama").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedOwner == key,
                            onClick = { selectedOwner = key },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it.filter { c -> c.isDigit() } },
                    label = { Text("Biaya Admin (opsional)") },
                    prefix = { Text("Rp") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    visualTransformation = com.trackit.app.ui.transaction.ThousandSeparatorVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitted = true
                    if (name.isNotBlank()) {
                        onAdd(name.trim(), selectedOwner, cost.toDoubleOrNull() ?: 0.0)
                    }
                }
            ) { Text("Tambah") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
