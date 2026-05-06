package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streetfood.pos.data.models.User
import com.streetfood.pos.data.models.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    // Default users for demo purposes
    private val defaultUsers = listOf(
        User(1, "admin", "admin123", UserRole.ADMIN),
        User(2, "cashier", "cashier123", UserRole.CASHIER)
    )

    init {
        // Check if user is already logged in (in a real app, this would check persistent storage)
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        // For demo, start with logged out state
        _isLoggedIn.value = false
        _currentUser.value = null
        _userRole.value = null
    }

    fun login(username: String, password: String): Boolean {
        val user = defaultUsers.find { 
            it.username == username && it.password == password 
        }
        
        return if (user != null) {
            viewModelScope.launch {
                _isLoggedIn.value = true
                _currentUser.value = user
                _userRole.value = user.role.name
            }
            true
        } else {
            false
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoggedIn.value = false
            _currentUser.value = null
            _userRole.value = null
        }
    }
}
