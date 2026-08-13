package com.streetfood.pos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.models.RawIngredient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class RawIngredientRepository(private val db: FirebaseFirestore) {
    private val collection = db.collection("raw_ingredients")

    fun getIngredients(): Flow<List<RawIngredient>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val ingredients = snapshot.documents.mapNotNull { it.toObject(RawIngredient::class.java) }
                trySend(ingredients.sortedBy { it.name })
            }
        }
        awaitClose { listener.remove() }
    }

    fun saveIngredient(ingredient: RawIngredient, onResult: (Boolean) -> Unit) {
        val id = if (ingredient.id.isEmpty()) UUID.randomUUID().toString() else ingredient.id
        val finalIngredient = ingredient.copy(
            id = id,
            lastUpdated = System.currentTimeMillis()
        )
        
        collection.document(id).set(finalIngredient)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    fun deleteIngredient(id: String, onResult: (Boolean) -> Unit) {
        collection.document(id).delete()
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}
