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
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(firebaseUser.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val dbUser = doc.toObject(User::class.java)?.copy(id = doc.id)
                        if (dbUser != null) {
                            UserSessionRepository.login(dbUser)
                            _currentUser.value = dbUser
                            _userRole.value = dbUser.role.name
                            _isLoggedIn.value = true
                        }
                    } else {
                        fallbackLogin(firebaseUser)
                    }
                }
                .addOnFailureListener {
                    val offlineUser = UserSessionRepository.tryOfflineLogin(firebaseUser.email ?: "", "") // We don't have password here but we try
                    if (offlineUser != null) {
                        UserSessionRepository.login(offlineUser)
                        _currentUser.value = offlineUser
                        _userRole.value = offlineUser.role.name
                        _isLoggedIn.value = true
                    } else {
                        fallbackLogin(firebaseUser)
                    }
                }
        }
    }

    private fun fallbackLogin(firebaseUser: com.google.firebase.auth.FirebaseUser) {
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

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginError.value = "Please enter your email and password."
            return
        }

        _isLoading.value = true
        _loginError.value = null

        val cleanEmail = email.trim().lowercase()
        val finalEmail = if (!cleanEmail.contains("@")) "$cleanEmail@test.com" else cleanEmail

        auth.signInWithEmailAndPassword(finalEmail, password)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("users").document(firebaseUser.uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val dbUser = doc.toObject(User::class.java)?.copy(id = doc.id)
                                if (dbUser != null) {
                                    UserSessionRepository.login(dbUser)
                                    UserSessionRepository.saveOfflineCredentials(finalEmail, password, dbUser.role.name, firebaseUser.uid)
                                    _currentUser.value = dbUser
                                    _userRole.value = dbUser.role.name
                                    _isLoggedIn.value = true
                                }
                            } else {
                                fallbackLogin(firebaseUser)
                                UserSessionRepository.saveOfflineCredentials(finalEmail, password, _userRole.value ?: "CASHIER", firebaseUser.uid)
                            }
                            _isLoading.value = false
                        }
                        .addOnFailureListener {
                            fallbackLogin(firebaseUser)
                            UserSessionRepository.saveOfflineCredentials(finalEmail, password, _userRole.value ?: "CASHIER", firebaseUser.uid)
                            _isLoading.value = false
                        }
                } else {
                    _isLoading.value = false
                }
            }
            .addOnFailureListener { e ->
                val msg = e.message ?: ""
                val isNetworkError = "network" in msg.lowercase() || "timeout" in msg.lowercase() || com.streetfood.pos.util.NetworkMonitor.isOffline.value
                
                if (isNetworkError) {
                    val offlineUser = UserSessionRepository.tryOfflineLogin(finalEmail, password)
                    if (offlineUser != null) {
                        UserSessionRepository.login(offlineUser)
                        _currentUser.value = offlineUser
                        _userRole.value = offlineUser.role.name
                        _isLoggedIn.value = true
                        _isLoading.value = false
                        return@addOnFailureListener
                    }
                }

                _loginError.value = when {
                    "INVALID_LOGIN_CREDENTIALS" in msg || "wrong-password" in msg || "invalid-credential" in msg ->
                        "Incorrect username or password. Please try again."
                    "user-not-found" in msg ->
                        "This account could not be found."
                    isNetworkError ->
                        "No internet connection and no offline account matches."
                    "too-many-requests" in msg ->
                        "Too many failed attempts. Please try again later."
                    "api_key" in msg.lowercase() || "blocked" in msg.lowercase() ->
                        "API key error: the API key was blocked or changed. Download the new google-services.json file."
                    else -> "Error: $msg"
                }
                _isLoading.value = false
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
