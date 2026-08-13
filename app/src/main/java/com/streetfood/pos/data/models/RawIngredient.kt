package com.streetfood.pos.data.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class RawIngredient(
    val id: String = "",
    val name: String = "",
    val unit: String = "pcs",
    val stock: Int = 0,
    val criticalLevel: Int = 10,
    val lastUpdated: Long = System.currentTimeMillis()
)
