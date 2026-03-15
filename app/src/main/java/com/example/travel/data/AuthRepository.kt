package com.example.travel.data

import com.example.travel.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    companion object {
        val instance = AuthRepository()
    }

    // Cached user profiles by user ID, avoids re-fetching on every screen
    private val cachedProfiles = mutableMapOf<String, User>()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = FirebaseFirestore.getInstance().collection("users")

    // Returns currently logged in user, or null
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Register with email/password, then create user profile in Firestore
    suspend fun register(email: String, password: String, username: String, displayName: String): Result<User> {
        return try {
            val usernameQuery = usersCollection
                .whereEqualTo("username", username)
                .get()
                .await()

            if (!usernameQuery.isEmpty) {
                return Result.failure(Exception("Username already taken"))
            }

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            val user = User(id = uid, username = username, displayName = displayName, email = email)
            usersCollection.document(uid).set(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Login with email and password
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Log out
    fun logOut() {
        auth.signOut()
        invalidateCache()
        TripRepository.instance.invalidateCache()
        PhotoRepository.instance.invalidateCache()
        LikeRepository.instance.invalidateCache()
        FriendsRepository.instance.invalidateCache()
    }

    // Get user profile from Firestore by ID
    suspend fun getUserProfile(uid: String): User? {
        return try {
            val user = usersCollection.document(uid).get().await().toObject(User::class.java)
            if (user != null) cachedProfiles[uid] = user
            user
        } catch (_: Exception) {
            null
        }
    }

    fun getCachedUserProfile(uid: String): User? {
        return cachedProfiles[uid]
    }

    // Updates user's profile picture URL in Firestore
    suspend fun updateProfilePicture(uid: String, url: String) {
        usersCollection.document(uid).update("profilePictureUrl", url).await()
        // Keep cache in sync with the new picture URL
        cachedProfiles[uid]?.let {
            cachedProfiles[uid] = it.copy(profilePictureUrl = url)
        }
    }

    fun invalidateCache() {
        cachedProfiles.clear()
    }
}