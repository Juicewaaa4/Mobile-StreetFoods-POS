package com.streetfood.pos.data.models

data class CartItem(
    val product: Product,
    val quantity: Int,
    val totalPrice: Double = product.price * quantity
)
