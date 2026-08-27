package com.trackit.app.ui.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.util.CategoryIconMapper
import com.trackit.app.util.CurrencyUtils
import com.trackit.app.ui.wedding.common.DeleteConfirmDialog
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryBudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.savedMessage) {
        uiState.savedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Budget Kategori", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Atur batas pengeluaran per kategori",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Info header
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Tekan ikon edit (✏️) pada kategori untuk mengatur batas budget dan persentase notifikasi peringatan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                items(uiState.items, key = { it.category.id }) { item ->
                    CategoryBudgetCard(
                        item = item,
                        onStartEditing = { viewModel.startEditing(item.category.id) },
                        onCancelEditing = { viewModel.cancelEditing(item.category.id) },
                        onAmountChange = { viewModel.updateInputAmount(item.category.id, it) },
                        onAlertPctChange = { viewModel.updateInputAlertPct(item.category.id, it) },
                        onSave = { viewModel.saveBudget(item.category.id) },
                        onDelete = { viewModel.deleteBudget(item.category.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetCard(
    item: CategoryBudgetItem,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onAmountChange: (String) -> Unit,
    onAlertPctChange: (Float) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(item.category.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val hasBudget = item.budgetAmount > 0.0
    val progressRatio = if (hasBudget) min(item.spent / item.budgetAmount, 1.0).toFloat() else 0f
    val progressColor = when {
        progressRatio >= 1f -> MaterialTheme.colorScheme.error
        progressRatio >= item.alertPercentage -> Color(0xFFF57C00) // Orange
        else -> Color(0xFF2E7D32) // Green
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: icon + name + budget summary + edit button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category icon (Categorical Squircle Badge)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIconMapper.getIcon(item.category.iconName),
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.category.name, fontWeight = FontWeight.SemiBold)
                    if (hasBudget) {
                        Text(
                            "Budget: ${CurrencyUtils.formatRupiah(item.budgetAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Belum ada budget",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = onStartEditing) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit budget",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Progress bar (hanya tampil jika ada budget)
            if (hasBudget) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Terpakai: ${CurrencyUtils.formatRupiah(item.spent)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = progressColor
                    )
                    Text(
                        "${(progressRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = progressColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Edit form (animated)
            AnimatedVisibility(
                visible = item.isEditing,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Amount input
                    Text(
                        "Batas Budget per Bulan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = item.inputAmount,
                        onValueChange = { onAmountChange(it.filter { c -> c.isDigit() }) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("0") },
                        prefix = { Text("Rp") },
                        visualTransformation = com.trackit.app.ui.transaction.ThousandSeparatorVisualTransformation(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Alert percentage slider
                    val alertPctDisplay = (item.inputAlertPct * 100).toInt()
                    Text(
                        "Notifikasi peringatan saat mencapai $alertPctDisplay%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = item.inputAlertPct,
                        onValueChange = onAlertPctChange,
                        valueRange = 0.5f..0.95f,
                        steps = 8, // 50%, 55%, 60%, 65%, 70%, 75%, 80%, 85%, 90%, 95%
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("50%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("95%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (hasBudget) {
                            OutlinedButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hapus")
                            }
                        }
                        OutlinedButton(
                            onClick = onCancelEditing,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            title = "Hapus Budget?",
            message = "Batas budget untuk kategori \"${item.category.name}\" akan dihapus.",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            }
        )
    }
}
