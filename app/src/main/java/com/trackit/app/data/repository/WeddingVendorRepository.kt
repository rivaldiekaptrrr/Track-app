package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingVendorDao
import com.trackit.app.data.local.entity.WeddingVendorEntity
import com.trackit.app.util.SyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingVendorRepository @Inject constructor(
    private val dao: WeddingVendorDao,
    private val syncManager: SyncManager
) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingVendorEntity>> = dao.getAllByProfile(profileId)
    fun getByCategory(profileId: String, category: String): Flow<List<WeddingVendorEntity>> = dao.getByCategory(profileId, category)
    suspend fun insert(vendor: WeddingVendorEntity) { dao.insert(vendor); syncManager.pushWeddingVendor(vendor) }
    suspend fun update(vendor: WeddingVendorEntity) { dao.update(vendor); syncManager.pushWeddingVendor(vendor) }
    suspend fun delete(vendor: WeddingVendorEntity) { dao.delete(vendor); syncManager.deleteWeddingVendor(vendor) }
}
