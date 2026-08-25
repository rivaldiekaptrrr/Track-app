package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingDocumentDao
import com.trackit.app.data.local.entity.WeddingDocumentEntity
import com.trackit.app.util.SyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingDocumentRepository @Inject constructor(
    private val dao: WeddingDocumentDao,
    private val syncManager: SyncManager
) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingDocumentEntity>> = dao.getAllByProfile(profileId)
    fun getTotalCount(profileId: String): Flow<Int> = dao.getTotalCount(profileId)
    fun getCompletedCount(profileId: String): Flow<Int> = dao.getCompletedCount(profileId)
    suspend fun insert(doc: WeddingDocumentEntity) { dao.insert(doc); syncManager.pushWeddingDocument(doc) }
    suspend fun insertAll(docs: List<WeddingDocumentEntity>) { dao.insertAll(docs); docs.forEach { syncManager.pushWeddingDocument(it) } }
    suspend fun update(doc: WeddingDocumentEntity) { dao.update(doc); syncManager.pushWeddingDocument(doc) }
    suspend fun delete(doc: WeddingDocumentEntity) { dao.delete(doc); syncManager.deleteWeddingDocument(doc) }
}
