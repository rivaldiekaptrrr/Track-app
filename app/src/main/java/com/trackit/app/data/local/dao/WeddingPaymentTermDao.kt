package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.WeddingPaymentTermEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeddingPaymentTermDao {
    @Query("SELECT * FROM wedding_payment_terms WHERE expenseId = :expenseId ORDER BY dueDate ASC")
    fun getByExpense(expenseId: String): Flow<List<WeddingPaymentTermEntity>>

    @Query("SELECT * FROM wedding_payment_terms WHERE isPaid = 0 ORDER BY dueDate ASC")
    fun getAllUnpaid(): Flow<List<WeddingPaymentTermEntity>>

    @Query("SELECT * FROM wedding_payment_terms WHERE isPaid = 1 ORDER BY paidDate DESC LIMIT 1")
    fun getLastPaidTerm(): Flow<WeddingPaymentTermEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(term: WeddingPaymentTermEntity)

    @Update
    suspend fun update(term: WeddingPaymentTermEntity)

    @Delete
    suspend fun delete(term: WeddingPaymentTermEntity)

    @Query("UPDATE wedding_payment_terms SET isPaid = 1, paidDate = :now WHERE isPaid = 0")
    suspend fun markAllUnpaidAsPaid(now: Long = System.currentTimeMillis())
}
