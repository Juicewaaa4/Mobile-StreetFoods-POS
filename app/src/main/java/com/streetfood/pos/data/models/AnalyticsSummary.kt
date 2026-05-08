package com.streetfood.pos.data.models

/** Full analytics data for the selected date range. */
data class AnalyticsSummary(
    val totalRevenue: Double = 0.0,
    val totalCost: Double = 0.0,        // total capital cost
    val totalProfit: Double = 0.0,      // net kita
    val transactionCount: Int = 0,
    val avgTransactionValue: Double = 0.0,
    val bestSellerName: String = "N/A",
    val activeProductCount: Int = 0,
    /** Revenue per day: list of (dateLabel, revenue) pairs sorted by date. */
    val dailyRevenue: List<Pair<String, Double>> = emptyList(),
    /** Top 5 products by qty sold in the selected period. */
    val topProducts: List<ProductSalesItem> = emptyList()
)
