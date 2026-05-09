package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.models.DateFilter
import com.streetfood.pos.data.models.toDateRange
import com.streetfood.pos.data.repository.ProductRepository
import com.streetfood.pos.data.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReportRow(
    val productName: String,
    val qtySold: Int,
    val grossSales: Double,
    val costOfSales: Double,
    val netAmount: Double,
    val percentage: Double
)

data class ReportTotals(
    val totalQty: Int,
    val totalGross: Double,
    val totalCost: Double,
    val totalNet: Double,
    val avgPercentage: Double
)

data class ReportState(
    val rows: List<ReportRow> = emptyList(),
    val totals: ReportTotals = ReportTotals(0, 0.0, 0.0, 0.0, 0.0),
    val reportDate: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class ReportViewModel(
    private val transactionRepo: TransactionRepository,
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _dateFilter = MutableStateFlow(DateFilter.TODAY)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    private val _customDateMillis = MutableStateFlow<Long?>(null)
    val customDateMillis: StateFlow<Long?> = _customDateMillis.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportState: StateFlow<ReportState> = combine(
        combine(_dateFilter, _customDateMillis) { filter, customDate ->
            val (start, end) = customDate?.let { dayRange(it) } ?: filter.toDateRange()
            ReportRequest(start, end, dateLabel(filter, customDate))
        }.flatMapLatest { request ->
            transactionRepo.getByDateRange(request.start, request.end)
                .map { transactions -> request to transactions }
        },
        productRepo.getAllProducts()
    ) { (request, transactions), products ->
        val costMap = products.associate { it.name to it.cost }
        val dateLabel = request.label

        if (transactions.isEmpty()) {
            ReportState(isLoading = false, reportDate = dateLabel)
        } else {
            val rows = transactions
                .flatMap { it.items }
                .groupBy { it.productName }
                .map { (name, items) ->
                    val qty = items.sumOf { it.quantity }
                    val gross = items.sumOf { it.totalPrice }
                    val cost = (costMap[name] ?: 0.0) * qty
                    val net = gross - cost
                    val pct = if (gross > 0.0) (net / gross) * 100.0 else 0.0
                    ReportRow(name, qty, gross, cost, net, pct)
                }
                .sortedBy { it.productName }

            val totalQty = rows.sumOf { it.qtySold }
            val totalGross = rows.sumOf { it.grossSales }
            val totalCost = rows.sumOf { it.costOfSales }
            val totalNet = rows.sumOf { it.netAmount }
            val avgPct = if (totalGross > 0.0) (totalNet / totalGross) * 100.0 else 0.0

            ReportState(
                rows = rows,
                totals = ReportTotals(totalQty, totalGross, totalCost, totalNet, avgPct),
                reportDate = dateLabel,
                isLoading = false
            )
        }
    }.catch { e ->
        emit(ReportState(isLoading = false, error = e.message ?: "Unknown error"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportState(isLoading = true)
    )

    fun setFilter(filter: DateFilter) {
        _customDateMillis.value = null
        _dateFilter.value = filter
    }

    fun setCustomDate(millis: Long) {
        _customDateMillis.value = millis
    }

    private fun dayRange(millis: Long): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val end = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return start to end
    }

    private fun dateLabel(filter: DateFilter, customDate: Long?): String {
        val sdf = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
        customDate?.let { return sdf.format(Date(it)) }
        return when (filter) {
            DateFilter.TODAY -> sdf.format(Date())
            DateFilter.YESTERDAY -> {
                val cal = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, -1) }
                sdf.format(cal.time)
            }
            DateFilter.THIS_WEEK -> "This Week"
            DateFilter.THIS_MONTH -> "This Month"
            DateFilter.ALL_TIME -> "All Time"
        }
    }
}

private data class ReportRequest(val start: Long, val end: Long, val label: String)

class ReportViewModelFactory(private val db: FirebaseFirestore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ReportViewModel(
            TransactionRepository(db),
            ProductRepository(db)
        ) as T
    }
}
