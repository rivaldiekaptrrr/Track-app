package com.trackit.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.repository.CategoryRepository
import com.trackit.app.data.repository.TransactionRepository
import com.trackit.app.util.CategoryIconMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { list ->
                _categories.value = list
            }
        }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (category.id == 0L) {
                categoryRepository.insert(category)
            } else {
                categoryRepository.update(category)
            }
        }
    }

    val showMigrationDialogFor = MutableStateFlow<CategoryEntity?>(null)
    val transactionCountToMigrate = MutableStateFlow(0)

    fun requestDelete(category: CategoryEntity) {
        viewModelScope.launch {
            val count = transactionRepository.countTransactionsByCategory(category.id)
            if (count > 0) {
                transactionCountToMigrate.value = count
                showMigrationDialogFor.value = category
            } else {
                categoryRepository.delete(category)
            }
        }
    }

    fun confirmDeleteAndMigrate(oldCategory: CategoryEntity, fallbackCategory: CategoryEntity?) {
        viewModelScope.launch {
            if (fallbackCategory != null) {
                transactionRepository.updateTransactionsCategory(oldCategory.id, fallbackCategory.id)
            }
            categoryRepository.delete(oldCategory)
            showMigrationDialogFor.value = null
        }
    }
    
    fun toggleVisibility(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.update(category.copy(isHidden = !category.isHidden))
        }
    }
}

val availableColors = listOf(
    "#E8963B", "#3D6373", "#C24D6E", "#7B61D9", "#1B6B4F", 
    "#4EADAD", "#D4A843", "#8B6BB5", "#2E7D32", "#F57F17", 
    "#1565C0", "#00838F", "#D32F2F", "#607D8B", "#795548"
)

val availableIcons = listOf(
    "restaurant", "directions_car", "movie", "receipt_long", "shopping_bag", 
    "local_hospital", "school", "payments", "card_giftcard", "trending_up", 
    "add_circle", "pets", "fitness_center", "home", "flight", "child_care", "build", "more_horiz"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomKeywordScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryManagementViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val showMigrationDialogFor by viewModel.showMigrationDialogFor.collectAsState()
    val transactionCountToMigrate by viewModel.transactionCountToMigrate.collectAsState()
    
    var selectedTab by remember { mutableStateOf("EXPENSE") }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val filteredCategories = categories.filter { it.type == selectedTab }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Kategori", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedCategory = CategoryEntity(name = "", iconName = "restaurant", colorHex = "#E8963B", type = selectedTab)
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kategori")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Selection
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SegmentedButton(
                    selected = selectedTab == "EXPENSE",
                    onClick = { selectedTab = "EXPENSE" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Pengeluaran")
                }
                SegmentedButton(
                    selected = selectedTab == "INCOME",
                    onClick = { selectedTab = "INCOME" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Pemasukan")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCategories) { category ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.clickable {
                            selectedCategory = category
                            showDialog = true
                        }
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
                                    .clip(CircleShape)
                                    .background(CategoryIconMapper.parseColor(category.colorHex).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconMapper.getIcon(category.iconName),
                                    contentDescription = category.name,
                                    tint = CategoryIconMapper.parseColor(category.colorHex),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (category.isHidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (category.customKeywords.isEmpty()) "Kata kunci AI: (Kosong)" 
                                           else "AI: ${category.customKeywords}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { viewModel.toggleVisibility(category) }) {
                                Icon(
                                    if (category.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDialog && selectedCategory != null) {
            CategoryFormDialog(
                category = selectedCategory!!,
                onDismiss = { showDialog = false },
                onSave = { updatedCategory ->
                    viewModel.saveCategory(updatedCategory)
                    showDialog = false
                },
                onDelete = { categoryToDelete ->
                    viewModel.requestDelete(categoryToDelete)
                    showDialog = false
                }
            )
        }

        if (showMigrationDialogFor != null) {
            val fallbackCategories = categories.filter { it.id != showMigrationDialogFor!!.id && it.type == showMigrationDialogFor!!.type }
            var selectedFallback by remember { mutableStateOf(fallbackCategories.firstOrNull()) }

            AlertDialog(
                onDismissRequest = { viewModel.showMigrationDialogFor.value = null },
                title = { Text("Hapus Kategori?") },
                text = {
                    Column {
                        Text("Ada $transactionCountToMigrate transaksi di kategori ini. Pilih kategori tujuan untuk memindahkan transaksi tersebut:")
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(fallbackCategories) { cat ->
                                FilterChip(
                                    selected = selectedFallback?.id == cat.id,
                                    onClick = { selectedFallback = cat },
                                    label = { Text(cat.name) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { 
                        viewModel.confirmDeleteAndMigrate(showMigrationDialogFor!!, selectedFallback)
                    }) {
                        Text("Pindahkan & Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showMigrationDialogFor.value = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormDialog(
    category: CategoryEntity,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var selectedIcon by remember { mutableStateOf(category.iconName) }
    var selectedColor by remember { mutableStateOf(category.colorHex) }
    var customKeywords by remember { mutableStateOf(category.customKeywords) }
    var type by remember { mutableStateOf(category.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category.id == 0L) "Tambah Kategori" else "Edit Kategori") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Type
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("Pengeluaran") }
                    SegmentedButton(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("Pemasukan") }
                }

                // Color Picker
                Text("Warna", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableColors) { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CategoryIconMapper.parseColor(colorHex))
                                .clickable { selectedColor = colorHex }
                                .border(
                                    width = if (selectedColor == colorHex) 3.dp else 0.dp,
                                    color = if (selectedColor == colorHex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Icon Picker
                Text("Ikon", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableIcons) { iconName ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedIcon == iconName) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconMapper.getIcon(iconName),
                                contentDescription = null,
                                tint = if (selectedIcon == iconName) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Keywords
                OutlinedTextField(
                    value = customKeywords,
                    onValueChange = { customKeywords = it },
                    label = { Text("Kata Kunci AI (pisahkan koma)") },
                    placeholder = { Text("kos, kontrakan, pdam") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            category.copy(
                                name = name.trim(),
                                iconName = selectedIcon,
                                colorHex = selectedColor,
                                type = type,
                                customKeywords = customKeywords.trim()
                            )
                        )
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (category.id != 0L) {
                    TextButton(
                        onClick = { onDelete(category) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        }
    )
}
