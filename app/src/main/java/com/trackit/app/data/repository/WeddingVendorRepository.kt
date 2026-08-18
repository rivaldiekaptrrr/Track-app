package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingVendorDao
import com.trackit.app.data.local.entity.WeddingVendorEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingVendorRepository @Inject constructor(private val dao: WeddingVendorDao) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingVendorEntity>> = dao.getAllByProfile(profileId)
    fun getByCategory(profileId: String, category: String): Flow<List<WeddingVendorEntity>> = dao.getByCategory(profileId, category)
    suspend fun insert(vendor: WeddingVendorEntity) = dao.insert(vendor)
    suspend fun update(vendor: WeddingVendorEntity) = dao.update(vendor)
    suspend fun delete(vendor: WeddingVendorEntity) = dao.delete(vendor)
}
