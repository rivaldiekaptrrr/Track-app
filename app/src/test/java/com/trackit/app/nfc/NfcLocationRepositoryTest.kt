package com.trackit.app.nfc

import com.trackit.app.data.local.dao.NfcLocationDao
import com.trackit.app.data.local.entity.NfcLocationEntity
import com.trackit.app.nfc.location.NfcLocationApiClient
import com.trackit.app.nfc.location.NfcLocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcLocationRepositoryTest {

    private class FakeNfcLocationDao : NfcLocationDao {
        val storage = mutableMapOf<String, NfcLocationEntity>()

        override suspend fun getLocationById(locationId: String): NfcLocationEntity? = storage[locationId]
        override fun observeLocationById(locationId: String): Flow<NfcLocationEntity?> = flowOf(storage[locationId])
        override suspend fun insertOrUpdate(entity: NfcLocationEntity) {
            storage[entity.locationId] = entity
        }
        override suspend fun insertAll(entities: List<NfcLocationEntity>) {
            entities.forEach { storage[it.locationId] = it }
        }
    }

    private class FakeApiClient(private val responses: Map<String, String>) : NfcLocationApiClient() {
        val submitted = mutableMapOf<String, String>()
        override suspend fun fetchLocationName(locationId: String): String? = responses[locationId]
        override suspend fun submitLocation(locationId: String, locationName: String): Boolean {
            submitted[locationId] = locationName
            return true
        }
    }

    @Test
    fun testResolveLocation_fromLocalDatabase() = runBlocking {
        val dao = FakeNfcLocationDao()
        dao.insertOrUpdate(NfcLocationEntity(locationId = "0012A4F1", locationName = "Stasiun KRL Tangerang"))

        val apiClient = FakeApiClient(emptyMap())
        val repo = NfcLocationRepository(dao, apiClient)

        val result = repo.resolveLocation("0012A4F1")
        assertEquals("Stasiun KRL Tangerang", result)
    }

    @Test
    fun testResolveLocation_fromRemoteApiAndCached() = runBlocking {
        val dao = FakeNfcLocationDao()
        val apiClient = FakeApiClient(mapOf("0012A4F1" to "Stasiun KRL Tangerang"))
        val repo = NfcLocationRepository(dao, apiClient)

        val result = repo.resolveLocation("0012A4F1")
        assertEquals("Stasiun KRL Tangerang", result)

        // Verifikasi tersimpan di Room database
        val cached = dao.getLocationById("0012A4F1")
        assertEquals("Stasiun KRL Tangerang", cached?.locationName)
        assertEquals(false, cached?.isUserCustom)
    }

    @Test
    fun testResolveLocation_fromLocalDatabase_variantMatched() = runBlocking {
        val dao = FakeNfcLocationDao()
        // Database menyimpan EPT74560, kartu mengirim EP74560
        dao.insertOrUpdate(NfcLocationEntity(locationId = "EPT74560", locationName = "Stasiun KRL Tangerang"))

        val apiClient = FakeApiClient(emptyMap())
        val repo = NfcLocationRepository(dao, apiClient)

        val result = repo.resolveLocation("EP74560")
        assertEquals("Stasiun KRL Tangerang", result)
    }

    @Test
    fun testResolveLocation_notFoundReturnsNull() = runBlocking {
        val dao = FakeNfcLocationDao()
        val apiClient = FakeApiClient(emptyMap())
        val repo = NfcLocationRepository(dao, apiClient)

        val result = repo.resolveLocation("UNKNOWN_ID")
        assertNull(result)
    }

    @Test
    fun testSaveCustomLocation() = runBlocking {
        val dao = FakeNfcLocationDao()
        val apiClient = FakeApiClient(emptyMap())
        val repo = NfcLocationRepository(dao, apiClient)

        repo.saveCustomLocation("0099FFFF", "Gerbang Tol Cilandak")
        val saved = dao.getLocationById("0099FFFF")
        assertEquals("Gerbang Tol Cilandak", saved?.locationName)
        assertTrue(saved?.isUserCustom == true)
        assertEquals("Gerbang Tol Cilandak", apiClient.submitted["0099FFFF"])
    }
}
