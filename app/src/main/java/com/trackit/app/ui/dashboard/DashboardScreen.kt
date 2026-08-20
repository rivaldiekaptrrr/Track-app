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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.util.CategoryIconMapper
import com.trackit.app.util.CurrencyUtils
import com.trackit.app.util.DateUtils
import com.trackit.app.util.DebugLogger
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.shadow
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onNavigateToProfiles: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    val haptic = LocalHapticFeedback.current
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showDebugTerminal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { showDebugTerminal = !showDebugTerminal }) {
                        Icon(Icons.Default.Terminal, contentDescription = "Debug Terminal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
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
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Izin Notifikasi Nonaktif",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        "Peringatan anggaran tidak akan muncul. Ketuk untuk mengaktifkan.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SummarySection(
                        totalSpent = uiState.totalSpent,
                        totalIncome = uiState.totalIncome,
                        allTimeBalance = uiState.allTimeBalance,
                        monthlyBudget = uiState.monthlyBudget,
                        budgetRemaining = uiState.budgetRemaining,
                        lastSyncTime = uiState.lastSyncTime,
                        isExpenseOnlyMode = uiState.isExpenseOnlyMode
                    )
                }

                // Month Navigation + Recent Transactions Header
                item {
                    Column {
                        // Month navigator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Transaksi",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.navigateToPreviousMonth()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Bulan sebelumnya",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    DateUtils.formatMonthYear(selectedMonth),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (viewModel.isCurrentMonth()) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.navigateToNextMonth()
                                    },
                                    enabled = !viewModel.isCurrentMonth(),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Bulan berikutnya",
                                        modifier = Modifier.size(20.dp),
                                        tint = if (!viewModel.isCurrentMonth()) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
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
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEditTransaction(txWithCategory.transaction.id) 
                            },
                            onDelete = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDeleteDialog = txWithCategory.transaction.id 
                            }
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

    if (showDebugTerminal) {
        DebugTerminalOverlay(onClose = { showDebugTerminal = false })
    }
}

@Composable
fun DebugTerminalOverlay(onClose: () -> Unit) {
    val logs by DebugLogger.logs.collectAsStateWithLifecycle()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .clickable(enabled = false) {}, // Prevent clicks from closing when clicking inside
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Terminal / Debug Logs", color = Color.White, fontWeight = FontWeight.Bold)
                    Row {
                        TextButton(onClick = { DebugLogger.clear() }) {
                            Text("Clear", color = Color.Gray)
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
                Divider(color = Color.DarkGray)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp),
                    reverseLayout = true
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("ERROR") || log.contains("Aborted")) Color.Red else if (log.contains("SUCCESS")) Color.Green else Color.LightGray,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummarySection(
    totalSpent: Double,
    totalIncome: Double,
    allTimeBalance: Double,
    monthlyBudget: Double,
    budgetRemaining: Double,
    lastSyncTime: Long,
    isExpenseOnlyMode: Boolean = false
) {
    val saldoAktif = allTimeBalance  // gunakan saldo akumulatif, bukan hanya bulan ini
    var isBalanceVisible by remember { mutableStateOf(true) }
    val cardColor = MaterialTheme.colorScheme.primaryContainer
    val circleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val onCardColor = MaterialTheme.colorScheme.onPrimaryContainer
    val expenseColor = MaterialTheme.colorScheme.error
    val incomeColor = Color(0xFF66BB6A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color.Black.copy(alpha = 0.15f), // Softer shadow
                ambientColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawCircle(
                        color = circleColor,
                        radius = size.width * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                    )
                    drawContent()
                }
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = if (isExpenseOnlyMode) "PENGELUARAN BULAN INI" else "TOTAL SALDO",
                            style = if (isExpenseOnlyMode) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
                            color = onCardColor.copy(alpha = 0.7f),
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isBalanceVisible) {
                                if (isExpenseOnlyMode) CurrencyUtils.formatRupiah(totalSpent)
                                else CurrencyUtils.formatRupiah(saldoAktif)
                            } else "Rp •••••••••",
                            style = if (isExpenseOnlyMode) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpenseOnlyMode) expenseColor else onCardColor
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(iconBgColor)
                            .clickable { isBalanceVisible = !isBalanceVisible },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Balance",
                            tint = onCardColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(onCardColor.copy(alpha = 0.1f))
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                if (!isExpenseOnlyMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "PEMASUKAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = onCardColor.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "+ " + CurrencyUtils.formatRupiah(totalIncome),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = incomeColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "PENGELUARAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = onCardColor.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "- " + CurrencyUtils.formatRupiah(totalSpent),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = expenseColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (monthlyBudget > 0) {
                    val progress = (totalSpent / monthlyBudget).toFloat().coerceIn(0f, 1f)
                    val progressColor = if (progress > 0.8f) expenseColor else incomeColor

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SISA ANGGARAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = onCardColor.copy(alpha = 0.6f)
                            )
                            Text(
                                text = CurrencyUtils.formatRupiah(budgetRemaining),
                                style = MaterialTheme.typography.labelSmall,
                                color = progressColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(onCardColor.copy(alpha = 0.15f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(progressColor)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(incomeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = onCardColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    val syncTime = remember(lastSyncTime) {
                        java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(lastSyncTime))
                    }
                    Text(
                        text = "Terakhir sinkron: $syncTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = onCardColor.copy(alpha = 0.6f)
                    )
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
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.05f),
                ambientColor = Color.Black.copy(alpha = 0.02f)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                val isIncome = tx.type == "INCOME"
                val sign = if (isIncome) "+" else "-"
                val amountColor = if (isIncome) Color(0xFF81C784) else MaterialTheme.colorScheme.error

                Text(
                    text = "$sign ${CurrencyUtils.formatRupiah(tx.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor
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
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Savings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Mulai Catat Keuanganmu",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Gunakan tombol suara untuk mencatat pengeluaran\ndengan cepat dan mudah.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = { /* Could trigger voice but FAB handles it */ },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Coba Ucapkan Sesuatu")
        }
    }
}
