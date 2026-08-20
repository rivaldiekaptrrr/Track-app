package com.trackit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.trackit.app.ui.theme.TrackItTheme
import java.text.SimpleDateFormat
import java.util.*

class TestFirestoreActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val logs = mutableStateListOf<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private fun log(message: String) {
        val time = dateFormat.format(Date())
        logs.add(0, "[$time] $message")
        if (logs.size > 100) logs.removeLast()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Log initial state
        log("Test App Started.")
        log("Auth User: ${auth.currentUser?.uid ?: "NOT LOGGED IN"}")
        
        setContent {
            TrackItTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        
                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(16.dp)
                        ) {
                            Text("Isolated Firestore Test", color = Color.White)
                        }
                        
                        // Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = { runFirestoreTest() }) {
                                Text("TEST WRITE")
                            }
                            Button(onClick = { 
                                logs.clear() 
                                log("Logs cleared.")
                            }) {
                                Text("CLEAR LOGS")
                            }
                        }
                        
                        // Terminal
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF1E1E1E))
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                reverseLayout = true
                            ) {
                                items(logs) { msg ->
                                    val color = when {
                                        msg.contains("ERROR") -> Color.Red
                                        msg.contains("SUCCESS") -> Color.Green
                                        else -> Color.LightGray
                                    }
                                    Text(
                                        text = msg,
                                        color = color,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun runFirestoreTest() {
        val user = auth.currentUser
        if (user == null) {
            log("ERROR: User is not logged in! Please login from the main app first.")
            return
        }
        
        val docId = "TEST_DOC_${System.currentTimeMillis()}"
        log("Attempting write to users/${user.uid}/transactions/$docId")
        
        val testData = hashMapOf(
            "amount" to 999.0,
            "description" to "Test from Isolated Activity",
            "date" to System.currentTimeMillis()
        )
        
        // We use pure Callbacks here, NO COROUTINES, NO AWAIT.
        firestore.collection("users").document(user.uid)
            .collection("transactions").document(docId)
            .set(testData)
            .addOnSuccessListener {
                log("SUCCESS: Data written successfully to Firestore!")
            }
            .addOnFailureListener { e ->
                log("ERROR: Write failed! ${e.message}")
                e.printStackTrace()
            }
            .addOnCanceledListener {
                log("ERROR: Write was CANCELLED by Firebase SDK!")
            }
    }
}
