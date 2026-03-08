package com.trackit.app.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Statistik - ${DateUtils.formatMonthYear(System.currentTimeMillis())}",
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.spendingByCategory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Belum ada data pengeluaran bulan ini",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pie Chart
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Pengeluaran per Kategori",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            PieChart(
                                data = uiState.spendingByCategory,
                                modifier = Modifier.size(220.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Total: ${CurrencyUtils.formatRupiah(uiState.totalSpent)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Legend / Category Breakdown
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
private fun PieChart(
    data: List<CategoryChartData>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasSize = min(size.width, size.height)
        val radius = canvasSize / 2f
        val strokeWidth = canvasSize * 0.18f
        val innerRadius = radius - strokeWidth

        var startAngle = -90f

        data.forEachIndexed { index, item ->
            val sweepAngle = item.percentage / 100f * 360f
            val color = if (item.category != null)
                CategoryIconMapper.parseColor(item.category.colorHex)
            else
                ChartColors[index % ChartColors.size]

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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (data.category != null)
                            CategoryIconMapper.parseColor(data.category.colorHex).copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconMapper.getIcon(data.category?.iconName ?: ""),
                    contentDescription = null,
                    tint = if (data.category != null)
                        CategoryIconMapper.parseColor(data.category.colorHex)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
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
                    progress = data.percentage / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (data.category != null)
                        CategoryIconMapper.parseColor(data.category.colorHex)
                    else
                        MaterialTheme.colorScheme.primary,
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
