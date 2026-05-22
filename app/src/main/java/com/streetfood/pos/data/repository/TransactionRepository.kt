package com.streetfood.pos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.streetfood.pos.data.models.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TransactionRepository(private val db: FirebaseFirestore) {

    private val transactionsCollection = db.collection("transactions")

    fun getAllTransactions(): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(Transaction::class.java)?.copy(id = it.id) })
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
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(Transaction::class.java)?.copy(id = it.id) })
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
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(Transaction::class.java)?.copy(id = it.id) })
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertTransaction(transaction: Transaction) =
        suspendCancellableCoroutine { cont ->
            transactionsCollection.document().set(
                mapOf(
                    "timestamp"       to transaction.timestamp,
                    "totalAmount"     to transaction.totalAmount,
                    "cashReceived"    to transaction.cashReceived,
                    "change"          to transaction.change,
                    "cashierName"     to transaction.cashierName,
                    "items"           to transaction.items,
                    "paymentMethod"   to transaction.paymentMethod,
                    "referenceNumber" to transaction.referenceNumber
                )
            )
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
        }
}
