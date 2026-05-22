package com.streetfood.pos.data.models

data class ActivityLog(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userName: String = "",
    val userRole: String = "",
    val actionType: String = "", // "Restock", "Sale", "Update"
    val details: String = ""
)
