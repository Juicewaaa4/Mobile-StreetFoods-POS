package com.streetfood.pos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.streetfood.pos.data.models.ActivityLog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ActivityLogRepository(private val db: FirebaseFirestore) {

    private val logsCollection = db.collection("activity_logs")

    fun getLogs(): Flow<List<ActivityLog>> = callbackFlow {
        val listener = logsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100) // Keep the UI snappy by limiting logs
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(ActivityLog::class.java)?.copy(id = it.id) })
                }
            }
        awaitClose { listener.remove() }
    }

    fun getLogsByDateRange(start: Long, end: Long): Flow<List<ActivityLog>> = callbackFlow {
        val listener = logsCollection
            .whereGreaterThanOrEqualTo("timestamp", start)
            .whereLessThanOrEqualTo("timestamp", end)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(500)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.documents.mapNotNull { it.toObject(ActivityLog::class.java)?.copy(id = it.id) })
                }
            }
        awaitClose { listener.remove() }
    }

    fun deleteOlderThan(timestamp: Long, onComplete: (Boolean) -> Unit) {
        logsCollection
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

    fun logAction(actionType: String, details: String) {
        val userName = UserSessionRepository.username
        val userRole = UserSessionRepository.currentRole?.name ?: "Unknown"

        val log = ActivityLog(
            userName = userName,
            userRole = userRole,
            actionType = actionType,
            details = details
        )

        logsCollection.document().set(log)
    }
}
