package com.trackit.app.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class PaymentResult {
    data class Success(
        val token: String,
        val redirectUrl: String,
        val orderId: String
    ) : PaymentResult()

    data class Error(val message: String) : PaymentResult()
}

data class PaymentPackage(
    val id: String,
    val title: String,
    val subtitle: String,
    val price: Long,
    val formattedPrice: String,
    val originalPrice: Long? = null,
    val formattedOriginalPrice: String? = null,
    val discountBadge: String? = null,
    val features: List<String>,
    val isRecommended: Boolean = false,
    val badge: String? = null
)

@Singleton
class PaymentRepository @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Vercel Serverless Backend Base URL
    private val backendUrl = "https://track-it-backend-sand.vercel.app/api/create-snap-token"

    val availablePackages = listOf(
        PaymentPackage(
            id = AccessLevel.EXPENSE,
            title = "Expense Tracker",
            subtitle = "Kelola Keuangan Pribadi",
            price = 49000,
            formattedPrice = "Rp 49.000",
            originalPrice = 69000,
            formattedOriginalPrice = "Rp 69.000",
            discountBadge = "Promo Rilis",
            features = listOf(
                "Catat Pemasukan & Pengeluaran",
                "Manajemen Multi-Profil Keuangan",
                "Fitur Cepat Voice Entry & AI",
                "Grafik & Analisis Keuangan",
                "Export Laporan Excel (CSV) & PDF"
            )
        ),
        PaymentPackage(
            id = AccessLevel.WEDDING,
            title = "Wedding Planner",
            subtitle = "Perencanaan Pernikahan Lengkap",
            price = 49000,
            formattedPrice = "Rp 49.000",
            originalPrice = 69000,
            formattedOriginalPrice = "Rp 69.000",
            discountBadge = "Promo Rilis",
            features = listOf(
                "Checklist Tugas & Timeline Acara",
                "Anggaran Biaya & Catatan Pembayaran",
                "Daftar Berkas Pernikahan",
                "Manajemen Buku Tamu & Undangan",
                "Daftar Panitia, Vendor & Seserahan"
            )
        ),
        PaymentPackage(
            id = AccessLevel.BOTH,
            title = "Full Access (Paket Hemat)",
            subtitle = "Akses Semua Modul Selamanya",
            price = 69000,
            formattedPrice = "Rp 69.000",
            originalPrice = 98000,
            formattedOriginalPrice = "Rp 98.000",
            discountBadge = "Hemat 30%",
            features = listOf(
                "2 Modul dalam 1 Aplikasi",
                "Akses Fitur Expense Tracker",
                "Akses Fitur Wedding Planner",
                "Bebas Iklan & Biaya Bulanan",
            ),
            isRecommended = true,
            badge = "PALING HEMAT"
        )
    )

    /**
     * Memanggil Serverless Backend Vercel untuk membuat Transaksi Midtrans Snap
     */
    suspend fun createSnapToken(
        userId: String,
        email: String,
        accessLevel: String
    ): PaymentResult = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("userId", userId)
                put("email", email)
                put("accessLevel", accessLevel)
            }.toString()

            val request = Request.Builder()
                .url(backendUrl)
                .post(jsonPayload.toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val token = json.optString("token")
                val redirectUrl = json.optString("redirect_url")
                val orderId = json.optString("orderId")

                if (token.isNotBlank() && redirectUrl.isNotBlank()) {
                    PaymentResult.Success(
                        token = token,
                        redirectUrl = redirectUrl,
                        orderId = orderId
                    )
                } else {
                    PaymentResult.Error("Respons Midtrans tidak lengkap.")
                }
            } else {
                Log.e("PaymentRepository", "Error ${response.code}: $responseBody")
                val json = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    null
                }
                val mainError = json?.optString("error") ?: "Gagal memproses pembayaran (HTTP ${response.code})"
                val details = json?.optString("details")
                val fullMsg = if (!details.isNullOrBlank() && details != "null") "$mainError ($details)" else mainError
                PaymentResult.Error(fullMsg)
            }
        } catch (e: Exception) {
            Log.e("PaymentRepository", "Exception: ${e.message}", e)
            PaymentResult.Error(e.message ?: "Terjadi kesalahan saat menghubungi server pembayaran.")
        }
    }
}
