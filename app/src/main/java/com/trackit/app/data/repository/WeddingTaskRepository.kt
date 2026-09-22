package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingTaskDao
import com.trackit.app.data.local.entity.WeddingTaskEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.trackit.app.util.SyncManager

@Singleton
class WeddingTaskRepository @Inject constructor(
    private val dao: WeddingTaskDao,
    private val syncManager: SyncManager
) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingTaskEntity>> = dao.getAllByProfile(profileId)
    fun getUpcomingTasks(profileId: String): Flow<List<WeddingTaskEntity>> = dao.getUpcomingTasks(profileId)
    fun getTotalCount(profileId: String): Flow<Int> = dao.getTotalCount(profileId)
    fun getCompletedCount(profileId: String): Flow<Int> = dao.getCompletedCount(profileId)
    suspend fun insert(task: WeddingTaskEntity) {
        dao.insert(task)
        syncManager.pushWeddingTask(task)
    }
    suspend fun insertAll(tasks: List<WeddingTaskEntity>) {
        dao.insertAll(tasks)
        tasks.forEach { syncManager.pushWeddingTask(it) }
    }
    suspend fun update(task: WeddingTaskEntity) {
        dao.update(task)
        syncManager.pushWeddingTask(task)
    }
    suspend fun delete(task: WeddingTaskEntity) {
        dao.delete(task)
        syncManager.deleteWeddingTask(task)
    }
    suspend fun deleteAllByProfile(profileId: String) {
        val tasks = dao.getAllByProfileSync(profileId)
        tasks.forEach { syncManager.deleteWeddingTask(it) }
        dao.deleteAllByProfile(profileId)
    }
}
