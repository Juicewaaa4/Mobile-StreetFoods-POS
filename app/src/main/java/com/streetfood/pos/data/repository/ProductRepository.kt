package com.streetfood.pos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.streetfood.pos.data.models.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ProductRepository(private val db: FirebaseFirestore) {

    private val productsCollection = db.collection("products")

    fun getAllProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(Product::class.java)?.copy(id = it.id) })
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAvailableProducts(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("isAvailable", true)
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(Product::class.java)?.copy(id = it.id) })
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertProduct(product: Product) =
        suspendCancellableCoroutine { cont ->
            productsCollection.document().set(
                mapOf(
                    "name"        to product.name,
                    "price"       to product.price,
                    "cost"        to product.cost,
                    "category"    to product.category,
                    "isAvailable" to product.isAvailable
                )
            )
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun updateProduct(product: Product) =
        suspendCancellableCoroutine { cont ->
            if (product.id.isEmpty()) { cont.resume(Unit); return@suspendCancellableCoroutine }
            productsCollection.document(product.id).set(
                mapOf(
                    "name"        to product.name,
                    "price"       to product.price,
                    "cost"        to product.cost,
                    "category"    to product.category,
                    "isAvailable" to product.isAvailable
                )
            )
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun deleteProduct(product: Product) =
        suspendCancellableCoroutine { cont ->
            if (product.id.isEmpty()) { cont.resume(Unit); return@suspendCancellableCoroutine }
            productsCollection.document(product.id).delete()
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}
