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
    fun getAllTransactions(profileId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions(profileId)

    fun getRecentTransactions(profileId: Long, limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(profileId, limit)

    fun getTransactionsByMonth(startOfMonth: Long, endOfMonth: Long, profileId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByMonth(startOfMonth, endOfMonth, profileId)

    fun getTotalSpentInMonth(startOfMonth: Long, endOfMonth: Long, profileId: Long): Flow<Double> =
        transactionDao.getTotalSpentInMonth(startOfMonth, endOfMonth, profileId)

    fun getTotalIncomeInMonth(startOfMonth: Long, endOfMonth: Long, profileId: Long): Flow<Double> =
        transactionDao.getTotalIncomeInMonth(startOfMonth, endOfMonth, profileId)

    suspend fun getTotalSpentInMonthSync(startOfMonth: Long, endOfMonth: Long, profileId: Long): Double =
        transactionDao.getTotalSpentInMonthSync(startOfMonth, endOfMonth, profileId)

    fun getSpendingByCategory(startOfMonth: Long, endOfMonth: Long, profileId: Long): Flow<List<CategorySpending>> =
        transactionDao.getSpendingByCategory(startOfMonth, endOfMonth, profileId)

    fun getAllTimeIncome(profileId: Long): Flow<Double> =
        transactionDao.getAllTimeIncome(profileId)

    fun getAllTimeExpense(profileId: Long): Flow<Double> =
        transactionDao.getAllTimeExpense(profileId)

    suspend fun getRecurringTransactions(profileId: Long): List<TransactionEntity> =
        transactionDao.getRecurringTransactions(profileId)

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
