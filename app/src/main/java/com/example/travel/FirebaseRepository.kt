package com.example.travel

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private val photosCollection = firestore.collection("photos")
    private val tripsCollection = firestore.collection("trips")
    private val usersCollection = firestore.collection("users")
    private val auth = FirebaseAuth.getInstance()

    // Save photo metadata to Firestore (imageUrl is now local file path)
    suspend fun savePhoto(photo: Photo) {
        photosCollection.document(photo.id).set(photo).await()
    }

    // Get all photos for a trip
    suspend fun getPhotosForTrip(tripId: String): List<Photo> {
        val snapshot = photosCollection
            .whereEqualTo("tripId", tripId)
            .get()
            .await()
        return snapshot.toObjects(Photo::class.java)
    }

    // Get all photos
    suspend fun getAllPhotos(): List<Photo> {
        val snapshot = photosCollection.get().await()
        return snapshot.toObjects(Photo::class.java)
    }

    // Save a new trip
    suspend fun saveTrip(trip: Trip) {
        tripsCollection.document(trip.id).set(trip).await()
    }

    // Get all trips
    suspend fun getAllTrips(): List<Trip> {
        val snapshot = tripsCollection.get().await()
        return snapshot.toObjects(Trip::class.java)
    }

    // Returns currently logged in user, or null
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Sign up with email/password, then create user profile in Firestore
    suspend fun signUp(email: String, password: String, username: String, displayName: String): Result<User> {
        return try {
            // Check if username is already taken
            val usernameQuery = usersCollection
                .whereEqualTo("username", username)
                .get()
                .await()

            if (!usernameQuery.isEmpty) {
                return Result.failure(Exception("Username already taken"))
            }

            // Create auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            // Create user profile in Firestore
            val user = User(id = uid, username = username, displayName = displayName, email = email)
            usersCollection.document(uid).set(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sign in with email and password
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sign out
    fun signOut() {
        auth.signOut()
    }

    // Get user profile from Firestore by ID
    suspend fun getUserProfile(uid: String): User? {
        return try {
            usersCollection.document(uid).get().await().toObject(User::class.java)
        } catch (_: Exception) {
            null
        }
    }
}
