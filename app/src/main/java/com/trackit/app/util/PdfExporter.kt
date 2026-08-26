package com.trackit.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val fileNameFormat = SimpleDateFormat("ddMMyyyy", Locale.US)

    fun exportReport(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: Map<String, CategoryEntity>,
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

        val totalIncome  = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netBalance   = totalIncome - totalExpense

        val document   = PdfDocument()
        val pageWidth  = 595
        val pageHeight = 842
        val leftMargin = 32f
        val rightEdge  = (pageWidth - leftMargin).toFloat()
        val lineH      = 20f

        // ── Paints ────────────────────────────────────────────────────────
        val titlePaint = Paint().apply {
            textSize = 20f; isFakeBoldText = true
            color = Color.parseColor("#1B6B4F")
        }
        val headerPaint = Paint().apply {
            textSize = 10f; isFakeBoldText = true
            color = Color.parseColor("#333333")
        }
        val subHeaderPaint = Paint().apply {
            textSize = 9f; color = Color.parseColor("#666666")
        }
        val textPaint = Paint().apply {
            textSize = 9f; color = Color.parseColor("#444444")
        }
        val boldTextPaint = Paint().apply {
            textSize = 9f; isFakeBoldText = true; color = Color.parseColor("#222222")
        }
        val expensePaint = Paint().apply {
            textSize = 9f; isFakeBoldText = true
            color = Color.parseColor("#C62828")
        }
        val incomePaint = Paint().apply {
            textSize = 9f; isFakeBoldText = true
            color = Color.parseColor("#2E7D32")
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CCCCCC"); strokeWidth = 0.5f
        }
        val thickLinePaint = Paint().apply {
            color = Color.parseColor("#1B6B4F"); strokeWidth = 1f
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#E8F5E9")
        }

        // ── Column Layout ──────────────────────────────────────────────────
        // NO | Tanggal | Pengeluaran | (Pemasukan) | Kategori | Keterangan
        val colNo       = leftMargin
        val colDate     = leftMargin + 24f
        val colExpense  = leftMargin + 105f
        val colIncome   = if (showIncomeColumn) leftMargin + 175f else 0f
        val colCategory = if (showIncomeColumn) leftMargin + 245f else leftMargin + 175f
        val colNote     = if (showIncomeColumn) leftMargin + 340f else leftMargin + 270f

        var pageNumber = 1
        var y = 0f
        var currentPage: PdfDocument.Page? = null
        lateinit var canvas: Canvas

        fun newPage() {
            currentPage?.let { document.finishPage(it) }
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            currentPage = document.startPage(info)
            canvas = currentPage!!.canvas
            y = 40f
        }

        fun drawTableHeader() {
            // Header row background
            canvas.drawRect(leftMargin, y - lineH + 4, rightEdge, y + 4, headerBgPaint)
            canvas.drawText("NO",          colNo,       y, headerPaint)
            canvas.drawText("TANGGAL",     colDate,     y, headerPaint)
            canvas.drawText("PENGELUARAN", colExpense,  y, headerPaint)
            if (showIncomeColumn) canvas.drawText("PEMASUKAN", colIncome, y, headerPaint)
            canvas.drawText("KATEGORI",    colCategory, y, headerPaint)
            canvas.drawText("KETERANGAN",  colNote,     y, headerPaint)
            y += 4f
            canvas.drawLine(leftMargin, y, rightEdge, y, thickLinePaint)
            y += lineH
        }

        // ── Page 1: Build ─────────────────────────────────────────────────
        newPage()

        // App title
        canvas.drawText("TrackIt", leftMargin, y, titlePaint)
        y += 24f
        canvas.drawLine(leftMargin, y, rightEdge, y, thickLinePaint)
        y += 12f

        // Report header info
        canvas.drawText("Judul Laporan  : $title", leftMargin, y, headerPaint)
        y += lineH
        canvas.drawText(
            "Periode          : ${dateFormat.format(Date(startDate))} — ${dateFormat.format(Date(endDate))}",
            leftMargin, y, subHeaderPaint
        )
        y += lineH
        val filterLabel = when (typeFilter) {
            "EXPENSE" -> "Pengeluaran"
            "INCOME"  -> "Pemasukan"
            else      -> "Semua Transaksi"
        }
        canvas.drawText("Jenis Data     : $filterLabel", leftMargin, y, subHeaderPaint)
        y += lineH
        canvas.drawText("Dibuat pada   : ${dateFormat.format(Date())}", leftMargin, y, subHeaderPaint)
        y += lineH + 8f

        // Summary box
        canvas.drawLine(leftMargin, y, rightEdge, y, linePaint)
        y += 10f
        if (showIncomeColumn) {
            canvas.drawText("Total Pemasukan  : +${CurrencyUtils.formatRupiah(totalIncome)}", leftMargin, y, incomePaint)
            y += lineH
        }
        canvas.drawText("Total Pengeluaran : -${CurrencyUtils.formatRupiah(totalExpense)}", leftMargin, y, expensePaint)
        y += lineH
        if (showIncomeColumn) {
            val balanceColor = if (netBalance >= 0) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            val balancePaint = Paint().apply {
                textSize = 9f; isFakeBoldText = true; color = balanceColor
            }
            canvas.drawText("Saldo Bersih       : ${CurrencyUtils.formatRupiah(netBalance)}", leftMargin, y, balancePaint)
            y += lineH
        }
        y += 8f
        canvas.drawLine(leftMargin, y, rightEdge, y, linePaint)
        y += 16f

        // Table header
        drawTableHeader()

        // Table rows
        filtered.forEachIndexed { index, tx ->
            if (y > pageHeight - 60) {
                newPage()
                drawTableHeader()
            }

            val categoryName = tx.categoryId?.let { categories[it]?.name } ?: "Lainnya"
            val note = if (tx.description.length > 28) tx.description.take(28) + "…" else tx.description.ifEmpty { "-" }
            val dateStr = dateFormat.format(Date(tx.date))
            val isIncome = tx.type == "INCOME"

            // Alternating row
            if (index % 2 == 1) {
                val rowBg = Paint().apply { color = Color.parseColor("#FAFAFA") }
                canvas.drawRect(leftMargin, y - lineH + 4, rightEdge, y + 4, rowBg)
            }

            canvas.drawText("${index + 1}", colNo, y, textPaint)
            canvas.drawText(dateStr, colDate, y, textPaint)

            if (isIncome) {
                canvas.drawText("-", colExpense, y, textPaint)
                if (showIncomeColumn) canvas.drawText("+${CurrencyUtils.formatRupiah(tx.amount)}", colIncome, y, incomePaint)
            } else {
                canvas.drawText("-${CurrencyUtils.formatRupiah(tx.amount)}", colExpense, y, expensePaint)
                if (showIncomeColumn) canvas.drawText("-", colIncome, y, textPaint)
            }

            canvas.drawText(categoryName, colCategory, y, textPaint)
            canvas.drawText(note, colNote, y, textPaint)

            canvas.drawLine(leftMargin, y + 4, rightEdge, y + 4, linePaint)
            y += lineH
        }

        // Footer summary row
        y += 4f
        canvas.drawLine(leftMargin, y, rightEdge, y, thickLinePaint)
        y += lineH
        canvas.drawText("TOTAL", colNo, y, boldTextPaint)
        canvas.drawText("-${CurrencyUtils.formatRupiah(totalExpense)}", colExpense, y, expensePaint)
        if (showIncomeColumn) canvas.drawText("+${CurrencyUtils.formatRupiah(totalIncome)}", colIncome, y, incomePaint)

        // Finish last page
        document.finishPage(currentPage!!)

        // Save & share
        try {
            val safeName = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val startStr = fileNameFormat.format(Date(startDate))
            val endStr   = fileNameFormat.format(Date(endDate))
            val fileName = "${safeName}_${startStr}-${endStr}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            FileOutputStream(file).use { document.writeTo(it) }
            document.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Laporan PDF"))
        } catch (e: Exception) {
            document.close()
            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Backward-compatible wrapper
    fun exportMonthlyReport(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: Map<String, CategoryEntity>,
        monthYear: String,
        totalSpent: Double
    ) {
        val cal = java.util.Calendar.getInstance()
        val start = DateUtils.getStartOfMonth(cal)
        val end   = DateUtils.getEndOfMonth(cal)
        exportReport(context, transactions, categories, "Laporan $monthYear", start, end)
    }
}
