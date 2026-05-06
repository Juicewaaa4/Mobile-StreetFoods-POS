package com.streetfood.pos.data.dao

import androidx.room.*
import com.streetfood.pos.data.models.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Query("SELECT SUM(totalAmount) FROM transactions")
    suspend fun getTotalRevenue(): Double?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTotalTransactions(): Int
}
