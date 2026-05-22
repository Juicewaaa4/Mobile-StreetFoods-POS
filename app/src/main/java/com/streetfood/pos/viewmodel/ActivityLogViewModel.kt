package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.models.ActivityLog
import com.streetfood.pos.data.repository.ActivityLogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed class ActivityLogState {
    object Loading : ActivityLogState()
    data class Success(val logs: List<ActivityLog>) : ActivityLogState()
    data class Error(val message: String) : ActivityLogState()
}

class ActivityLogViewModel(private val repo: ActivityLogRepository) : ViewModel() {

    val logsState: StateFlow<ActivityLogState> = repo.getLogs()
        .map { logs -> ActivityLogState.Success(logs) as ActivityLogState }
        .catch { e -> emit(ActivityLogState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ActivityLogState.Loading
        )
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
