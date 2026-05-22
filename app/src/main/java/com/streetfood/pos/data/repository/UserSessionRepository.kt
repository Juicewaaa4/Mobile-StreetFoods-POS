package com.streetfood.pos.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.streetfood.pos.data.models.User
import com.streetfood.pos.data.models.UserRole

/**
 * In-memory session store. Holds the currently logged-in user.
 * Cleared on logout. Not persisted between app restarts (by design — security).
 */
object UserSessionRepository {
    private var _currentUser: User? = null
    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("offline_auth", Context.MODE_PRIVATE)
    }

    val currentUser: User? get() = _currentUser
    val currentRole: UserRole? get() = _currentUser?.role
    val isAdmin: Boolean get() = currentRole == UserRole.ADMIN
    val isCashier: Boolean get() = currentRole == UserRole.CASHIER
    val username: String get() = _currentUser?.username ?: "User"

    fun login(user: User) { _currentUser = user }
    fun logout() { _currentUser = null }
    fun isLoggedIn(): Boolean = _currentUser != null

    fun saveOfflineCredentials(email: String, pass: String, role: String, uid: String) {
        prefs?.edit()?.apply {
            putString("offline_email", email)
            putString("offline_pass", pass)
            putString("offline_role", role)
            putString("offline_uid", uid)
            apply()
        }
    }

    fun tryOfflineLogin(email: String, pass: String): User? {
        val savedEmail = prefs?.getString("offline_email", null)
        val savedPass = prefs?.getString("offline_pass", null)
        if (savedEmail == email && savedPass == pass) {
            val roleStr = prefs?.getString("offline_role", UserRole.CASHIER.name) ?: UserRole.CASHIER.name
            val uid = prefs?.getString("offline_uid", "offline_user") ?: "offline_user"
            return User(
                id = uid,
                username = email.substringBefore("@"),
                email = email,
                role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.CASHIER }
            )
        }
        return null
    }
}
