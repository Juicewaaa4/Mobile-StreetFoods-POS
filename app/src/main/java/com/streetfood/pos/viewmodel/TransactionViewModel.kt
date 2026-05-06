package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streetfood.pos.data.models.*
import com.streetfood.pos.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewModel(
    private val repo: TransactionRepository
) : ViewModel() {

    private val _dateFilter = MutableStateFlow(DateFilter.TODAY)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    /** Transactions for the currently selected date filter. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredTransactions: StateFlow<UiState<List<Transaction>>> = _dateFilter
        .flatMapLatest { filter ->
            val (start, end) = filter.toDateRange()
            repo.getByDateRange(start, end).map { list ->
                if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    /** Today's transactions for a specific cashier — used in Cashier Dashboard. */
    fun getCashierTodayTransactions(cashierName: String): Flow<List<Transaction>> {
        val (start, end) = DateFilter.TODAY.toDateRange()
        return repo.getByCashierToday(cashierName, start, end)
    }

    /** All transactions (for Admin Transaction History). */
    val allTransactions: StateFlow<UiState<List<Transaction>>> =
        repo.getAllTransactions()
            .map { list -> if (list.isEmpty()) UiState.Empty else UiState.Success(list) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setDateFilter(filter: DateFilter) { _dateFilter.value = filter }

    fun formatTimestamp(ts: Long): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ts))

    fun formatDate(ts: Long): String =
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(ts))

    fun formatCurrency(amount: Double): String = "₱%.2f".format(amount)
}
