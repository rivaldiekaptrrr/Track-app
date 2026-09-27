package com.trackit.app.ui.wedding.guests

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.WeddingGuestEntity
import com.trackit.app.util.ContactUtils
import com.trackit.app.util.DeviceContact
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.trackit.app.ui.wedding.common.DeleteConfirmDialog
import com.trackit.app.ui.wedding.common.WeddingScreenGuideDialog
import com.trackit.app.ui.wedding.common.WeddingGuideFeature
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
    var showGuideDialog by remember { mutableStateOf(false) }
    var deviceContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var selectedContactsForBatch by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var contactsLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var editingGuest by remember { mutableStateOf<WeddingGuestEntity?>(null) }

    val loadContactsAndOpenPicker: () -> Unit = {
        scope.launch {
            contactsLoading = true
            val contacts = ContactUtils.getAllContacts(context)
            contactsLoading = false
            if (contacts.isNotEmpty()) {
                deviceContacts = contacts
                showContactPicker = true
            } else {
                snackbarHostState.showSnackbar("Tidak ada kontak ber-nomor telepon yang ditemukan di HP.")
            }
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadContactsAndOpenPicker()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Izin akses kontak diperlukan untuk memilih dan mengimpor kontak sekaligus.")
            }
        }
    }

    val onImportContactsClick: () -> Unit = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            loadContactsAndOpenPicker()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
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
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") } },
                actions = {
                    IconButton(onClick = { showGuideDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Panduan Fitur")
                    }
                    if (contactsLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onImportContactsClick) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Impor Kontak HP Sekaligus")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (activeTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Tambah Tamu")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Tab Switcher: Daftar Tamu vs Kalkulator Katering
            PrimaryTabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Daftar Tamu (${uiState.totalGuests})", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium) },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Kalkulator Katering", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium) },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (activeTab == 1) {
                // === Kalkulator Katering ===
                CateringCalculatorTab(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize(),
                    onBufferChange = { viewModel.setBufferPct(it) },
                    onStallsChange = { viewModel.setActiveStalls(it) }
                )
            } else {
                // === Daftar Tamu ===
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
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
                            onSessionFilter = { viewModel.setSessionFilter(it) },
                            onRsvpFilter = { viewModel.setRsvpFilter(it) }
                        )
                    }

                    if (uiState.filtered.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.People, null, Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Belum ada tamu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(4.dp))
                                    TextButton(onClick = { showAddDialog = true }) { Text("Tambah Tamu") }
                                }
                            }
                        }
                    } else {
                        items(uiState.filtered, key = { it.guestId }) { guest ->
                            GuestItem(
                                guest = guest,
                                availableGroups = uiState.availableGroups,
                                onRsvpChange = { status -> viewModel.updateRsvp(guest, status) },
                                onDelete = { viewModel.deleteGuest(guest) },
                                onClick = { editingGuest = guest }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGuideDialog) {
        WeddingScreenGuideDialog(
            title = "Manajemen Tamu & Katering",
            screenPurpose = "Mendata seluruh tamu undangan pernikahan, memantau konfirmasi kehadiran (RSVP), serta menghitung kebutuhan porsi katering prasmanan & pondokan secara otomatis.",
            features = listOf(
                WeddingGuideFeature("Impor Kontak Massal", "Impor puluhan kontak langsung dari buku telepon HP sekaligus ke kelompok tertentu."),
                WeddingGuideFeature("Kalkulator Katering Cerdas", "Hitung otomatis porsi prasmanan (60%) dan gubukan (40%) dengan toleransi buffer no-show."),
                WeddingGuideFeature("Alokasi Kelompok & Sesi", "Bagi tamu ke dalam kelompok (Keluarga CPP/CPW, Teman, VIP) dan tentukan sesi (Akad/Resepsi)."),
                WeddingGuideFeature("Pelacak RSVP Kehadiran", "Pantau status konfirmasi tamu (Hadir, Menunggu, Tidak Hadir) untuk estimasi konsumsi yang akurat.")
            ),
            proTip = "Gunakan kategori kustom jika Anda ingin memisahkan kelompok khusus seperti 'Rekan Kantor' atau 'Alumni Kampus'.",
            onDismiss = { showGuideDialog = false }
        )
    }

    if (showAddDialog) {
        AddGuestDialog(
            availableGroups = uiState.availableGroups,
            onDismiss = { showAddDialog = false },
            onAdd = { name, phone, group, session, pax ->
                viewModel.addGuest(weddingProfileId, name, phone, group, session, pax)
                showAddDialog = false
            },
            onRenameGroup = { oldKey, newName ->
                viewModel.renameGroup(weddingProfileId, oldKey, newName)
            }
        )
    }

    if (showContactPicker) {
        ContactPickerDialog(
            contacts = deviceContacts,
            onDismiss = { showContactPicker = false },
            onConfirm = { selected ->
                showContactPicker = false
                selectedContactsForBatch = selected
            }
        )
    }

    if (selectedContactsForBatch.isNotEmpty()) {
        BatchImportConfigDialog(
            selectedContactsCount = selectedContactsForBatch.size,
            availableGroups = uiState.availableGroups,
            onDismiss = { selectedContactsForBatch = emptyList() },
            onConfirm = { group, session ->
                viewModel.addMultipleGuests(weddingProfileId, selectedContactsForBatch, group, session)
                val count = selectedContactsForBatch.size
                selectedContactsForBatch = emptyList()
                scope.launch {
                    snackbarHostState.showSnackbar("$count kontak berhasil diimpor!")
                }
            },
            onRenameGroup = { oldKey, newName ->
                viewModel.renameGroup(weddingProfileId, oldKey, newName)
            }
        )
    }

    editingGuest?.let { guest ->
        EditGuestDialog(
            guest = guest,
            availableGroups = uiState.availableGroups,
            onDismiss = { editingGuest = null },
            onConfirm = { name, phone, group, session, pax ->
                viewModel.updateGuest(guest, name, phone, group, session, pax)
                editingGuest = null
                scope.launch {
                    snackbarHostState.showSnackbar("Kontak berhasil diperbarui!")
                }
            },
            onRenameGroup = { oldKey, newName ->
                viewModel.renameGroup(weddingProfileId, oldKey, newName)
            }
        )
    }
}

@Composable
private fun GuestSummaryCard(uiState: WeddingGuestsUiState) {
    val attendancePct = if (uiState.totalGuests > 0) {
        (uiState.attendingCount.toFloat() / uiState.totalGuests.toFloat())
    } else 0f

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ringkasan Kehadiran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.attendingCount} dari ${uiState.totalGuests} tamu terkonfirmasi hadir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${(attendancePct * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { attendancePct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GuestFigure("Total Tamu", "${uiState.totalGuests}")
                VerticalDivider(modifier = Modifier.height(28.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                GuestFigure("Est. Pax", "${uiState.totalPax}")
                VerticalDivider(modifier = Modifier.height(28.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                GuestFigure("Hadir (RSVP)", "${uiState.attendingCount}")
            }

            if (uiState.byGroup.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Distribusi Kelompok:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(uiState.byGroup.toList()) { (group, pax) ->
                        val label = GUEST_GROUPS.find { it.first == group }?.second ?: group
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "$pax pax",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuestFigure(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuestFilters(
    uiState: WeddingGuestsUiState,
    onGroupFilter: (String) -> Unit,
    onSessionFilter: (String) -> Unit,
    onRsvpFilter: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: RSVP Filters
        val rsvpFilters = listOf(
            "ALL" to "Semua RSVP",
            "ATTENDING" to "Hadir",
            "PENDING" to "Menunggu",
            "DECLINED" to "Tidak Hadir"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rsvpFilters) { (key, label) ->
                val selected = uiState.filterRsvp == key
                FilterChip(
                    selected = selected,
                    onClick = { onRsvpFilter(key) },
                    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Row 2: Group and Session Filters
        val groupFilters = listOf("ALL" to "Semua Kelompok") + uiState.availableGroups
        val sessionFilters = listOf("ALL" to "Semua Sesi", "AKAD" to "Akad", "RESEPSI" to "Resepsi")

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Kelompok:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, end = 4.dp)
                )
            }
            items(groupFilters) { (key, label) ->
                val selected = uiState.filterGroup == key
                FilterChip(
                    selected = selected,
                    onClick = { onGroupFilter(key) },
                    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Spacer(Modifier.width(8.dp))
                Text(
                    "Sesi:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, end = 4.dp)
                )
            }
            items(sessionFilters) { (key, label) ->
                val selected = uiState.filterSession == key
                FilterChip(
                    selected = selected,
                    onClick = { onSessionFilter(key) },
                    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
private fun GuestItem(
    guest: WeddingGuestEntity,
    availableGroups: List<Pair<String, String>>,
    onRsvpChange: (String) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showRsvpDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    // RSVP visual assets
    val (rsvpBg, rsvpText, rsvpLabel) = when (guest.rsvpStatus) {
        "ATTENDING" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Hadir")
        "DECLINED" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Tidak Hadir")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.onSurfaceVariant, "Menunggu")
    }

    val groupLabel = availableGroups.find { it.first == guest.groupAllocation }?.second ?: guest.groupAllocation

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
                modifier = Modifier.size(42.dp),
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${guest.estimatedPax} Pax",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
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
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = guest.phoneNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Row 3: Group Badge & Session Badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = groupBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = groupLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = groupText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (guest.sessionTarget.isNotBlank() && guest.sessionTarget != "KEDUANYA") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (guest.sessionTarget == "AKAD") "Akad" else "Resepsi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right side actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                // RSVP badge - Clickable dialog trigger
                Surface(
                    color = rsvpBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { showRsvpDialog = true },
                    border = BorderStroke(0.5.dp, rsvpText.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = rsvpLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = rsvpText,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
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
                
                Spacer(Modifier.height(12.dp))

                // Delete action button
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
    }

    if (showRsvpDialog) {
        GuestRsvpSelectionDialog(
            currentStatus = guest.rsvpStatus,
            onDismiss = { showRsvpDialog = false },
            onSelect = { newStatus ->
                onRsvpChange(newStatus)
                showRsvpDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = "Hapus Tamu?",
            message = "\"${guest.guestName}\" akan dihapus dari daftar tamu permanen.",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = onDelete
        )
    }
}

@Composable
private fun GuestRsvpSelectionDialog(
    currentStatus: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val statuses = listOf(
        Triple("PENDING", "Menunggu Konfirmasi", MaterialTheme.colorScheme.surfaceVariant),
        Triple("ATTENDING", "Hadir", Color(0xFFE8F5E9)),
        Triple("DECLINED", "Tidak Hadir", Color(0xFFFFEBEE))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Status RSVP") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                statuses.forEach { (code, label, color) ->
                    val isSelected = currentStatus == code
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) color else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Terpilih",
                                    tint = MaterialTheme.colorScheme.primary,
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
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun CateringCalculatorTab(
    uiState: WeddingGuestsUiState,
    modifier: Modifier = Modifier,
    onBufferChange: (Float) -> Unit,
    onStallsChange: (Int) -> Unit
) {
    val calc = uiState.cateringCalc
    val bufferInt = (calc.bufferPct * 100).roundToInt()
    val totalGubukPortions = (calc.effectivePax * 0.40).roundToInt()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Hero Banner
        item {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Kalkulator Porsi Katering",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Estimasi cerdas prasmanan & pondokan dari ${calc.totalPax} estimasi pax",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Primary Results Cards (Side-by-Side: Prasmanan 60% & Gubukan 40%)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Prasmanan Card (60%)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "Buffet 60%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Icon(
                                Icons.Default.DinnerDining,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Prasmanan",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            "${calc.buffetPortions}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Porsi makanan utama",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Gubukan Card (40%)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "Stall 40%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Per Booth Gubukan",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            "≈ ${calc.gubukPortions}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "${calc.activeStalls} booth (total $totalGubukPortions)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // 3. Interactive Parameter Controls
        item {
            OutlinedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Parameter Perhitungan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Buffer slider
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Toleransi No-Show (Ketidakhadiran)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Standar WO: 10% – 15%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "$bufferInt%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Slider(
                            value = uiState.bufferPct,
                            onValueChange = onBufferChange,
                            valueRange = 0.05f..0.30f,
                            steps = 4
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("5% (Minimal)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${calc.totalPax} Pax → ${calc.effectivePax} Pax Efektif",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("30% (Maksimal)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Booth count stepper & presets
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Jumlah Booth Gubukan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Rata-rata porsi per pondokan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { onStallsChange((uiState.activeStalls - 1).coerceAtLeast(1)) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Kurang Booth", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    "${uiState.activeStalls}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )
                                IconButton(
                                    onClick = { onStallsChange((uiState.activeStalls + 1).coerceAtMost(20)) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tambah Booth", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Quick presets: 2, 4, 6, 8 booth
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(2, 4, 6, 8).forEach { preset ->
                                val isSelected = uiState.activeStalls == preset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            BorderStroke(
                                                if (isSelected) 1.5.dp else 0.5.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onStallsChange(preset) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$preset Booth",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Breakdown Summary Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Rincian Lengkap Perhitungan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    CalcRowItem("Jumlah Tamu Terdaftar", "${calc.totalGuests} Tamu")
                    CalcRowItem("Total Estimasi Pax", "${calc.totalPax} Pax")
                    CalcRowItem("Toleransi Buffer ($bufferInt%)", "-${calc.totalPax - calc.effectivePax} Pax")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    CalcRowItem("Total Pax Efektif", "${calc.effectivePax} Porsi", isBold = true, isPrimary = true)
                    CalcRowItem("Porsi Prasmanan (60%)", "${calc.buffetPortions} Porsi", isBold = true)
                    CalcRowItem("Total Porsi Gubukan (40%)", "$totalGubukPortions Porsi", isBold = true)
                    CalcRowItem("Rata-rata per Booth (${calc.activeStalls} Booth)", "≈ ${calc.gubukPortions} Porsi/Booth", isBold = true)
                }
            }
        }

        // 5. Educational Guide / Tips
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Panduan & Rasio Katering WO",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "• Rasio 60:40 adalah standar emas resepsi pernikahan agar antrean prasmanan utama dan booth pondokan seimbang.\n" +
                        "• Porsi gubukan dibagi rata ke seluruh booth agar seluruh variasi menu pondokan habis bersamaan.\n" +
                        "• Buffer 10% mengantisipasi tamu yang hadir sendiri (tanpa pasangan) atau berhalangan hadir mendadak.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcRowItem(
    label: String,
    value: String,
    isBold: Boolean = false,
    isPrimary: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SessionCheckboxSelector(
    selectedSession: String,
    onSessionSelected: (String) -> Unit
) {
    val isAkad = selectedSession == "AKAD" || selectedSession == "KEDUANYA"
    val isResepsi = selectedSession == "RESEPSI" || selectedSession == "KEDUANYA"

    fun update(newAkad: Boolean, newResepsi: Boolean) {
        val result = when {
            newAkad && newResepsi -> "KEDUANYA"
            newAkad -> "AKAD"
            newResepsi -> "RESEPSI"
            else -> "KEDUANYA" // Default if both unchecked
        }
        onSessionSelected(result)
    }

    Column {
        Text("Sesi Acara", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { update(!isAkad, isResepsi) }) {
                Checkbox(checked = isAkad, onCheckedChange = { update(it, isResepsi) })
                Text("Akad", style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { update(isAkad, !isResepsi) }) {
                Checkbox(checked = isResepsi, onCheckedChange = { update(isAkad, it) })
                Text("Resepsi", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DynamicGroupDropdownSelector(
    selectedGroup: String,
    availableGroups: List<Pair<String, String>>,
    onGroupSelected: (String) -> Unit,
    onRenameGroup: (oldKey: String, newName: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var editingGroupKey by remember { mutableStateOf<String?>(null) }

    val displayLabel = availableGroups.find { it.first == selectedGroup }?.second ?: selectedGroup

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = displayLabel,
            onValueChange = {},
            label = { Text("Kelompok") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            availableGroups.forEach { (key, label) ->
                DropdownMenuItem(
                    text = {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    editingGroupKey = key
                                    expanded = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Nama Kelompok", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    onClick = {
                        onGroupSelected(key)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Tambah Kelompok Baru", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                onClick = {
                    expanded = false
                    showAddCustomDialog = true
                }
            )
        }
    }

    if (showAddCustomDialog) {
        var newGroupName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = { Text("Tambah Kelompok Baru") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Nama Kelompok") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            val cleanName = newGroupName.trim()
                            onGroupSelected(cleanName)
                            showAddCustomDialog = false
                        }
                    }
                ) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) { Text("Batal") }
            }
        )
    }

    editingGroupKey?.let { groupKey ->
        val currentName = availableGroups.find { it.first == groupKey }?.second ?: groupKey
        var updatedName by remember { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { editingGroupKey = null },
            title = { Text("Edit Nama Kelompok") },
            text = {
                OutlinedTextField(
                    value = updatedName,
                    onValueChange = { updatedName = it },
                    label = { Text("Nama Kelompok") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (updatedName.isNotBlank() && updatedName != currentName) {
                            onRenameGroup(groupKey, updatedName.trim())
                            if (selectedGroup == groupKey) {
                                onGroupSelected(updatedName.trim())
                            }
                        }
                        editingGroupKey = null
                    }
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { editingGroupKey = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun BatchImportConfigDialog(
    selectedContactsCount: Int,
    availableGroups: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (group: String, session: String) -> Unit,
    onRenameGroup: (oldKey: String, newName: String) -> Unit
) {
    var selectedGroup by remember { mutableStateOf(availableGroups.firstOrNull()?.first ?: "VIP") }
    var selectedSession by remember { mutableStateOf("KEDUANYA") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Impor $selectedContactsCount Kontak") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Pilih kelompok dan sesi acara untuk $selectedContactsCount kontak terpilih:",
                    style = MaterialTheme.typography.bodyMedium
                )
                DynamicGroupDropdownSelector(
                    selectedGroup = selectedGroup,
                    availableGroups = availableGroups,
                    onGroupSelected = { selectedGroup = it },
                    onRenameGroup = onRenameGroup
                )
                SessionCheckboxSelector(
                    selectedSession = selectedSession,
                    onSessionSelected = { selectedSession = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedGroup, selectedSession)
                }
            ) { Text("Simpan & Impor") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGuestDialog(
    availableGroups: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String?, group: String, session: String, pax: Int) -> Unit,
    onRenameGroup: (oldKey: String, newName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(availableGroups.firstOrNull()?.first ?: "VIP") }
    var selectedSession by remember { mutableStateOf("KEDUANYA") }
    var pax by remember { mutableStateOf(2) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Tamu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; submitted = false },
                    label = { Text("Nama Tamu / Keluarga") }, 
                    isError = submitted && name.isBlank(),
                    supportingText = { if (submitted && name.isBlank()) Text("Nama tamu wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("No. HP (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )

                // Dynamic Group Selector
                DynamicGroupDropdownSelector(
                    selectedGroup = selectedGroup,
                    availableGroups = availableGroups,
                    onGroupSelected = { selectedGroup = it },
                    onRenameGroup = onRenameGroup
                )

                // Checkbox Session Selector
                SessionCheckboxSelector(
                    selectedSession = selectedSession,
                    onSessionSelected = { selectedSession = it }
                )

                // Pax counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
    availableGroups: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String?, group: String, session: String, pax: Int) -> Unit,
    onRenameGroup: (oldKey: String, newName: String) -> Unit
) {
    var name by remember { mutableStateOf(guest.guestName) }
    var phone by remember { mutableStateOf(guest.phoneNumber ?: "") }
    var selectedGroup by remember { mutableStateOf(guest.groupAllocation) }
    var selectedSession by remember { mutableStateOf(guest.sessionTarget) }
    var pax by remember { mutableStateOf(guest.estimatedPax) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tamu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; submitted = false },
                    label = { Text("Nama Tamu / Keluarga") }, 
                    isError = submitted && name.isBlank(),
                    supportingText = { if (submitted && name.isBlank()) Text("Nama tamu wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("No. HP (opsional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )

                // Dynamic Group Selector
                DynamicGroupDropdownSelector(
                    selectedGroup = selectedGroup,
                    availableGroups = availableGroups,
                    onGroupSelected = { selectedGroup = it },
                    onRenameGroup = onRenameGroup
                )

                // Checkbox Session Selector
                SessionCheckboxSelector(
                    selectedSession = selectedSession,
                    onSessionSelected = { selectedSession = it }
                )

                // Pax counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

