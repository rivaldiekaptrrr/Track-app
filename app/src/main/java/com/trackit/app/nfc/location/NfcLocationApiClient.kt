package com.trackit.app.nfc.location

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class NfcLocationApiClient @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    open suspend fun fetchLocationName(locationId: String): String? = withContext(Dispatchers.IO) {
        if (locationId.isBlank()) return@withContext null
        try {
            val reqBodyJson = JSONObject()
            val idArray = JSONArray()
            val cleanId = locationId.trim()
            idArray.put(cleanId)

            // Dukung varian terminal: misal "EP74560" <-> "EPT74560" (Electronic Payment Train / Transit)
            if (cleanId.startsWith("EP", ignoreCase = true) && cleanId.length == 7) {
                idArray.put("EPT" + cleanId.substring(2))
            } else if (cleanId.startsWith("EPT", ignoreCase = true) && cleanId.length == 8) {
                idArray.put("EP" + cleanId.substring(3))
            }

            reqBodyJson.put("location_id", idArray)

            val request = Request.Builder()
                .url(API_URL)
                .post(reqBodyJson.toString().toRequestBody(jsonMediaType))
                .header("Authorization", AUTH_HEADER)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "fetchLocationName: HTTP error code ${response.code}")
                return@withContext null
            }

            val bodyStr = response.body?.string() ?: return@withContext null
            val rootObj = JSONObject(bodyStr)
            val resultArray = rootObj.optJSONArray("result") ?: return@withContext null

            // Prioritaskan exact match, fallback ke hasil pertama jika cocok dengan varian yang dikirim
            var fallbackName: String? = null
            for (i in 0 until resultArray.length()) {
                val item = resultArray.optJSONObject(i) ?: continue
                val id = item.optString("location_id", "")
                val name = item.optString("location_name", "").trim()
                if (name.isBlank()) continue

                if (id.equals(cleanId, ignoreCase = true)) {
                    return@withContext name
                }
                if (fallbackName == null) {
                    fallbackName = name
                }
            }
            fallbackName
        } catch (e: Exception) {
            Log.w(TAG, "fetchLocationName failed: ${e.message}")
            null
        }
    }

    /**
     * Mengirimkan nama lokasi baru/kustom ke crowd-sourced API.
     */
    open suspend fun submitLocation(locationId: String, locationName: String): Boolean = withContext(Dispatchers.IO) {
        if (locationId.isBlank() || locationName.isBlank()) return@withContext false
        try {
            val reqBodyJson = JSONObject().apply {
                put("location_id", locationId.trim())
                put("location_name", locationName.trim())
            }

            val request = Request.Builder()
                .url(INSERT_API_URL)
                .post(reqBodyJson.toString().toRequestBody(jsonMediaType))
                .header("Authorization", AUTH_HEADER)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "submitLocation failed: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "NfcLocationApiClient"
        private const val API_URL = "https://robycohen.pythonanywhere.com/api/location/list"
        private const val INSERT_API_URL = "https://robycohen.pythonanywhere.com/api/insert-location"
        // Basic Auth: base64(wakwaw:wakwaw123)
        private const val AUTH_HEADER = "Basic d2Frd2F3Ondha3dhdzEyMw=="
        private const val USER_AGENT = "okhttp/4.9.0"
    }
}
