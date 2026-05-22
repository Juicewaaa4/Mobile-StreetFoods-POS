package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.models.ActivityLog
import com.streetfood.pos.data.repository.ActivityLogRepository
import com.streetfood.pos.data.models.DateFilter
import com.streetfood.pos.data.models.toDateRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed class ActivityLogState {
    object Loading : ActivityLogState()
    data class Success(val logs: List<ActivityLog>) : ActivityLogState()
    data class Error(val message: String) : ActivityLogState()
}

class ActivityLogViewModel(private val repo: ActivityLogRepository) : ViewModel() {

    private val _dateFilter = MutableStateFlow(DateFilter.TODAY)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    private val _roleFilter = MutableStateFlow("All")
    val roleFilter: StateFlow<String> = _roleFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val logsState: StateFlow<ActivityLogState> = kotlinx.coroutines.flow.combine(
        _dateFilter.flatMapLatest { filter ->
            if (filter == DateFilter.ALL_TIME) {
                repo.getLogs()
            } else {
                val (start, end) = filter.toDateRange()
                repo.getLogsByDateRange(start, end)
            }
        },
        _roleFilter
    ) { logs, role ->
        val filtered = if (role == "All") logs else logs.filter { 
            if (role == "Admin") it.userRole == "Admin" || it.userRole == "SuperAdmin"
            else it.userRole == role 
        }
        ActivityLogState.Success(filtered) as ActivityLogState
    }
    .catch { e -> emit(ActivityLogState.Error(e.message ?: "Unknown error")) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActivityLogState.Loading
    )

    fun setDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun setRoleFilter(role: String) {
        _roleFilter.value = role
    }

    fun purgeOldData(timestamp: Long, onComplete: (Boolean) -> Unit) {
        repo.deleteOlderThan(timestamp, onComplete)
    }
}

class ActivityLogViewModelFactory(private val db: FirebaseFirestore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = ActivityLogRepository(db)
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(ActivityLogViewModel::class.java) -> ActivityLogViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.simpleName}")
        }
    }
}
