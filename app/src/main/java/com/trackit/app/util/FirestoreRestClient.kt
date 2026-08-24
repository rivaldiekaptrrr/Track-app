package com.trackit.app.util

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * Handles all Firestore communications using HTTP REST API (instead of gRPC SDK).
 * This is required because the Firebase Android SDK's gRPC transport is blocked
 * on the target device/network.
 *
 * API Reference: https://firebase.google.com/docs/firestore/reference/rest
 */
@Singleton
class FirestoreRestClient @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://firestore.googleapis.com/v1/projects/trackit-a83c3/databases/(default)/documents"

    /**
     * Gets a fresh ID Token from the currently logged-in Firebase Auth user.
     * Returns null if no user is logged in.
     */
    private suspend fun getIdToken(): String? {
        val user = auth.currentUser ?: return null
        return try {
            user.getIdToken(false).await().token
        } catch (e: Exception) {
            Log.e("FirestoreREST", "Failed to get ID Token: ${e.message}")
            null
        }
    }

    /**
     * Creates or overwrites a Firestore document using HTTP PATCH.
     * Equivalent to: firestore.document(path).set(data)
     *
     * @param path Full collection path, e.g. "users/{uid}/transactions/{docId}"
     * @param firestoreJson Firestore-formatted JSON body (with typed value fields)
     * @return True if successful, false otherwise.
     */
    suspend fun put(path: String, firestoreJson: String): Boolean = withContext(Dispatchers.IO) {
        val idToken = getIdToken()
        if (idToken == null) {
            Log.e("FirestoreREST", "PUT Aborted: No ID Token")
            return@withContext false
        }

        val url = "$baseUrl/$path"
        val request = Request.Builder()
            .url(url)
            .patch(firestoreJson.toRequestBody(jsonMediaType))
            .addHeader("Authorization", "Bearer $idToken")
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                true
            } else {
                val body = response.body?.string() ?: ""
                Log.e("FirestoreREST", "PUT Error ${response.code}: ${body.take(150)}")
                false
            }
        } catch (e: Exception) {
            Log.e("FirestoreREST", "PUT Exception: ${e.message}")
            false
        }
    }

    /**
     * Deletes a Firestore document using HTTP DELETE.
     * Equivalent to: firestore.document(path).delete()
     *
     * @param path Full collection path, e.g. "users/{uid}/transactions/{docId}"
     * @return True if successful, false otherwise.
     */
    suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        val idToken = getIdToken()
        if (idToken == null) {
            Log.e("FirestoreREST", "DELETE Aborted: No ID Token")
            return@withContext false
        }

        val url = "$baseUrl/$path"
        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer $idToken")
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                true
            } else {
                val body = response.body?.string() ?: ""
                Log.e("FirestoreREST", "DELETE Error ${response.code}: ${body.take(150)}")
                false
            }
        } catch (e: Exception) {
            Log.e("FirestoreREST", "DELETE Exception: ${e.message}")
            false
        }
    }

    /**
     * Lists all documents in a Firestore collection using HTTP GET.
     * Equivalent to: firestore.collection(path).get()
     * Handles Firestore pagination via pageToken automatically.
     *
     * @param path Collection path e.g. "users/{uid}/transactions"
     * @return List of document JSONObjects (each is the raw Firestore REST document object),
     *         or empty list on failure.
     */
    suspend fun listDocuments(path: String): List<JSONObject> = withContext(Dispatchers.IO) {
        val idToken = getIdToken()
        if (idToken == null) {
            Log.e("FirestoreREST", "LIST Aborted: No ID Token")
            return@withContext emptyList()
        }

        val allDocs = mutableListOf<JSONObject>()
        var pageToken: String? = null

        try {
            do {
                val urlBuilder = StringBuilder("$baseUrl/$path?pageSize=300")
                if (pageToken != null) urlBuilder.append("&pageToken=$pageToken")

                val request = Request.Builder()
                    .url(urlBuilder.toString())
                    .get()
                    .addHeader("Authorization", "Bearer $idToken")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e("FirestoreREST", "LIST Error ${response.code}: ${body.take(150)}")
                    break
                }

                val json = JSONObject(body)
                if (json.has("documents")) {
                    val docs = json.getJSONArray("documents")
                    for (i in 0 until docs.length()) {
                        allDocs.add(docs.getJSONObject(i))
                    }
                }
                pageToken = if (json.has("nextPageToken")) json.getString("nextPageToken") else null

            } while (pageToken != null)

        } catch (e: Exception) {
            Log.e("FirestoreREST", "LIST Exception: ${e.message}")
        }

        return@withContext allDocs
    }
}
