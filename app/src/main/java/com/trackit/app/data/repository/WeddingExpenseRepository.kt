package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingExpenseDao
import com.trackit.app.data.local.dao.WeddingPaymentTermDao
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import com.trackit.app.data.local.entity.WeddingPaymentTermEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.trackit.app.util.SyncManager

@Singleton
class WeddingExpenseRepository @Inject constructor(
    private val expenseDao: WeddingExpenseDao,
    private val termDao: WeddingPaymentTermDao,
    private val syncManager: SyncManager
) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingExpenseEntity>> = expenseDao.getAllByProfile(profileId)
    fun getTotalEstimated(profileId: String): Flow<Double?> = expenseDao.getTotalEstimated(profileId)
    fun getTotalPaid(profileId: String): Flow<Double?> = expenseDao.getTotalPaid(profileId)
    suspend fun insert(expense: WeddingExpenseEntity) {
        expenseDao.insert(expense)
        syncManager.pushWeddingExpense(expense)
    }
    suspend fun update(expense: WeddingExpenseEntity) {
        expenseDao.update(expense)
        syncManager.pushWeddingExpense(expense)
    }
    suspend fun delete(expense: WeddingExpenseEntity) {
        expenseDao.delete(expense)
        syncManager.deleteWeddingExpense(expense)
    }

    fun getTermsByExpense(expenseId: String): Flow<List<WeddingPaymentTermEntity>> = termDao.getByExpense(expenseId)
    fun getAllUnpaidTerms(): Flow<List<WeddingPaymentTermEntity>> = termDao.getAllUnpaid()
    suspend fun insertTerm(term: WeddingPaymentTermEntity) { termDao.insert(term); syncManager.pushWeddingPaymentTerm(term) }
    suspend fun updateTerm(term: WeddingPaymentTermEntity) { termDao.update(term); syncManager.pushWeddingPaymentTerm(term) }
    suspend fun deleteTerm(term: WeddingPaymentTermEntity) { termDao.delete(term); syncManager.deleteWeddingPaymentTerm(term) }
}
