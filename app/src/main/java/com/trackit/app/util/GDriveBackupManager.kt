package com.trackit.app.util

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Manajer Backup & Restore Google Drive untuk TrackIt.
 *
 * ─── Setup yang dibutuhkan Developer ────────────────────────────────────────
 * Sebelum menggunakan fitur ini, ikuti panduan lengkap di:
 * docs/GDRIVE_SETUP_GUIDE.md
 * ────────────────────────────────────────────────────────────────────────────
 *
 * Cara kerja:
 * 1. User login dengan akun Google via [getSignInIntent].
 * 2. Setelah login, panggil [backupDatabase] untuk meng-upload file database ke
 *    folder "TrackIt Backups" di Google Drive user.
 * 3. Untuk restore, panggil [listBackups] untuk mendapatkan daftar file backup,
 *    lalu panggil [restoreDatabase] dengan ID file yang dipilih.
 */
object GDriveBackupManager {

    private const val BACKUP_FOLDER_NAME = "TrackIt Backups"
    private const val APP_NAME = "TrackIt"
    private const val DB_NAME = "trackit_database"

    // ─── Sign-In ─────────────────────────────────────────────────────────────

    /**
     * Mengembalikan Intent untuk memulai alur Google Sign-In.
     * Gunakan dengan [androidx.activity.result.ActivityResultLauncher].
     *
     * Contoh:
     * ```kotlin
     * val signInLauncher = rememberLauncherForActivityResult(
     *     ActivityResultContracts.StartActivityForResult()
     * ) { result ->
     *     if (result.resultCode == Activity.RESULT_OK) {
     *         val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
     *         // handle task...
     *     }
     * }
     * signInLauncher.launch(GDriveBackupManager.getSignInIntent(context))
     * ```
     */
    fun getSignInIntent(context: Context): Intent {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, signInOptions).signInIntent
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun isSignedIn(context: Context): Boolean = getLastSignedInAccount(context) != null

    fun signOut(context: Context, onDone: () -> Unit = {}) {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, signInOptions).signOut().addOnCompleteListener { onDone() }
    }

    // ─── Drive Service ────────────────────────────────────────────────────────

    private fun buildDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply { selectedAccount = account.account }

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APP_NAME).build()
    }

    // ─── Backup ───────────────────────────────────────────────────────────────

    /**
     * Meng-upload file database ke Google Drive user.
     * @return ID file Drive yang berhasil diupload, atau null jika gagal.
     */
    suspend fun backupDatabase(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val account = getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Belum login dengan Google"))

            val drive = buildDriveService(context, account)

            // Cari atau buat folder "TrackIt Backups"
            val folderId = getOrCreateFolder(drive)

            // Ambil path file database Room
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("File database tidak ditemukan"))
            }

            // Buat nama file dengan timestamp
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                .format(java.util.Date())
            val fileName = "trackit_backup_$timestamp.db"

            // Cek apakah sudah ada backup sebelumnya dengan nama yang sama
            val existingId = findFileInFolder(drive, folderId, fileName)

            val fileMetadata = File().apply {
                name = fileName
                if (existingId == null) parents = listOf(folderId)
            }

            val mediaContent = com.google.api.client.http.FileContent(
                "application/octet-stream", dbFile
            )

            val driveFile = if (existingId != null) {
                drive.files().update(existingId, fileMetadata, mediaContent).execute()
            } else {
                drive.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, createdTime")
                    .execute()
            }

            Result.success(driveFile.id ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── List Backups ─────────────────────────────────────────────────────────

    data class DriveBackupFile(
        val id: String,
        val name: String,
        val createdTimeMs: Long
    )

    suspend fun listBackups(context: Context): Result<List<DriveBackupFile>> = withContext(Dispatchers.IO) {
        try {
            val account = getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Belum login dengan Google"))

            val drive = buildDriveService(context, account)
            val folderId = findFolder(drive) ?: return@withContext Result.success(emptyList())

            val result = drive.files().list()
                .setQ("'$folderId' in parents and trashed = false and name contains 'trackit_backup'")
                .setOrderBy("createdTime desc")
                .setFields("files(id, name, createdTime)")
                .execute()

            val files = result.files.map { f ->
                DriveBackupFile(
                    id = f.id,
                    name = f.name,
                    createdTimeMs = f.createdTime?.value ?: 0L
                )
            }
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Restore ──────────────────────────────────────────────────────────────

    /**
     * Mendownload backup dari Drive dan menggantikan database lokal.
     * ⚠️ App harus di-restart setelah restore.
     */
    suspend fun restoreDatabase(context: Context, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val account = getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("Belum login dengan Google"))

            val drive = buildDriveService(context, account)

            val outputStream = ByteArrayOutputStream()
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)

            val dbFile = context.getDatabasePath(DB_NAME)
            val walFile = java.io.File(dbFile.path + "-wal")
            val shmFile = java.io.File(dbFile.path + "-shm")

            // Hapus WAL dan SHM agar tidak conflict
            walFile.delete()
            shmFile.delete()

            // Tulis file database yang baru
            FileOutputStream(dbFile).use { it.write(outputStream.toByteArray()) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun getOrCreateFolder(drive: Drive): String {
        return findFolder(drive) ?: run {
            val folderMetadata = File().apply {
                name = BACKUP_FOLDER_NAME
                mimeType = "application/vnd.google-apps.folder"
            }
            drive.files().create(folderMetadata).setFields("id").execute().id
        }
    }

    private fun findFolder(drive: Drive): String? {
        val result = drive.files().list()
            .setQ("mimeType='application/vnd.google-apps.folder' and name='$BACKUP_FOLDER_NAME' and trashed=false")
            .setFields("files(id)")
            .execute()
        return result.files.firstOrNull()?.id
    }

    private fun findFileInFolder(drive: Drive, folderId: String, fileName: String): String? {
        val result = drive.files().list()
            .setQ("'$folderId' in parents and name='$fileName' and trashed=false")
            .setFields("files(id)")
            .execute()
        return result.files.firstOrNull()?.id
    }
}
