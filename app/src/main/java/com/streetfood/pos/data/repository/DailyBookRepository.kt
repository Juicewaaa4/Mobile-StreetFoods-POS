package com.streetfood.pos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.models.DailyBookEntry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class DailyBookRepository(private val db: FirebaseFirestore) {

    private val collection = db.collection("dailyBook")

    fun getEntriesForMonth(month: Int, year: Int): Flow<List<DailyBookEntry>> = callbackFlow {
        val listener = collection
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val entries = snapshot.documents.mapNotNull {
                        it.toObject(DailyBookEntry::class.java)?.copy(id = it.id)
                    }.sortedBy { it.day }
                    trySend(entries)
                }
            }
        awaitClose { listener.remove() }
    }

    fun saveEntry(entry: DailyBookEntry, onComplete: (Boolean) -> Unit) {
        val docId = "${entry.year}-${entry.month.toString().padStart(2, '0')}-${entry.day.toString().padStart(2, '0')}"
        val data = mapOf(
            "date" to entry.date,
            "day" to entry.day,
            "month" to entry.month,
            "year" to entry.year,
            "expenses1Day" to entry.expenses1Day,
            "expenses2Day" to entry.expenses2Day,
            "overShortDay" to entry.overShortDay,
            "expenses1Night" to entry.expenses1Night,
            "expenses2Night" to entry.expenses2Night,
            "overShortNight" to entry.overShortNight,
            "salesDay" to entry.salesDay,
            "salesNight" to entry.salesNight,
            "totalSales" to entry.totalSales,
            "notes" to entry.notes
        )
        collection.document(docId).set(data)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
