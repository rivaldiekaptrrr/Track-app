package com.trackit.app.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateChecker @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    suspend fun checkForUpdate(owner: String, repo: String, currentVersion: String): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                val jsonObject = JSONObject(responseBody)
                
                val tagName = jsonObject.optString("tag_name", "")
                val body = jsonObject.optString("body", "")
                
                var downloadUrl = ""
                val assetsArray = jsonObject.optJSONArray("assets")
                if (assetsArray != null && assetsArray.length() > 0) {
                    val firstAsset = assetsArray.getJSONObject(0)
                    downloadUrl = firstAsset.optString("browser_download_url", "")
                }

                if (tagName.isEmpty() || downloadUrl.isEmpty()) {
                    return@withContext null
                }

                val isNewer = isVersionNewer(tagName, currentVersion)

                UpdateInfo(
                    latestVersion = tagName,
                    downloadUrl = downloadUrl,
                    releaseNotes = body,
                    isUpdateAvailable = isNewer
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Compares remote version (e.g. "v1.2.0") with local version (e.g. "1.1.0").
     * Handles versions with or without "v" prefix.
     */
    fun isVersionNewer(remote: String, local: String): Boolean {
        try {
            val cleanRemote = remote.removePrefix("v").split(".")
            val cleanLocal = local.removePrefix("v").split(".")

            val maxLength = maxOf(cleanRemote.size, cleanLocal.size)

            for (i in 0 until maxLength) {
                val remotePart = cleanRemote.getOrNull(i)?.toIntOrNull() ?: 0
                val localPart = cleanLocal.getOrNull(i)?.toIntOrNull() ?: 0

                if (remotePart > localPart) return true
                if (remotePart < localPart) return false
            }
            return false // Equal
        } catch (e: Exception) {
            return false
        }
    }
}
