package com.trackit.app.ui.nfc

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.nfc.model.EmoneyCardResult
import com.trackit.app.nfc.model.NfcTransactionType
import com.trackit.app.util.CategoryIconMapper
import com.trackit.app.util.CurrencyUtils
import kotlinx.coroutines.delay

/**
 * Modal Bottom Sheet interaktif untuk pemindaian, penampil saldo, serta riwayat transaksi terakhir kartu e-Money via NFC.
 * Mematuhi antislop-ui, humanizer-indonesia, dan aturan no-emoji-icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcScanBottomSheet(
    uiState: NfcUiState,
    categories: List<CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onRecordExpense: (amount: Long, cardName: String) -> Unit,
    onRecordLastTransaction: (category: CategoryEntity, keywordToLearn: String?, customLocationName: String?) -> Unit = { _, _, _ -> },
    recordSuccessMessage: String? = null,
    onClearRecordSuccessMessage: () -> Unit = {},
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

    // Auto-dismiss pesan sukses transaksi setelah 3.5 detik
    LaunchedEffect(recordSuccessMessage) {
        if (recordSuccessMessage != null) {
            delay(3500)
            onClearRecordSuccessMessage()
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
                .padding(bottom = 32.dp, top = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Banner Pesan Sukses Catat Transaksi
            AnimatedVisibility(
                visible = recordSuccessMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = recordSuccessMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            when (uiState) {
                is NfcUiState.Idle, is NfcUiState.Scanning -> {
                    ScanningStateView()
                }

                is NfcUiState.Success -> {
                    SuccessStateView(
                        card = uiState.card,
                        lastTransactionState = uiState.lastTransactionState,
                        categories = categories,
                        onRecordExpense = { onRecordExpense(uiState.card.balance, uiState.card.cardType.displayName) },
                        onRecordLastTransaction = onRecordLastTransaction,
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
    var showSupportedCardsDialog by remember { mutableStateOf(false) }

    if (showSupportedCardsDialog) {
        SupportedCardsDialog(onDismiss = { showSupportedCardsDialog = false })
    }

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

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "Tempelkan Kartu di Belakang Ponsel",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = "Posisikan kartu di dekat sensor NFC hingga ponsel bergetar",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Tombol info kecil dan elegan (Progressive Disclosure)
    Surface(
        onClick = { showSupportedCardsDialog = true },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Informasi Kartu",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Lihat kartu yang didukung",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

private data class SupportedCardInfo(
    val name: String,
    val features: String,
    val brandColor: Color
)

@Composable
private fun SupportedCardsDialog(
    onDismiss: () -> Unit
) {
    val supportedCards = remember {
        listOf(
            SupportedCardInfo("BCA Flazz (Gen 1 & 2)", "Saldo & riwayat transaksi (Tol, Parkir, Belanja)", Color(0xFF005BAC)),
            SupportedCardInfo("Mandiri e-Money (G1 & G2)", "Saldo & riwayat transaksi (Tol, Parkir, Belanja)", Color(0xFFF3B329)),
            SupportedCardInfo("BNI TapCash", "Saldo & riwayat transaksi", Color(0xFFEA5A00)),
            SupportedCardInfo("BRI Brizzi", "Saldo & riwayat transaksi", Color(0xFF00529C)),
            SupportedCardInfo("KAI Commuter KMT", "Saldo & log stasiun gerbang KRL (Sony FeliCa)", Color(0xFFB71C1C)),
            SupportedCardInfo("Bank DKI JakCard", "Saldo & nomor kartu", Color(0xFFC62828)),
            SupportedCardInfo("Bank Mega MegaCash", "Saldo & nomor kartu", Color(0xFFFF9800))
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Kartu yang Didukung",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "TrackIt mendukung pembacaan saldo dan riwayat mutasi untuk 7 jenis kartu e-money contactless:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                supportedCards.forEach { card ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(card.brandColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = card.features,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tempelkan kartu langsung tanpa terhalang dompet atau casing logam tebal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Mengerti",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

/**
 * Tampilan saat kartu berhasil dibaca
 */
