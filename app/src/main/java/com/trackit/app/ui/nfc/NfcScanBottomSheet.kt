package com.trackit.app.ui.nfc

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trackit.app.nfc.model.EmoneyCardResult

/**
 * Modal Bottom Sheet interaktif untuk pemindaian dan penampil saldo kartu e-Money via NFC.
 * Mematuhi antislop-ui, humanizer-indonesia, dan aturan no-emoji-icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcScanBottomSheet(
    uiState: NfcUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onRecordExpense: (amount: Long, cardName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Trigger haptic feedback saat pembacaan sukses
    LaunchedEffect(uiState) {
        if (uiState is NfcUiState.Success) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState) {
                is NfcUiState.Idle, is NfcUiState.Scanning -> {
                    ScanningStateView()
                }

                is NfcUiState.Success -> {
                    SuccessStateView(
                        card = uiState.card,
                        onRecordExpense = { onRecordExpense(uiState.card.balance, uiState.card.cardType.displayName) },
                        onClose = onDismiss
                    )
                }

                is NfcUiState.Error -> {
                    ErrorStateView(
                        errorMessage = uiState.message,
                        onRetry = onRetry
                    )
                }

                is NfcUiState.Disabled -> {
                    DisabledStateView(
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }

                is NfcUiState.NotSupported -> {
                    NotSupportedStateView(onClose = onDismiss)
                }
            }
        }
    }
}

/**
 * Tampilan saat menunggu kartu ditempelkan (dengan radar animasi halus)
 */
@Composable
private fun ScanningStateView() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Lingkaran Radar NFC
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(130.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Tempelkan Kartu di Belakang Ponsel",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Mendukung Mandiri e-Money, BCA Flazz, BNI TapCash, BRI Brizzi, dan Bank DKI JakCard. Tahan posisi kartu sebentar hingga bergetar.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))
}

/**
 * Tampilan saat kartu berhasil dibaca
 */
@Composable
private fun SuccessStateView(
    card: EmoneyCardResult,
    onRecordExpense: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Kartu Berhasil Terbaca",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2E7D32)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Widget Kartu Fisik Realistis
    EmoneyCardWidget(
        card = card,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Tombol Aksi
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onRecordExpense,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Catat Transaksi Kartu Ini",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(
                text = "Selesai",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Tampilan jika terjadi kesalahan transmisi atau kartu lepas
 */
@Composable
private fun ErrorStateView(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    val errorTitle = when {
        errorMessage.contains("belum didukung", ignoreCase = true) ||
        errorMessage.contains("tidak mendukung", ignoreCase = true) ->
            "Kartu Belum Didukung"
        else ->
            "Pembacaan Terputus"
    }

    Text(
        text = errorTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onRetry,
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Coba Pindai Lagi")
    }
}

/**
 * Tampilan jika sensor NFC pada perangkat sedang dimatikan
 */
@Composable
private fun DisabledStateView(
    onOpenSettings: () -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "NFC Sedang Tidak Aktif",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Aktifkan sensor NFC di pengaturan perangkat ponsel Anda untuk memindai kartu uang elektronik.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onOpenSettings,
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Buka Pengaturan NFC")
    }
}

/**
 * Tampilan jika perangkat tidak memiliki hardware NFC
 */
@Composable
private fun NotSupportedStateView(
    onClose: () -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Sensor NFC Tidak Tersedia",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Perangkat ponsel ini tidak dilengkapi dengan perangkat keras sensor NFC.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedButton(
        onClick = onClose,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Tutup")
    }
}

