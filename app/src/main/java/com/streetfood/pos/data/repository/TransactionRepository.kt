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

    fun deleteOlderThan(timestamp: Long, onComplete: (Boolean) -> Unit) {
        transactionsCollection
            .whereLessThan("timestamp", timestamp)
            .get()
            .addOnSuccessListener { snapshot ->
                val docs = snapshot.documents
                if (docs.isEmpty()) {
                    onComplete(true)
                    return@addOnSuccessListener
                }

                val batches = docs.chunked(500)
                var completedBatches = 0
                var hasError = false

                batches.forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit()
                        .addOnSuccessListener {
                            completedBatches++
                            if (completedBatches == batches.size) {
                                onComplete(!hasError)
                            }
                        }
                        .addOnFailureListener {
                            hasError = true
                            completedBatches++
                            if (completedBatches == batches.size) {
                                onComplete(false)
                            }
                        }
                }
            }
            .addOnFailureListener { onComplete(false) }
    }
}
