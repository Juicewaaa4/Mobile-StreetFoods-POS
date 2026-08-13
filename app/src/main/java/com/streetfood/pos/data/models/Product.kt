package com.streetfood.pos.data.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,       // selling price
    val cost: Double = 0.0,        // capital cost
    val category: String = "Lugaw",
    val stock: Int = 0             // inventory stock
) {
    /** Profit per item = selling price - cost */
    @get:Exclude
    val profitPerItem: Double get() = price - cost
    
    /** Product is available if there is stock */
    @get:Exclude
    val isAvailable: Boolean get() = stock > 0
}
