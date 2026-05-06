package com.streetfood.pos.data.models

data class CartItem(
    val product: Product = Product(),
    val quantity: Int = 1
) {
    val totalPrice: Double get() = product.price * quantity
}
