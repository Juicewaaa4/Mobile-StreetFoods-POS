package com.streetfood.pos.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.streetfood.pos.data.models.User
import com.streetfood.pos.data.models.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(
    private val db: FirebaseFirestore,
    private val context: Context
) {
    private val usersCollection = db.collection("users")

    /** Real-time stream of all users from Firestore. */
    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .orderBy("username", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        val username = doc.getString("username") ?: return@mapNotNull null
                        val email    = doc.getString("email") ?: ""
                        val roleStr  = doc.getString("role") ?: UserRole.CASHIER.name
                        val role     = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.CASHIER }
                        User(id = doc.id, username = username, email = email, role = role)
                    }
                    trySend(users)
                }
            }
        awaitClose { listener.remove() }
    }

    /** Ensures the two demo accounts are visible in User Management. */
    suspend fun ensureDefaultUsers(): Result<Unit> {
        return try {
            ensureDefaultUser("admin", UserRole.ADMIN)
            ensureDefaultUser("cashier", UserRole.CASHIER)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun ensureDefaultUser(username: String, role: UserRole) {
        val existing = usersCollection
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .await()

        if (!existing.isEmpty) return

        usersCollection.document("default_$username").set(
            mapOf(
                "username" to username,
                "email" to buildEmail(username),
                "role" to role.name
            )
        ).await()
    }

    /**
     * Creates a new Firebase Auth account using a secondary app instance
     * (so the admin session is not disturbed) and stores the user in Firestore.
     */
    suspend fun createUser(username: String, password: String, role: UserRole): Result<Unit> {
        return try {
            val email = buildEmail(username)

            // Secondary Firebase app to avoid signing out the current admin
            val options = FirebaseApp.getInstance().options
            val secondaryApp = try {
                FirebaseApp.getInstance("secondary")
            } catch (_: IllegalStateException) {
                FirebaseApp.initializeApp(context, options, "secondary")
            }
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp!!)

            val result = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val uid    = result.user?.uid ?: throw Exception("Failed to get UID after creation.")
            secondaryAuth.signOut()

            // Store in Firestore
            usersCollection.document(uid).set(
                mapOf(
                    "username" to username,
                    "email"    to email,
                    "role"     to role.name
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Updates username and/or role in Firestore. */
    suspend fun updateUser(uid: String, newUsername: String, newRole: UserRole): Result<Unit> {
        return try {
            usersCollection.document(uid).update(
                mapOf(
                    "username" to newUsername,
                    "email"    to buildEmail(newUsername),
                    "role"     to newRole.name
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Deletes user from Firestore. Firebase Auth account is left intact. */
    suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Saves a user record to Firestore (used when seeding the admin on first login). */
    suspend fun upsertUser(user: User) = suspendCancellableCoroutine { cont ->
        if (user.id.isBlank()) { cont.resume(Unit); return@suspendCancellableCoroutine }
        usersCollection.document(user.id).set(
            mapOf(
                "username" to user.username,
                "email"    to user.email,
                "role"     to user.role.name
            )
        )
        .addOnSuccessListener { cont.resume(Unit) }
        .addOnFailureListener { cont.resumeWithException(it) }
    }

    /** Looks up a user in Firestore by Firebase Auth UID. Returns null if not found. */
    suspend fun getUserByUid(uid: String): User? {
        return try {
            val doc = usersCollection.document(uid).get().await()
            if (!doc.exists()) return null
            val username = doc.getString("username") ?: return null
            val email    = doc.getString("email") ?: buildEmail(username)
            val roleStr  = doc.getString("role") ?: UserRole.CASHIER.name
            val role     = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.CASHIER }
            User(id = uid, username = username, email = email, role = role)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun buildEmail(username: String): String =
            if (username.contains("@")) username.trim().lowercase()
            else "${username.trim().lowercase()}@test.com"
    }
}
