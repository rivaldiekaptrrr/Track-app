package com.trackit.app.ui.wedding.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.WeddingTaskEntity
import com.trackit.app.ui.wedding.common.DeleteConfirmDialog
import com.trackit.app.ui.wedding.common.WeddingScreenGuideDialog
import com.trackit.app.ui.wedding.common.WeddingGuideFeature
import com.trackit.app.util.DateUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingTasksScreen(
    weddingProfileId: String,
    onNavigateBack: () -> Unit,
    viewModel: WeddingTasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<WeddingTaskEntity?>(null) }

    LaunchedEffect(weddingProfileId) {
        viewModel.loadForProfile(weddingProfileId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Timeline & Tugas", fontWeight = FontWeight.Bold)
                        Text(
                            "${uiState.completedCount}/${uiState.totalCount} selesai · ${(uiState.progressPct * 100).roundToInt()}%",
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
                Icon(Icons.Default.Add, contentDescription = "Tambah Tugas")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Hero Progress Card
                item {
                    TaskHeroCard(uiState = uiState)
                }

                // PIC filter chips
                item {
                    val filters = listOf("ALL" to "Semua") + uiState.availablePics
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(filters) { (key, label) ->
                            val selected = uiState.filterPic == key
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
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada tugas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = { showAddDialog = true }) { Text("Tambah Tugas") }
                            }
                        }
                    }
                }

                // Grouped by phase
                uiState.grouped.forEach { (phase, tasks) ->
                    item(key = "header_$phase") {
                        PhaseHeader(phase = phase, tasks = tasks)
                    }
                    items(tasks, key = { it.taskId }) { task ->
                        TaskItem(
                            task = task,
                            onToggle = { viewModel.toggleCompleted(task) },
                            onEdit = { editingTask = task },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showGuideDialog) {
        WeddingScreenGuideDialog(
            title = "Timeline & Checklist Tugas",
            screenPurpose = "Memandu persiapan pernikahan secara terstruktur dari H-12 bulan hingga Hari H agar tidak ada tugas penting yang terlewat.",
            features = listOf(
                WeddingGuideFeature("Timeline Berfase", "Tugas dikelompokkan otomatis berdasarkan fase waktu (H-6 Bulan, H-3 Bulan, H-1 Bulan, H-1 Minggu, Hari H)."),
                WeddingGuideFeature("Penanggung Jawab (PIC)", "Tugaskan pihak yang bertanggung jawab (CPP, CPW, Keluarga, atau WO) untuk tiap tugas."),
                WeddingGuideFeature("Tenggat Waktu & Selesai", "Atur batas waktu penyelesaian dan centang tugas yang sudah berhasil diselesaikan."),
                WeddingGuideFeature("Persentase Kesiapan", "Pantau visual progress bar kesiapan pernikahan secara real-time.")
            ),
            proTip = "Prioritaskan pos-pos krusial seperti Booking Gedung & Katering di fase awal sebelum mengurus souvenir atau seragam.",
            onDismiss = { showGuideDialog = false }
        )
    }

    if (showAddDialog) {
        AddTaskDialog(
            availablePics = uiState.availablePics,
            onDismiss = { showAddDialog = false },
            onAdd = { title, desc, phase, pic, dueDate, completedDate ->
                viewModel.addTask(weddingProfileId, title, desc, phase, pic, dueDate, completedDate)
                showAddDialog = false
            }
        )
    }

    editingTask?.let { task ->
        EditTaskDialog(
            task = task,
            availablePics = uiState.availablePics,
            onDismiss = { editingTask = null },
            onSave = { title, desc, phase, pic, dueDate, completedDate ->
                viewModel.updateTask(task, title, desc, phase, pic, dueDate, completedDate)
                editingTask = null
            }
        )
    }
}

@Composable
private fun TaskHeroCard(uiState: WeddingTasksUiState) {
    val remaining = (uiState.totalCount - uiState.completedCount).coerceAtLeast(0)

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
                        "Progress Persiapan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${uiState.completedCount} dari ${uiState.totalCount} tugas selesai",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = if (uiState.progressPct >= 1f) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${(uiState.progressPct * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (uiState.progressPct >= 1f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { uiState.progressPct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (uiState.progressPct >= 1f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TaskFigure("Total Tugas", "${uiState.totalCount}")
                VerticalDivider(modifier = Modifier.height(28.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                TaskFigure("Selesai", "${uiState.completedCount}")
                VerticalDivider(modifier = Modifier.height(28.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                TaskFigure("Sisa Tugas", "$remaining")
            }
        }
    }
}

@Composable
private fun TaskFigure(label: String, value: String) {
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

@Composable
private fun PhaseHeader(phase: Int, tasks: List<WeddingTaskEntity>) {
    val completed = tasks.count { it.isCompleted }
    val isAllDone = completed == tasks.size && tasks.isNotEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            phase.phaseLabel(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isAllDone) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
        )
        Surface(
            color = if (isAllDone) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "$completed/${tasks.size}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAllDone) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: WeddingTaskEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        if (task.isCompleted) Color(0xFF1B5E20).copy(alpha = 0.07f)
        else MaterialTheme.colorScheme.surface,
        label = "task_bg"
    )
    val picColor = when (task.pic) {
        "GROOM" -> Color(0xFF1565C0)
        "BRIDE" -> Color(0xFFC62828)
        "FAMILY" -> Color(0xFF6A1B9A)
        "WO" -> Color(0xFF795548)
        else -> Color(0xFF00695C)
    }
    val picLabel = when (task.pic) {
        "GROOM" -> "CPP"; "BRIDE" -> "CPW"; "FAMILY" -> "Panitia"; "WO" -> "WO"; else -> task.pic
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!task.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Dates display
                if (task.dueDate != null || (task.isCompleted && task.completedDate != null)) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (task.dueDate != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Target: ${DateUtils.formatDate(task.dueDate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (task.isCompleted && task.completedDate != null) {
                            Surface(
                                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "✓ Selesai: ${DateUtils.formatDate(task.completedDate)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Surface(
                    color = picColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        picLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = picColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
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
            title = "Hapus Tugas?",
            message = "Tugas \"${task.title}\" akan dihapus permanen.",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = onDelete
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(
    availablePics: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String?, phaseMonth: Int, pic: String, dueDate: Long?, completedDate: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedPhase by remember { mutableStateOf(6) }
    var selectedPic by remember { mutableStateOf(availablePics.firstOrNull()?.first ?: "BOTH") }
    var selectedDueDate by remember { mutableStateOf<Long?>(null) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var phaseExpanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var showAddCustomPicDialog by remember { mutableStateOf(false) }

    val phases = listOf(12 to "H-12 Bulan", 6 to "H-6 Bulan", 3 to "H-3 Bulan", 1 to "H-1 Bulan", 0 to "Hari-H")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Tugas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it; submitted = false },
                    label = { Text("Nama Tugas") }, 
                    isError = submitted && title.isBlank(),
                    supportingText = { if (submitted && title.isBlank()) Text("Nama tugas wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = desc, onValueChange = { desc = it },
                    label = { Text("Keterangan (opsional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2
                )
                ExposedDropdownMenuBox(expanded = phaseExpanded, onExpandedChange = { phaseExpanded = it }) {
                    OutlinedTextField(
                        value = phases.find { it.first == selectedPhase }?.second ?: "",
                        onValueChange = {}, label = { Text("Fase") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = phaseExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = phaseExpanded, onDismissRequest = { phaseExpanded = false }) {
                        phases.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedPhase = key; phaseExpanded = false })
                        }
                    }
                }

                // Target Due Date picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedDueDate != null) DateUtils.formatDate(selectedDueDate!!) else "Tidak diatur",
                        onValueChange = {},
                        label = { Text("Target Rencana Selesai") },
                        readOnly = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedDueDate != null) {
                                    IconButton(onClick = { selectedDueDate = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus Tanggal", modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(onClick = { showDueDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDueDatePicker = true }
                    )
                }

                // PIC chips
                Text("PIC (Penanggung Jawab)", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availablePics) { (key, label) ->
                        FilterChip(
                            selected = selectedPic == key,
                            onClick = { selectedPic = key },
                            label = { Text(label) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { showAddCustomPicDialog = true },
                            label = { Text("+ PIC Baru", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                submitted = true
                if (title.isNotBlank()) {
                    onAdd(title.trim(), desc.ifBlank { null }, selectedPhase, selectedPic, selectedDueDate, null)
                }
            }) { Text("Tambah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )

    if (showDueDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = selectedDueDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { selectedDueDate = it }
                    showDueDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text("Batal") } }
        ) { DatePicker(state = dpState) }
    }

    if (showAddCustomPicDialog) {
        var newPicName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomPicDialog = false },
            title = { Text("Tambah Penanggung Jawab (PIC)") },
            text = {
                OutlinedTextField(
                    value = newPicName,
                    onValueChange = { newPicName = it },
                    label = { Text("Nama PIC Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPicName.isNotBlank()) {
                            selectedPic = newPicName.trim()
                            showAddCustomPicDialog = false
                        }
                    }
                ) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomPicDialog = false }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskDialog(
    task: WeddingTaskEntity,
    availablePics: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String?, phaseMonth: Int, pic: String, dueDate: Long?, completedDate: Long?) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var desc by remember { mutableStateOf(task.description ?: "") }
    var selectedPhase by remember { mutableStateOf(task.phaseMonth) }
    var selectedPic by remember { mutableStateOf(task.pic) }
    var selectedDueDate by remember { mutableStateOf(task.dueDate) }
    var selectedCompletedDate by remember { mutableStateOf(task.completedDate) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showCompletedDatePicker by remember { mutableStateOf(false) }
    var phaseExpanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var showAddCustomPicDialog by remember { mutableStateOf(false) }

    val phases = listOf(12 to "H-12 Bulan", 6 to "H-6 Bulan", 3 to "H-3 Bulan", 1 to "H-1 Bulan", 0 to "Hari-H")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Tugas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it; submitted = false },
                    label = { Text("Nama Tugas") },
                    isError = submitted && title.isBlank(),
                    supportingText = { if (submitted && title.isBlank()) Text("Nama tugas wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = desc, onValueChange = { desc = it },
                    label = { Text("Keterangan (opsional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2
                )
                ExposedDropdownMenuBox(expanded = phaseExpanded, onExpandedChange = { phaseExpanded = it }) {
                    OutlinedTextField(
                        value = phases.find { it.first == selectedPhase }?.second ?: "",
                        onValueChange = {}, label = { Text("Fase") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = phaseExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = phaseExpanded, onDismissRequest = { phaseExpanded = false }) {
                        phases.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedPhase = key; phaseExpanded = false })
                        }
                    }
                }

                // Target Due Date picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedDueDate != null) DateUtils.formatDate(selectedDueDate!!) else "Tidak diatur",
                        onValueChange = {},
                        label = { Text("Target Rencana Selesai") },
                        readOnly = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedDueDate != null) {
                                    IconButton(onClick = { selectedDueDate = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus Tanggal", modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(onClick = { showDueDatePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDueDatePicker = true }
                    )
                }

                // Realization / Completed Date picker
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedCompletedDate != null) DateUtils.formatDate(selectedCompletedDate!!) else "Belum selesai",
                        onValueChange = {},
                        label = { Text("Tanggal Realisasi Selesai") },
                        readOnly = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedCompletedDate != null) {
                                    IconButton(onClick = { selectedCompletedDate = null }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Hapus Tanggal Selesai", modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(onClick = { showCompletedDatePicker = true }) {
                                    Icon(Icons.Default.EventAvailable, contentDescription = "Pilih Tanggal Selesai")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCompletedDatePicker = true }
                    )
                }

                Text("PIC (Penanggung Jawab)", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availablePics) { (key, label) ->
                        FilterChip(
                            selected = selectedPic == key,
                            onClick = { selectedPic = key },
                            label = { Text(label) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { showAddCustomPicDialog = true },
                            label = { Text("+ PIC Baru", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                submitted = true
                if (title.isNotBlank()) {
                    onSave(title.trim(), desc.ifBlank { null }, selectedPhase, selectedPic, selectedDueDate, selectedCompletedDate)
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )

    if (showDueDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = selectedDueDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { selectedDueDate = it }
                    showDueDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text("Batal") } }
        ) { DatePicker(state = dpState) }
    }

    if (showCompletedDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = selectedCompletedDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showCompletedDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { selectedCompletedDate = it }
                    showCompletedDatePicker = false
                }) { Text("Pilih") }
            },
            dismissButton = { TextButton(onClick = { showCompletedDatePicker = false }) { Text("Batal") } }
        ) { DatePicker(state = dpState) }
    }

    if (showAddCustomPicDialog) {
        var newPicName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomPicDialog = false },
            title = { Text("Tambah Penanggung Jawab (PIC)") },
            text = {
                OutlinedTextField(
                    value = newPicName,
                    onValueChange = { newPicName = it },
                    label = { Text("Nama PIC Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPicName.isNotBlank()) {
                            selectedPic = newPicName.trim()
                            showAddCustomPicDialog = false
                        }
                    }
                ) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomPicDialog = false }) { Text("Batal") }
            }
        )
    }
}
