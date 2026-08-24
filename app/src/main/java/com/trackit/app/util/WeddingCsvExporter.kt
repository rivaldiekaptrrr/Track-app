package com.trackit.app.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WeddingCsvExporter {

    private val fileNameFormat = SimpleDateFormat("ddMMyyyy", Locale.US)

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
        try {
            val sb = StringBuilder()
            // BOM for Excel UTF-8 compatibility
            sb.append("\uFEFF")
            // Title row
            sb.append("Laporan Anggaran Pernikahan\n")
            sb.append("$profileName\n")
            sb.append("Tanggal Ekspor,${SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())}\n")
            sb.append("\n")

            // Column headers
            sb.append("No,Kategori,Judul Item,Total Estimasi,Total Terbayar,Sisa Tagihan,Status Pembayaran,Sumber Dana,Catatan\n")

            var totalEstimated = 0.0
            var totalPaid = 0.0

            expenses.forEachIndexed { index, expense ->
                val sisa = expense.totalEstimated - expense.totalPaid
                val catLabel = categoryLabels[expense.category] ?: expense.category
                val statusLabel = when (expense.paymentStatus) {
                    "FULLY_PAID" -> "Lunas"
                    "PARTIAL_DP" -> "DP / Sebagian"
                    else -> "Belum Dibayar"
                }
                val sourceLabel = when (expense.paidBySource) {
                    "TABUNGAN_CPP" -> "Tabungan Calon Pengantin Pria"
                    "TABUNGAN_CPW" -> "Tabungan Calon Pengantin Wanita"
                    "ORTU_CPP" -> "Orang Tua Pria"
                    "ORTU_CPW" -> "Orang Tua Wanita"
                    else -> "Bersama"
                }
                val notes = expense.notes?.replace(",", " ") ?: ""

                sb.append("${index + 1},")
                sb.append("$catLabel,")
                sb.append("\"${expense.title}\",")
                sb.append("${expense.totalEstimated.toLong()},")
                sb.append("${expense.totalPaid.toLong()},")
                sb.append("${sisa.toLong()},")
                sb.append("$statusLabel,")
                sb.append("$sourceLabel,")
                sb.append("\"$notes\"\n")

                totalEstimated += expense.totalEstimated
                totalPaid += expense.totalPaid
            }

            // Summary rows
            sb.append("\n")
            sb.append("RINGKASAN\n")
            sb.append("Total Estimasi,,,,${totalEstimated.toLong()}\n")
            sb.append("Total Terbayar,,,,,${totalPaid.toLong()}\n")
            sb.append("Total Sisa Tagihan,,,,,,${(totalEstimated - totalPaid).toLong()}\n")

            OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(sb.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
