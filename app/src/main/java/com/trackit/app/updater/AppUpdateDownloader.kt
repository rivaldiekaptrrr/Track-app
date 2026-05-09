package com.trackit.app.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    data class Done(val apkFile: File) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

@Singleton
class AppUpdateDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    fun downloadApk(downloadUrl: String, fileName: String = "trackit-update.apk"): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))

        try {
            val request = Request.Builder().url(downloadUrl).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadState.Failed("Server returned code: ${response.code}"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadState.Failed("Response body is empty"))
                return@flow
            }

            // Create apk_downloads directory in cache
            val cacheDir = File(context.cacheDir, "apk_downloads")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val apkFile = File(cacheDir, fileName)
            if (apkFile.exists()) {
                apkFile.delete()
            }

            val totalBytes = body.contentLength()
            var bytesCopied = 0L

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytes = input.read(buffer)
                    var lastProgress = 0

                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        
                        if (totalBytes > 0) {
                            val currentProgress = ((bytesCopied * 100) / totalBytes).toInt()
                            // Only emit if progress changed to avoid flooding UI
                            if (currentProgress > lastProgress) {
                                emit(DownloadState.Downloading(currentProgress))
                                lastProgress = currentProgress
                            }
                        }
                        bytes = input.read(buffer)
                    }
                }
            }

            emit(DownloadState.Done(apkFile))

        } catch (e: Exception) {
            e.printStackTrace()
            emit(DownloadState.Failed(e.localizedMessage ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)

    fun installApk(apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        context.startActivity(intent)
    }
}
