package com.trackit.app.ui.biometric

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BiometricLockScreen(
    onAuthenticate: () -> Unit,
    errorMessage: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Logo / Title
            Text(
                "TrackIt",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Smart Expense Tracker",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Fingerprint Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Sidik Jari",
                    modifier = Modifier.size(56.dp),
                    tint = Color.White
                )
            }

            Text(
                "Sentuh sensor sidik jari\natau gunakan face unlock untuk masuk",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF6B6B),
                    textAlign = TextAlign.Center
                )
            }

            FilledTonalButton(
                onClick = onAuthenticate,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Autentikasi")
            }
        }
    }
}
