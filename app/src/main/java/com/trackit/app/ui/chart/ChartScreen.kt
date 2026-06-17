package com.trackit.app.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.ui.theme.ChartColors
import com.trackit.app.util.CategoryIconMapper
import com.trackit.app.util.CurrencyUtils
import com.trackit.app.util.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Statistik", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Month navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.navigateToPreviousMonth()
                }
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Bulan sebelumnya",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = DateUtils.formatMonthYear(selectedMonth),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (viewModel.isCurrentMonth()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.navigateToNextMonth()
                },
                enabled = !viewModel.isCurrentMonth()
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Bulan berikutnya",
                    tint = if (!viewModel.isCurrentMonth()) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Type filter (tersembunyi jika expense only mode aktif)
        if (!uiState.isExpenseOnlyMode) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SegmentedButton(
                    selected = uiState.selectedTransactionType == "EXPENSE",
                    onClick = { viewModel.setTransactionType("EXPENSE") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Pengeluaran") }
                SegmentedButton(
                    selected = uiState.selectedTransactionType == "INCOME",
                    onClick = { viewModel.setTransactionType("INCOME") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Pemasukan") }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.spendingByCategory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Belum ada data ${if (uiState.selectedTransactionType == "INCOME") "pemasukan" else "pengeluaran"} bulan ini",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (uiState.selectedTransactionType == "INCOME") "Pemasukan per Kategori"
                                else "Pengeluaran per Kategori",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            PieChart(data = uiState.spendingByCategory, modifier = Modifier.size(220.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Total: ${CurrencyUtils.formatRupiah(uiState.totalSpent)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.selectedTransactionType == "INCOME")
                                    Color(0xFF66BB6A) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Detail per Kategori",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(uiState.spendingByCategory) { data ->
                    CategoryBreakdownItem(data)
                }
            }
        }
    }
}

@Composable
private fun PieChart(data: List<CategoryChartData>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val canvasSize = min(size.width, size.height)
        val radius = canvasSize / 2f
        val strokeWidth = canvasSize * 0.18f
        var startAngle = -90f
        data.forEachIndexed { index, item ->
            val sweepAngle = item.percentage / 100f * 360f
            val color = if (item.category != null)
                CategoryIconMapper.parseColor(item.category.colorHex)
            else ChartColors[index % ChartColors.size]
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    (size.width - canvasSize) / 2f + strokeWidth / 2f,
                    (size.height - canvasSize) / 2f + strokeWidth / 2f
                ),
                size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth),
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun CategoryBreakdownItem(data: CategoryChartData) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (data.category != null)
                            CategoryIconMapper.parseColor(data.category.colorHex).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconMapper.getIcon(data.category?.iconName ?: ""),
                    contentDescription = null,
                    tint = if (data.category != null)
                        CategoryIconMapper.parseColor(data.category.colorHex)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.category?.name ?: "Lainnya",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { data.percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (data.category != null)
                        CategoryIconMapper.parseColor(data.category.colorHex)
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyUtils.formatRupiah(data.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${String.format("%.1f", data.percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
