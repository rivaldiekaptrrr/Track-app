package com.trackit.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payment

import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trackit.app.data.local.PreferencesManager
import com.trackit.app.data.repository.ADMIN_WHATSAPP_NUMBER
import com.trackit.app.data.repository.AccessLevel
import com.trackit.app.data.repository.AuthRepository
import com.trackit.app.data.repository.PaymentPackage
import com.trackit.app.ui.theme.PlusJakartaSans
import kotlinx.coroutines.launch
import java.net.URLEncoder

/* ---------------------------------------------------------------- *
 *  Warna lokal untuk layar ini                                      *
 * ---------------------------------------------------------------- */
private object TrackColor {
    val BgDeep    = Color(0xFFF7F9F7)  // Soft Mint Cream
    val BgCard    = Color(0xFFFFFFFF)
    val BgMuted   = Color(0xFFEEF2EE)  // Light sage tint
    val Surface   = Color(0xFFADC3B7).copy(alpha = 0.25f)
    val Stroke    = Color(0xFFBDC9BB)
    val TextPrime = Color(0xFF191C1A)
    val TextMuted = Color(0xFF536357)
    val TextFaint = Color(0xFF7D8C80)

    // Per-paket accent
    val Expense  = Color(0xFF2D5E4E)  // Deep Sage Green
    val Wedding  = Color(0xFFC24D6E)  // Rose (dari ChartColors)
    val Full     = Color(0xFFD4A843)  // Gold (dari ChartColors)
}

/* ---------------------------------------------------------------- *
 *  Custom shape: tag card (pojok kanan atas dipotong)               *
 * ---------------------------------------------------------------- */
private class TagCardShape(private val cut: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val px = with(density) { cut.toPx() }
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width - px, 0f)
            lineTo(size.width, px)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

/* ---------------------------------------------------------------- *
 *  Helpers: warna & ikon per paket                                  *
 * ---------------------------------------------------------------- */
private fun accentFor(id: String) = when (id) {
    AccessLevel.EXPENSE -> TrackColor.Expense
    AccessLevel.WEDDING -> TrackColor.Wedding
    else                -> TrackColor.Full
}

private fun iconFor(id: String) = when (id) {
    AccessLevel.EXPENSE -> Icons.Default.AccountBalanceWallet
    AccessLevel.WEDDING -> Icons.Default.Favorite
    else                -> Icons.Default.Star
}

private fun levelFor(id: String) = when (id) {
    AccessLevel.EXPENSE -> 1
    AccessLevel.WEDDING -> 2
    else                -> 3
}

/* ---------------------------------------------------------------- *
 *  Screen                                                            *
 * ---------------------------------------------------------------- */
