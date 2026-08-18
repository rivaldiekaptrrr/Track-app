package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingEventDao
import com.trackit.app.data.local.dao.WeddingRundownItemDao
import com.trackit.app.data.local.entity.WeddingEventEntity
import com.trackit.app.data.local.entity.WeddingRundownItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingEventRepository @Inject constructor(private val dao: WeddingEventDao) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingEventEntity>> = dao.getAllByProfile(profileId)
    suspend fun insert(event: WeddingEventEntity) = dao.insert(event)
    suspend fun insertAll(events: List<WeddingEventEntity>) = dao.insertAll(events)
    suspend fun update(event: WeddingEventEntity) = dao.update(event)
    suspend fun delete(event: WeddingEventEntity) = dao.delete(event)
}

@Singleton
class WeddingRundownRepository @Inject constructor(private val dao: WeddingRundownItemDao) {
    fun getByEvent(eventId: String): Flow<List<WeddingRundownItemEntity>> = dao.getByEvent(eventId)
    suspend fun insert(item: WeddingRundownItemEntity) = dao.insert(item)
    suspend fun insertAll(items: List<WeddingRundownItemEntity>) = dao.insertAll(items)
    suspend fun update(item: WeddingRundownItemEntity) = dao.update(item)
    suspend fun delete(item: WeddingRundownItemEntity) = dao.delete(item)
    suspend fun deleteAllByEvent(eventId: String) = dao.deleteAllByEvent(eventId)
}
