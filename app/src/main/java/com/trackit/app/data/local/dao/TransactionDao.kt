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

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :startOfMonth AND date < :endOfMonth 
        ORDER BY date DESC
    """)
    fun getTransactionsByMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE date >= :startOfMonth AND date < :endOfMonth
    """)
    fun getTotalSpentInMonth(startOfMonth: Long, endOfMonth: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE date >= :startOfMonth AND date < :endOfMonth
    """)
    suspend fun getTotalSpentInMonthSync(startOfMonth: Long, endOfMonth: Long): Double

    @Query("""
        SELECT categoryId, COALESCE(SUM(amount), 0.0) as total 
        FROM transactions 
        WHERE date >= :startOfMonth AND date < :endOfMonth 
        GROUP BY categoryId
    """)
    fun getSpendingByCategory(startOfMonth: Long, endOfMonth: Long): Flow<List<CategorySpending>>

    @Query("SELECT * FROM transactions WHERE isRecurring = 1")
    suspend fun getRecurringTransactions(): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        ORDER BY date DESC 
        LIMIT :limit
    """)
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>>
}

data class CategorySpending(
    val categoryId: Long?,
    val total: Double
)
