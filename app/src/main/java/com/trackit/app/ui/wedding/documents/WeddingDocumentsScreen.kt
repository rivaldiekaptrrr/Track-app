package com.trackit.app.ui.wedding.documents

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.WeddingDocumentEntity
import com.trackit.app.ui.transaction.ThousandSeparatorVisualTransformation
import com.trackit.app.ui.wedding.common.DeleteConfirmDialog
import com.trackit.app.ui.wedding.common.WeddingScreenGuideDialog
import com.trackit.app.ui.wedding.common.WeddingGuideFeature
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
    var showGuideDialog by remember { mutableStateOf(false) }
    var editingDoc by remember { mutableStateOf<WeddingDocumentEntity?>(null) }

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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showGuideDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Panduan Fitur")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Berkas")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Hero Progress Card
                item {
                    DocumentHeroCard(uiState = uiState)
                }

                // Filter chips in LazyRow
                item {
                    val filters = listOf(
                        "ALL" to "Semua",
                        "GROOM" to "CPP",
                        "BRIDE" to "CPW",
                        "BOTH" to "Bersama"
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filters) { (key, label) ->
                            val selected = uiState.filterOwner == key
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setFilter(key) },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada berkas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = { showAddDialog = true }) { Text("Tambah Berkas") }
                            }
                        }
                    }
                } else {
                    items(uiState.filtered, key = { it.docId }) { doc ->
                        DocumentItem(
                            doc = doc,
                            onClick = { editingDoc = doc },
                            onToggle = { viewModel.toggleCompleted(doc) },
                            onDelete = { viewModel.deleteDocument(doc) }
                        )
                    }
                }
            }
        }
    }

    if (showGuideDialog) {
        WeddingScreenGuideDialog(
            title = "Berkas Legalitas & KUA",
            screenPurpose = "Memastikan seluruh dokumen syarat pernikahan resmi (KUA / Catatan Sipil) lengkap dan tervalidasi sebelum batas waktu pendaftaran.",
            features = listOf(
                WeddingGuideFeature("Kategori Berkas Pihak", "Pisahkan checklist dokumen milik Pengantin Pria (CPP), Pengantin Wanita (CPW), atau Berkas Bersama."),
                WeddingGuideFeature("Daftar Syarat Resmi KUA", "Catat kelengkapan surat N1, N2, N4, surat sehat puskesmas, pas foto, dan fotokopi KTP/KK."),
                WeddingGuideFeature("Biaya Administrasi Resmi", "Catat biaya pendaftaran nikah (Rp 0 di KUA jam kerja, atau Rp 600.000 untuk nikah di luar KUA/akhir pekan)."),
                WeddingGuideFeature("Status Kelengkapan", "Tandai dokumen yang 'Sudah Siap' atau 'Belum Diurus' dengan visual progress bar.")
            ),
            proTip = "Urus surat pengantar RT/RW dan kelurahan (N1-N4) minimal 1-2 bulan sebelum akad agar jadwal penghulu KUA terkunci aman.",
            onDismiss = { showGuideDialog = false }
        )
    }

    if (showAddDialog) {
        AddEditDocumentDialog(
            document = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, owner, cost ->
                viewModel.addDocument(weddingProfileId, name, owner, cost)
                showAddDialog = false
            }
        )
    }

    editingDoc?.let { doc ->
        AddEditDocumentDialog(
            document = doc,
            onDismiss = { editingDoc = null },
            onConfirm = { name, owner, cost ->
                viewModel.updateDocument(
                    doc.copy(
                        docName = name,
                        ownerType = owner,
                        adminCost = cost
                    )
                )
                editingDoc = null
            }
        )
    }
}

@Composable
private fun DocumentHeroCard(uiState: WeddingDocumentsUiState) {
    val progress = if (uiState.totalCount > 0)
        uiState.completedCount.toFloat() / uiState.totalCount else 0f

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Kelengkapan Berkas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${uiState.completedCount} dari ${uiState.totalCount} dokumen selesai disiapkan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = if (progress >= 1f) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (progress >= 1f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress >= 1f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (uiState.totalAdminCost > 0) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MonetizationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Total Biaya Administrasi:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            CurrencyUtils.formatRupiah(uiState.totalAdminCost),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentItem(
    doc: WeddingDocumentEntity,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = doc.isCompleted, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = doc.docName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (doc.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (doc.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = "Hapus Berkas?",
            message = "Berkas \"${doc.docName}\" akan dihapus dari daftar persyaratan legalitas.",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = onDelete
        )
    }
}

@Composable
private fun AddEditDocumentDialog(
    document: WeddingDocumentEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, owner: String, cost: Double) -> Unit
) {
    var name by remember { mutableStateOf(document?.docName ?: "") }
    var selectedOwner by remember { mutableStateOf(document?.ownerType ?: "BOTH") }
    var cost by remember {
        mutableStateOf(if (document != null && document.adminCost > 0) document.adminCost.toLong().toString() else "")
    }
    var submitted by remember { mutableStateOf(false) }

    val isEdit = document != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Berkas" else "Tambah Berkas") },
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
                Text("Pemilik Berkas", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("GROOM" to "CPP", "BRIDE" to "CPW", "BOTH" to "Bersama").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedOwner == key,
                            onClick = { selectedOwner = key },
                            label = { Text(label) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it.filter { c -> c.isDigit() } },
                    label = { Text("Biaya Admin (opsional)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorVisualTransformation(),
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
                        onConfirm(name.trim(), selectedOwner, cost.toDoubleOrNull() ?: 0.0)
                    }
                }
            ) { Text(if (isEdit) "Simpan" else "Tambah") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
