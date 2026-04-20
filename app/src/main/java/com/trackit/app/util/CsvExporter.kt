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

object CsvExporter {

    fun exportMonthlyReport(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: Map<Long, CategoryEntity>,
        monthYear: String
    ) {
        try {
            val fileName = "TrackIt_Laporan_${monthYear.replace(" ", "_")}.csv"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            // Write CSV using OutputStreamWriter with UTF-8 encoding
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Write BOM for Excel to properly recognize UTF-8
                    fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    
                    // CSV Header
                    writer.write("Tanggal,Kategori,Tipe,Deskripsi,Nominal\n")
                    
                    // CSV Body
                    for (transaction in transactions) {
                        val dateStr = DateUtils.formatDate(transaction.date)
                        val categoryName = transaction.categoryId?.let { categories[it]?.name } ?: "Lainnya"
                        val typeStr = if (transaction.type == "INCOME") "Pemasukan" else "Pengeluaran"
                        
                        // Escape description to handle commas and quotes
                        var desc = transaction.description
                        if (desc.contains(",") || desc.contains("\"") || desc.contains("\n")) {
                            desc = desc.replace("\"", "\"\"")
                            desc = "\"$desc\""
                        }
                        
                        // Negative sign for expenses in nominal column
                        val nominalStr = if (transaction.type == "EXPENSE") "-${transaction.amount}" else "${transaction.amount}"
                        
                        writer.write("$dateStr,$categoryName,$typeStr,$desc,$nominalStr\n")
                    }
                }
            }

            // Share the CSV file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Excel (CSV)"))

        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
