package com.trackit.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WeddingPdfExporter {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val fileNameFormat = SimpleDateFormat("ddMMyyyy", Locale.US)

    private fun formatRupiah(amount: Double): String {
        val formatted = String.format("%,.0f", amount).replace(",", ".")
        return "Rp $formatted"
    }

    private val categoryLabels = mapOf(
        "VENUE" to "Venue",
        "CATERING" to "Katering",
        "DECOR" to "Dekorasi",
        "MUA" to "MUA",
        "DOKUMENTASI" to "Dokumentasi",
        "SESERAHAN" to "Seserahan",
        "UNDANGAN" to "Undangan",
        "LAINNYA" to "Lainnya"
    )

    fun writeToStream(
        expenses: List<WeddingExpenseEntity>,
        profileName: String = "Anggaran Pernikahan",
        outputStream: java.io.OutputStream
    ) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val leftMargin = 28f
        val rightEdge = (pageWidth - leftMargin)
        val lineH = 18f

        // ── Paints ──────────────────────────────────────────────────────────
        val headerPaint = Paint().apply {
            color = Color.parseColor("#0D47A1")
            style = Paint.Style.FILL
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            isFakeBoldText = true
        }
        val subTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
        }
        val colHeaderPaint = Paint().apply {
            color = Color.parseColor("#1976D2")
            style = Paint.Style.FILL
        }
        val colTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8f
            isFakeBoldText = true
        }
        val rowEvenPaint = Paint().apply {
            color = Color.parseColor("#E3F2FD")
            style = Paint.Style.FILL
        }
        val cellTextPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 8f
        }
        val cellSmallPaint = Paint().apply {
            color = Color.parseColor("#424242")
            textSize = 7.5f
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#BBDEFB")
            strokeWidth = 0.5f
        }
        val summaryBgPaint = Paint().apply {
            color = Color.parseColor("#E1F5FE")
            style = Paint.Style.FILL
        }
        val summaryTextPaint = Paint().apply {
            color = Color.parseColor("#0D47A1")
            textSize = 9f
            isFakeBoldText = true
        }

        // ── Column setup ─────────────────────────────────────────────────────
        // Kolom: No | Kategori | Judul | Estimasi | Terbayar | Sisa | Status
        val colX = floatArrayOf(leftMargin, leftMargin + 18, leftMargin + 75, leftMargin + 175, leftMargin + 280, leftMargin + 375, leftMargin + 470)
        val colHeaders = arrayOf("No", "Kategori", "Judul Item", "Estimasi", "Terbayar", "Sisa Tagihan", "Status")

        var pageIndex = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var y = 0f

        fun startNewPage() {
            document.finishPage(page)
            pageIndex++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = 0f
        }

        fun drawHeader() {
            // Background header
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 70f, headerPaint)
            canvas.drawText(profileName, leftMargin, 30f, titlePaint)
            canvas.drawText("Laporan Anggaran Pernikahan  •  Dibuat: ${dateFormat.format(Date())}", leftMargin, 50f, subTitlePaint)
            y = 80f

            // Column headers
            canvas.drawRect(leftMargin, y, rightEdge.toFloat(), y + lineH + 4, colHeaderPaint)
            colHeaders.forEachIndexed { i, h ->
                canvas.drawText(h, colX[i] + 2, y + lineH - 3, colTextPaint)
            }
            y += lineH + 4
            canvas.drawLine(leftMargin, y, rightEdge.toFloat(), y, dividerPaint)
        }

        drawHeader()

        // ── Rows ─────────────────────────────────────────────────────────────
        var totalEstimated = 0.0
        var totalPaid = 0.0

        expenses.forEachIndexed { index, expense ->
            if (y > pageHeight - 60) {
                startNewPage()
                drawHeader()
            }

            if (index % 2 == 0) {
                canvas.drawRect(leftMargin, y, rightEdge.toFloat(), y + lineH, rowEvenPaint)
            }

            val sisa = expense.totalEstimated - expense.totalPaid
            val statusLabel = when (expense.paymentStatus) {
                "FULLY_PAID" -> "Lunas"
                "PARTIAL_DP" -> "DP"
                else -> "Belum"
            }
            val catLabel = categoryLabels[expense.category] ?: expense.category

            canvas.drawText("${index + 1}", colX[0] + 2, y + lineH - 3, cellTextPaint)
            canvas.drawText(catLabel, colX[1] + 2, y + lineH - 3, cellSmallPaint)
            canvas.drawText(expense.title.take(22), colX[2] + 2, y + lineH - 3, cellSmallPaint)
            canvas.drawText(formatRupiah(expense.totalEstimated), colX[3] + 2, y + lineH - 3, cellSmallPaint)
            canvas.drawText(formatRupiah(expense.totalPaid), colX[4] + 2, y + lineH - 3, cellSmallPaint)
            canvas.drawText(formatRupiah(sisa), colX[5] + 2, y + lineH - 3, cellSmallPaint)
            canvas.drawText(statusLabel, colX[6] + 2, y + lineH - 3, cellSmallPaint)

            canvas.drawLine(leftMargin, y + lineH, rightEdge.toFloat(), y + lineH, dividerPaint)
            y += lineH

            totalEstimated += expense.totalEstimated
            totalPaid += expense.totalPaid
        }

        // ── Summary ───────────────────────────────────────────────────────────
        y += 10f
        if (y > pageHeight - 55) startNewPage()
        val summaryHeight = 42f
        canvas.drawRect(leftMargin, y, rightEdge.toFloat(), y + summaryHeight, summaryBgPaint)
        canvas.drawText("Total Estimasi   : ${formatRupiah(totalEstimated)}", leftMargin + 8, y + 14, summaryTextPaint)
        canvas.drawText("Total Terbayar   : ${formatRupiah(totalPaid)}", leftMargin + 8, y + 28, summaryTextPaint)
        canvas.drawText("Total Sisa Tagihan: ${formatRupiah(totalEstimated - totalPaid)}", leftMargin + 8, y + 42, summaryTextPaint)

        document.finishPage(page)

        // ── Save to Stream ────────────────────────────────────────────────────
        try {
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }
}
