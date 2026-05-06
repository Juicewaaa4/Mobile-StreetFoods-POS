package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.streetfood.pos.data.models.User
import com.streetfood.pos.data.models.UserRole
import com.streetfood.pos.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    init {
        // Check if user is already logged in
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            // For simplicity, we define role by email domain or specific email.
            // In a real app, this should come from Firestore.
            val role = if (firebaseUser.email?.contains("admin") == true) UserRole.ADMIN else UserRole.CASHIER
            val user = User(
                id = firebaseUser.uid,
                username = firebaseUser.email?.substringBefore("@") ?: "User",
                email = firebaseUser.email ?: "",
                role = role
            )
            UserSessionRepository.login(user)
            _currentUser.value = user
            _userRole.value = user.role.name
            _isLoggedIn.value = true
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginError.value = "Please enter your email and password."
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            try {
                // Ensure the input looks like an email. If the user types "admin", format it as "admin@test.com"
                val cleanEmail = email.trim().lowercase()
                val finalEmail = if (!cleanEmail.contains("@")) "$cleanEmail@test.com" else cleanEmail

                val result = auth.signInWithEmailAndPassword(finalEmail, password).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    val role = if (firebaseUser.email?.contains("admin") == true) UserRole.ADMIN else UserRole.CASHIER
                    val user = User(
                        id = firebaseUser.uid,
                        username = firebaseUser.email?.substringBefore("@") ?: "User",
                        email = firebaseUser.email ?: "",
                        role = role
                    )
                    UserSessionRepository.login(user)
                    _currentUser.value = user
                    _userRole.value = user.role.name
                    _isLoggedIn.value = true
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                _loginError.value = when {
                    "INVALID_LOGIN_CREDENTIALS" in msg || "wrong-password" in msg || "invalid-credential" in msg ->
                        "Mali ang username o password. Subukan ulit."
                    "user-not-found" in msg ->
                        "Hindi mahanap ang account na ito."
                    "network" in msg.lowercase() || "timeout" in msg.lowercase() ->
                        "Walang internet connection. I-check ang iyong WiFi o Data."
                    "too-many-requests" in msg ->
                        "Maraming beses na nagkamali. Subukan ulit mamaya."
                    else -> "Login failed. Suriin ang iyong credentials."
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            UserSessionRepository.logout()
            _isLoggedIn.value = false
            _currentUser.value = null
            _userRole.value = null
            _loginError.value = null
        }
    }

    fun clearError() { _loginError.value = null }
}
