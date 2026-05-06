package com.streetfood.pos.data.models

enum class UserRole {
    ADMIN,
    CASHIER
}

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val role: UserRole = UserRole.CASHIER
)
