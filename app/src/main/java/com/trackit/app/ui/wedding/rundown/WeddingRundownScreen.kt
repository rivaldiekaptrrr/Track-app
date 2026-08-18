package com.trackit.app.ui.wedding.rundown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.WeddingEventEntity
import com.trackit.app.data.local.entity.WeddingRundownItemEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingRundownScreen(
    weddingProfileId: String,
    onNavigateBack: () -> Unit,
    viewModel: WeddingRundownViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    LaunchedEffect(weddingProfileId) { viewModel.loadForProfile(weddingProfileId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rundown Acara", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.events.size} event · ${uiState.currentRundown.size} sesi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { showAddEventDialog = true }) {
                        Icon(Icons.Default.LibraryAdd, "Tambah Event")
                    }
                    if (uiState.selectedEvent != null) {
                        IconButton(onClick = { showAddItemDialog = true }) {
                            Icon(Icons.Default.Add, "Tambah Sesi")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        if (uiState.events.isEmpty()) {
            // Empty state
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("📋", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(16.dp))
                    Text("Belum ada event", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(
                        "Tambahkan event acara pernikahan seperti Akad Nikah, Resepsi, Siraman, dll.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showAddEventDialog = true }) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tambah Event Pertama")
                    }
                }
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Event Tabs scroll
                ScrollableTabRow(
                    selectedTabIndex = uiState.events.indexOfFirst { it.eventId == uiState.selectedEvent?.eventId }.coerceAtLeast(0),
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.events.forEachIndexed { index, event ->
                        Tab(
                            selected = event.eventId == uiState.selectedEvent?.eventId,
                            onClick = { viewModel.selectEvent(event.eventId) },
                            text = { Text(event.eventName, maxLines = 1) }
                        )
                    }
                }

                // Rundown list for selected event
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Selected event header
                    item {
                        uiState.selectedEvent?.let { event ->
                            EventHeaderCard(
                                event = event,
                                onRename = { viewModel.renameEvent(event, it) },
                                onDelete = {
                                    viewModel.deleteEvent(event)
                                }
                            )
                        }
                    }

                    if (uiState.currentRundown.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🕐", style = MaterialTheme.typography.displaySmall)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Belum ada sesi rundown", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = { showAddItemDialog = true }) { Text("Tambah Sesi") }
                                }
                            }
                        }
                    } else {
                        items(uiState.currentRundown, key = { it.itemId }) { item ->
                            RundownItemRow(
                                item = item,
                                onDelete = { viewModel.deleteRundownItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        AddEventDialog(
            onDismiss = { showAddEventDialog = false },
            onAdd = { name, date, location ->
                viewModel.addEvent(name, date, location)
                showAddEventDialog = false
            }
        )
    }

    if (showAddItemDialog && uiState.selectedEvent != null) {
        AddRundownItemDialog(
            onDismiss = { showAddItemDialog = false },
            onAdd = { time, duration, title, pic, script ->
                viewModel.addRundownItem(uiState.selectedEvent!!.eventId, time, duration, title, pic, script)
                showAddItemDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventHeaderCard(
    event: WeddingEventEntity,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    val dateStr = remember(event.eventDate) {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date(event.eventDate))
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.eventName, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("📅 $dateStr", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                if (!event.eventLocation.isNullOrBlank()) {
                    Text("📍 ${event.eventLocation}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
            }
            IconButton(onClick = { showRenameDialog = true }) {
                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(event.eventName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Ubah Nama Event") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it },
                    label = { Text("Nama Event") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            },
            confirmButton = {
                Button(onClick = { if (newName.isNotBlank()) { onRename(newName.trim()); showRenameDialog = false } }) {
                    Text("Simpan")
                }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun RundownItemRow(
    item: WeddingRundownItemEntity,
    onDelete: () -> Unit
) {
    val endTime = remember(item.timeStart, item.durationMinutes) {
        try {
            val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val start = fmt.parse(item.timeStart)!!
            val cal = Calendar.getInstance().apply { time = start; add(Calendar.MINUTE, item.durationMinutes) }
            fmt.format(cal.time)
        } catch (e: Exception) { "??:??" }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(64.dp)
        ) {
            Text(item.timeStart, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(endTime, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${item.durationMinutes}m", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Timeline vertical line
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(64.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )
        Spacer(Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(item.sessionTitle, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            if (!item.pic.isNullOrBlank()) {
                Text("PIC: ${item.pic}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            if (!item.mcScript.isNullOrBlank()) {
                Text("🎙️ ${item.mcScript}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2)
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp))
        }
    }
    Divider(modifier = Modifier.padding(start = 92.dp, end = 16.dp), thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEventDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, date: Long, location: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    val dateError = remember(dateText) {
        if (dateText.isBlank()) false
        else try { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateText); false }
        catch (e: Exception) { true }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Event Acara") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nama Event (mis. Akad Nikah, Resepsi)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = dateText, onValueChange = { dateText = it },
                    label = { Text("Tanggal (DD/MM/YYYY)") },
                    placeholder = { Text("25/12/2025") },
                    isError = dateError,
                    supportingText = { if (dateError) Text("Format tanggal tidak valid") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = location, onValueChange = { location = it },
                    label = { Text("Lokasi (opsional)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && !dateError && dateText.isNotBlank()) {
                    val date = try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateText)!!.time
                    } catch (e: Exception) { System.currentTimeMillis() }
                    onAdd(name.trim(), date, location.ifBlank { null })
                }
            }) { Text("Tambah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRundownItemDialog(
    onDismiss: () -> Unit,
    onAdd: (time: String, duration: Int, title: String, pic: String?, script: String?) -> Unit
) {
    var time by remember { mutableStateOf("08:00") }
    var duration by remember { mutableStateOf("30") }
    var title by remember { mutableStateOf("") }
    var pic by remember { mutableStateOf("") }
    var script by remember { mutableStateOf("") }
    val timeError = remember(time) {
        !Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$").matches(time)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Sesi Rundown") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = time, onValueChange = { time = it },
                        label = { Text("Mulai (HH:mm)") }, singleLine = true,
                        isError = timeError,
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(value = duration, onValueChange = { duration = it.filter { c -> c.isDigit() } },
                        label = { Text("Durasi (menit)") }, singleLine = true,
                        modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Nama Sesi / Kegiatan") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = pic, onValueChange = { pic = it },
                    label = { Text("PIC (MC, CPP, CPW, dst.)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = script, onValueChange = { script = it },
                    label = { Text("Teks Panduan MC (opsional)") },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() && !timeError) {
                    onAdd(time, duration.toIntOrNull() ?: 30, title.trim(),
                        pic.ifBlank { null }, script.ifBlank { null })
                }
            }) { Text("Tambah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
