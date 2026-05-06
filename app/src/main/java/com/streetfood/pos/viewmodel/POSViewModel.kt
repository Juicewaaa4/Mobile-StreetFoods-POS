package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streetfood.pos.data.database.AppDatabase
import com.streetfood.pos.data.models.CartItem
import com.streetfood.pos.data.models.Product
import com.streetfood.pos.data.models.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class POSViewModel(
    private val database: AppDatabase
) : ViewModel() {
    
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    private val _cashReceived = MutableStateFlow(0.0)
    val cashReceived: StateFlow<Double> = _cashReceived.asStateFlow()

    private val _change = MutableStateFlow(0.0)
    val change: StateFlow<Double> = _change.asStateFlow()

    private val _isProcessingPayment = MutableStateFlow(false)
    val isProcessingPayment: StateFlow<Boolean> = _isProcessingPayment.asStateFlow()

    init {
        loadProducts()
        initializeDefaultProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            database.productDao().getAvailableProducts().collect { productList ->
                _products.value = productList
            }
        }
    }

    private fun initializeDefaultProducts() {
        viewModelScope.launch {
            val existingProducts = database.productDao().getAvailableProducts()
            existingProducts.collect { products ->
                if (products.isEmpty()) {
                    // Add default street food products
                    val defaultProducts = listOf(
                        Product(name = "Fishball", price = 30.0, category = "Street Food"),
                        Product(name = "Kwek-Kwek", price = 40.0, category = "Street Food"),
                        Product(name = "Squidball", price = 35.0, category = "Street Food"),
                        Product(name = "Chicken Balls", price = 45.0, category = "Street Food"),
                        Product(name = "Hotdog on Stick", price = 50.0, category = "Street Food"),
                        Product(name = "Banana Cue", price = 25.0, category = "Street Food"),
                        Product(name = "Camote Cue", price = 25.0, category = "Street Food"),
                        Product(name = "Saging na Saba", price = 20.0, category = "Street Food")
                    )
                    
                    defaultProducts.forEach { product ->
                        database.productDao().insertProduct(product)
                    }
                }
            }
        }
    }

    fun addToCart(product: Product) {
        val currentCart = _cart.value.toMutableList()
        val existingItem = currentCart.find { it.product.id == product.id }
        
        if (existingItem != null) {
            val updatedCart = currentCart.map { item ->
                if (item.product.id == product.id) {
                    item.copy(quantity = item.quantity + 1)
                } else {
                    item
                }
            }
            _cart.value = updatedCart
        } else {
            _cart.value = currentCart + CartItem(product, 1)
        }
        
        calculateTotal()
    }

    fun updateQuantity(cartItem: CartItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(cartItem)
        } else {
            val updatedCart = _cart.value.map { item ->
                if (item.product.id == cartItem.product.id) {
                    item.copy(quantity = newQuantity)
                } else {
                    item
                }
            }
            _cart.value = updatedCart
            calculateTotal()
        }
    }

    fun removeFromCart(cartItem: CartItem) {
        _cart.value = _cart.value.filter { it.product.id != cartItem.product.id }
        calculateTotal()
    }

    fun clearCart() {
        _cart.value = emptyList()
        _totalAmount.value = 0.0
        _cashReceived.value = 0.0
        _change.value = 0.0
    }

    private fun calculateTotal() {
        _totalAmount.value = _cart.value.sumOf { it.totalPrice }
        calculateChange()
    }

    fun updateCashReceived(amount: String) {
        val cash = amount.toDoubleOrNull() ?: 0.0
        _cashReceived.value = cash
        calculateChange()
    }

    private fun calculateChange() {
        _change.value = (_cashReceived.value - _totalAmount.value).coerceAtLeast(0.0)
    }

    fun processTransaction(cashierName: String = "Cashier"): Boolean {
        if (_cart.value.isEmpty() || _cashReceived.value < _totalAmount.value) {
            return false
        }

        viewModelScope.launch {
            _isProcessingPayment.value = true
            
            try {
                val transaction = Transaction(
                    totalAmount = _totalAmount.value,
                    cashReceived = _cashReceived.value,
                    change = _change.value,
                    cashierName = cashierName,
                    items = _cart.value.map { 
                        com.streetfood.pos.data.models.TransactionItem(
                            productName = it.product.name,
                            quantity = it.quantity,
                            unitPrice = it.product.price,
                            totalPrice = it.totalPrice
                        )
                    }
                )
                
                database.transactionDao().insertTransaction(transaction)
                clearCart()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isProcessingPayment.value = false
            }
        }
        
        return true
    }
}
