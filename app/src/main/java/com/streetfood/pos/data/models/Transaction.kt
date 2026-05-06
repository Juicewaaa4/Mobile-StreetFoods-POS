package com.streetfood.pos.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val cashReceived: Double,
    val change: Double,
    val cashierName: String,
    val items: List<TransactionItem> = emptyList()
)

data class TransactionItem(
    val transactionId: Int = 0,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)
