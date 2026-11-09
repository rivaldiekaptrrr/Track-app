package com.trackit.app.nfc.location

import com.trackit.app.data.local.dao.NfcLocationDao
import com.trackit.app.data.local.entity.NfcLocationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcLocationRepository @Inject constructor(
    private val nfcLocationDao: NfcLocationDao,
    private val apiClient: NfcLocationApiClient
) {

    /**
     * Menyelesaikan nama lokasi manusiawi dari locationId hex kartu.
     * Alur:
     * 1. Cek database lokal Room
     * 2. Jika belum ada, panggil API robycohen.pythonanywhere.com
     * 3. Jika API berhasil, simpan ke Room sebagai cache
     * 4. Jika gagal/offline, return null (fallback ke raw hex)
     */
    suspend fun resolveLocation(locationId: String): String? = withContext(Dispatchers.IO) {
        if (locationId.isBlank()) return@withContext null
        val cleanId = locationId.trim()

        // 1. Cek cache lokal
        val local = nfcLocationDao.getLocationById(cleanId)
        if (local != null && local.locationName.isNotBlank()) {
            return@withContext local.locationName
        }

        // Cek varian lokal (misal EP74560 <-> EPT74560)
        val variantId = if (cleanId.startsWith("EP", ignoreCase = true) && cleanId.length == 7) {
            "EPT" + cleanId.substring(2)
        } else if (cleanId.startsWith("EPT", ignoreCase = true) && cleanId.length == 8) {
            "EP" + cleanId.substring(3)
        } else null

        if (variantId != null) {
            val localVariant = nfcLocationDao.getLocationById(variantId)
            if (localVariant != null && localVariant.locationName.isNotBlank()) {
                return@withContext localVariant.locationName
            }
        }

        // 2. Query Remote API
        val remoteName = apiClient.fetchLocationName(cleanId)
        if (!remoteName.isNullOrBlank()) {
            nfcLocationDao.insertOrUpdate(
                NfcLocationEntity(
                    locationId = cleanId,
                    locationName = remoteName,
                    isUserCustom = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
            return@withContext remoteName
        }

        null
    }

    /**
     * Simpan pemetaan lokasi kustom manual yang diinput oleh pengguna.
     */
    suspend fun saveCustomLocation(locationId: String, customName: String) = withContext(Dispatchers.IO) {
        if (locationId.isBlank() || customName.isBlank()) return@withContext
        val cleanCustom = customName.trim()
        val cleanId = locationId.trim()
        nfcLocationDao.insertOrUpdate(
            NfcLocationEntity(
                locationId = cleanId,
                locationName = cleanCustom,
                isUserCustom = true,
                updatedAt = System.currentTimeMillis()
            )
        )
        // Kontribusi juga ke crowd-sourced API
        apiClient.submitLocation(cleanId, cleanCustom)
    }
}
