package com.example.travel.data

import com.example.travel.models.Trip
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TripRepository {

    private val tripsCollection = FirebaseFirestore.getInstance().collection("trips")

    // Save a new trip
    suspend fun saveTrip(trip: Trip) {
        tripsCollection.document(trip.id).set(trip).await()
    }

    // Get active trip for user (only one can be active)
    suspend fun getActiveTrip(userId: String): Trip? {
        val snapshot = tripsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("active", true)
            .get()
            .await()
        return snapshot.toObjects(Trip::class.java).firstOrNull()
    }

    // Get a single trip by its ID
    suspend fun getTripById(tripId: String): Trip? {
        val snapshot = tripsCollection.document(tripId).get().await()
        return snapshot.toObject(Trip::class.java)
    }

    // Get all completed trips for user (with photos)
    suspend fun getCompletedTrips(userId: String): List<Trip> {
        val snapshot = tripsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("active", false)
            .get()
            .await()
        return snapshot.toObjects(Trip::class.java).filter { it.photoCount > 0 }
    }

    // Get completed trips for multiple users (for home feed), batched for >10 friends
    suspend fun getCompletedTripsForUsers(userIds: List<String>): List<Trip> {
        if (userIds.isEmpty()) return emptyList()

        val allTrips = mutableListOf<Trip>()

        userIds.chunked(10).forEach { batch ->
            val snapshot = tripsCollection
                .whereIn("userId", batch)
                .whereEqualTo("active", false)
                .get()
                .await()
            allTrips.addAll(snapshot.toObjects(Trip::class.java))
        }

        return allTrips.filter { it.photoCount > 0 }
    }

    suspend fun endTrip(tripId: String, endDate: String) {
        tripsCollection.document(tripId).update(
            mapOf(
                "active" to false,
                "endDate" to endDate
            )
        ).await()
    }

    // Increment photo count for a trip
    suspend fun incrementPhotoCount(tripId: String) {
        val trip = tripsCollection.document(tripId).get().await().toObject(Trip::class.java)
        trip?.let {
            tripsCollection.document(tripId).update("photoCount", it.photoCount + 1).await()
        }
    }

    // Delete trip (for empty trips)
    suspend fun deleteTrip(tripId: String) {
        tripsCollection.document(tripId).delete().await()
    }
}