package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingTaskDao
import com.trackit.app.data.local.entity.WeddingTaskEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingTaskRepository @Inject constructor(
    private val dao: WeddingTaskDao
) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingTaskEntity>> = dao.getAllByProfile(profileId)
    fun getUpcomingTasks(profileId: String): Flow<List<WeddingTaskEntity>> = dao.getUpcomingTasks(profileId)
    fun getTotalCount(profileId: String): Flow<Int> = dao.getTotalCount(profileId)
    fun getCompletedCount(profileId: String): Flow<Int> = dao.getCompletedCount(profileId)
    suspend fun insert(task: WeddingTaskEntity) = dao.insert(task)
    suspend fun insertAll(tasks: List<WeddingTaskEntity>) = dao.insertAll(tasks)
    suspend fun update(task: WeddingTaskEntity) = dao.update(task)
    suspend fun delete(task: WeddingTaskEntity) = dao.delete(task)
    suspend fun deleteAllByProfile(profileId: String) = dao.deleteAllByProfile(profileId)
}
