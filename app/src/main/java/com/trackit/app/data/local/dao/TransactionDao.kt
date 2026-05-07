package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("UPDATE transactions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun updateTransactionsCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countTransactionsByCategory(categoryId: Long): Int

    @Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY date DESC")
    fun getAllTransactions(profileId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :startOfMonth AND date < :endOfMonth AND profileId = :profileId
        ORDER BY date DESC
    """)
    fun getTransactionsByMonth(startOfMonth: Long, endOfMonth: Long, profileId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE date >= :startOfMonth AND date < :endOfMonth AND type = 'EXPENSE' AND profileId = :profileId
    """)
    fun getTotalSpentInMonth(startOfMonth: Long, endOfMonth: Long, profileId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE date >= :startOfMonth AND date < :endOfMonth AND type = 'INCOME' AND profileId = :profileId
    """)
    fun getTotalIncomeInMonth(startOfMonth: Long, endOfMonth: Long, profileId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE date >= :startOfMonth AND date < :endOfMonth AND type = 'EXPENSE' AND profileId = :profileId
    """)
    suspend fun getTotalSpentInMonthSync(startOfMonth: Long, endOfMonth: Long, profileId: Long): Double

    @Query("""
        SELECT categoryId, COALESCE(SUM(amount), 0.0) as total 
        FROM transactions 
        WHERE date >= :startOfMonth AND date < :endOfMonth AND profileId = :profileId
        GROUP BY categoryId
    """)
    fun getSpendingByCategory(startOfMonth: Long, endOfMonth: Long, profileId: Long): Flow<List<CategorySpending>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'INCOME' AND profileId = :profileId
    """)
    fun getAllTimeIncome(profileId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'EXPENSE' AND profileId = :profileId
    """)
    fun getAllTimeExpense(profileId: Long): Flow<Double>

    @Query("""
        SELECT * FROM transactions WHERE isRecurring = 1 AND profileId = :profileId
    """)
    suspend fun getRecurringTransactions(profileId: Long): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE profileId = :profileId
        ORDER BY date DESC 
        LIMIT :limit
    """)
    fun getRecentTransactions(profileId: Long, limit: Int = 10): Flow<List<TransactionEntity>>
}

data class CategorySpending(
    val categoryId: Long?,
    val total: Double
)
