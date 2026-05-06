package com.streetfood.pos.data.repository

import com.streetfood.pos.data.models.User
import com.streetfood.pos.data.models.UserRole

/**
 * In-memory session store. Holds the currently logged-in user.
 * Cleared on logout. Not persisted between app restarts (by design — security).
 */
object UserSessionRepository {
    private var _currentUser: User? = null

    val currentUser: User? get() = _currentUser
    val currentRole: UserRole? get() = _currentUser?.role
    val isAdmin: Boolean get() = currentRole == UserRole.ADMIN
    val isCashier: Boolean get() = currentRole == UserRole.CASHIER
    val username: String get() = _currentUser?.username ?: "User"

    fun login(user: User) { _currentUser = user }
    fun logout() { _currentUser = null }
    fun isLoggedIn(): Boolean = _currentUser != null
}
