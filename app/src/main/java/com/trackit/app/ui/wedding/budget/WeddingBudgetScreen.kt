package com.trackit.app.ui.wedding.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import com.trackit.app.util.CurrencyUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeddingBudgetScreen(
    weddingProfileId: String,
    onNavigateBack: () -> Unit,
    viewModel: WeddingBudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expandedExpenseId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(weddingProfileId) {
        viewModel.loadForProfile(weddingProfileId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        topBar = {
            TopAppBar(
                title = { Text("Anggaran Pernikahan", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { showAddExpenseDialog = true }) {
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
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // === Budget Summary Card ===
                item {
                    BudgetSummaryCard(uiState = uiState)
                }

                // === Split-Bill Breakdown ===
                if (uiState.bySource.isNotEmpty()) {
                    item {
                        SplitBillCard(bySource = uiState.bySource)
                    }
                }

                // === Category Filters ===
                item {
                    Text(
                        "Pengeluaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.filterCategory == "ALL",
                                onClick = { viewModel.setFilter("ALL") },
                                label = { Text("Semua") }
                            )
                        }
                        items(EXPENSE_CATEGORIES) { (key, label) ->
                            FilterChip(
                                selected = uiState.filterCategory == key,
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
                                Icon(Icons.Default.Receipt, null, modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada pengeluaran", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { showAddExpenseDialog = true }) { Text("Tambah Pengeluaran") }
                            }
                        }
                    }
                } else {
                    items(uiState.filtered, key = { it.expenseId }) { expense ->
                        ExpenseItem(
                            expense = expense,
                            isExpanded = expandedExpenseId == expense.expenseId,
                            onToggle = {
                                expandedExpenseId = if (expandedExpenseId == expense.expenseId) null else expense.expenseId
                            },
                            onAddPayment = { termName, amount, dueDate ->
                                viewModel.addPayment(expense, termName, amount, dueDate)
                            },
                            onDelete = { viewModel.deleteExpense(expense) }
                        )
                    }
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onAdd = { cat, title, est, source, notes ->
                viewModel.addExpense(weddingProfileId, cat, title, est, source, notes)
                showAddExpenseDialog = false
            }
        )
    }
}

@Composable
private fun BudgetSummaryCard(uiState: WeddingBudgetUiState) {
    val progress = if (uiState.totalBudgetCap > 0)
        (uiState.totalEstimated / uiState.totalBudgetCap).toFloat().coerceIn(0f, 1f) else 0f

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BudgetFigure("Pagu Anggaran", CurrencyUtils.formatRupiah(uiState.totalBudgetCap))
                BudgetFigure("Total Estimasi", CurrencyUtils.formatRupiah(uiState.totalEstimated))
                BudgetFigure("Sudah Bayar", CurrencyUtils.formatRupiah(uiState.totalPaid))
            }
            Spacer(Modifier.height(12.dp))
            if (uiState.totalBudgetCap > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (uiState.isOverBudget) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                val statusText = if (uiState.isOverBudget)
                    "Melebihi pagu ${CurrencyUtils.formatRupiah(uiState.totalEstimated - uiState.totalBudgetCap)}"
                else "Sisa pagu ${CurrencyUtils.formatRupiah(uiState.remaining)}"
                Text(statusText, style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.isOverBudget) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun BudgetFigure(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SplitBillCard(bySource: Map<String, Double>) {
    val total = bySource.values.sum()
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Split-Bill (Sumber Dana)", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            bySource.forEach { (source, amount) ->
                val label = FUND_SOURCES.find { it.first == source }?.second ?: source
                val pct = if (total > 0) (amount / total * 100).roundToInt() else 0
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("${pct}%", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp))
                    Text(CurrencyUtils.formatRupiah(amount), style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ExpenseItem(
    expense: WeddingExpenseEntity,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddPayment: (String, Double, Long) -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (expense.paymentStatus) {
        "FULLY_PAID" -> Color(0xFF2E7D32)
        "PARTIAL_DP" -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (expense.paymentStatus) {
        "FULLY_PAID" -> "Lunas"
        "PARTIAL_DP" -> "Sebagian DP"
        else -> "Belum Bayar"
    }
    var showPayDialog by remember { mutableStateOf(false) }
    val catLabel = EXPENSE_CATEGORIES.find { it.first == expense.category }?.second ?: expense.category

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onToggle
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)) {
                            Text(catLabel, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(CurrencyUtils.formatRupiah(expense.totalEstimated),
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text("Bayar: ${CurrencyUtils.formatRupiah(expense.totalPaid)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (!expense.notes.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Notes, null, modifier = Modifier.size(14.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(expense.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    val sourceLabel = FUND_SOURCES.find { it.first == expense.paidBySource }?.second ?: expense.paidBySource
                    Text("Sumber dana: $sourceLabel", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showPayDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Catat Bayar")
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Hapus")
                        }
                    }
                }
            }
        }
    }

    if (showPayDialog) {
        AddPaymentDialog(
            onDismiss = { showPayDialog = false },
            onAdd = { termName, amount, dueDate ->
                onAddPayment(termName, amount, dueDate)
                showPayDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onAdd: (category: String, title: String, estimated: Double, source: String, notes: String?) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("VENUE") }
    var title by remember { mutableStateOf("") }
    var estimated by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("BERSAMA") }
    var notes by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Pengeluaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category
                var catExpanded by remember { mutableStateOf(false) }
                val catLabel = EXPENSE_CATEGORIES.find { it.first == selectedCategory }?.second ?: ""
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = catLabel, onValueChange = {},
                        label = { Text("Kategori") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        EXPENSE_CATEGORIES.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                selectedCategory = key; catExpanded = false
                            })
                        }
                    }
                }
                // Title
                OutlinedTextField(value = title, onValueChange = { title = it; submitted = false },
                    label = { Text("Nama Vendor / Item") }, 
                    isError = submitted && title.isBlank(),
                    supportingText = { if (submitted && title.isBlank()) Text("Nama item wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                // Estimated
                OutlinedTextField(value = estimated,
                    onValueChange = { estimated = it.filter { c -> c.isDigit() }; submitted = false },
                    label = { Text("Total Estimasi (Rp)") }, prefix = { Text("Rp") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    visualTransformation = com.trackit.app.ui.transaction.ThousandSeparatorVisualTransformation(),
                    isError = submitted && estimated.isBlank(),
                    supportingText = { if (submitted && estimated.isBlank()) Text("Estimasi wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                // Source
                var srcExpanded by remember { mutableStateOf(false) }
                val srcLabel = FUND_SOURCES.find { it.first == selectedSource }?.second ?: ""
                ExposedDropdownMenuBox(expanded = srcExpanded, onExpandedChange = { srcExpanded = it }) {
                    OutlinedTextField(
                        value = srcLabel, onValueChange = {},
                        label = { Text("Sumber Dana") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = srcExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = srcExpanded, onDismissRequest = { srcExpanded = false }) {
                        FUND_SOURCES.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedSource = key; srcExpanded = false })
                        }
                    }
                }
                // Notes
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Catatan (opsional)") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = {
                submitted = true
                if (title.isNotBlank() && estimated.isNotBlank()) {
                    onAdd(selectedCategory, title.trim(), estimated.toDouble(), selectedSource,
                        notes.ifBlank { null })
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun AddPaymentDialog(
    onDismiss: () -> Unit,
    onAdd: (termName: String, amount: Double, dueDate: Long) -> Unit
) {
    var termName by remember { mutableStateOf("DP 1") }
    var amount by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat Pembayaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = termName, onValueChange = { termName = it; submitted = false },
                    label = { Text("Jenis Bayar (DP 1, Pelunasan, dll)") },
                    isError = submitted && termName.isBlank(),
                    supportingText = { if (submitted && termName.isBlank()) Text("Jenis bayar wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() }; submitted = false },
                    label = { Text("Nominal (Rp)") }, prefix = { Text("Rp") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    visualTransformation = com.trackit.app.ui.transaction.ThousandSeparatorVisualTransformation(),
                    isError = submitted && amount.isBlank(),
                    supportingText = { if (submitted && amount.isBlank()) Text("Nominal wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                submitted = true
                if (termName.isNotBlank() && amount.isNotBlank()) {
                    onAdd(termName, amount.toDouble(), System.currentTimeMillis())
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
