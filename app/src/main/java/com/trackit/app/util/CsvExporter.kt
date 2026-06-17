package com.trackit.app.util

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val fileNameFormat = SimpleDateFormat("ddMMyyyy", Locale.US)

    fun exportReport(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: Map<Long, CategoryEntity>,
        title: String,
        startDate: Long,
        endDate: Long,
        typeFilter: String = "ALL",  // "ALL", "EXPENSE", "INCOME"
        showIncomeColumn: Boolean = true
    ) {
        val filtered = when (typeFilter) {
            "EXPENSE" -> transactions.filter { it.type == "EXPENSE" }
            "INCOME"  -> transactions.filter { it.type == "INCOME" }
            else      -> transactions
        }

        val totalIncome  = filtered.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = filtered.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netBalance   = totalIncome - totalExpense

        val filterLabel = when (typeFilter) {
            "EXPENSE" -> "Pengeluaran"
            "INCOME"  -> "Pemasukan"
            else      -> "Semua"
        }

        try {
            val safeName = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val startStr = fileNameFormat.format(Date(startDate))
            val endStr   = fileNameFormat.format(Date(endDate))
            val fileName = "${safeName}_${startStr}-${endStr}.csv"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            FileOutputStream(file).use { fos ->
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // UTF-8 BOM for Excel
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->

                    // ─── Header Meta-data ────────────────────────────────────────
                    writer.write("Judul Laporan,${escapeCsv(title)}\n")
                    writer.write("Periode,${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}\n")
                    writer.write("Jenis Data,$filterLabel\n")
                    writer.write("Dibuat pada,${dateFormat.format(Date())}\n")
                    writer.write("\n")

                    // ─── Ringkasan ───────────────────────────────────────────────
                    writer.write("RINGKASAN\n")
                    if (showIncomeColumn) {
                        writer.write("Total Pemasukan,${totalIncome}\n")
                    }
                    writer.write("Total Pengeluaran,${totalExpense}\n")
                    if (showIncomeColumn) {
                        writer.write("Saldo Bersih,${netBalance}\n")
                    }
                    writer.write("\n")

                    // ─── Table Header ────────────────────────────────────────────
                    val headerCols = buildList {
                        add("NO")
                        add("Tanggal")
                        add("Pengeluaran")
                        if (showIncomeColumn) add("Pemasukan")
                        add("Kategori")
                        add("Keterangan")
                    }
                    writer.write(headerCols.joinToString(",") + "\n")

                    // ─── Table Rows ──────────────────────────────────────────────
                    filtered.forEachIndexed { index, tx ->
                        val dateStr      = dateFormat.format(Date(tx.date))
                        val categoryName = tx.categoryId?.let { categories[it]?.name } ?: "Lainnya"
                        val note         = escapeCsv(tx.description.ifEmpty { "-" })
                        val isIncome     = tx.type == "INCOME"

                        val expenseVal = if (!isIncome) tx.amount.toString() else "0"
                        val incomeVal  = if (isIncome) tx.amount.toString() else "0"

                        val row = buildList {
                            add("${index + 1}")
                            add(dateStr)
                            add(expenseVal)
                            if (showIncomeColumn) add(incomeVal)
                            add(escapeCsv(categoryName))
                            add(note)
                        }
                        writer.write(row.joinToString(",") + "\n")
                    }

                    // ─── Footer Total ────────────────────────────────────────────
                    writer.write("\n")
                    val totalRow = buildList {
                        add("TOTAL")
                        add("")
                        add(totalExpense.toString())
                        if (showIncomeColumn) add(totalIncome.toString())
                        add("")
                        add("")
                    }
                    writer.write(totalRow.joinToString(",") + "\n")
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Laporan Excel (CSV)"))

        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }

    // Backward-compatible wrapper
    fun exportMonthlyReport(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: Map<Long, CategoryEntity>,
        monthYear: String
    ) {
        val cal = java.util.Calendar.getInstance()
        val start = DateUtils.getStartOfMonth(cal)
        val end   = DateUtils.getEndOfMonth(cal)
        exportReport(context, transactions, categories, "Laporan $monthYear", start, end)
    }
}
