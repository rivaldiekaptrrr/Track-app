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

        // 1. Cek cache lokal
        val local = nfcLocationDao.getLocationById(locationId)
        if (local != null && local.locationName.isNotBlank()) {
            return@withContext local.locationName
        }

        // 2. Query Remote API
        val remoteName = apiClient.fetchLocationName(locationId)
        if (!remoteName.isNullOrBlank()) {
            nfcLocationDao.insertOrUpdate(
                NfcLocationEntity(
                    locationId = locationId,
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
        nfcLocationDao.insertOrUpdate(
            NfcLocationEntity(
                locationId = locationId,
                locationName = customName.trim(),
                isUserCustom = true,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
