package com.trackit.app.ui.auth

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackit.app.ui.theme.PlusJakartaSans

/* ---------------------------------------------------------------- *
 *  Warna tema konsisten TrackIt (Light Theme)                      *
 * ---------------------------------------------------------------- */
private object SelectionColor {
    val BgDeep    = Color(0xFFF7F9F7)  // Soft Mint Cream
    val BgCard    = Color(0xFFFFFFFF)
    val BgMuted   = Color(0xFFEEF2EE)  // Light sage tint
    val Stroke    = Color(0xFFE2E8F0)
    val TextPrime = Color(0xFF191C1A)
    val TextMuted = Color(0xFF536357)
    val TextFaint = Color(0xFF7D8C80)

    val Expense   = Color(0xFF2D5E4E)  // Deep Sage Green
    val Wedding   = Color(0xFFC24D6E)  // Warm Rose
    val Gold      = Color(0xFFD4A843)  // Warm Gold
}

@Composable
fun ModuleSelectionScreen(
    onSelectExpense: () -> Unit,
    onSelectWedding: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SelectionColor.BgDeep, Color(0xFFEFF3EF), SelectionColor.BgCard)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .systemBarsPadding()
        ) {
            // ── Top Bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Verified Full Access Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SelectionColor.Gold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SelectionColor.Gold.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = SelectionColor.Gold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "AKSES LISENSI LENGKAP",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SelectionColor.Gold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Logout Button
                TextButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.textButtonColors(contentColor = SelectionColor.TextMuted)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Keluar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Keluar",
                        fontFamily = PlusJakartaSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Header Title & Subtitle ──────────────────────────────
            Text(
                text = "Selamat Datang di TrackIt",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = SelectionColor.TextPrime
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Akun Anda memiliki akses tak terbatas ke semua modul. Pilih modul kerja yang ingin Anda buka:",
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                color = SelectionColor.TextMuted,
                lineHeight = 21.sp
            )

            Spacer(Modifier.height(32.dp))

            // ── Card 1: Expense Tracker ──────────────────────────────
            ModuleCard(
                categoryTag = "MODUL KEUANGAN",
                title = "Catatan Keuangan",
                description = "Catat transaksi pemasukan & pengeluaran, pantau anggaran bulanan, dan kelola multi-profil akun.",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = SelectionColor.Expense,
                onClick = onSelectExpense
            )

            Spacer(Modifier.height(18.dp))

            // ── Card 2: Wedding Planner ──────────────────────────────
            ModuleCard(
                categoryTag = "MODUL PERNIKAHAN",
                title = "Wedding Planner",
                description = "Susun checklist persiapan nikah, anggaran pesta, daftar berkas, buku tamu, rundown & vendor.",
                icon = Icons.Default.Favorite,
                accentColor = SelectionColor.Wedding,
                onClick = onSelectWedding
            )

            Spacer(Modifier.weight(1f))

            // ── Footer Note ──────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SelectionColor.BgMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = SelectionColor.TextFaint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Anda dapat berpindah modul kapan saja melalui menu Pengaturan aplikasi.",
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                        color = SelectionColor.TextFaint,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(
    categoryTag: String,
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "module_card_scale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SelectionColor.BgCard,
        shadowElevation = if (isPressed) 2.dp else 5.dp,
        border = BorderStroke(1.dp, SelectionColor.Stroke),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header baris atas: Tag & Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = categoryTag,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Buka",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Body: Icon + Title + Description
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = accentColor
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = PlusJakartaSans,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SelectionColor.TextPrime
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.5.sp,
                        color = SelectionColor.TextMuted,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
