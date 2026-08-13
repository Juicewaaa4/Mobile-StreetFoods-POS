package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.models.MonthlyExpense
import com.streetfood.pos.data.repository.MonthlyExpenseRepository
import com.streetfood.pos.data.repository.TransactionRepository
import com.streetfood.pos.data.repository.DailyBookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class MonthlySummaryViewModel(
    private val monthlyRepo: MonthlyExpenseRepository,
    private val dailyBookRepo: DailyBookRepository,
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    private val _selectedMonthYear = MutableStateFlow(
        MonthYear(
            Calendar.getInstance().get(Calendar.MONTH) + 1,
            Calendar.getInstance().get(Calendar.YEAR)
        )
    )
    val selectedMonthYear: StateFlow<MonthYear> = _selectedMonthYear.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyExpense: StateFlow<MonthlyExpense?> = _selectedMonthYear
        .flatMapLatest { my -> monthlyRepo.getForMonth(my.month, my.year) }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** Auto-pull daily book totals for the month */
    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyBookTotals: StateFlow<Pair<Double, Double>> = _selectedMonthYear
        .flatMapLatest { my ->
            dailyBookRepo.getEntriesForMonth(my.month, my.year)
                .map { entries ->
                    val totalExp1 = entries.sumOf { it.expenses1 }
                    val totalExp2 = entries.sumOf { it.expenses2 }
                    Pair(totalExp1, totalExp2)
                }
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Pair(0.0, 0.0)
        )

    /** Auto-pull total POS sales for the month */
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyPOSSales: StateFlow<Double> = _selectedMonthYear
        .flatMapLatest { my ->
            val cal = Calendar.getInstance()
            cal.set(my.year, my.month - 1, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            transactionRepo.getByDateRange(start, end)
                .map { txs -> txs.sumOf { it.totalAmount } }
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    fun setMonthYear(month: Int, year: Int) {
        _selectedMonthYear.value = MonthYear(month, year)
    }

    fun previousMonth() {
        val current = _selectedMonthYear.value
        if (current.month == 1) {
            _selectedMonthYear.value = MonthYear(12, current.year - 1)
        } else {
            _selectedMonthYear.value = MonthYear(current.month - 1, current.year)
        }
    }

    fun nextMonth() {
        val current = _selectedMonthYear.value
        if (current.month == 12) {
            _selectedMonthYear.value = MonthYear(1, current.year + 1)
        } else {
            _selectedMonthYear.value = MonthYear(current.month + 1, current.year)
        }
    }

    fun saveMonthlyExpense(expense: MonthlyExpense, onResult: (Boolean) -> Unit) {
        monthlyRepo.save(expense, onResult)
    }
}

class MonthlySummaryViewModelFactory(private val db: FirebaseFirestore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val monthlyRepo = MonthlyExpenseRepository(db)
        val dailyBookRepo = DailyBookRepository(db)
        val transactionRepo = TransactionRepository(db)
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(MonthlySummaryViewModel::class.java) ->
                MonthlySummaryViewModel(monthlyRepo, dailyBookRepo, transactionRepo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.simpleName}")
        }
    }
}
