package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.models.RawIngredient
import com.streetfood.pos.data.repository.RawIngredientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class InventoryViewModel(private val repository: RawIngredientRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val ingredients: StateFlow<List<RawIngredient>> = repository.getIngredients()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveIngredient(ingredient: RawIngredient, onResult: (Boolean) -> Unit) {
        repository.saveIngredient(ingredient, onResult)
    }

    fun deleteIngredient(id: String, onResult: (Boolean) -> Unit) {
        repository.deleteIngredient(id, onResult)
    }

    fun updateStock(ingredient: RawIngredient, newStock: Int, onResult: (Boolean) -> Unit = {}) {
        val updated = ingredient.copy(stock = newStock.coerceAtLeast(0))
        saveIngredient(updated, onResult)
    }
}

class InventoryViewModelFactory(private val db: FirebaseFirestore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = RawIngredientRepository(db)
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(InventoryViewModel::class.java) ->
                InventoryViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
