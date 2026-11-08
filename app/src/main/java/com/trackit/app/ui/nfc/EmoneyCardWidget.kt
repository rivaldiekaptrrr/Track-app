package com.trackit.app.ui.nfc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackit.app.nfc.model.EmoneyCardResult
import com.trackit.app.util.CurrencyUtils

/**
 * Komponen kartu uang elektronik fisik realistis sesuai proporsi ISO/IEC 7810 ID-1 (1.586 : 1).
 * Bebas dari gradien generik AI; menggunakan palet warna korporat resmi penerbit kartu.
 */
@Composable
fun EmoneyCardWidget(
    card: EmoneyCardResult,
    modifier: Modifier = Modifier
) {
    val cardType = card.cardType

    CardPhysicalShell(
        primaryColor = cardType.primaryColor,
        secondaryColor = cardType.secondaryColor,
        accentColor = cardType.accentColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Baris Atas: Chip Kontak & Logo/Nama Bank
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Chip Kontak Fisik (EMV Microchip Graphic)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MicrochipGraphic()
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = "Contactless Wave",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Nama Bank & Label Kartu
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = cardType.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = cardType.bankName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    if (card.isChipG2) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = cardType.accentColor.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardType.accentColor)
                        ) {
                            Text(
                                text = "Chip G2",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = cardType.accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Bagian Tengah: Saldo Utama
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Sisa Saldo",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = CurrencyUtils.formatRupiah(card.balance.toDouble()),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        letterSpacing = 0.5.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Baris Bawah: Nomor Kartu Bertopeng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.formattedCardNumber(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.90f)
                )

                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Cangkang Kartu dengan Rasio Aspek ISO/IEC 7810 ID-1 (1.586 : 1)
 */
@Composable
private fun CardPhysicalShell(
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.25f),
                ambientColor = Color.Black.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        content = content
    )
}

/**
 * Grafis Microchip Kontak Emas Fisik
 */
@Composable
private fun MicrochipGraphic(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(width = 38.dp, height = 30.dp),
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFFD4AF37), // Metallic Gold
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF997A15))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Garis sirkuit dalam chip
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color(0xFF8C6D0F))
                    .align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF8C6D0F))
                    .align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 12.dp)
                    .border(1.dp, Color(0xFF8C6D0F), RoundedCornerShape(2.dp))
                    .align(Alignment.Center)
            )
        }
    }
}

