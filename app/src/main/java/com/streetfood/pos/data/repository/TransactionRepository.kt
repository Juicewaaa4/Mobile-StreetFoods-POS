package com.streetfood.pos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.streetfood.pos.data.models.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TransactionRepository(private val db: FirebaseFirestore) {

    private val transactionsCollection = db.collection("transactions")

    fun getAllTransactions(): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                    }
                    trySend(transactions)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                    }
                    trySend(transactions)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getByCashierToday(cashierName: String, start: Long, end: Long): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection
            .whereEqualTo("cashierName", cashierName)
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val transactions = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Transaction::class.java)?.copy(id = doc.id)
                    }
                    trySend(transactions)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertTransaction(transaction: Transaction) {
        val docRef = transactionsCollection.document()
        // Keep document ID in Firestore doc id (not as a field) to avoid rules/merging issues.
        docRef.set(
            mapOf(
                "timestamp" to transaction.timestamp,
                "totalAmount" to transaction.totalAmount,
                "cashReceived" to transaction.cashReceived,
                "change" to transaction.change,
                "cashierName" to transaction.cashierName,
                "items" to transaction.items
            )
        ).await()
    }
}
