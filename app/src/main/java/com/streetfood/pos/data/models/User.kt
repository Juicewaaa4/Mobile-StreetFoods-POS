package com.streetfood.pos.data.models

enum class UserRole {
    ADMIN,
    CASHIER
}

data class User(
    val id: Int = 0,
    val username: String,
    val password: String,
    val role: UserRole
)
