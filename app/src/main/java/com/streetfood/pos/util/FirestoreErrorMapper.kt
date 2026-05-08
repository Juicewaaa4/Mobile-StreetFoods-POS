package com.streetfood.pos.util

import kotlinx.coroutines.TimeoutCancellationException

/** Maps Firestore / network exceptions to short English messages for cashiers. */
fun mapFirestoreOrNetworkError(e: Throwable): String {
    if (e is TimeoutCancellationException) {
        return "The server took too long to respond. Please try again."
    }
    val msg = e.message.orEmpty()
    return when {
        msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
            "Database access was denied. Check the Firebase rules."
        msg.contains("UNAVAILABLE", ignoreCase = true) ->
            "No internet connection or the service is unavailable."
        msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) ->
            "The connection timed out. Please try again."
        msg.contains("network", ignoreCase = true) ->
            "There was a connection problem. Please try again."
        else -> "Something went wrong. Please try again."
    }
}
