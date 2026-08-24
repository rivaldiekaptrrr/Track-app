package com.trackit.app.ui.wedding.guests

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.WeddingGuestEntity
import com.trackit.app.util.ContactUtils
import com.trackit.app.util.DeviceContact
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingGuestsScreen(
    weddingProfileId: String,
    onNavigateBack: () -> Unit,
    viewModel: WeddingGuestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var showCateringCalc by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0=Daftar, 1=Kalkulator Katering
    var showContactPicker by remember { mutableStateOf(false) }
    var deviceContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var contactsLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var editingGuest by remember { mutableStateOf<WeddingGuestEntity?>(null) }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contactsLoading = true
            scope.launch {
                deviceContacts = ContactUtils.getDeviceContacts(context)
                contactsLoading = false
                showContactPicker = true
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Izin kontak diperlukan untuk mengimpor. Aktifkan di Pengaturan aplikasi.")
            }
        }
    }

    fun launchContactImport() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            contactsLoading = true
            scope.launch {
                deviceContacts = ContactUtils.getDeviceContacts(context)
                contactsLoading = false
                showContactPicker = true
            }
        } else {
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(weddingProfileId) {
        viewModel.loadForProfile(weddingProfileId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Manajemen Tamu", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.totalGuests} tamu · ${uiState.totalPax} estimasi pax",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (contactsLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { launchContactImport() }) {
                            Icon(Icons.Default.Contacts, contentDescription = "Impor Kontak")
                        }
                    }
                    IconButton(onClick = { activeTab = if (activeTab == 0) 1 else 0 }) {
                        Icon(
                            if (activeTab == 0) Icons.Default.Restaurant else Icons.Default.People,
                            contentDescription = "Toggle view"
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.PersonAdd, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (activeTab == 1) {
            // === Kalkulator Katering ===
            CateringCalculatorTab(
                uiState = uiState,
                modifier = Modifier.padding(padding),
                onBufferChange = { viewModel.setBufferPct(it) },
                onStallsChange = { viewModel.setActiveStalls(it) }
            )
        } else {
            // === Daftar Tamu ===
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Summary breakdown by group
                item {
                    GuestSummaryCard(uiState = uiState)
                }

                // Filters
                item {
                    GuestFilters(
                        uiState = uiState,
                        onGroupFilter = { viewModel.setGroupFilter(it) },
                        onRsvpFilter = { viewModel.setRsvpFilter(it) }
                    )
                }

                if (uiState.filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.People, null, Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada tamu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { showAddDialog = true }) { Text("Tambah Tamu") }
                            }
                        }
                    }
                } else {
                    items(uiState.filtered, key = { it.guestId }) { guest ->
                        GuestItem(
                            guest = guest,
                            onRsvpChange = { status -> viewModel.updateRsvp(guest, status) },
                            onDelete = { viewModel.deleteGuest(guest) },
                            onClick = { editingGuest = guest }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGuestDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, phone, group, session, pax ->
                viewModel.addGuest(weddingProfileId, name, phone, group, session, pax)
                showAddDialog = false
            }
        )
    }

    if (showContactPicker) {
        ContactPickerDialog(
            contacts = deviceContacts,
            onDismiss = { showContactPicker = false },
            onConfirm = { selected ->
                viewModel.addMultipleGuests(weddingProfileId, selected)
                showContactPicker = false
                scope.launch {
                    snackbarHostState.showSnackbar("${selected.size} kontak berhasil diimpor!")
                }
            }
        )
    }

    editingGuest?.let { guest ->
        EditGuestDialog(
            guest = guest,
            onDismiss = { editingGuest = null },
            onConfirm = { name, phone, group, session, pax ->
                viewModel.updateGuest(guest, name, phone, group, session, pax)
                editingGuest = null
                scope.launch {
                    snackbarHostState.showSnackbar("Kontak berhasil diperbarui!")
                }
            }
        )
    }
}

