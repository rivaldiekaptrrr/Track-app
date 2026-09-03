package com.trackit.app.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackit.app.ui.dashboard.TransactionItem
import com.trackit.app.ui.theme.ChartColors
import com.trackit.app.util.CategoryIconMapper
import com.trackit.app.util.CurrencyUtils
import com.trackit.app.util.DateUtils
import kotlin.math.min

private val MONTH_LABELS = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pie Chart", "Tren")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Statistik", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )
        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Medium) }
                )
            }
        }
        when (selectedTab) {
            0 -> PieChartTab(uiState, selectedMonth, haptic, viewModel)
            1 -> TrendTab(uiState, selectedYear, searchQuery, haptic, viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PieChartTab(
    uiState: ChartUiState,
    selectedMonth: Long,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    viewModel: ChartViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (!uiState.isExpenseOnlyMode) {
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SegmentedButton(selected = uiState.selectedTransactionType == "EXPENSE", onClick = { viewModel.setTransactionType("EXPENSE") }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) { Text("Pengeluaran") }
                SegmentedButton(selected = uiState.selectedTransactionType == "INCOME", onClick = { viewModel.setTransactionType("INCOME") }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) { Text("Pemasukan") }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.navigateToPreviousMonth() }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan sebelumnya", tint = MaterialTheme.colorScheme.primary)
            }
            Text(text = DateUtils.formatMonthYear(selectedMonth), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (viewModel.isCurrentMonth()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.navigateToNextMonth() }, enabled = !viewModel.isCurrentMonth()) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Bulan berikutnya", tint = if (!viewModel.isCurrentMonth()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.spendingByCategory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada data bulan ini", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (uiState.selectedTransactionType == "INCOME") "Pemasukan per Kategori" else "Pengeluaran per Kategori", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(24.dp))
                            PieChart(data = uiState.spendingByCategory, modifier = Modifier.size(220.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Total: ${CurrencyUtils.formatRupiah(uiState.totalSpent)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (uiState.selectedTransactionType == "INCOME") Color(0xFF66BB6A) else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item { Text("Detail per Kategori", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                items(uiState.spendingByCategory) { data -> CategoryBreakdownItem(data) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendTab(
    uiState: ChartUiState,
    selectedYear: Int,
    searchQuery: String,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    viewModel: ChartViewModel
) {
    val trendData = uiState.monthlyTrendData
    val totalYear = trendData.sumOf { it.amount }
    val activeMonths = trendData.count { it.amount > 0 }
    val avgPerMonth = if (activeMonths > 0) totalYear / activeMonths else 0.0
    val highestMonth = trendData.maxByOrNull { it.amount }
    val lowestMonth = trendData.filter { it.amount > 0 }.minByOrNull { it.amount }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.navigateToPreviousYear() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Tahun sebelumnya", tint = MaterialTheme.colorScheme.primary)
                }
                Text(text = "$selectedYear", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (viewModel.isCurrentYear()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.navigateToNextYear() }, enabled = !viewModel.isCurrentYear()) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Tahun berikutnya", tint = if (!viewModel.isCurrentYear()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Ringkasan $selectedYear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SummaryStatItem("Total Tahun", CurrencyUtils.formatRupiah(totalYear), Modifier.weight(1f))
                        SummaryStatItem("Rata-rata/Bulan", CurrencyUtils.formatRupiah(avgPerMonth), Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SummaryStatItem("Bulan Tertinggi", if (highestMonth != null && highestMonth.amount > 0) "${MONTH_LABELS[highestMonth.monthIndex]}\n${CurrencyUtils.formatRupiah(highestMonth.amount)}" else "-", Modifier.weight(1f))
                        SummaryStatItem("Bulan Terendah", if (lowestMonth != null) "${MONTH_LABELS[lowestMonth.monthIndex]}\n${CurrencyUtils.formatRupiah(lowestMonth.amount)}" else "-", Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Tren Bulanan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (trendData.isEmpty() || trendData.all { it.amount == 0.0 }) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada data di tahun ini", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LineChart(data = trendData, modifier = Modifier.fillMaxWidth().height(160.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MONTH_LABELS.forEach { label ->
                                Text(text = label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
        item { Text("Cari Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        item {
            OutlinedTextField(value = searchQuery, onValueChange = { viewModel.updateSearchQuery(it) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Ketik nama pengeluaran...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") }, shape = RoundedCornerShape(12.dp), singleLine = true)
        }
        if (searchQuery.isNotEmpty()) {
            if (uiState.searchResults.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ada hasil pencarian", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            } else {
                item { Text("${uiState.searchResults.size} transaksi ditemukan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(uiState.searchResults) { tx -> TransactionItem(transactionWithCategory = tx, onClick = { }, onDelete = { }) }
            }
        }
    }
}

@Composable
private fun SummaryStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun PieChart(data: List<CategoryChartData>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val canvasSize = min(size.width, size.height)
        val strokeWidth = canvasSize * 0.18f
        var startAngle = -90f
        data.forEachIndexed { index, item ->
            val sweepAngle = item.percentage / 100f * 360f
            val color = if (item.category != null) CategoryIconMapper.parseColor(item.category.colorHex) else ChartColors[index % ChartColors.size]
            drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, topLeft = Offset((size.width - canvasSize) / 2f + strokeWidth / 2f, (size.height - canvasSize) / 2f + strokeWidth / 2f), size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth), style = Stroke(width = strokeWidth))
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun CategoryBreakdownItem(data: CategoryChartData) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (data.category != null) CategoryIconMapper.parseColor(data.category.colorHex).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(imageVector = CategoryIconMapper.getIcon(data.category?.iconName ?: ""), contentDescription = null, tint = if (data.category != null) CategoryIconMapper.parseColor(data.category.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = data.category?.name ?: "Lainnya", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(progress = { data.percentage / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = if (data.category != null) CategoryIconMapper.parseColor(data.category.colorHex) else MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(text = CurrencyUtils.formatRupiah(data.amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = "${String.format("%.1f", data.percentage)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LineChart(data: List<MonthlyTrendData>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var chartWidthPx by remember { mutableStateOf(0f) }
    var chartHeightPx by remember { mutableStateOf(0f) }
    var stepXPx by remember { mutableStateOf(0f) }
    var paddingTopPx by remember { mutableStateOf(0f) }
    var chartAreaHeightPx by remember { mutableStateOf(0f) }
    var effectiveMaxVal by remember { mutableStateOf(1.0) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures { tapOffset ->
                        if (data.size < 2) return@detectTapGestures
                        val step = size.width.toFloat() / (data.size - 1).coerceAtLeast(1)
                        // Find the nearest point index to the tap
                        val tappedIndex = (tapOffset.x / step).toInt().coerceIn(0, data.size - 1)
                        // Toggle off if tapped same point again
                        selectedIndex = if (selectedIndex == tappedIndex) null else tappedIndex
                    }
                }
        ) {
            if (data.isEmpty()) return@Canvas
            val maxAmount = data.maxOfOrNull { it.amount } ?: 1.0
            effectiveMaxVal = if (maxAmount == 0.0) 1.0 else maxAmount
            paddingTopPx = 12.dp.toPx()
            val paddingBottom = 12.dp.toPx()
            chartAreaHeightPx = size.height - paddingTopPx - paddingBottom
            stepXPx = size.width / (data.size - 1).coerceAtLeast(1)
            chartWidthPx = size.width
            chartHeightPx = size.height

            // Grid lines
            for (i in 0..2) {
                val y = paddingTopPx + chartAreaHeightPx * (i / 2f)
                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            // Line path
            val path = Path()
            var firstPoint = true
            data.forEachIndexed { index, item ->
                val x = index * stepXPx
                val y = paddingTopPx + chartAreaHeightPx - (item.amount / effectiveMaxVal * chartAreaHeightPx).toFloat()
                if (firstPoint) { path.moveTo(x, y); firstPoint = false } else { path.lineTo(x, y) }
            }
            drawPath(path = path, color = primaryColor, style = Stroke(width = 3.dp.toPx()))
            // Dots
            data.forEachIndexed { index, item ->
                val x = index * stepXPx
                val y = paddingTopPx + chartAreaHeightPx - (item.amount / effectiveMaxVal * chartAreaHeightPx).toFloat()
                val isSelected = selectedIndex == index
                if (isSelected) {
                    // Highlight ring for selected point
                    drawCircle(color = selectedColor.copy(alpha = 0.25f), radius = 12.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = Color.White, radius = 7.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = selectedColor, radius = 5.dp.toPx(), center = Offset(x, y))
                } else {
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = primaryColor, radius = 3.5.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        // Tooltip overlay
        selectedIndex?.let { idx ->
            if (idx in data.indices && chartWidthPx > 0f && stepXPx > 0f) {
                val item = data[idx]
                val dotXPx = idx * stepXPx
                val dotYPx = paddingTopPx + chartAreaHeightPx - (item.amount / effectiveMaxVal * chartAreaHeightPx).toFloat()

                val tooltipWidthDp = 120.dp
                val tooltipHeightDp = 52.dp

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val totalWidthPx = constraints.maxWidth.toFloat()
                    val dotXDp = with(LocalDensity.current) { dotXPx.toDp() }
                    val dotYDp = with(LocalDensity.current) { dotYPx.toDp() }

                    // Clamp tooltip horizontally so it never goes off screen
                    val rawOffsetX = dotXDp - tooltipWidthDp / 2
                    val tooltipOffsetX = rawOffsetX.coerceIn(0.dp, with(LocalDensity.current) { totalWidthPx.toDp() } - tooltipWidthDp)
                    // Show tooltip above the dot (with 14dp gap)
                    val tooltipOffsetY = (dotYDp - tooltipHeightDp - 14.dp).coerceAtLeast(0.dp)

                    Card(
                        modifier = Modifier
                            .offset(x = tooltipOffsetX, y = tooltipOffsetY)
                            .width(tooltipWidthDp)
                            .height(tooltipHeightDp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = MONTH_LABELS.getOrElse(item.monthIndex) { "Bln ${item.monthIndex + 1}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = CurrencyUtils.formatRupiah(item.amount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
