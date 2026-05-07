package com.streetfood.pos.util

import kotlinx.coroutines.TimeoutCancellationException

/** Maps Firestore / network exceptions to short Tagalog-first messages for cashiers. */
fun mapFirestoreOrNetworkError(e: Throwable): String {
    if (e is TimeoutCancellationException) {
        return "Matagal ang sagot ng server. Subukan ulit."
    }
    val msg = e.message.orEmpty()
    return when {
        msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
            "Walang pahintulot sa database. Tingnan ang Firebase rules."
        msg.contains("UNAVAILABLE", ignoreCase = true) ->
            "Walang internet o hindi available ang serbisyo."
        msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) ->
            "Na-timeout ang koneksyon. Subukan ulit."
        msg.contains("network", ignoreCase = true) ->
            "Problema sa koneksyon. Subukan ulit."
        else -> "May naganap na error. Subukan ulit."
    }
}
