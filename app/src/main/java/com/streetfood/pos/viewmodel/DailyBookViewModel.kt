package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.models.DailyBookEntry
import com.streetfood.pos.data.repository.DailyBookRepository
import com.streetfood.pos.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class MonthYear(val month: Int, val year: Int)

class DailyBookViewModel(
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
    val entries: StateFlow<List<DailyBookEntry>> = _selectedMonthYear
        .flatMapLatest { my ->
            dailyBookRepo.getEntriesForMonth(my.month, my.year)
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

    fun saveEntry(entry: DailyBookEntry, onResult: (Boolean) -> Unit) {
        dailyBookRepo.saveEntry(entry, onResult)
    }

    /** Get total POS sales for the entire selected month */
    fun getMonthSalesFromPOS(month: Int, year: Int): Flow<Double> {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfMonth = cal.timeInMillis

        return transactionRepo.getByDateRange(startOfMonth, endOfMonth)
            .map { transactions -> transactions.sumOf { it.totalAmount } }
    }

    suspend fun getShiftSales(day: Int, month: Int, year: Int, isDayShift: Boolean): Double {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        
        val start: Long
        val end: Long
        
        if (isDayShift) {
            // Day shift: 3:00 AM to 2:59:59 PM
            cal.set(Calendar.HOUR_OF_DAY, 3)
            start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 14)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            end = cal.timeInMillis
        } else {
            // Night shift: 3:00 PM to 2:59:59 AM (next day)
            cal.set(Calendar.HOUR_OF_DAY, 15)
            start = cal.timeInMillis
            
            cal.add(Calendar.DAY_OF_MONTH, 1) // Move to next day
            cal.set(Calendar.HOUR_OF_DAY, 2)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            end = cal.timeInMillis
        }
        
        return try {
            val transactions = transactionRepo.getByDateRange(start, end).first()
            transactions.sumOf { it.totalAmount }
        } catch (e: Exception) {
            0.0
        }
    }
}

class DailyBookViewModelFactory(private val db: FirebaseFirestore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val dailyBookRepo = DailyBookRepository(db)
        val transactionRepo = TransactionRepository(db)
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(DailyBookViewModel::class.java) ->
                DailyBookViewModel(dailyBookRepo, transactionRepo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.simpleName}")
        }
    }
}
