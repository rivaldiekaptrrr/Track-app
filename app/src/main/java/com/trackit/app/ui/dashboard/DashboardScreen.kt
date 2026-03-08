package com.trackit.app.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.util.CategoryIconMapper
import com.trackit.app.util.CurrencyUtils
import com.trackit.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onNavigateToChart: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TrackIt",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToChart) {
                        Icon(Icons.Default.PieChart, contentDescription = "Statistik")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tambah") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Cards
                item {
                    SummarySection(
                        totalSpent = uiState.totalSpent,
                        monthlyBudget = uiState.monthlyBudget,
                        budgetRemaining = uiState.budgetRemaining
                    )
                }

                // Recent Transactions Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Transaksi Terbaru",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            DateUtils.formatMonthYear(System.currentTimeMillis()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        EmptyState()
                    }
                } else {
                    items(
                        items = uiState.recentTransactions,
                        key = { it.transaction.id }
                    ) { txWithCategory ->
                        TransactionItem(
                            transactionWithCategory = txWithCategory,
                            onClick = { onEditTransaction(txWithCategory.transaction.id) },
                            onDelete = { showDeleteDialog = txWithCategory.transaction.id }
                        )
                    }
                }

                // Bottom spacer for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Delete Confirmation Dialog
        showDeleteDialog?.let { id ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Hapus Transaksi") },
                text = { Text("Yakin ingin menghapus transaksi ini?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTransaction(id)
                            showDeleteDialog = null
                        }
                    ) {
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
private fun SummarySection(
    totalSpent: Double,
    monthlyBudget: Double,
    budgetRemaining: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Total Pengeluaran Bulan Ini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    CurrencyUtils.formatRupiah(totalSpent),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (monthlyBudget > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val progress = (totalSpent / monthlyBudget).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (progress > 0.8f) Color(0xFFFF6B6B) else Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Sisa: ${CurrencyUtils.formatRupiah(budgetRemaining)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            "Anggaran: ${CurrencyUtils.formatRupiah(monthlyBudget)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transactionWithCategory: TransactionWithCategory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val tx = transactionWithCategory.transaction
    val category = transactionWithCategory.category

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
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
            // Category Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (category != null)
                            CategoryIconMapper.parseColor(category.colorHex).copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconMapper.getIcon(category?.iconName ?: ""),
                    contentDescription = null,
                    tint = if (category != null)
                        CategoryIconMapper.parseColor(category.colorHex)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Description & Category
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description.ifEmpty { category?.name ?: "Transaksi" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${category?.name ?: "Lainnya"} • ${DateUtils.formatDate(tx.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "- ${CurrencyUtils.formatRupiah(tx.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Delete Button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Hapus",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Belum ada transaksi",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Tekan tombol + untuk menambah transaksi pertama Anda",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
