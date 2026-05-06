package com.streetfood.pos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.streetfood.pos.data.database.AppDatabase

class POSViewModelFactory(
    private val database: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(POSViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return POSViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ProductViewModelFactory(
    private val database: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class TransactionViewModelFactory(
    private val database: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
