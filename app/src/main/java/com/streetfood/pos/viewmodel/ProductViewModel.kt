package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streetfood.pos.data.models.Product
import com.streetfood.pos.data.models.UiState
import com.streetfood.pos.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductViewModel(private val repo: ProductRepository) : ViewModel() {

    val products: StateFlow<UiState<List<Product>>> = repo.getAllProducts()
        .map { list -> if (list.isEmpty()) UiState.Empty else UiState.Success(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    fun selectProduct(p: Product?) { _selectedProduct.value = p }

    fun addProduct(product: Product) { viewModelScope.launch { repo.insertProduct(product) } }

    fun updateProduct(product: Product) { viewModelScope.launch { repo.updateProduct(product) } }

    fun deleteProduct(product: Product) { viewModelScope.launch { repo.deleteProduct(product) } }

    fun toggleAvailability(product: Product) {
        viewModelScope.launch { repo.updateProduct(product.copy(isAvailable = !product.isAvailable)) }
    }
}
