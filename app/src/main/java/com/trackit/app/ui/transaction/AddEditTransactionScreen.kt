package com.trackit.app.ui.transaction

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.util.CategoryIconMapper
import com.trackit.app.util.DateUtils
import com.trackit.app.util.NumberUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    ocrAmount: Double? = null,
    onNavigateBack: () -> Unit,
    onOpenCamera: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = formState.date
    )

    // Set OCR amount if available
    LaunchedEffect(ocrAmount) {
        ocrAmount?.let { viewModel.setAmountFromOcr(it) }
    }

    val formattedAmount = remember(formState.amount) {
        NumberUtils.formatWithThousandSeparators(formState.amount)
    }

    // Navigate back on save
    LaunchedEffect(formState.savedSuccessfully) {
        if (formState.savedSuccessfully) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (formState.isEditing) "Edit Transaksi" else "Tambah Transaksi",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Amount Input with Camera Icon
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Nominal",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Rp",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = formattedAmount,
                            onValueChange = { viewModel.updateAmount(it) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("0") },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalIconButton(onClick = onOpenCamera) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Pindai Struk"
                            )
                        }
                    }
                }
            }

            // Description
            OutlinedTextField(
                value = formState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Deskripsi (opsional)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Date Picker
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Tanggal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            DateUtils.formatDate(formState.date),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Category Selection
            Text(
                "Kategori",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.heightIn(max = 250.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(formState.categories) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = formState.selectedCategoryId == category.id,
                        onClick = { viewModel.updateCategory(category.id) }
                    )
                }
            }

            // Recurring Transaction Toggle
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Transaksi Berulang",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Switch(
                            checked = formState.isRecurring,
                            onCheckedChange = { viewModel.updateRecurring(it) }
                        )
                    }
                    if (formState.isRecurring) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("DAILY" to "Harian", "WEEKLY" to "Mingguan", "MONTHLY" to "Bulanan").forEach { (type, label) ->
                                FilterChip(
                                    selected = formState.recurringType == type,
                                    onClick = { viewModel.updateRecurringType(type) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // Error Message
            formState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Save Button
            Button(
                onClick = { viewModel.saveTransaction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !formState.isSaving,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Simpan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                viewModel.updateDate(it)
                            }
                            showDatePicker = false
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Batal")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected)
            CategoryIconMapper.parseColor(category.colorHex).copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "category_bg"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (isSelected) Modifier.border(
                    2.dp,
                    CategoryIconMapper.parseColor(category.colorHex),
                    RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(CategoryIconMapper.parseColor(category.colorHex).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIconMapper.getIcon(category.iconName),
                contentDescription = category.name,
                tint = CategoryIconMapper.parseColor(category.colorHex),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
