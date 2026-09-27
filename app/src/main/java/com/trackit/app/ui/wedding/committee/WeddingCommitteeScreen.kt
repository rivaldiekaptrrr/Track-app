package com.trackit.app.ui.wedding.committee

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
import com.trackit.app.data.local.entity.WeddingCommitteeEntity
import com.trackit.app.ui.wedding.common.DeleteConfirmDialog
import com.trackit.app.ui.wedding.common.WeddingScreenGuideDialog
import com.trackit.app.ui.wedding.common.WeddingGuideFeature
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingCommitteeScreen(
    weddingProfileId: String,
    onNavigateBack: () -> Unit,
    viewModel: WeddingCommitteeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<WeddingCommitteeEntity?>(null) }
    var memberForStatusChange by remember { mutableStateOf<WeddingCommitteeEntity?>(null) }
    var memberToDelete by remember { mutableStateOf<WeddingCommitteeEntity?>(null) }

    LaunchedEffect(weddingProfileId) { viewModel.loadForProfile(weddingProfileId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
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
                Icon(Icons.Default.Add, contentDescription = "Tambah Anggota")
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
                    CommitteeHeroCard(uiState = uiState)
                }

                // 2. Filter Chips
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.filterSide == "ALL",
                                onClick = { viewModel.setFilter("ALL") },
                                label = { Text("Semua (${uiState.members.size})") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        items(COMMITTEE_SIDES) { (key, label) ->
                            val count = uiState.members.count { it.side == key }
                            FilterChip(
                                selected = uiState.filterSide == key,
                                onClick = { viewModel.setFilter(key) },
                                label = { Text("$label ($count)") },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // 3. Members List / Empty State
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
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = if (uiState.filterSide == "ALL") "Belum ada anggota panitia"
                                           else "Tidak ada anggota untuk filter ini",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                FilledTonalButton(onClick = { showAddDialog = true }) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Tambah Anggota Baru")
                                }
                            }
                        }
                    }
                } else {
                    items(uiState.filtered, key = { it.memberId }) { member ->
                        CommitteeMemberCard(
                            member = member,
                            onStatusClick = { memberForStatusChange = member },
                            onEdit = { editingMember = member },
                            onDelete = { memberToDelete = member }
                        )
                    }
                }
            }
        }
    }

    if (showGuideDialog) {
        WeddingScreenGuideDialog(
            title = "Panitia Keluarga & Seragam",
            screenPurpose = "Mendata susunan panitia keluarga dan panitia inti, membagi tugas per seksi acara, serta mengontrol distribusi bahan kain dan kesiapan seragam.",
            features = listOf(
                WeddingGuideFeature("Pembagian Pihak & Seksi", "Pisahkan panitia pihak Pria (CPP), Wanita (CPW), atau Bersama, lalu kelompokkan ke seksi (Among Tamu, Konsumsi, Acara, dll)."),
                WeddingGuideFeature("Kontak Cepat WhatsApp", "Hubungi panitia keluarga langsung via WhatsApp dalam sekali klik untuk koordinasi cepat."),
                WeddingGuideFeature("Pelacak Seragam & Kain", "Catat kebutuhan panjang kain (meter), jenis pakaian, dan status jahitan/kesiapan seragam."),
                WeddingGuideFeature("Ringkasan Kesiapan Seragam", "Pantau persentase seragam yang sudah siap dipakai menjelang hari H.")
            ),
            proTip = "Tunjuk 1 koordinator utama untuk setiap seksi agar alur instruksi dan pembagian seragam tidak tumpang tindih.",
            onDismiss = { showGuideDialog = false }
        )
    }

    // Status Selection Dialog
    memberForStatusChange?.let { member ->
        UniformStatusDialog(
            currentStatus = member.uniformStatus,
            onDismiss = { memberForStatusChange = null },
            onSelect = { newStatus ->
                viewModel.updateUniformStatus(member, newStatus)
                memberForStatusChange = null
            }
        )
    }

    // Add Member Dialog
    if (showAddDialog) {
        AddEditMemberDialog(
            member = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, role, side, phone, uniformDesc, fabric ->
                viewModel.addMember(weddingProfileId, name, role, side, phone, uniformDesc, fabric)
                showAddDialog = false
            }
        )
    }

    // Edit Member Dialog
    editingMember?.let { member ->
        AddEditMemberDialog(
            member = member,
            onDismiss = { editingMember = null },
            onSave = { name, role, side, phone, uniformDesc, fabric ->
                viewModel.updateMember(member, name, role, side, phone, uniformDesc, fabric)
                editingMember = null
            }
        )
    }

    // Delete Confirmation Dialog
    memberToDelete?.let { member ->
        DeleteConfirmDialog(
            title = "Hapus Anggota?",
            message = "Apakah Anda yakin ingin menghapus '${member.memberName}' dari susunan panitia?",
            onDismiss = { memberToDelete = null },
            onConfirm = {
                viewModel.deleteMember(member)
                memberToDelete = null
            }
        )
    }
}

