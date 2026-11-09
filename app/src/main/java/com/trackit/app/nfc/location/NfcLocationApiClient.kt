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
            idArray.put(locationId)
            reqBodyJson.put("location_id", idArray)

            val request = Request.Builder()
                .url(API_URL)
                .post(reqBodyJson.toString().toRequestBody(jsonMediaType))
                .header("User-Agent", "TrackIt-App")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "fetchLocationName: HTTP error code ${response.code}")
                return@withContext null
            }

            val bodyStr = response.body?.string() ?: return@withContext null
            val rootObj = JSONObject(bodyStr)
            val resultArray = rootObj.optJSONArray("result") ?: return@withContext null

            for (i in 0 until resultArray.length()) {
                val item = resultArray.optJSONObject(i) ?: continue
                val id = item.optString("location_id", "")
                val name = item.optString("location_name", "")
                if (id.equals(locationId, ignoreCase = true) && name.isNotBlank()) {
                    return@withContext name
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "fetchLocationName failed: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "NfcLocationApiClient"
        private const val API_URL = "https://robycohen.pythonanywhere.com/api/location/list"
    }
}
