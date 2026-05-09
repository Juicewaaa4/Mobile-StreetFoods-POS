package com.streetfood.pos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.models.User
import com.streetfood.pos.data.models.UserRole
import com.streetfood.pos.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val users: List<User>       = emptyList(),
    val isLoading: Boolean      = true,
    val isSaving: Boolean       = false,
    val successMessage: String? = null,
    val errorMessage: String?   = null
)

class UserManagementViewModel(
    app: Application,
    private val userRepo: UserRepository
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val result = userRepo.ensureDefaultUsers()
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Unable to load default accounts.")
                }
            }
        }
        viewModelScope.launch {
            userRepo.getAllUsers()
                .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
                .collect { users -> _uiState.update { it.copy(users = users, isLoading = false) } }
        }
    }

    fun createUser(username: String, password: String, role: UserRole) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Username and password are required.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = userRepo.createUser(username.trim(), password, role)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, successMessage = "User \"${username.trim()}\" created.") }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Unknown error"
                _uiState.update { it.copy(isSaving = false, errorMessage = friendlyAuthError(msg)) }
            }
        }
    }

    fun updateUser(uid: String, newUsername: String, newRole: UserRole) {
        if (newUsername.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Username cannot be empty.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = userRepo.updateUser(uid, newUsername.trim(), newRole)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, successMessage = "User updated successfully.") }
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = userRepo.deleteUser(user.id)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, successMessage = "\"${user.username}\" deleted.") }
            } else {
                _uiState.update { it.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun consumeSuccess() { _uiState.update { it.copy(successMessage = null) } }
    fun consumeError()   { _uiState.update { it.copy(errorMessage = null) } }

    private fun friendlyAuthError(msg: String): String = when {
        "email-already-in-use" in msg || "already in use" in msg.lowercase() ->
            "A user with that username already exists."
        "weak-password" in msg -> "Password must be at least 6 characters."
        "network" in msg.lowercase() || "timeout" in msg.lowercase() ->
            "No internet connection. Check your network."
        else -> msg
    }
}

class UserManagementViewModelFactory(
    private val app: Application,
    private val db: FirebaseFirestore
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        val repo = UserRepository(db, app.applicationContext)
        @Suppress("UNCHECKED_CAST")
        return UserManagementViewModel(app, repo) as T
    }
}
