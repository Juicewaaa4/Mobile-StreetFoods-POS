package com.streetfood.pos.data.models

data class Transaction(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double = 0.0,
    val cashReceived: Double = 0.0,
    val change: Double = 0.0,
    val cashierName: String = "",
    val items: List<TransactionItem> = emptyList()
)

data class TransactionItem(
    val productName: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)
