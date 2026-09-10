package com.trackit.app.util

expect class FileExporter {
    fun exportCsv(fileName: String, content: String, onComplete: (Boolean) -> Unit)
    fun exportPdf(fileName: String, bytes: ByteArray, onComplete: (Boolean) -> Unit)
}
