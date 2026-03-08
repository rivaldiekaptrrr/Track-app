package com.trackit.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.data.local.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun exportMonthlyReport(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: Map<Long, CategoryEntity>,
        monthYear: String,
        totalSpent: Double
    ) {
        val document = PdfDocument()
        val pageWidth = 595  // A4
        val pageHeight = 842

        var pageNumber = 1
        var yPosition = 80f
        val leftMargin = 40f
        val lineHeight = 22f

        val titlePaint = Paint().apply {
            textSize = 24f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor("#1B6B4F")
        }

        val headerPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor("#333333")
        }

        val textPaint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.parseColor("#555555")
        }

        val amountPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor("#C24D6E")
        }

        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#DDDDDD")
            strokeWidth = 1f
        }

        fun newPage(): Pair<PdfDocument.Page, Canvas> {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            val page = document.startPage(pageInfo)
            yPosition = 80f
            return Pair(page, page.canvas)
        }

        var currentPage: PdfDocument.Page? = null
        lateinit var canvas: Canvas
        
        val (initialPage, initialCanvas) = newPage()
        currentPage = initialPage
        canvas = initialCanvas

        // Title
        canvas.drawText("TrackIt - Laporan Pengeluaran", leftMargin, yPosition, titlePaint)
        yPosition += 30f

        canvas.drawText("Periode: $monthYear", leftMargin, yPosition, headerPaint)
        yPosition += 20f

        canvas.drawText("Total: ${CurrencyUtils.formatRupiah(totalSpent)}", leftMargin, yPosition, headerPaint)
        yPosition += 30f

        // Table Header
        canvas.drawLine(leftMargin, yPosition, pageWidth - leftMargin, yPosition, linePaint)
        yPosition += 15f

        canvas.drawText("Tanggal", leftMargin, yPosition, headerPaint)
        canvas.drawText("Kategori", leftMargin + 100, yPosition, headerPaint)
        canvas.drawText("Deskripsi", leftMargin + 220, yPosition, headerPaint)
        canvas.drawText("Nominal", leftMargin + 400, yPosition, headerPaint)
        yPosition += 5f

        canvas.drawLine(leftMargin, yPosition, pageWidth - leftMargin, yPosition, linePaint)
        yPosition += lineHeight

        // Transactions
        for (transaction in transactions) {
            if (yPosition > pageHeight - 60) {
                document.finishPage(currentPage)
                val (newPage, newCanvas) = newPage()
                currentPage = newPage
                canvas = newCanvas
            }

            val categoryName = transaction.categoryId?.let { categories[it]?.name } ?: "Lainnya"
            val dateStr = DateUtils.formatDate(transaction.date)
            val desc = if (transaction.description.length > 25) 
                transaction.description.substring(0, 25) + "..." 
            else 
                transaction.description.ifEmpty { "-" }

            canvas.drawText(dateStr, leftMargin, yPosition, textPaint)
            canvas.drawText(categoryName, leftMargin + 100, yPosition, textPaint)
            canvas.drawText(desc, leftMargin + 220, yPosition, textPaint)
            canvas.drawText(CurrencyUtils.formatRupiah(transaction.amount), leftMargin + 400, yPosition, amountPaint)

            yPosition += lineHeight
        }

        // Finish last page
        document.finishPage(currentPage)

        // Save file
        try {
            val fileName = "TrackIt_${monthYear.replace(" ", "_")}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.close()
            document.close()

            // Share the PDF
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
            document.close()
        }
    }
}
