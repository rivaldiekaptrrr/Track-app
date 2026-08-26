package com.trackit.app.ui.wedding.tasks

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.WeddingTaskEntity
import com.trackit.app.ui.wedding.common.DeleteConfirmDialog
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
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Progress bar overall
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        LinearProgressIndicator(
                            progress = { uiState.progressPct },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (uiState.progressPct >= 1f) Color(0xFF2E7D32)
                                    else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // PIC filter chips
                item {
                    val filters = listOf(
                        "ALL" to "Semua",
                        "GROOM" to "CPP",
                        "BRIDE" to "CPW",
                        "BOTH" to "Bersama",
                        "FAMILY" to "Panitia",
                        "WO" to "WO"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(filters) { (key, label) ->
                            FilterChip(
                                selected = uiState.filterPic == key,
                                onClick = { viewModel.setFilter(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                if (uiState.filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada tugas", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, desc, phase, pic ->
                viewModel.addTask(weddingProfileId, title, desc, phase, pic)
                showAddDialog = false
            }
        )
    }

    editingTask?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { editingTask = null },
            onSave = { title, desc, phase, pic ->
                viewModel.updateTask(task, title, desc, phase, pic)
                editingTask = null
            }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            phase.phaseLabel(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isAllDone) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
        )
        Text(
            "$completed/${tasks.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
        "GROOM" -> "CPP"; "BRIDE" -> "CPW"; "FAMILY" -> "Panitia"; "WO" -> "WO"; else -> "Bersama"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = bgColor)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                )
                if (!task.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = picColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        picLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = picColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp))
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
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String?, phaseMonth: Int, pic: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedPhase by remember { mutableStateOf(6) }
    var selectedPic by remember { mutableStateOf("BOTH") }
    var phaseExpanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val phases = listOf(12 to "H-12 Bulan", 6 to "H-6 Bulan", 3 to "H-3 Bulan", 1 to "H-1 Bulan", 0 to "Hari-H")
    val picOptions = listOf("GROOM" to "CPP", "BRIDE" to "CPW", "BOTH" to "Bersama", "FAMILY" to "Panitia", "WO" to "WO")

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
                // Phase dropdown
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
                // PIC chips
                Text("PIC", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(picOptions) { (key, label) ->
                        FilterChip(selected = selectedPic == key, onClick = { selectedPic = key }, label = { Text(label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                submitted = true
                if (title.isNotBlank()) {
                    onAdd(title.trim(), desc.ifBlank { null }, selectedPhase, selectedPic)
                }
            }) { Text("Tambah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskDialog(
    task: WeddingTaskEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String?, phaseMonth: Int, pic: String) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var desc by remember { mutableStateOf(task.description ?: "") }
    var selectedPhase by remember { mutableStateOf(task.phaseMonth) }
    var selectedPic by remember { mutableStateOf(task.pic) }
    var phaseExpanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val phases = listOf(12 to "H-12 Bulan", 6 to "H-6 Bulan", 3 to "H-3 Bulan", 1 to "H-1 Bulan", 0 to "Hari-H")
    val picOptions = listOf("GROOM" to "CPP", "BRIDE" to "CPW", "BOTH" to "Bersama", "FAMILY" to "Panitia", "WO" to "WO")

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
                Text("PIC", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(picOptions) { (key, label) ->
                        FilterChip(selected = selectedPic == key, onClick = { selectedPic = key }, label = { Text(label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                submitted = true
                if (title.isNotBlank()) {
                    onSave(title.trim(), desc.ifBlank { null }, selectedPhase, selectedPic)
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

