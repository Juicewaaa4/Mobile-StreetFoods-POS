package com.streetfood.pos.data.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class MonthlyExpense(
    val id: String = "",
    val month: Int = 0,             // Month (1-12)
    val year: Int = 0,              // Year (2026)
    val salary: Double = 0.0,
    val rentalStore: Double = 0.0,
    val rentalBH: Double = 0.0,
    val meralcoStore: Double = 0.0,
    val meralcoBH: Double = 0.0,
    val mayniladStore: Double = 0.0,
    val mayniladBH: Double = 0.0,
    val otherExpenses: Double = 0.0,
    val otherLabel: String = ""     // Label for other expenses
) {
    /** Sum of all fixed monthly expenses */
    val totalFixed: Double get() = salary + rentalStore + rentalBH +
            meralcoStore + meralcoBH + mayniladStore + mayniladBH + otherExpenses
}
