package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streetfood.pos.data.database.AppDatabase
import com.streetfood.pos.data.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductViewModel(
    private val database: AppDatabase
) : ViewModel() {
    
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                database.productDao().getAllProducts().collect { productList ->
                    _products.value = productList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun selectProduct(product: Product?) {
        _selectedProduct.value = product
    }

    fun addProduct(product: Product) {
        viewModelScope.launch {
            try {
                database.productDao().insertProduct(product)
                loadProducts()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            try {
                database.productDao().updateProduct(product)
                loadProducts()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            try {
                database.productDao().deleteProduct(product)
                loadProducts()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun toggleProductAvailability(product: Product) {
        viewModelScope.launch {
            try {
                val updatedProduct = product.copy(isAvailable = !product.isAvailable)
                database.productDao().updateProduct(updatedProduct)
                loadProducts()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
