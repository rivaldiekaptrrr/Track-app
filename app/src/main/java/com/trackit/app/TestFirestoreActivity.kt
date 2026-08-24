package com.trackit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.trackit.app.ui.theme.TrackItTheme
import java.text.SimpleDateFormat
import java.util.*

class TestFirestoreActivity : ComponentActivity() {

    private lateinit var auth: FirebaseFirestore
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val logs = mutableStateListOf<Pair<String, LogLevel>>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    enum class LogLevel { INFO, SUCCESS, ERROR, WARN }

    private fun log(message: String, level: LogLevel = LogLevel.INFO) {
        val time = dateFormat.format(Date())
        logs.add(0, Pair("[$time] $message", level))
    }

    private fun buildFirestore(): FirebaseFirestore {
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
        db.firestoreSettings = settings
        return db
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TrackItTheme {
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var isLoggedIn by remember { mutableStateOf(false) }
                var currentUid by remember { mutableStateOf("") }
                var isLoading by remember { mutableStateOf(false) }

                // Check if already logged in
                LaunchedEffect(Unit) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        isLoggedIn = true
                        currentUid = user.uid
                        log("Existing session found.", LogLevel.INFO)
                        log("UID: ${user.uid}", LogLevel.SUCCESS)
                        log("Email: ${user.email}", LogLevel.SUCCESS)
                    } else {
                        log("No active session. Silakan login.", LogLevel.WARN)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1565C0))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text(
                                    "Firebase E2E Test App",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Simulasi Firebase Android SDK (Auth + gRPC Firestore)",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Controls Panel
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E1E1E))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Login Form
                            if (!isLoggedIn) {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email", color = Color.Gray) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF1565C0),
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password", color = Color.Gray) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF1565C0),
                                        unfocusedBorderColor = Color.Gray
                                    )
                                )
                            } else {
                                Text(
                                    "Logged in as: ${firebaseAuth.currentUser?.email}",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 13.sp
                                )
                                Text(
                                    "UID: $currentUid",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Login / Logout Button
                                Button(
                                    onClick = {
                                        if (isLoggedIn) {
                                            log("--- LOGOUT ---", LogLevel.WARN)
                                            firebaseAuth.signOut()
                                            isLoggedIn = false
                                            currentUid = ""
                                            log("Signed out dari Firebase Auth.", LogLevel.INFO)
                                        } else {
                                            isLoading = true
                                            log("--- STEP 1: FIREBASE AUTH LOGIN ---", LogLevel.INFO)
                                            log("Memanggil signInWithEmailAndPassword...", LogLevel.INFO)
                                            log("Email: $email", LogLevel.INFO)

                                            firebaseAuth.signInWithEmailAndPassword(email, password)
                                                .addOnSuccessListener { result ->
                                                    val user = result.user!!
                                                    isLoggedIn = true
                                                    isLoading = false
                                                    currentUid = user.uid
                                                    log("LOGIN BERHASIL!", LogLevel.SUCCESS)
                                                    log("UID: ${user.uid}", LogLevel.SUCCESS)

                                                    // Get and print the ID Token
                                                    log("Mengambil ID Token dari Firebase...", LogLevel.INFO)
                                                    user.getIdToken(false)
                                                        .addOnSuccessListener { tokenResult ->
                                                            val token = tokenResult.token ?: ""
                                                            log("ID Token berhasil diambil (${token.length} karakter)", LogLevel.SUCCESS)
                                                            log("Token (50 char pertama): ${token.take(50)}...", LogLevel.INFO)
                                                        }
                                                        .addOnFailureListener { e ->
                                                            log("Gagal ambil ID Token: ${e.message}", LogLevel.ERROR)
                                                        }
                                                }
                                                .addOnFailureListener { e ->
                                                    isLoading = false
                                                    log("LOGIN GAGAL!", LogLevel.ERROR)
                                                    log("Error: ${e.message}", LogLevel.ERROR)
                                                }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isLoggedIn) Color(0xFF616161) else Color(0xFF1565C0)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(if (isLoggedIn) "LOGOUT" else "LOGIN")
                                    }
                                }

                                // Test Write Button
                                Button(
                                    onClick = {
                                        val user = firebaseAuth.currentUser
                                        if (user == null) {
                                            log("Tidak bisa test: belum login!", LogLevel.ERROR)
                                            return@Button
                                        }

                                        val docId = "test_android_${System.currentTimeMillis()}"
                                        log("--- STEP 2: FIRESTORE WRITE (gRPC) ---", LogLevel.INFO)
                                        log("Persistence: DISABLED", LogLevel.INFO)
                                        log("Target: users/${user.uid}/transactions/$docId", LogLevel.INFO)
                                        log("Mengirim perintah .set() ke Firestore SDK...", LogLevel.INFO)
                                        log("Menunggu callback (SUCCESS/FAILURE/CANCEL)...", LogLevel.WARN)

                                        val startTime = System.currentTimeMillis()
                                        val db = buildFirestore()

                                        db.collection("users").document(user.uid)
                                            .collection("transactions").document(docId)
                                            .set(mapOf(
                                                "amount" to 9999.0,
                                                "description" to "Test E2E Android",
                                                "type" to "EXPENSE",
                                                "profileId" to 1L,
                                                "createdAt" to System.currentTimeMillis()
                                            ))
                                            .addOnSuccessListener {
                                                val elapsed = System.currentTimeMillis() - startTime
                                                log("WRITE BERHASIL! (${elapsed}ms)", LogLevel.SUCCESS)
                                                log("Doc ID: $docId", LogLevel.SUCCESS)
                                                log("Cek Firebase Console sekarang.", LogLevel.SUCCESS)
                                            }
                                            .addOnFailureListener { e ->
                                                val elapsed = System.currentTimeMillis() - startTime
                                                log("WRITE GAGAL! (${elapsed}ms)", LogLevel.ERROR)
                                                log("Exception: ${e.javaClass.simpleName}", LogLevel.ERROR)
                                                log("Message: ${e.message}", LogLevel.ERROR)
                                            }
                                            .addOnCanceledListener {
                                                val elapsed = System.currentTimeMillis() - startTime
                                                log("WRITE DIBATALKAN oleh SDK! (${elapsed}ms)", LogLevel.ERROR)
                                            }
                                    },
                                    enabled = isLoggedIn,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32),
                                        disabledContainerColor = Color(0xFF424242)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("TEST WRITE")
                                }

                                // Clear Logs
                                Button(
                                    onClick = { logs.clear() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                                ) {
                                    Text("CLR")
                                }
                            }
                        }

                        // Terminal
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0D0D0D))
                                .padding(8.dp),
                            reverseLayout = true
                        ) {
                            items(logs) { (msg, level) ->
                                val color = when (level) {
                                    LogLevel.SUCCESS -> Color(0xFF4CAF50)
                                    LogLevel.ERROR   -> Color(0xFFF44336)
                                    LogLevel.WARN    -> Color(0xFFFFB300)
                                    LogLevel.INFO    -> Color(0xFFB0BEC5)
                                }
                                Text(
                                    text = msg,
                                    color = color,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
