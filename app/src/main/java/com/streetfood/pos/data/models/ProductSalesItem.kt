package com.streetfood.pos.data.models

/** Aggregated sales data for a single product — used in Analytics. */
data class ProductSalesItem(
    val productName: String,
    val qtySold: Int,
    val revenue: Double
)
