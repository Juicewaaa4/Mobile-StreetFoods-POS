package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streetfood.pos.data.models.*
import com.streetfood.pos.data.repository.ProductRepository
import com.streetfood.pos.data.repository.TransactionRepository
import com.streetfood.pos.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ProductFilterMode { ALL, AVAILABLE, UNAVAILABLE }

class POSViewModel(
    private val productRepo: ProductRepository,
    private val transactionRepo: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterMode = MutableStateFlow(ProductFilterMode.ALL)
    val filterMode: StateFlow<ProductFilterMode> = _filterMode.asStateFlow()

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())

    /** Products filtered by search query and availability filter. */
    val filteredProducts: StateFlow<List<Product>> = combine(_allProducts, _searchQuery, _filterMode) { products, query, mode ->
        products.filter { p ->
            val matchesSearch = query.isBlank() || p.name.contains(query, ignoreCase = true)
            val matchesFilter = when (mode) {
                ProductFilterMode.ALL -> true
                ProductFilterMode.AVAILABLE -> p.isAvailable
                ProductFilterMode.UNAVAILABLE -> !p.isAvailable
            }
            matchesSearch && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    /** Set to true after a successful transaction, consumed by UI to show success dialog. */
    private val _transactionSuccess = MutableStateFlow(false)
    val transactionSuccess: StateFlow<Boolean> = _transactionSuccess.asStateFlow()

    private val _isProcessingPayment = MutableStateFlow(false)
    val isProcessingPayment: StateFlow<Boolean> = _isProcessingPayment.asStateFlow()

    /** How many of a given product are in the cart — used for badge display. */
    fun cartQtyFor(productId: String): Int = _cart.value.find { it.product.id == productId }?.quantity ?: 0

    init {
        viewModelScope.launch {
            productRepo.getAllProducts().collect { products ->
                _allProducts.value = products
                if (products.isEmpty()) initializeDefaultProducts()
            }
        }
    }

    private fun initializeDefaultProducts() {
        viewModelScope.launch {
            listOf(
                Product(name = "Fishball",        price = 30.0,  cost = 15.0),
                Product(name = "Kwek-Kwek",       price = 40.0,  cost = 20.0),
                Product(name = "Squidball",       price = 35.0,  cost = 18.0),
                Product(name = "Chicken Balls",   price = 45.0,  cost = 22.0),
                Product(name = "Hotdog on Stick", price = 50.0,  cost = 25.0),
                Product(name = "Banana Cue",      price = 25.0,  cost = 10.0),
                Product(name = "Camote Cue",      price = 25.0,  cost = 10.0),
                Product(name = "Saging na Saba",  price = 20.0,  cost = 8.0)
            ).forEach { productRepo.insertProduct(it) }
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setFilterMode(m: ProductFilterMode) { _filterMode.value = m }

    fun addToCart(product: Product) {
        if (!product.isAvailable) return
        val current = _cart.value.toMutableList()
        val idx = current.indexOfFirst { it.product.id == product.id }
        if (idx >= 0) {
            current[idx] = current[idx].let { it.copy(quantity = it.quantity + 1) }
        } else {
            current.add(CartItem(product, 1))
        }
        _cart.value = current
        recalcTotal()
    }

    fun updateQuantity(cartItem: CartItem, qty: Int) {
        if (qty <= 0) removeFromCart(cartItem) else {
            _cart.value = _cart.value.map { if (it.product.id == cartItem.product.id) it.copy(quantity = qty) else it }
            recalcTotal()
        }
    }

    fun removeFromCart(cartItem: CartItem) {
        _cart.value = _cart.value.filter { it.product.id != cartItem.product.id }
        recalcTotal()
    }

    fun clearCart() {
        _cart.value = emptyList()
        _totalAmount.value = 0.0
    }

    private fun recalcTotal() {
        _totalAmount.value = _cart.value.sumOf { it.totalPrice }
    }

    fun processTransaction(cashDigits: String): Boolean {
        val cash = cashDigits.toDoubleOrNull()?.div(100.0) ?: 0.0
        if (_cart.value.isEmpty() || cash < _totalAmount.value) return false

        viewModelScope.launch {
            _isProcessingPayment.value = true
            try {
                val cashierName = UserSessionRepository.username
                val transaction = Transaction(
                    totalAmount = _totalAmount.value,
                    cashReceived = cash,
                    change = cash - _totalAmount.value,
                    cashierName = cashierName,
                    items = _cart.value.map {
                        TransactionItem(productName = it.product.name, quantity = it.quantity, unitPrice = it.product.price, totalPrice = it.totalPrice)
                    }
                )
                transactionRepo.insertTransaction(transaction)
                clearCart()
                _transactionSuccess.value = true
            } catch (_: Exception) {
            } finally {
                _isProcessingPayment.value = false
            }
        }
        return true
    }

    fun consumeTransactionSuccess() { _transactionSuccess.value = false }
}
