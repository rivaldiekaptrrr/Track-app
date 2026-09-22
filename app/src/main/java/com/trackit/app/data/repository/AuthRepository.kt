package com.trackit.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.trackit.app.util.FirestoreRestClient
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/** Access levels for premium access control. */
object AccessLevel {
    const val NONE = "NONE"
    const val EXPENSE = "EXPENSE"
    const val WEDDING = "WEDDING"
    const val BOTH = "BOTH"
    const val ADMIN = "ADMIN"
}

/** Email akun Super Admin. Ganti dengan email Anda sendiri. */
const val ADMIN_EMAIL = "rivaldiekaptr2001@gmail.com"

/** Nomor WhatsApp Admin untuk konfirmasi pembayaran (format internasional tanpa +, contoh: 6281234567890). */
const val ADMIN_WHATSAPP_NUMBER = "6289516181056"

data class UserInfo(
    val uid: String,
    val email: String,
    val displayName: String,
    val accessLevel: String
)

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val restClient: FirestoreRestClient
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = auth.currentUser != null

    fun isAdminEmail(email: String?): Boolean {
        return email != null && email.equals(ADMIN_EMAIL, ignoreCase = true)
    }

    /**
     * Fetches the user's Firestore document. If it doesn't exist (new user),
     * creates one with accessLevel = NONE (or ADMIN for the admin email).
     * Returns the accessLevel string.
     */
    suspend fun fetchOrCreateUserDoc(user: FirebaseUser): String {
        val uid = user.uid
        val email = user.email ?: ""
        val isAdmin = email.equals(ADMIN_EMAIL, ignoreCase = true)

        if (isAdmin) {
            // Admin email ALWAYS gets ADMIN access
            val docJson = restClient.getDocument("users/$uid")
            if (docJson == null) {
                val newDocJson = """
                    {
                      "fields": {
                        "email": { "stringValue": "$email" },
                        "displayName": { "stringValue": "${user.displayName ?: ""}" },
                        "accessLevel": { "stringValue": "${AccessLevel.ADMIN}" },
                        "createdAt": { "stringValue": "${System.currentTimeMillis()}" }
                      }
                    }
                """.trimIndent()
                restClient.put("users/$uid", newDocJson)
            } else {
                updateUserAccessLevel(uid, AccessLevel.ADMIN)
            }
            return AccessLevel.ADMIN
        }

        // Try to read existing doc
        val docJson = restClient.getDocument("users/$uid")
        if (docJson != null) {
            // Doc exists - read accessLevel
            return try {
                val fields = docJson.optJSONObject("fields")
                fields?.optJSONObject("accessLevel")?.optString("stringValue") ?: AccessLevel.NONE
            } catch (e: Exception) {
                AccessLevel.NONE
            }
        }

        // Doc doesn't exist - create it with default NONE
        val newDocJson = """
            {
              "fields": {
                "email": { "stringValue": "$email" },
                "displayName": { "stringValue": "${user.displayName ?: ""}" },
                "accessLevel": { "stringValue": "${AccessLevel.NONE}" },
                "createdAt": { "stringValue": "${System.currentTimeMillis()}" }
              }
            }
        """.trimIndent()
        restClient.put("users/$uid", newDocJson)
        return AccessLevel.NONE
    }

    /**
     * For Admin: List all registered users from the Firestore 'users' collection.
     * Returns a list of UserInfo.
     */
    suspend fun getAllUsers(): List<UserInfo> {
        val currentUser = auth.currentUser
        if (currentUser != null && isAdminEmail(currentUser.email)) {
            try {
                fetchOrCreateUserDoc(currentUser)
            } catch (e: Exception) {
                // Ignore if creating doc fails, still proceed to fetch
            }
        }
        val docs = restClient.listDocuments("users")
        return docs.mapNotNull { doc ->
            try {
                val name = doc.optString("name")
                if (name.isBlank()) return@mapNotNull null
                val uid = name.substringAfterLast("/")
                val fields = doc.optJSONObject("fields") ?: return@mapNotNull null
                val email = fields.optJSONObject("email")?.optString("stringValue") ?: ""
                val displayName = fields.optJSONObject("displayName")?.optString("stringValue") ?: ""
                val level = fields.optJSONObject("accessLevel")?.optString("stringValue") ?: AccessLevel.NONE
                UserInfo(uid = uid, email = email, displayName = displayName, accessLevel = level)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * For Admin: Update a specific user's access level in Firestore.
     */
    suspend fun updateUserAccessLevel(targetUid: String, newAccessLevel: String): Boolean {
        val patchJson = """
            {
              "fields": {
                "accessLevel": { "stringValue": "$newAccessLevel" }
              }
            }
        """.trimIndent()
        // Use PATCH with updateMask to only update the accessLevel field
        return restClient.patch("users/$targetUid", patchJson, listOf("accessLevel"))
    }

    suspend fun deleteAccount(): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Error("Tidak ada pengguna yang sedang login")
        return try {
            val userId = user.uid
            try {
                restClient.delete("users/$userId")
            } catch (e: Exception) {
                // Ignore cloud deletion errors to ensure user deletion proceeds
            }
            user.delete().await()
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Gagal menghapus akun. Silakan login ulang dan coba lagi.")
        }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { AuthResult.Success(it) }
                ?: AuthResult.Error("Login gagal: User tidak ditemukan")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Login gagal")
        }
    }

    suspend fun registerWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { AuthResult.Success(it) }
                ?: AuthResult.Error("Registrasi gagal")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Registrasi gagal")
        }
    }

    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { AuthResult.Success(it) }
                ?: AuthResult.Error("Login Google gagal")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Login Google gagal")
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