@Composable
fun PendingVerificationScreen(
    onAccessGranted: (accessLevel: String) -> Unit,
    onLogout: () -> Unit,
    authRepository: AuthRepository,
    preferencesManager: PreferencesManager,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val packages = viewModel.availablePackages

    var selectedIndex by remember { mutableStateOf(2) }          // default: Full Access
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val uriHandler = LocalUriHandler.current
    val currentUser = authRepository.currentUser
    val userEmail   = currentUser?.email ?: "-"
    val scope       = rememberCoroutineScope()

    // Auto-polling akses setelah pembayaran
    LaunchedEffect(Unit) {
        viewModel.startAutoPollingAccess { grantedLevel -> onAccessGranted(grantedLevel) }
    }
    LaunchedEffect(currentUser?.uid) {
        preferencesManager.accessLevel.collect { level ->
            if (level != AccessLevel.NONE) onAccessGranted(level)
        }
    }

    val selectedPackage = packages.getOrNull(selectedIndex) ?: packages.last()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(TrackColor.BgDeep, Color(0xFFEFF3EF), TrackColor.BgCard)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Scrollable content ──────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(28.dp))

                Text(
                    "Pilih Paket Aksesmu",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = TrackColor.TextPrime,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bayar sekali, pakai selamanya. Bebas biaya berlangganan bulanan.",
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    color = TrackColor.TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(24.dp))

                // ── Segmented Selector ──────────────────────────────
                SegmentedSelector(
                    packages = packages,
                    selected = selectedIndex,
                    onSelect = { selectedIndex = it }
                )

                Spacer(Modifier.height(8.dp))

                // "Paling Hemat" hint
                val isRecommended = selectedPackage.isRecommended
                Text(
                    text = if (isRecommended) "●  Pilihan paling hemat & lengkap" else " ",
                    fontFamily = PlusJakartaSans,
                    fontSize = 11.5.sp,
                    color = TrackColor.Full.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(18.dp))

                // ── Tag Card ────────────────────────────────────────
                PlanTagCard(pkg = selectedPackage)

                Spacer(Modifier.height(16.dp))

                // Error Message
                val activeError = errorMessage ?: uiState.errorMessage
                AnimatedVisibility(visible = activeError != null) {
                    activeError?.let { errMsg ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF7F1D1D).copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = errMsg,
                                color = Color(0xFFFCA5A5),
                                fontFamily = PlusJakartaSans,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // ── Secondary buttons ────────────────────────────────
                Spacer(Modifier.height(4.dp))

                // WhatsApp Admin
                OutlinedButton(
                    onClick = {
                        val message = "Halo Admin Track-It, saya ingin bertanya seputar pembayaran/aktivasi akun saya ($userEmail)."
                        val encoded = URLEncoder.encode(message, "UTF-8")
                        val clean   = ADMIN_WHATSAPP_NUMBER.filter { it.isDigit() }
                        val waUrl   = if (clean.isNotBlank() && !clean.startsWith("000")) {
                            "https://wa.me/$clean?text=$encoded"
                        } else "https://wa.me/?text=$encoded"
                        uriHandler.openUri(waUrl)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF34D399)),
                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bantuan WhatsApp Admin", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                Spacer(Modifier.height(12.dp))

                // Trust badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "QRIS • GoPay • ShopeePay • VA BCA, Mandiri, BRI, BNI",
                        color = TrackColor.TextFaint,
                        fontFamily = PlusJakartaSans,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Logout
                TextButton(onClick = onLogout) {
                    Icon(
                        Icons.Default.Logout, contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = TrackColor.TextFaint
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Keluar dari akun ini", color = TrackColor.TextFaint, fontFamily = PlusJakartaSans, fontSize = 13.sp)
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── Sticky Bottom CTA ───────────────────────────────────
            Surface(
                color = TrackColor.BgCard,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, TrackColor.Stroke)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            errorMessage = null
                            viewModel.createPaymentTransaction(
                                accessLevel = selectedPackage.id,
                                onSuccess = { redirectUrl ->
                                    try { uriHandler.openUri(redirectUrl) }
                                    catch (e: Exception) { errorMessage = "Gagal membuka halaman pembayaran: ${e.message}" }
                                },
                                onError = { err -> errorMessage = err }
                            )
                        },
                        enabled = !uiState.isPaymentLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentFor(selectedPackage.id),
                            contentColor   = Color.White
                        )
                    ) {
                        if (uiState.isPaymentLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.5.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Menyiapkan Pembayaran...", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        } else {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Aktifkan ${selectedPackage.title} — ${selectedPackage.formattedPrice}",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pembayaran sekali bayar. Akses aktif selamanya.",
                        fontFamily = PlusJakartaSans,
                        fontSize = 11.sp,
                        color = TrackColor.TextFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/* ---------------------------------------------------------------- *
 *  Segmented Selector                                               *
 * ---------------------------------------------------------------- */
@Composable
private fun SegmentedSelector(
    packages: List<PaymentPackage>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TrackColor.BgMuted)
            .padding(4.dp)
    ) {
        val segW = maxWidth / packages.size
        val offset by animateDpAsState(
            targetValue = segW * selected,
            animationSpec = tween(380),
            label = "seg_offset"
        )

        // Active pill
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(segW)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accentFor(packages.getOrNull(selected)?.id ?: ""))
        )

        // Labels
        Row(modifier = Modifier.fillMaxWidth()) {
            packages.forEachIndexed { index, pkg ->
                val isActive = index == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (pkg.id) {
                                AccessLevel.EXPENSE -> "Expense"
                                AccessLevel.WEDDING -> "Wedding"
                                else                -> "Full"
                            },
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = if (isActive) Color.White else TrackColor.TextMuted
                        )
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------------------------- *
 *  Tag Card                                                          *
 * ---------------------------------------------------------------- */
@Composable
private fun PlanTagCard(pkg: PaymentPackage) {
    val accent = accentFor(pkg.id)
    val icon   = iconFor(pkg.id)
    val level  = levelFor(pkg.id)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TagCardShape(cut = 28.dp))
            .background(TrackColor.BgCard)
    ) {
        Column {
            // ── Price / Header panel ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.55f))
                        )
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = pkg.subtitle,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = pkg.title,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (pkg.formattedOriginalPrice != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = pkg.formattedOriginalPrice,
                                fontFamily = PlusJakartaSans,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.7f),
                                textDecoration = TextDecoration.LineThrough
                            )
                            if (pkg.discountBadge != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = pkg.discountBadge,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = pkg.formattedPrice,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "/ selamanya",
                            fontFamily = PlusJakartaSans,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Feature level gauge (analogous to roast gauge in reference)
                    FeatureLevelGauge(level = level, accent = Color.White)
                }
            }

            // ── Perforation ──────────────────────────────────────────
            PerforationDivider(bgColor = TrackColor.BgCard)

            // ── Feature list ─────────────────────────────────────────
            Column(modifier = Modifier.padding(22.dp)) {
                pkg.features.forEach { feature ->
                    FeatureRow(text = feature, accent = accent)
                    Spacer(Modifier.height(10.dp))
                }

                if (pkg.badge != null) {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = TrackColor.Stroke)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Hemat Rp 29.000 dibanding beli terpisah",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = accent
                        )
                    }
                }
            }
        }

        // Tag hole (dekoratif, kiri atas)
        Box(
            modifier = Modifier
                .padding(top = 14.dp, start = 14.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(TrackColor.BgCard)
        )
    }
}


