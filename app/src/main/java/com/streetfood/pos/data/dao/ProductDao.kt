package com.streetfood.pos.data.dao

import androidx.room.*
import com.streetfood.pos.data.models.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isAvailable = 1 ORDER BY name ASC")
    fun getAvailableProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Insert
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Int)
}
