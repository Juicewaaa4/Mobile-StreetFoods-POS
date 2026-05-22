package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streetfood.pos.data.models.DateFilter
import com.streetfood.pos.data.models.Transaction
import com.streetfood.pos.data.models.UiState
import com.streetfood.pos.data.models.toDateRange
import com.streetfood.pos.data.repository.TransactionRepository
import com.streetfood.pos.util.mapFirestoreOrNetworkError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TransactionViewModel(
    private val repo: TransactionRepository
) : ViewModel() {

    private val _dateFilter = MutableStateFlow(DateFilter.TODAY)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    private val _cashierDashboardError = MutableStateFlow<String?>(null)
    val cashierDashboardError: StateFlow<String?> = _cashierDashboardError.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredTransactions: StateFlow<UiState<List<Transaction>>> = _dateFilter
        .flatMapLatest { filter ->
            val (start, end) = filter.toDateRange()
            repo.getByDateRange(start, end).map { list ->
                if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun getCashierTodayTransactions(cashierName: String): Flow<List<Transaction>> {
        val (start, end) = DateFilter.TODAY.toDateRange()
        return repo.getByDateRange(start, end)
            .map { txs -> txs.filter { it.cashierName.equals(cashierName, ignoreCase = true) } }
            .catch { e ->
                _cashierDashboardError.value = mapFirestoreOrNetworkError(e)
                emit(emptyList())
            }
    }

    val allTransactions: StateFlow<UiState<List<Transaction>>> =
        repo.getAllTransactions()
            .map { list -> if (list.isEmpty()) UiState.Empty else UiState.Success(list) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun formatTimestamp(ts: Long): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ts))

    fun formatDate(ts: Long): String =
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(ts))

    fun formatCurrency(amount: Double): String = "₱%.2f".format(amount)

    fun consumeCashierDashboardError() {
        _cashierDashboardError.value = null
    }

    fun purgeOldData(timestamp: Long, onComplete: (Boolean) -> Unit) {
        repo.deleteOlderThan(timestamp, onComplete)
    }
}