@Composable
private fun GuestSummaryCard(uiState: WeddingGuestsUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GuestFigure("Total Tamu", "${uiState.totalGuests}")
                GuestFigure("Est. Pax", "${uiState.totalPax}")
                GuestFigure("Hadir (RSVP)", "${uiState.attendingCount}")
            }
            Spacer(Modifier.height(12.dp))
            Text("Distribusi per Kelompok", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            uiState.byGroup.forEach { (group, pax) ->
                val label = GUEST_GROUPS.find { it.first == group }?.second ?: group
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(label, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$pax pax", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun GuestFigure(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GuestFilters(
    uiState: WeddingGuestsUiState,
    onGroupFilter: (String) -> Unit,
    onRsvpFilter: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        // Group filter
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(selected = uiState.filterGroup == "ALL",
                    onClick = { onGroupFilter("ALL") }, label = { Text("Semua") })
            }
            items(GUEST_GROUPS) { (key, label) ->
                FilterChip(selected = uiState.filterGroup == key,
                    onClick = { onGroupFilter(key) }, label = { Text(label) })
            }
        }
        // RSVP filter
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(selected = uiState.filterRsvp == "ALL",
                    onClick = { onRsvpFilter("ALL") }, label = { Text("Semua RSVP") })
            }
            items(listOf("PENDING" to "Menunggu", "ATTENDING" to "Hadir", "DECLINED" to "Tidak Hadir")) { (key, label) ->
                FilterChip(selected = uiState.filterRsvp == key,
                    onClick = { onRsvpFilter(key) }, label = { Text(label) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuestItem(
    guest: WeddingGuestEntity,
    onRsvpChange: (String) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var rsvpExpanded by remember { mutableStateOf(false) }
    
    // RSVP visual assets
    val (rsvpBg, rsvpText, rsvpLabel) = when (guest.rsvpStatus) {
        "ATTENDING" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Hadir")
        "DECLINED" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Tidak Hadir")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.onSurfaceVariant, "Menunggu")
    }

    val groupLabel = GUEST_GROUPS.find { it.first == guest.groupAllocation }?.second ?: guest.groupAllocation
    val sessionLabel = GUEST_SESSIONS.find { it.first == guest.sessionTarget }?.second ?: guest.sessionTarget

    // Group badge colors
    val (groupBg, groupText) = when (guest.groupAllocation) {
        "VIP" -> Pair(Color(0xFFFFF9C4), Color(0xFFF57F17)) // Golden for VIP
        "KELUARGA_CPP", "KELUARGA_CPW" -> Pair(Color(0xFFE1F5FE), Color(0xFF0288D1)) // Light Blue
        else -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2)) // Light Purple for others
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Accent Bar (RSVP based)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(rsvpText)
            )
            
            Spacer(Modifier.width(10.dp))

            // Avatar initial
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = groupBg
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        guest.guestName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = groupText
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Guest Info (Center)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Row 1: Name and Pax Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = guest.guestName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    // Compact Pax Badge
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${guest.estimatedPax} Pax",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // Row 2: Phone number (optional)
                if (!guest.phoneNumber.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = guest.phoneNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Row 3: Badges Row (Group & Session)
                // Use Experimental Layout FlowRow if we had it, but standard row with weight is safe here.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Group Badge
                    Surface(
                        color = groupBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = groupLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = groupText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Session Badge
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = sessionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            // Right side actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                // RSVP Menu Action
                Box {
                    Surface(
                        color = rsvpBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { rsvpExpanded = true },
                        border = BorderStroke(0.5.dp, rsvpText.copy(alpha = 0.2f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = rsvpLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = rsvpText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = rsvpText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = rsvpExpanded,
                        onDismissRequest = { rsvpExpanded = false }
                    ) {
                        listOf("PENDING" to "Menunggu", "ATTENDING" to "Hadir", "DECLINED" to "Tidak Hadir")
                            .forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onRsvpChange(key)
                                        rsvpExpanded = false
                                    }
                                )
                            }
                    }
                }
                
                Spacer(Modifier.height(12.dp))

                // Delete action button
                IconButton(
                    onClick = onDelete,
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
    }
}

@Composable
private fun CateringCalculatorTab(
    uiState: WeddingGuestsUiState,
    modifier: Modifier = Modifier,
    onBufferChange: (Float) -> Unit,
    onStallsChange: (Int) -> Unit
) {
    val calc = uiState.cateringCalc
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Kalkulator Katering",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Berdasarkan PRD §8 — rumus yang sudah dikoreksi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Input
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Parameter", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    // Buffer pct slider
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Buffer no-show", style = MaterialTheme.typography.bodySmall)
                            Text("${(uiState.bufferPct * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = uiState.bufferPct,
                            onValueChange = onBufferChange,
                            valueRange = 0.05f..0.30f,
                            steps = 4
                        )
                        Text("Default 10%. Tamu RSVP biasanya tidak semua datang.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Active stalls
                    Column {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Jumlah booth gubukan", style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onStallsChange(uiState.activeStalls - 1) },
                                    modifier = Modifier.size(32.dp)
                                ) { Icon(Icons.Default.Remove, null, Modifier.size(16.dp)) }
                                Text("${uiState.activeStalls}", fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(
                                    onClick = { onStallsChange(uiState.activeStalls + 1) },
                                    modifier = Modifier.size(32.dp)
                                ) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
                            }
                        }
                    }
                }
            }
        }

        // Result
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hasil Kalkulasi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)

                    CalcRow("Jumlah tamu tercatat", "${calc.totalGuests} tamu")
                    CalcRow("Total estimasi pax", "${calc.totalPax} pax")
                    CalcRow("Setelah buffer ${(calc.bufferPct * 100).roundToInt()}%", "${calc.effectivePax} pax efektif")
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    CalcRow("Porsi Prasmanan (60%)", "≈ ${calc.buffetPortions} porsi", bold = true)
                    CalcRow("Per booth Gubukan (${calc.activeStalls} booth)",
                        "≈ ${calc.gubukPortions} porsi/booth", bold = true)
                }
            }
        }

        // Formula explanation
        item {
            Card(shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Rumus yang Dipakai", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Total Porsi = Σ pax × (1 − buffer%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text("Prasmanan = 60% × Total Porsi",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text("Per Booth = 40% × Total Porsi ÷ Jumlah Booth",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun CalcRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGuestDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String?, group: String, session: String, pax: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("TEMAN_CPP") }
    var selectedSession by remember { mutableStateOf("KEDUANYA") }
    var pax by remember { mutableStateOf(2) }
    var groupExpanded by remember { mutableStateOf(false) }
    var sessionExpanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Tamu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it; submitted = false },
                    label = { Text("Nama Tamu / Keluarga") }, 
                    isError = submitted && name.isBlank(),
                    supportingText = { if (submitted && name.isBlank()) Text("Nama tamu wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it },
                    label = { Text("No. HP (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Group dropdown
                ExposedDropdownMenuBox(expanded = groupExpanded, onExpandedChange = { groupExpanded = it }) {
                    OutlinedTextField(
                        value = GUEST_GROUPS.find { it.first == selectedGroup }?.second ?: "",
                        onValueChange = {}, label = { Text("Kelompok") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                        GUEST_GROUPS.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedGroup = key; groupExpanded = false })
                        }
                    }
                }

                // Session dropdown
                ExposedDropdownMenuBox(expanded = sessionExpanded, onExpandedChange = { sessionExpanded = it }) {
                    OutlinedTextField(
                        value = GUEST_SESSIONS.find { it.first == selectedSession }?.second ?: "",
                        onValueChange = {}, label = { Text("Sesi Acara") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sessionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = sessionExpanded, onDismissRequest = { sessionExpanded = false }) {
                        GUEST_SESSIONS.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedSession = key; sessionExpanded = false })
                        }
                    }
                }

                // Pax counter
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estimasi pax", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { pax = (pax - 1).coerceAtLeast(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, null, Modifier.size(16.dp))
                    }
                    Text("$pax", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    IconButton(onClick = { pax++ }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                submitted = true
                if (name.isNotBlank()) {
                    onAdd(name.trim(), phone.ifBlank { null }, selectedGroup, selectedSession, pax)
                }
            }) { Text("Tambah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditGuestDialog(
    guest: WeddingGuestEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?, group: String, session: String, pax: Int) -> Unit
) {
    var name by remember { mutableStateOf(guest.guestName) }
    var phone by remember { mutableStateOf(guest.phoneNumber ?: "") }
    var selectedGroup by remember { mutableStateOf(guest.groupAllocation) }
    var selectedSession by remember { mutableStateOf(guest.sessionTarget) }
    var pax by remember { mutableStateOf(guest.estimatedPax) }
    var groupExpanded by remember { mutableStateOf(false) }
    var sessionExpanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tamu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it; submitted = false },
                    label = { Text("Nama Tamu / Keluarga") }, 
                    isError = submitted && name.isBlank(),
                    supportingText = { if (submitted && name.isBlank()) Text("Nama tamu wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it },
                    label = { Text("No. HP (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Group dropdown
                ExposedDropdownMenuBox(expanded = groupExpanded, onExpandedChange = { groupExpanded = it }) {
                    OutlinedTextField(
                        value = GUEST_GROUPS.find { it.first == selectedGroup }?.second ?: "",
                        onValueChange = {}, label = { Text("Kelompok") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                        GUEST_GROUPS.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedGroup = key; groupExpanded = false })
                        }
                    }
                }

                // Session dropdown
                ExposedDropdownMenuBox(expanded = sessionExpanded, onExpandedChange = { sessionExpanded = it }) {
                    OutlinedTextField(
                        value = GUEST_SESSIONS.find { it.first == selectedSession }?.second ?: "",
                        onValueChange = {}, label = { Text("Sesi Acara") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sessionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = sessionExpanded, onDismissRequest = { sessionExpanded = false }) {
                        GUEST_SESSIONS.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedSession = key; sessionExpanded = false })
                        }
                    }
                }

                // Pax counter
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estimasi pax", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { pax = (pax - 1).coerceAtLeast(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, null, Modifier.size(16.dp))
                    }
                    Text("$pax", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    IconButton(onClick = { pax++ }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                submitted = true
                if (name.isNotBlank()) {
                    onConfirm(name.trim(), phone.ifBlank { null }, selectedGroup, selectedSession, pax)
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

