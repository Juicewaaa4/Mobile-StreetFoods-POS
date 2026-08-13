package com.streetfood.pos.data.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class DailyBookEntry(
    val id: String = "",
    val date: String = "",          // "2026-03-01" format
    val day: Int = 0,               // Day of month (1-31)
    val month: Int = 0,             // Month (1-12)
    val year: Int = 0,              // Year (2026)
    
    // Day Shift
    val expenses1Day: Double = 0.0,    
    val expenses2Day: Double = 0.0,    
    val overShortDay: Double = 0.0,
    
    // Night Shift
    val expenses1Night: Double = 0.0,    
    val expenses2Night: Double = 0.0,    
    val overShortNight: Double = 0.0,
    
    val salesDay: Double = 0.0,
    val salesNight: Double = 0.0,
    val totalSales: Double = 0.0,   // Final total
    val notes: String = ""          // Optional notes
) {
    @get:Exclude
    val sales: Double get() = salesDay + salesNight
    @get:Exclude
    val expenses1: Double get() = expenses1Day + expenses1Night

    @get:Exclude
    val expenses2: Double get() = expenses2Day + expenses2Night

    @get:Exclude
    val overShort: Double get() = overShortDay + overShortNight
}
