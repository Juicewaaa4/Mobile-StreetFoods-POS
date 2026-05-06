package com.streetfood.pos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.streetfood.pos.data.models.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository(private val db: FirebaseFirestore) {

    private val productsCollection = db.collection("products")

    fun getAllProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    }
                    trySend(products)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAvailableProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("isAvailable", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    }
                    trySend(products)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertProduct(product: Product) {
        val docRef = productsCollection.document()
        val productWithId = product.copy(id = docRef.id)
        docRef.set(productWithId).await()
    }

    suspend fun updateProduct(product: Product) {
        if (product.id.isNotEmpty()) {
            productsCollection.document(product.id).set(product).await()
        }
    }

    suspend fun deleteProduct(product: Product) {
        if (product.id.isNotEmpty()) {
            productsCollection.document(product.id).delete().await()
        }
    }
}
