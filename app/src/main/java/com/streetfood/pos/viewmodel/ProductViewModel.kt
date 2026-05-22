package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streetfood.pos.data.models.Product
import com.streetfood.pos.data.models.UiState
import com.streetfood.pos.data.repository.ProductRepository
import com.streetfood.pos.util.mapFirestoreOrNetworkError
import java.util.Locale
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductViewModel(private val repo: ProductRepository, private val logRepo: com.streetfood.pos.data.repository.ActivityLogRepository) : ViewModel() {

    private val _userMessages = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val userMessages = _userMessages.asSharedFlow()

    val products: StateFlow<UiState<List<Product>>> = repo.getAllProducts()
        .map { list ->
            val sorted = list.sortedWith(
                compareBy({ it.stock <= 0 }, { it.name.lowercase(Locale.getDefault()) })
            )
            if (sorted.isEmpty()) UiState.Empty else UiState.Success(sorted)
        }
        .catch { emit(UiState.Error(mapFirestoreOrNetworkError(it))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    fun selectProduct(p: Product?) { _selectedProduct.value = p }

    fun addProduct(product: Product) {
        viewModelScope.launch {
            try {
                repo.insertProduct(product)
                logRepo.logAction("Add Product", "Added ${product.name} with ${product.stock} stock")
                _userMessages.emit("Product saved.")
            } catch (e: Exception) {
                _userMessages.emit(mapFirestoreOrNetworkError(e))
            }
        }
    }

    fun updateProduct(product: Product, notify: Boolean = true, oldStock: Int? = null) {
        viewModelScope.launch {
            try {
                repo.updateProduct(product)
                val diff = if (oldStock != null) product.stock - oldStock else 0
                val detail = if (diff > 0) "Added $diff to ${product.name} (Now: ${product.stock})"
                             else if (diff < 0) "Removed ${-diff} from ${product.name} (Now: ${product.stock})"
                             else "Updated ${product.name}"
                logRepo.logAction("Update Product", detail)

                if (notify) _userMessages.emit("Product changes saved.")
            } catch (e: Exception) {
                _userMessages.emit(mapFirestoreOrNetworkError(e))
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            try {
                repo.deleteProduct(product)
                logRepo.logAction("Delete Product", "Deleted ${product.name}")
                _userMessages.emit("Product deleted.")
            } catch (e: Exception) {
                _userMessages.emit(mapFirestoreOrNetworkError(e))
            }
        }
    }
}
