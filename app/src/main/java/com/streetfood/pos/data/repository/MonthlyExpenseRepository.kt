package com.streetfood.pos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.streetfood.pos.data.models.MonthlyExpense
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MonthlyExpenseRepository(private val db: FirebaseFirestore) {

    private val collection = db.collection("monthlyExpenses")

    fun getForMonth(month: Int, year: Int): Flow<MonthlyExpense?> = callbackFlow {
        val docId = "${year}-${month.toString().padStart(2, '0')}"
        val listener = collection.document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(MonthlyExpense::class.java)?.copy(id = snapshot.id))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    fun save(expense: MonthlyExpense, onComplete: (Boolean) -> Unit) {
        val docId = "${expense.year}-${expense.month.toString().padStart(2, '0')}"
        val data = mapOf(
            "month" to expense.month,
            "year" to expense.year,
            "salary" to expense.salary,
            "rentalStore" to expense.rentalStore,
            "rentalBH" to expense.rentalBH,
            "meralcoStore" to expense.meralcoStore,
            "meralcoBH" to expense.meralcoBH,
            "mayniladStore" to expense.mayniladStore,
            "mayniladBH" to expense.mayniladBH,
            "otherExpenses" to expense.otherExpenses,
            "otherLabel" to expense.otherLabel
        )
        collection.document(docId).set(data)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
