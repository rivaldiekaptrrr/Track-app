package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingGuestDao
import com.trackit.app.data.local.entity.WeddingGuestEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingGuestRepository @Inject constructor(
    private val dao: WeddingGuestDao
) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingGuestEntity>> = dao.getAllByProfile(profileId)
    fun getTotalCount(profileId: String): Flow<Int> = dao.getTotalCount(profileId)
    fun getTotalPax(profileId: String): Flow<Int?> = dao.getTotalPax(profileId)
    suspend fun insert(guest: WeddingGuestEntity) = dao.insert(guest)
    suspend fun insertAll(guests: List<WeddingGuestEntity>) = dao.insertAll(guests)
    suspend fun update(guest: WeddingGuestEntity) = dao.update(guest)
    suspend fun delete(guest: WeddingGuestEntity) = dao.delete(guest)
    suspend fun renameGroup(profileId: String, oldGroup: String, newGroup: String) = dao.renameGroup(profileId, oldGroup, newGroup)
}
