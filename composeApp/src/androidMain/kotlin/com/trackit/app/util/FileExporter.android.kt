package com.trackit.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

actual class FileExporter(private val context: Context) {
    actual fun exportCsv(fileName: String, content: String, onComplete: (Boolean) -> Unit) {
        try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, fileName)
            file.writeText(content)
            shareFile(file, "text/csv")
            onComplete(true)
        } catch (e: Exception) {
            onComplete(false)
        }
    }

    actual fun exportPdf(fileName: String, bytes: ByteArray, onComplete: (Boolean) -> Unit) {
        try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            shareFile(file, "application/pdf")
            onComplete(true)
        } catch (e: Exception) {
            onComplete(false)
        }
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Dokumen").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
