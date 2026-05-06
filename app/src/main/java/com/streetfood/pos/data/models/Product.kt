package com.streetfood.pos.data.models

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,       // selling price
    val cost: Double = 0.0,        // puhunan / capital cost
    val category: String = "Street Food",
    val isAvailable: Boolean = true
) {
    /** Profit per item = selling price - cost */
    val profitPerItem: Double get() = price - cost
}
