package com.trackit.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackit.app.R

data class OnboardingPage(
    val title: String,
    val description: String,
    val imageRes: Int
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    
    var currentPage by remember { mutableStateOf(0) }
    
    val pages = remember {
        listOf(
            OnboardingPage(
                title = "2-in-1 Tracker & Wedding Planner",
                description = "Satu aplikasi, dua kebutuhan penting. Kelola finansial pribadi harian Anda sekaligus persiapkan anggaran & agenda pernikahan impian bersama pasangan secara terintegrasi.",
                imageRes = R.drawable.img_onboarding_2in1
            ),
            OnboardingPage(
                title = "Atur Keuangan Jadi Lebih Mudah",
                description = "Catat setiap pemasukan & pengeluaran secara cepat. Pantau sisa budget bulanan dan evaluasi kategori belanja agar impian menabung Anda berjalan maksimal.",
                imageRes = R.drawable.img_onboarding_finance
            ),
            OnboardingPage(
                title = "Capai Rencana Wedding Impian",
                description = "Persiapkan detail hari bahagia bersama pasangan. Kelola anggaran vendor pernikahan, atur tugas / checklist, hitung pembayaran DP & pelunasan secara praktis.",
                imageRes = R.drawable.img_onboarding_wedding
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Illustration Image Area (Top 45% Screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.48f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(initialOffsetX = { it }) + fadeIn() with
                                    slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                        } else {
                            slideInHorizontally(initialOffsetX = { -it }) + fadeIn() with
                                    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                        }.using(SizeTransform(clip = false))
                    },
                    modifier = Modifier.fillMaxSize()
                ) { targetPage ->
                    Image(
                        painter = painterResource(id = pages[targetPage].imageRes),
                        contentDescription = pages[targetPage].title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // 2. Info / Text Area (Dynamic Slide content)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.TopStart
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(200))
                    }
                ) { targetPage ->
                    val page = pages[targetPage]
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = page.title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 34.sp
                        )

                        Text(
                            text = page.description,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // 3. Bottom controls (Dots Indicator & Buttons)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Indicators Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { index, _ ->
                        val isSelected = index == currentPage
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 18.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) primaryColor else outlineVariantColor.copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                // Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip / Kembali Button
                    if (currentPage > 0) {
                        Text(
                            text = "Kembali",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable { currentPage-- }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    } else {
                        // Skip onboarding straight to login if desired
                        Text(
                            text = "Lewati",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable { onContinue() }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Next / Mulai Button
                    Button(
                        onClick = {
                            if (currentPage < pages.size - 1) {
                                currentPage++
                            } else {
                                onContinue()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (currentPage == pages.size - 1) "Mulai Sekarang" else "Lanjut",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = onPrimaryColor
                        )
                        if (currentPage < pages.size - 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = onPrimaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
