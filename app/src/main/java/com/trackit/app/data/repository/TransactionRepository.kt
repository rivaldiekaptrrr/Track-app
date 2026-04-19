package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.CategorySpending
import com.trackit.app.data.local.dao.TransactionDao
import com.trackit.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    fun getTransactionsByMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByMonth(startOfMonth, endOfMonth)

    fun getTotalSpentInMonth(startOfMonth: Long, endOfMonth: Long): Flow<Double> =
        transactionDao.getTotalSpentInMonth(startOfMonth, endOfMonth)

    suspend fun getTotalSpentInMonthSync(startOfMonth: Long, endOfMonth: Long): Double =
        transactionDao.getTotalSpentInMonthSync(startOfMonth, endOfMonth)

    fun getSpendingByCategory(startOfMonth: Long, endOfMonth: Long): Flow<List<CategorySpending>> =
        transactionDao.getSpendingByCategory(startOfMonth, endOfMonth)

    suspend fun getRecurringTransactions(): List<TransactionEntity> =
        transactionDao.getRecurringTransactions()

    suspend fun insert(transaction: TransactionEntity): Long =
        transactionDao.insert(transaction)

    suspend fun update(transaction: TransactionEntity) =
        transactionDao.update(transaction)

    suspend fun delete(transaction: TransactionEntity) =
        transactionDao.delete(transaction)

    suspend fun deleteById(id: Long) =
        transactionDao.deleteById(id)

    suspend fun getById(id: Long): TransactionEntity? =
        transactionDao.getById(id)

    suspend fun updateTransactionsCategory(oldCategoryId: Long, newCategoryId: Long) =
        transactionDao.updateTransactionsCategory(oldCategoryId, newCategoryId)

    suspend fun countTransactionsByCategory(categoryId: Long): Int =
        transactionDao.countTransactionsByCategory(categoryId)
}
