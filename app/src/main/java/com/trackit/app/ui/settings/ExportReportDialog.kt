package com.trackit.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportDialog(
    onDismiss: () -> Unit,
    onExportPdf: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit,
    onExportCsv: (title: String, startDate: Long, endDate: Long, typeFilter: String) -> Unit,
    isExpenseOnlyMode: Boolean = false
) {
    val today = Calendar.getInstance()
    val firstDayOfMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    var reportTitle by remember { mutableStateOf("Laporan Keuangan") }
    var startDateMillis by remember { mutableStateOf(firstDayOfMonth.timeInMillis) }
    var endDateMillis by remember { mutableStateOf(today.timeInMillis) }
    var typeFilter by remember { mutableStateOf(if (isExpenseOnlyMode) "EXPENSE" else "ALL") }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateDisplayFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    val typeOptions = if (isExpenseOnlyMode) {
        listOf("EXPENSE" to "Pengeluaran")
    } else {
        listOf(
            "ALL" to "Semua",
            "EXPENSE" to "Pengeluaran",
            "INCOME" to "Pemasukan"
        )
    }

    if (showStartDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { startDateMillis = it }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Batal") }
            }
        ) { DatePicker(state = state) }
    }

    if (showEndDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDateMillis)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { endDateMillis = it }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Batal") }
            }
        ) { DatePicker(state = state) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Ekspor Laporan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                // Judul laporan
                OutlinedTextField(
                    value = reportTitle,
                    onValueChange = { reportTitle = it },
                    label = { Text("Judul Laporan") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Tanggal Mulai
                Text(
                    "Rentang Tanggal",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Dari", style = MaterialTheme.typography.labelSmall)
                            Text(
                                dateDisplayFormat.format(Date(startDateMillis)),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Sampai", style = MaterialTheme.typography.labelSmall)
                            Text(
                                dateDisplayFormat.format(Date(endDateMillis)),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Filter Tipe
                Text(
                    "Jenis Transaksi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    typeOptions.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = typeFilter == value,
                            onClick = { typeFilter = value },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = typeOptions.size)
                        ) { Text(label) }
                    }
                }

                HorizontalDivider()

                // Tombol ekspor
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onExportCsv(reportTitle, startDateMillis, endDateMillis, typeFilter) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel")
                    }
                    Button(
                        onClick = { onExportPdf(reportTitle, startDateMillis, endDateMillis, typeFilter) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF")
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Batal")
                }
            }
        }
    }
}
