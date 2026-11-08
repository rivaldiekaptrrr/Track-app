package com.trackit.app.nfc.model

import androidx.compose.ui.graphics.Color

enum class EmoneyCardType(
    val displayName: String,
    val bankName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color
) {
    MANDIRI_EMONEY(
        displayName = "Mandiri e-Money",
        bankName = "Bank Mandiri",
        primaryColor = Color(0xFF0A2540),    // Deep Navy
        secondaryColor = Color(0xFF071B2F),  // Midnight Navy
        accentColor = Color(0xFFF3B329)     // Gold
    ),
    BCA_FLAZZ(
        displayName = "BCA Flazz",
        bankName = "Bank Central Asia",
        primaryColor = Color(0xFF005BAC),    // BCA Royal Blue
        secondaryColor = Color(0xFF00386C),  // Darker Royal Blue
        accentColor = Color(0xFFE2F1FF)     // Soft Blue White
    ),
    BNI_TAPCASH(
        displayName = "BNI TapCash",
        bankName = "Bank Negara Indonesia",
        primaryColor = Color(0xFFEA5A00),    // BNI Orange
        secondaryColor = Color(0xFFB84500),  // Deep Orange
        accentColor = Color(0xFF006677)     // BNI Teal
    ),
    BRI_BRIZZI(
        displayName = "BRI Brizzi",
        bankName = "Bank Rakyat Indonesia",
        primaryColor = Color(0xFF00529C),    // BRI Cobalt
        secondaryColor = Color(0xFF003366),  // Deep Cobalt
        accentColor = Color(0xFFF7941D)     // BRI Accent Orange
    ),
    DKI_JAKCARD(
        displayName = "JakCard",
        bankName = "Bank DKI",
        primaryColor = Color(0xFFC62828),    // Crimson Maroon
        secondaryColor = Color(0xFF8E0000),  // Deep Maroon
        accentColor = Color(0xFFFFB300)     // Amber
    ),
    MEGA_CASH(
        displayName = "Mega Cash",
        bankName = "Bank Mega",
        primaryColor = Color(0xFF263238),    // Dark Slate
        secondaryColor = Color(0xFF102027),  // Deep Slate
        accentColor = Color(0xFFFF9800)     // Orange Accent
    ),
    UNKNOWN(
        displayName = "Kartu Tidak Dikenal",
        bankName = "Tidak Teridentifikasi",
        primaryColor = Color(0xFF37474F),    // Neutral Slate
        secondaryColor = Color(0xFF263238),  // Darker Slate
        accentColor = Color(0xFF90A4AE)     // Soft Slate
    )
}

