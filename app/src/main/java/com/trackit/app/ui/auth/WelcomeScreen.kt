package com.trackit.app.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit
) {
    val salmonCoral = Color(0xFFFD827E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 1. Wavy Topographic Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw coral wavy background shape
                val backgroundPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(width, 0f)
                    lineTo(width, height * 0.75f)
                    cubicTo(
                        width * 0.7f, height * 0.95f,
                        width * 0.3f, height * 0.6f,
                        0f, height * 0.85f
                    )
                    close()
                }
                drawPath(path = backgroundPath, color = salmonCoral)

                // Clip topographic lines inside the background shape
                clipPath(backgroundPath) {
                    val lineCount = 12
                    for (i in -lineCount..lineCount) {
                        val offset = i * 26.dp.toPx()
                        val path = Path().apply {
                            moveTo(0f, height * 0.85f + offset)
                            cubicTo(
                                width * 0.3f, height * 0.6f + offset * 0.8f,
                                width * 0.7f, height * 0.95f + offset * 1.2f,
                                width, height * 0.75f + offset
                            )
                        }
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = 0.08f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // 2. Content Column (Bottom White Section)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 32.dp)
                .padding(bottom = 54.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2C2C2C)
            )

            Text(
                text = "Kelola pengeluaran harian dan persiapkan rencana pernikahan impian Anda bersama pasangan secara praktis dan terintegrasi.",
                fontSize = 15.sp,
                color = Color(0xFF888888),
                lineHeight = 22.sp,
                modifier = Modifier.padding(end = 24.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Continue Button Row
            Row(
                modifier = Modifier
                    .clickable { onContinue() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(end = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(salmonCoral),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Lanjutkan",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
