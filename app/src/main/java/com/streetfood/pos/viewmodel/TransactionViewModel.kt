package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streetfood.pos.data.database.AppDatabase
import com.streetfood.pos.data.models.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionViewModel(
    private val database: AppDatabase
) : ViewModel() {
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _totalRevenue = MutableStateFlow(0.0)
    val totalRevenue: StateFlow<Double> = _totalRevenue.asStateFlow()

    private val _totalTransactions = MutableStateFlow(0)
    val totalTransactions: StateFlow<Int> = _totalTransactions.asStateFlow()

    init {
        loadTransactions()
        loadStats()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                database.transactionDao().getAllTransactions().collect { transactionList ->
                    _transactions.value = transactionList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val revenue = database.transactionDao().getTotalRevenue() ?: 0.0
                val count = database.transactionDao().getTotalTransactions()
                _totalRevenue.value = revenue
                _totalTransactions.value = count
            } catch (e: Exception) {
                _totalRevenue.value = 0.0
                _totalTransactions.value = 0
            }
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatCurrency(amount: Double): String {
        return "₱%.2f".format(amount)
    }
}
