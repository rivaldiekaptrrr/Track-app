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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
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
import com.trackit.app.ui.wedding.common.DeleteConfirmDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
    var showAddCustomCategoryDialog by remember { mutableStateOf(false) }
    var preselectedCategory by remember { mutableStateOf<String?>(null) }

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

                // === Filter Sumber Dana ===
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Filter Sumber Dana",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.availableSources) { (key, label) ->
                                val isSelected = uiState.filterSource == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setSourceFilter(key) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // === Category Budgets & Expenses Grouped ===
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rincian Kategori",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tambah Item",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showAddExpenseDialog = true }
                        )
                    }
                }

                if (uiState.categoryBudgets.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Receipt, null, modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("Belum ada rincian anggaran", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { showAddExpenseDialog = true }) { Text("Tambah Pengeluaran") }
                            }
                        }
                    }
                } else {
                    items(uiState.categoryBudgets, key = { it.categoryKey }) { budget ->
                        val isExpanded = expandedCategories[budget.categoryKey] ?: false
                        
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                            ) {
                                // Category Header Row (Clickable to expand)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedCategories[budget.categoryKey] = !isExpanded
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icon Box
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getWeddingCategoryIcon(budget.iconName),
                                            contentDescription = budget.categoryName,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    
                                    // Name, nominals, progress bar
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = budget.categoryName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${(budget.progress * 100).roundToInt()}%",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        
                                        Text(
                                            text = "${CurrencyUtils.formatRupiah(budget.totalPaid)} / ${CurrencyUtils.formatRupiah(budget.totalEstimated)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        
                                        LinearProgressIndicator(
                                            progress = { budget.progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(2.5.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        )
                                    }
                                }

                                // Expanded content (individual expenses under this category)
                                if (isExpanded) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                    val categoryExpenses = uiState.filteredExpenses.filter { it.category == budget.categoryKey }
                                    if (categoryExpenses.isEmpty()) {
                                        Text(
                                            text = "Belum ada item pengeluaran",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        ) {
                                            categoryExpenses.forEachIndexed { index, expense ->
                                                if (index > 0) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                                    )
                                                }
                                                ExpenseItem(
                                                    expense = expense,
                                                    onAddPayment = { termName, amount, dueDate ->
                                                        viewModel.addPayment(expense, termName, amount, dueDate)
                                                    },
                                                    onEdit = { category, title, estimated, source, notes ->
                                                        viewModel.editExpense(expense, category, title, estimated, source, notes)
                                                    },
                                                    onDelete = { viewModel.deleteExpense(expense) },
                                                    availableCategories = uiState.availableCategories,
                                                    availableSources = uiState.availableSources,
                                                    onRequestAddCustomCategory = {
                                                        showAddCustomCategoryDialog = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // === Add Category Button at Bottom ===
                item {
                    Button(
                        onClick = { showAddCustomCategoryDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tambah Kategori", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            availableCategories = uiState.availableCategories,
            availableSources = uiState.availableSources,
            initialCategory = preselectedCategory,
            onDismiss = { 
                showAddExpenseDialog = false
                preselectedCategory = null
            },
            onAdd = { cat, title, est, source, notes ->
                viewModel.addExpense(weddingProfileId, cat, title, est, source, notes)
                showAddExpenseDialog = false
                preselectedCategory = null
            },
            onRequestAddCustomCategory = {
                showAddCustomCategoryDialog = true
            }
        )
    }

    if (showAddCustomCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var selectedIconKey by remember { mutableStateOf("favorite") }

        AlertDialog(
            onDismissRequest = { showAddCustomCategoryDialog = false },
            title = { Text("Tambah Kategori Pengeluaran") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Nama Kategori Baru") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Pilih Ikon Kategori",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val weddingIcons = listOf(
                        "favorite" to Icons.Default.Favorite,
                        "camera" to Icons.Default.PhotoCamera,
                        "checkroom" to Icons.Default.Checkroom,
                        "restaurant" to Icons.Default.Restaurant,
                        "apartment" to Icons.Default.Apartment,
                        "brush" to Icons.Default.Brush,
                        "music" to Icons.Default.MusicNote,
                        "giftcard" to Icons.Default.CardGiftcard,
                        "car" to Icons.Default.DirectionsCar,
                        "celebration" to Icons.Default.Celebration
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        weddingIcons.chunked(5).forEach { rowIcons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowIcons.forEach { (key, icon) ->
                                    val isSelected = selectedIconKey == key
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .clickable { selectedIconKey = key },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = key,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            preselectedCategory = "CUSTOM:${newCatName.trim()}:$selectedIconKey"
                            showAddCustomCategoryDialog = false
                            showAddExpenseDialog = true
                        }
                    }
                ) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomCategoryDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun BudgetSummaryCard(uiState: WeddingBudgetUiState) {
    val progress = if (uiState.totalBudgetCap > 0)
        (uiState.totalPaid / uiState.totalBudgetCap).toFloat().coerceIn(0f, 1f) else 0f
    val percentage = (progress * 100).roundToInt()

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Nominal Budgets
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        text = "Total Budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatRupiah(uiState.totalBudgetCap),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column {
                    Text(
                        text = "Total Terpakai",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatRupiah(uiState.totalPaid),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Right Column: Radial Chart Progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(80.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


@Composable
private fun ExpenseItem(
    expense: WeddingExpenseEntity,
    onAddPayment: (termName: String, amount: Double, dueDate: Long) -> Unit,
    onEdit: (category: String, title: String, estimated: Double, source: String, notes: String?) -> Unit,
    onDelete: () -> Unit,
    availableCategories: List<Pair<String, String>>,
    availableSources: List<Pair<String, String>>,
    onRequestAddCustomCategory: () -> Unit
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
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sourceLabel = FUND_SOURCES.find { it.first == expense.paidBySource }?.second ?: expense.paidBySource
                    Text(
                        text = "Sumber dana: $sourceLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Surface(
                        color = when (expense.paymentStatus) {
                            "FULLY_PAID" -> Color(0xFFE8F5E9)
                            "PARTIAL_DP" -> Color(0xFFFFF3E0)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text(
                    text = CurrencyUtils.formatRupiah(expense.totalEstimated),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Terbayar: ${CurrencyUtils.formatRupiah(expense.totalPaid)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!expense.notes.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Catatan: ${expense.notes}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        // Premium soft-tint action buttons (Without icons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Bayar" Pill Button
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showPayDialog = true }
            ) {
                Text(
                    text = "Bayar",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // "Edit" Pill Button
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showEditDialog = true }
            ) {
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // "Hapus" Pill Button
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDeleteConfirm = true }
            ) {
                Text(
                    text = "Hapus",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
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

    if (showEditDialog) {
        EditExpenseDialog(
            expense = expense,
            availableCategories = availableCategories,
            availableSources = availableSources,
            onDismiss = { showEditDialog = false },
            onSave = { category, title, estimated, source, notes ->
                onEdit(category, title, estimated, source, notes)
                showEditDialog = false
            },
            onRequestAddCustomCategory = onRequestAddCustomCategory
        )
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = "Hapus Pengeluaran?",
            message = "Apakah Anda yakin ingin menghapus '${expense.title}'? Tindakan ini tidak dapat dibatalkan.",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditExpenseDialog(
    expense: WeddingExpenseEntity,
    availableCategories: List<Pair<String, String>>,
    availableSources: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (category: String, title: String, estimated: Double, source: String, notes: String?) -> Unit,
    onRequestAddCustomCategory: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(expense.category) }
    var title by remember { mutableStateOf(expense.title) }
    var estimated by remember { mutableStateOf(expense.totalEstimated.toInt().toString()) }
    var selectedSource by remember { mutableStateOf(expense.paidBySource) }
    var notes by remember { mutableStateOf(expense.notes ?: "") }
    var submitted by remember { mutableStateOf(false) }

    var showAddCustomSourceDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Pengeluaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category
                var catExpanded by remember { mutableStateOf(false) }
                val catLabel = availableCategories.find { it.first == selectedCategory }?.second ?: selectedCategory
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = catLabel, onValueChange = {},
                        label = { Text("Kategori") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        availableCategories.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                selectedCategory = key; catExpanded = false
                            })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Tambah Kategori Baru", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                catExpanded = false
                                onRequestAddCustomCategory()
                            }
                        )
                    }
                }
                // Title
                OutlinedTextField(value = title, onValueChange = { title = it; submitted = false },
                    label = { Text("Nama Vendor / Item") }, 
                    isError = submitted && title.isBlank(),
                    supportingText = { if (submitted && title.isBlank()) Text("Nama item wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)

                // Estimated Cost
                OutlinedTextField(
                    value = estimated, 
                    onValueChange = { estimated = it.filter { c -> c.isDigit() }; submitted = false },
                    label = { Text("Estimasi Biaya (Rp)") }, 
                    prefix = { Text("Rp") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    visualTransformation = com.trackit.app.ui.transaction.ThousandSeparatorVisualTransformation(),
                    isError = submitted && estimated.isBlank(),
                    supportingText = { if (submitted && estimated.isBlank()) Text("Estimasi biaya wajib diisi") },
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true
                )

                // Paid By Source
                var srcExpanded by remember { mutableStateOf(false) }
                val srcLabel = availableSources.find { it.first == selectedSource }?.second ?: selectedSource
                ExposedDropdownMenuBox(expanded = srcExpanded, onExpandedChange = { srcExpanded = it }) {
                    OutlinedTextField(
                        value = srcLabel, onValueChange = {},
                        label = { Text("Dibayar Oleh") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = srcExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = srcExpanded, onDismissRequest = { srcExpanded = false }) {
                        availableSources.filter { it.first != "ALL" }.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                selectedSource = key; srcExpanded = false
                            })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Tambah Sumber Dana Baru", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                srcExpanded = false
                                showAddCustomSourceDialog = true
                            }
                        )
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
                    onSave(selectedCategory, title.trim(), estimated.toDouble(), selectedSource,
                        notes.ifBlank { null })
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )

    if (showAddCustomSourceDialog) {
        var newSourceName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomSourceDialog = false },
            title = { Text("Tambah Sumber Dana") },
            text = {
                OutlinedTextField(
                    value = newSourceName,
                    onValueChange = { newSourceName = it },
                    label = { Text("Nama Sumber Dana Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSourceName.isNotBlank()) {
                            selectedSource = newSourceName.trim()
                            showAddCustomSourceDialog = false
                        }
                    }
                ) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomSourceDialog = false }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    availableCategories: List<Pair<String, String>>,
    availableSources: List<Pair<String, String>>,
    initialCategory: String? = null,
    onDismiss: () -> Unit,
    onAdd: (category: String, title: String, estimated: Double, source: String, notes: String?) -> Unit,
    onRequestAddCustomCategory: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialCategory ?: availableCategories.firstOrNull()?.first ?: "VENUE") }
    LaunchedEffect(initialCategory) {
        if (initialCategory != null) {
            selectedCategory = initialCategory
        }
    }
    var title by remember { mutableStateOf("") }
    var estimated by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(availableSources.firstOrNull()?.first ?: "BERSAMA") }
    var notes by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    
    var showAddCustomSourceDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Pengeluaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category
                var catExpanded by remember { mutableStateOf(false) }
                val catLabel = availableCategories.find { it.first == selectedCategory }?.second ?: selectedCategory
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                    OutlinedTextField(
                        value = catLabel, onValueChange = {},
                        label = { Text("Kategori") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        availableCategories.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                selectedCategory = key; catExpanded = false
                            })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Tambah Kategori Baru", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                catExpanded = false
                                onRequestAddCustomCategory()
                            }
                        )
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
                val srcLabel = availableSources.find { it.first == selectedSource }?.second ?: selectedSource
                ExposedDropdownMenuBox(expanded = srcExpanded, onExpandedChange = { srcExpanded = it }) {
                    OutlinedTextField(
                        value = srcLabel, onValueChange = {},
                        label = { Text("Sumber Dana") }, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = srcExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = srcExpanded, onDismissRequest = { srcExpanded = false }) {
                        availableSources.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { selectedSource = key; srcExpanded = false })
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Tambah Sumber Dana Baru", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                srcExpanded = false
                                showAddCustomSourceDialog = true
                            }
                        )
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



    if (showAddCustomSourceDialog) {
        var newSourceName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCustomSourceDialog = false },
            title = { Text("Tambah Sumber Dana") },
            text = {
                OutlinedTextField(
                    value = newSourceName,
                    onValueChange = { newSourceName = it },
                    label = { Text("Nama Sumber Dana Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSourceName.isNotBlank()) {
                            selectedSource = newSourceName.trim()
                            showAddCustomSourceDialog = false
                        }
                    }
                ) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomSourceDialog = false }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentDialog(
    onDismiss: () -> Unit,
    onAdd: (termName: String, amount: Double, dueDate: Long) -> Unit
) {
    var termName by remember { mutableStateOf("DP 1") }
    var amount by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("id")) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: selectedDate
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                // Date picker field
                OutlinedTextField(
                    value = dateFormatter.format(Date(selectedDate)),
                    onValueChange = {},
                    label = { Text("Tanggal Bayar / Jatuh Tempo") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                submitted = true
                if (termName.isNotBlank() && amount.isNotBlank()) {
                    onAdd(termName, amount.toDouble(), selectedDate)
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
 
private fun getWeddingCategoryIcon(iconName: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        "apartment" -> Icons.Default.Apartment
        "restaurant" -> Icons.Default.Restaurant
        "brush" -> Icons.Default.Brush
        "face" -> Icons.Default.Face
        "checkroom" -> Icons.Default.Checkroom
        "camera" -> Icons.Default.PhotoCamera
        "email" -> Icons.Default.Email
        "giftcard" -> Icons.Default.CardGiftcard
        "redeem" -> Icons.Default.Redeem
        "car" -> Icons.Default.DirectionsCar
        "music" -> Icons.Default.MusicNote
        "celebration" -> Icons.Default.Celebration
        else -> Icons.Default.MoreHoriz
    }
}

