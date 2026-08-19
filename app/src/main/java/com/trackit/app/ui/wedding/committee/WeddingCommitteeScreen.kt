package com.trackit.app.ui.wedding.committee

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
import com.trackit.app.data.local.entity.WeddingCommitteeEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingCommitteeScreen(
    weddingProfileId: String,
    onNavigateBack: () -> Unit,
    viewModel: WeddingCommitteeViewModel = hiltViewModel()
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
                        Text("Panitia & Seragam", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.members.size} anggota · ${uiState.readyCount} seragam siap · ${uiState.totalFabric}m kain",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = { IconButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.PersonAdd, null) } },
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
                // Side filter
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(selected = uiState.filterSide == "ALL",
                                onClick = { viewModel.setFilter("ALL") }, label = { Text("Semua") })
                        }
                        items(COMMITTEE_SIDES) { (key, label) ->
                            FilterChip(selected = uiState.filterSide == key,
                                onClick = { viewModel.setFilter(key) }, label = { Text(label) })
                        }
                    }
                }

                // Fabric total info card
                if (uiState.totalFabric > 0) {
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Total Kain", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${uiState.totalFabric} meter", fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Seragam Siap", style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${uiState.readyCount}/${uiState.members.size}", fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                if (uiState.filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Group, null, Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada anggota panitia", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { showAddDialog = true }) { Text("Tambah Anggota") }
                            }
                        }
                    }
                } else {
                    items(uiState.filtered, key = { it.memberId }) { member ->
                        CommitteeMemberItem(
                            member = member,
                            onStatusChange = { viewModel.updateUniformStatus(member, it) },
                            onDelete = { viewModel.deleteMember(member) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMemberDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, role, side, phone, uniformDesc, fabric ->
                viewModel.addMember(weddingProfileId, name, role, side, phone, uniformDesc, fabric)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommitteeMemberItem(
    member: WeddingCommitteeEntity,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var statusExpanded by remember { mutableStateOf(false) }
    val sideLabel = COMMITTEE_SIDES.find { it.first == member.side }?.second ?: member.side
    val uniformStatusInfo = UNIFORM_STATUSES.find { it.first == member.uniformStatus }
    val statusColor = when (member.uniformStatus) {
        "SIAP_PAKAI" -> Color(0xFF2E7D32)
        "SEDANG_JAHIT" -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp), shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(member.memberName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.memberName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(member.role, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(sideLabel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!member.phoneNumber.isNullOrBlank()) {
                        Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(member.phoneNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!member.uniformDescription.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Checkroom, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${member.uniformDescription}${if (member.fabricMeters > 0) " · ${member.fabricMeters}m" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                    Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.menuAnchor()) {
                        Text(uniformStatusInfo?.second ?: member.uniformStatus,
                            style = MaterialTheme.typography.labelSmall, color = statusColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        UNIFORM_STATUSES.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { onStatusChange(key); statusExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, role: String, side: String, phone: String?, uniformDesc: String?, fabric: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var selectedSide by remember { mutableStateOf("KELUARGA_CPP") }
    var phone by remember { mutableStateOf("") }
    var uniformDesc by remember { mutableStateOf("") }
    var fabric by remember { mutableStateOf("") }
    var sideExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Anggota Panitia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nama") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = role, onValueChange = { role = it },
                    label = { Text("Peran (misal: Saksi, Sambutan, MC)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)

                ExposedDropdownMenuBox(expanded = sideExpanded, onExpandedChange = { sideExpanded = it }) {
                    OutlinedTextField(
                        value = COMMITTEE_SIDES.find { it.first == selectedSide }?.second ?: "",
                        onValueChange = {}, label = { Text("Pihak") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sideExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = sideExpanded, onDismissRequest = { sideExpanded = false }) {
                        COMMITTEE_SIDES.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedSide = key; sideExpanded = false })
                        }
                    }
                }
                OutlinedTextField(value = phone, onValueChange = { phone = it },
                    label = { Text("No. HP (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = uniformDesc, onValueChange = { uniformDesc = it },
                    label = { Text("Deskripsi Seragam (misal: Batik Hijau)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = fabric, onValueChange = { fabric = it.replace(",", ".").filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Jatah Kain (meter)") }, suffix = { Text("m") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && role.isNotBlank()) {
                    onAdd(name.trim(), role.trim(), selectedSide,
                        phone.ifBlank { null }, uniformDesc.ifBlank { null },
                        fabric.toDoubleOrNull() ?: 0.0)
                }
            }) { Text("Tambah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
