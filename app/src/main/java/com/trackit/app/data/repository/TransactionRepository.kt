package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.CategorySpending
import com.trackit.app.data.local.dao.TransactionDao
import com.trackit.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.trackit.app.util.SyncManager

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val syncManager: SyncManager
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

    suspend fun getTotalSpentByCategoryInMonthSync(categoryId: String, startOfMonth: Long, endOfMonth: Long, profileId: Long): Double =
        transactionDao.getTotalSpentByCategoryInMonthSync(categoryId, startOfMonth, endOfMonth, profileId)

    fun getSpendingByCategory(startOfMonth: Long, endOfMonth: Long, profileId: Long): Flow<List<CategorySpending>> =
        transactionDao.getSpendingByCategory(startOfMonth, endOfMonth, profileId)

    fun getSpendingByCategoryAndType(startOfMonth: Long, endOfMonth: Long, type: String, profileId: Long): Flow<List<CategorySpending>> =
        transactionDao.getSpendingByCategoryAndType(startOfMonth, endOfMonth, type, profileId)

    suspend fun getTransactionsByDateRange(startDate: Long, endDate: Long, profileId: Long): List<TransactionEntity> =
        transactionDao.getTransactionsByDateRange(startDate, endDate, profileId)

    fun getAllTimeIncome(profileId: Long): Flow<Double> =
        transactionDao.getAllTimeIncome(profileId)

    fun getAllTimeExpense(profileId: Long): Flow<Double> =
        transactionDao.getAllTimeExpense(profileId)

    suspend fun getRecurringTransactions(profileId: Long): List<TransactionEntity> =
        transactionDao.getRecurringTransactions(profileId)

    suspend fun insert(transaction: TransactionEntity): String {
        transactionDao.insert(transaction)
        syncManager.pushTransaction(transaction)
        return transaction.id
    }

    suspend fun update(transaction: TransactionEntity) {
        transactionDao.update(transaction)
        syncManager.pushTransaction(transaction)
    }

    suspend fun delete(transaction: TransactionEntity) {
        transactionDao.delete(transaction)
        syncManager.deleteTransaction(transaction)
    }

    suspend fun deleteById(id: String) {
        val transaction = transactionDao.getById(id)
        transactionDao.deleteById(id)
        if (transaction != null) {
            syncManager.deleteTransaction(transaction)
        }
    }

    suspend fun getById(id: String): TransactionEntity? =
        transactionDao.getById(id)

    suspend fun updateTransactionsCategory(oldCategoryId: String, newCategoryId: String) =
        transactionDao.updateTransactionsCategory(oldCategoryId, newCategoryId)

    suspend fun countTransactionsByCategory(categoryId: String): Int =
        transactionDao.countTransactionsByCategory(categoryId)

    suspend fun countExpensesForDaySync(startOfDay: Long, endOfDay: Long, profileId: Long): Int =
        transactionDao.countExpensesForDaySync(startOfDay, endOfDay, profileId)
}