@Composable
private fun CommitteeHeroCard(uiState: WeddingCommitteeUiState) {
    val totalCount = uiState.members.size
    val readyCount = uiState.readyCount
    val progress = if (totalCount > 0) readyCount.toFloat() / totalCount else 0f
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
            // Row 1: Ready Status & Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kesiapan Seragam Panitia",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$readyCount dari $totalCount Siap",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (percent == 100 && totalCount > 0) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (percent == 100 && totalCount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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
                color = if (percent == 100 && totalCount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Row 2: Fabric Total & Status Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Checkroom, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Total Kain: ${uiState.totalFabric} m",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                        Text("Siap: $readyCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE65100)))
                        Text("Jahit: ${uiState.members.count { it.uniformStatus == "SEDANG_JAHIT" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommitteeMemberCard(
    member: WeddingCommitteeEntity,
    onStatusClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val sideLabel = COMMITTEE_SIDES.find { it.first == member.side }?.second ?: member.side
    val uniformStatusInfo = UNIFORM_STATUSES.find { it.first == member.uniformStatus }
    val (statusColor, statusBg) = when (member.uniformStatus) {
        "SIAP_PAKAI" -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
        "SEDANG_JAHIT" -> Pair(Color(0xFFE65100), Color(0xFFFFF3E0))
        else -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    }

    val (sideBg, sideColor) = when (member.side) {
        "KELUARGA_CPP" -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        "KELUARGA_CPW" -> Pair(Color(0xFFFCE4EC), Color(0xFFC2185B))
        "TEMAN_CPP" -> Pair(Color(0xFFE0F2F1), Color(0xFF00796B))
        else -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2))
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar with Initial
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = sideBg
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = member.memberName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = sideColor
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Name & Role & Side Tag
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.memberName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = member.role,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = sideBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = sideLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = sideColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Uniform & Contact details
            if (!member.uniformDescription.isNullOrBlank() || member.fabricMeters > 0 || !member.phoneNumber.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!member.uniformDescription.isNullOrBlank() || member.fabricMeters > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Checkroom, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${member.uniformDescription ?: "Seragam"}${if (member.fabricMeters > 0) " (${member.fabricMeters}m)" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!member.phoneNumber.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = member.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Bottom Row: Uniform Status Pill (Left) & Quick Action Buttons (Right)
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
                            text = uniformStatusInfo?.second ?: member.uniformStatus,
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

                // Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!member.phoneNumber.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                val cleanPhone = member.phoneNumber.replace(Regex("[^0-9]"), "")
                                val formattedPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedPhone"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = "WhatsApp",
                                tint = Color(0xFF25D366),
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
private fun UniformStatusDialog(
    currentStatus: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Status Seragam", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UNIFORM_STATUSES.forEach { (key, label) ->
                    val isSelected = currentStatus == key
                    val (statusColor, statusBg) = when (key) {
                        "SIAP_PAKAI" -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
                        "SEDANG_JAHIT" -> Pair(Color(0xFFE65100), Color(0xFFFFF3E0))
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
private fun AddEditMemberDialog(
    member: WeddingCommitteeEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, role: String, side: String, phone: String?, uniformDesc: String?, fabric: Double) -> Unit
) {
    val isEditing = member != null
    var name by remember { mutableStateOf(member?.memberName ?: "") }
    var role by remember { mutableStateOf(member?.role ?: "") }
    var selectedSide by remember { mutableStateOf(member?.side ?: "KELUARGA_CPP") }
    var phone by remember { mutableStateOf(member?.phoneNumber ?: "") }
    var uniformDesc by remember { mutableStateOf(member?.uniformDescription ?: "") }
    var fabric by remember {
        mutableStateOf(if (member != null && member.fabricMeters > 0) member.fabricMeters.toString() else "")
    }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Anggota Panitia" else "Tambah Anggota Panitia",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Side Selection Choice Chips
                Text("Pihak Keluarga / Tim", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(COMMITTEE_SIDES) { (key, label) ->
                        FilterChip(
                            selected = selectedSide == key,
                            onClick = { selectedSide = key },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Member Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; submitted = false },
                    label = { Text("Nama Anggota") },
                    placeholder = { Text("Contoh: Paman Budi, Tante Siti") },
                    isError = submitted && name.isBlank(),
                    supportingText = { if (submitted && name.isBlank()) Text("Nama wajib diisi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Role
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it; submitted = false },
                    label = { Text("Peran / Tugas") },
                    placeholder = { Text("Saksi Nikah, Penerima Tamu, MC, dll") },
                    isError = submitted && role.isBlank(),
                    supportingText = { if (submitted && role.isBlank()) Text("Peran wajib diisi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Phone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. HP / WhatsApp (Opsional)") },
                    placeholder = { Text("081234567890") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Uniform Description
                OutlinedTextField(
                    value = uniformDesc,
                    onValueChange = { uniformDesc = it },
                    label = { Text("Deskripsi Seragam (Opsional)") },
                    placeholder = { Text("Batik Cokelat Lengan Panjang") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Fabric Meters
                OutlinedTextField(
                    value = fabric,
                    onValueChange = { fabric = it.replace(",", ".").filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Jatah Kain (Opsional)") },
                    placeholder = { Text("2.5") },
                    suffix = { Text("meter") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitted = true
                    if (name.isNotBlank() && role.isNotBlank()) {
                        onSave(
                            name.trim(),
                            role.trim(),
                            selectedSide,
                            phone.ifBlank { null },
                            uniformDesc.ifBlank { null },
                            fabric.toDoubleOrNull() ?: 0.0
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isEditing) "Simpan Perubahan" else "Tambah Anggota")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
