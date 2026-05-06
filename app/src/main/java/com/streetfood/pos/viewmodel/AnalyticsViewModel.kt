package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streetfood.pos.data.models.*
import com.streetfood.pos.data.repository.ProductRepository
import com.streetfood.pos.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsViewModel(
    private val transactionRepo: TransactionRepository,
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _dateFilter = MutableStateFlow(DateFilter.TODAY)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    val analyticsData: StateFlow<UiState<AnalyticsSummary>> = combine(
        _dateFilter.flatMapLatest { filter ->
            val (start, end) = filter.toDateRange()
            transactionRepo.getByDateRange(start, end)
        },
        productRepo.getAllProducts()
    ) { transactions, products ->
        // Build a map of productName -> cost for profit calculation
        val costMap = products.associate { it.name to it.cost }

        if (transactions.isEmpty()) {
            UiState.Success(AnalyticsSummary(activeProductCount = products.count { it.isAvailable }))
        } else {
            val totalRevenue = transactions.sumOf { it.totalAmount }
            val count = transactions.size
            val avg = totalRevenue / count

            // Aggregate all items across transactions for top products & cost
            val allItems = transactions.flatMap { it.items }
            val productAgg = allItems
                .groupBy { it.productName }
                .map { (name, items) ->
                    ProductSalesItem(name, items.sumOf { it.quantity }, items.sumOf { it.totalPrice })
                }
                .sortedByDescending { it.qtySold }

            // Total cost = sum of (quantity * costPerItem) across all sold items
            val totalCost = allItems.sumOf { item ->
                val cost = costMap[item.productName] ?: 0.0
                item.quantity * cost
            }
            val totalProfit = totalRevenue - totalCost

            val bestSeller = productAgg.firstOrNull()?.productName ?: "N/A"

            // Build daily revenue map
            val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
            val dailyMap = transactions
                .groupBy { sdf.format(Date(it.timestamp)) }
                .map { (date, txns) -> Pair(date, txns.sumOf { it.totalAmount }) }
                .sortedBy { it.first }

            UiState.Success(
                AnalyticsSummary(
                    totalRevenue = totalRevenue,
                    totalCost = totalCost,
                    totalProfit = totalProfit,
                    transactionCount = count,
                    avgTransactionValue = avg,
                    bestSellerName = bestSeller,
                    activeProductCount = products.count { it.isAvailable },
                    dailyRevenue = dailyMap,
                    topProducts = productAgg.take(5)
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setFilter(filter: DateFilter) { _dateFilter.value = filter }
}
