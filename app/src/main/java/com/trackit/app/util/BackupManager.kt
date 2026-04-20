package com.trackit.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.trackit.app.data.local.TrackItDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

import android.os.Environment

object BackupManager {

    var isRestoring = false

    private const val DATABASE_NAME = "trackit_database"
    private const val BACKUP_FOLDER = "TrackIt"
    private const val AUTO_BACKUP_FILE = "autobackup.db"

    fun backupDatabase(context: Context) {
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            val backupFile = File(context.cacheDir, "TrackIt_Backup_${System.currentTimeMillis()}.db")

            if (dbFile.exists()) {
                // Ensure the database is closed or flushed? 
                // For Room, it's better to use a checkpoint or just copy if app is idle.
                copyFile(dbFile, backupFile)
                
                // Also copy -wal and -shm files if they exist (Write-Ahead Logging)
                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")
                if (walFile.exists()) copyFile(walFile, File(backupFile.path + "-wal"))
                if (shmFile.exists()) copyFile(shmFile, File(backupFile.path + "-shm"))

                shareFile(context, backupFile)
            } else {
                Toast.makeText(context, "Database tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal backup: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun restoreDatabase(context: Context, backupUri: Uri) {
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            
            // Close database before restore? 
            // In a real app, you'd need to restart the app after this.
            
            context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // Delete WAL and SHM files to ensure the new main DB file is used
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            Toast.makeText(context, "Data berhasil dipulihkan! Silakan buka ulang aplikasi.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal restore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun autoBackup(context: Context) {
        if (isRestoring) return
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) return

            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val trackItDir = File(documentsDir, BACKUP_FOLDER)
            if (!trackItDir.exists()) trackItDir.mkdirs()

            val backupFile = File(trackItDir, AUTO_BACKUP_FILE)
            
            copyFile(dbFile, backupFile)
            
            // Backup WAL and SHM if exists
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            if (walFile.exists()) copyFile(walFile, File(backupFile.path + "-wal"))
            if (shmFile.exists()) copyFile(shmFile, File(backupFile.path + "-shm"))
        } catch (e: Exception) {
            // Silently fail or log for auto backup
            e.printStackTrace()
        }
    }

    fun getAutoBackupFile(): File? {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val backupFile = File(File(documentsDir, BACKUP_FOLDER), AUTO_BACKUP_FILE)
        return if (backupFile.exists()) backupFile else null
    }

    fun restoreFromAutoBackup(context: Context) {
        val backupFile = getAutoBackupFile() ?: return
        try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            copyFile(backupFile, dbFile)
            
            // Restore WAL and SHM if exists
            val walBackup = File(backupFile.path + "-wal")
            val shmBackup = File(backupFile.path + "-shm")
            if (walBackup.exists()) copyFile(walBackup, File(dbFile.path + "-wal"))
            if (shmBackup.exists()) copyFile(shmBackup, File(dbFile.path + "-shm"))

            Toast.makeText(context, "Data berhasil dipulihkan secara otomatis!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memulihkan data otomatis: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun copyFile(from: File, to: File) {
        FileInputStream(from).channel.use { source ->
            FileOutputStream(to).channel.use { destination ->
                destination.transferFrom(source, 0, source.size())
            }
        }
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Simpan Cadangan Database"))
    }
}
