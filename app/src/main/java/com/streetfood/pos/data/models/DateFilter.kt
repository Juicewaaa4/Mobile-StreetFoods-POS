package com.streetfood.pos.data.models

import java.util.Calendar

enum class DateFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

/** Helper to get the 8:30 AM to 8:29:59 AM (next day) bounds for a specific calendar day */
private fun getShiftBoundsForDay(cal: Calendar): Pair<Long, Long> {
    val start = cal.clone() as Calendar
    start.set(Calendar.HOUR_OF_DAY, 8)
    start.set(Calendar.MINUTE, 30)
    start.set(Calendar.SECOND, 0)
    start.set(Calendar.MILLISECOND, 0)

    val end = cal.clone() as Calendar
    end.add(Calendar.DAY_OF_YEAR, 1)
    end.set(Calendar.HOUR_OF_DAY, 8)
    end.set(Calendar.MINUTE, 29)
    end.set(Calendar.SECOND, 59)
    end.set(Calendar.MILLISECOND, 999)

    return Pair(start.timeInMillis, end.timeInMillis)
}

/** 
 * Returns the Calendar representing the "current shift base day".
 * If the current time is before 8:30 AM, it's still considered the previous day's shift.
 */
private fun getCurrentShiftBaseDay(): Calendar {
    val now = Calendar.getInstance()
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val min = now.get(Calendar.MINUTE)
    
    if (hour < 8 || (hour == 8 && min < 30)) {
        now.add(Calendar.DAY_OF_YEAR, -1)
    }
    return now
}

/** Returns start/end timestamp (ms) for a given DateFilter based on shift hours (8:30 AM - 8:29 AM). */
fun DateFilter.toDateRange(): Pair<Long, Long> {
    val currentShiftBase = getCurrentShiftBaseDay()
    val (_, currentShiftEnd) = getShiftBoundsForDay(currentShiftBase)

    return when (this) {
        DateFilter.TODAY -> {
            getShiftBoundsForDay(currentShiftBase)
        }
        DateFilter.YESTERDAY -> {
            val yesterdayBase = currentShiftBase.clone() as Calendar
            yesterdayBase.add(Calendar.DAY_OF_YEAR, -1)
            getShiftBoundsForDay(yesterdayBase)
        }
        DateFilter.THIS_WEEK -> {
            val weekBase = currentShiftBase.clone() as Calendar
            weekBase.set(Calendar.DAY_OF_WEEK, weekBase.firstDayOfWeek)
            val (weekStart, _) = getShiftBoundsForDay(weekBase)
            Pair(weekStart, currentShiftEnd)
        }
        DateFilter.THIS_MONTH -> {
            val monthBase = currentShiftBase.clone() as Calendar
            monthBase.set(Calendar.DAY_OF_MONTH, 1)
            val (monthStart, _) = getShiftBoundsForDay(monthBase)
            Pair(monthStart, currentShiftEnd)
        }
        DateFilter.ALL_TIME -> Pair(0L, currentShiftEnd)
    }
}

/** Returns the start-of-shift timestamp for today's shift (8:30 AM). */
fun todayStart(): Long = getShiftBoundsForDay(getCurrentShiftBaseDay()).first

/** Returns the end-of-shift timestamp for today's shift (8:29:59 AM next day). */
fun todayEnd(): Long = getShiftBoundsForDay(getCurrentShiftBaseDay()).second
