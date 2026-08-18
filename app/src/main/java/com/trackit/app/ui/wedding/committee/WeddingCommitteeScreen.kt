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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Column {
                                    Text("Total Kain", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("${uiState.totalFabric} meter", fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                                Column {
                                    Text("Seragam Siap", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("${uiState.readyCount}/${uiState.members.size}", fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer)
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

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp), shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(member.memberName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.memberName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(member.role, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sideLabel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!member.phoneNumber.isNullOrBlank()) {
                        Text("📞 ${member.phoneNumber}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!member.uniformDescription.isNullOrBlank()) {
                    Text("👗 ${member.uniformDescription}${if (member.fabricMeters > 0) " · ${member.fabricMeters}m" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                    Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.menuAnchor()) {
                        Text(uniformStatusInfo?.second ?: member.uniformStatus,
                            style = MaterialTheme.typography.labelSmall, color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        UNIFORM_STATUSES.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { onStatusChange(key); statusExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp))
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
