package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingSeserahanDao
import com.trackit.app.data.local.entity.WeddingSeserahanEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingSeserahanRepository @Inject constructor(private val dao: WeddingSeserahanDao) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingSeserahanEntity>> = dao.getAllByProfile(profileId)
    fun getByDirection(profileId: String, direction: String): Flow<List<WeddingSeserahanEntity>> = dao.getByDirection(profileId, direction)
    suspend fun insert(item: WeddingSeserahanEntity) = dao.insert(item)
    suspend fun insertAll(items: List<WeddingSeserahanEntity>) = dao.insertAll(items)
    suspend fun update(item: WeddingSeserahanEntity) = dao.update(item)
    suspend fun delete(item: WeddingSeserahanEntity) = dao.delete(item)
}