/* ---------------------------------------------------------------- *
 *  Feature Level Gauge (3-bar, mirip roast gauge di reference)     *
 * ---------------------------------------------------------------- */
@Composable
private fun FeatureLevelGauge(level: Int, accent: Color) {
    Column {
        Text(
            text = when (level) {
                1    -> "Fitur Dasar"
                2    -> "Fitur Lengkap"
                else -> "Akses Penuh"
            },
            fontFamily = PlusJakartaSans,
            fontSize = 10.5.sp,
            color = accent.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (index < level) Color.White
                            else Color.White.copy(alpha = 0.25f)
                        )
                )
            }
        }
    }
}

/* ---------------------------------------------------------------- *
 *  Perforation Divider                                              *
 * ---------------------------------------------------------------- */
@Composable
private fun PerforationDivider(bgColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = TrackColor.Stroke,
            start = Offset(0f, 0f),
            end   = Offset(size.width, 0f),
            strokeWidth = 2f,
            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
        // Notch circles di kedua ujung
        drawCircle(color = bgColor, radius = 10.dp.toPx(), center = Offset(0f, 0f))
        drawCircle(color = bgColor, radius = 10.dp.toPx(), center = Offset(size.width, 0f))
    }
}

/* ---------------------------------------------------------------- *
 *  Feature Row                                                      *
 * ---------------------------------------------------------------- */
@Composable
private fun FeatureRow(text: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(11.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            fontFamily = PlusJakartaSans,
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
            color = TrackColor.TextPrime.copy(alpha = 0.9f)
        )
    }
}
