package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingCommitteeDao
import com.trackit.app.data.local.entity.WeddingCommitteeEntity
import com.trackit.app.util.SyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingCommitteeRepository @Inject constructor(
    private val dao: WeddingCommitteeDao,
    private val syncManager: SyncManager
) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingCommitteeEntity>> = dao.getAllByProfile(profileId)
    suspend fun insert(member: WeddingCommitteeEntity) { dao.insert(member); syncManager.pushWeddingCommitteeMember(member) }
    suspend fun insertAll(members: List<WeddingCommitteeEntity>) { dao.insertAll(members); members.forEach { syncManager.pushWeddingCommitteeMember(it) } }
    suspend fun update(member: WeddingCommitteeEntity) { dao.update(member); syncManager.pushWeddingCommitteeMember(member) }
    suspend fun delete(member: WeddingCommitteeEntity) { dao.delete(member); syncManager.deleteWeddingCommitteeMember(member) }
}