@Composable
private fun SuccessStateView(
    card: EmoneyCardResult,
    lastTransactionState: ResolvedLastTransaction?,
    categories: List<CategoryEntity>,
    onRecordExpense: () -> Unit,
    onRecordLastTransaction: (category: CategoryEntity, keywordToLearn: String?, customLocationName: String?) -> Unit,
    onClose: () -> Unit
) {
    var showCategoryDialog by remember { mutableStateOf(false) }

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

    // Kartu Transaksi Terakhir (jika tersedia)
    if (lastTransactionState != null) {
        Spacer(modifier = Modifier.height(16.dp))
        LastTransactionCard(
            lastTx = lastTransactionState,
            onRecordClicked = {
                if (lastTransactionState.matchedCategory != null) {
                    onRecordLastTransaction(lastTransactionState.matchedCategory, null, null)
                } else {
                    showCategoryDialog = true
                }
            }
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Tombol Aksi Manual & Tutup
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
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
                text = "Catat Transaksi Manual",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        FilledTonalButton(
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

    // Dialog Pemilihan Kategori & Pembelajaran Kata Kunci
    if (showCategoryDialog && lastTransactionState != null) {
        CategoryPickerDialog(
            lastTx = lastTransactionState,
            categories = categories,
            onDismiss = { showCategoryDialog = false },
            onConfirm = { selectedCategory, keyword, customLoc ->
                showCategoryDialog = false
                onRecordLastTransaction(selectedCategory, keyword, customLoc)
            }
        )
    }
}

/**
 * Kartu Ringkasan Transaksi Terakhir dari NFC Chip
 */
@Composable
private fun LastTransactionCard(
    lastTx: ResolvedLastTransaction,
    onRecordClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Transaksi Terakhir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Transaksi Terakhir",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (lastTx.transaction.type) {
                        NfcTransactionType.PAYMENT -> MaterialTheme.colorScheme.errorContainer
                        NfcTransactionType.TOPUP -> Color(0xFFE8F5E9)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (lastTx.transaction.type) {
                            NfcTransactionType.PAYMENT -> "Pembayaran"
                            NfcTransactionType.TOPUP -> "Top Up"
                            else -> "Transaksi"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (lastTx.transaction.type) {
                            NfcTransactionType.PAYMENT -> MaterialTheme.colorScheme.onErrorContainer
                            NfcTransactionType.TOPUP -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nominal & Tanggal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Nominal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "- ${CurrencyUtils.formatRupiah(lastTx.transaction.amount.toDouble())}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = lastTx.transaction.dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = lastTx.transaction.timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Lokasi
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (lastTx.isResolvingLocation) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mencari lokasi...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = lastTx.locationName ?: if (lastTx.transaction.locationId.isNotBlank()) "Terminal ${lastTx.transaction.locationId}" else "Lokasi Tidak Diketahui",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Kategori Terdeteksi
            if (lastTx.matchedCategory != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = CategoryIconMapper.getIcon(lastTx.matchedCategory.iconName),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kategori: ${lastTx.matchedCategory.name} (Otomatis)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tombol Catat
            if (lastTx.isRecorded) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Transaksi Berhasil Dicatat",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onRecordClicked,
                    enabled = !lastTx.isRecording && !lastTx.isResolvingLocation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (lastTx.isRecording) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lastTx.matchedCategory != null) "Catat Transaksi Ini" else "Pilih Kategori & Catat",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modal / Dialog untuk memilih kategori dan mempelajari kata kunci baru
 */
@Composable
private fun CategoryPickerDialog(
    lastTx: ResolvedLastTransaction,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (category: CategoryEntity, keyword: String?, customLocationName: String?) -> Unit
) {
    val expenseCategories = remember(categories) {
        categories.filter { it.type == "EXPENSE" && !it.isHidden }
    }

    var selectedCategory by remember { mutableStateOf(expenseCategories.firstOrNull()) }
    var keywordInput by remember { mutableStateOf(lastTx.suggestedKeyword) }
    var customLocationInput by remember { mutableStateOf(lastTx.locationName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pilih Kategori Transaksi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info Transaksi Singkat
                Text(
                    text = "Pengeluaran sebesar ${CurrencyUtils.formatRupiah(lastTx.transaction.amount.toDouble())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Input Nama Lokasi Manual (jika belum terpetakan)
                if (lastTx.locationName == null) {
                    OutlinedTextField(
                        value = customLocationInput,
                        onValueChange = { customLocationInput = it },
                        label = { Text("Nama Lokasi") },
                        placeholder = { Text("Contoh: Stasiun KRL Tangerang") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Input Kata Kunci Pembelajaran Otomatis
                Column {
                    OutlinedTextField(
                        value = keywordInput,
                        onValueChange = { keywordInput = it },
                        label = { Text("Kata Kunci Pengingat") },
                        placeholder = { Text("Contoh: Stasiun") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TrackIt akan otomatis mengingat kata kunci ini untuk transaksi berikutnya.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Pemilihan Kategori
                Text(
                    text = "Daftar Kategori",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(expenseCategories) { category ->
                        val isSelected = category.id == selectedCategory?.id
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = containerColor,
                            modifier = Modifier
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = category }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = CategoryIconMapper.getIcon(category.iconName),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cat = selectedCategory ?: return@Button
                    val kw = keywordInput.trim().takeIf { it.isNotBlank() }
                    val loc = customLocationInput.trim().takeIf { it.isNotBlank() }
                    onConfirm(cat, kw, loc)
                },
                enabled = selectedCategory != null
            ) {
                Text("Simpan Transaksi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
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
