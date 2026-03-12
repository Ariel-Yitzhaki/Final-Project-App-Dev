package com.example.travel.data

import com.example.travel.models.Trip
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TripRepository {

    companion object {
        val instance = TripRepository()
    }
    private val tripsCollection = FirebaseFirestore.getInstance().collection("trips")

    // Cached trips per use ID, so navigating back shows data instantly
    private val cachedUserTrips = mutableMapOf<String, List<Trip>>()

    // Caches single trips by trip ID
    private val cachedTripsById = mutableMapOf<String, Trip>()

    // Cached active trip per user ID
    private var cachedActiveTrip = mutableMapOf<String, Trip?>()

    // Cache for friend feed trips (keyed by sorted friend IDs to detect changes)
    private var cachedFeedTrips: List<Trip>? = null

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
        val trip = snapshot.toObjects(Trip::class.java).firstOrNull()
        cachedActiveTrip[userId] = trip
        return trip
    }

    // Returns cached active trip if available
    fun getCachedActiveTrip(userId: String): Trip? {
        return cachedActiveTrip[userId]
    }

    // If we have cached data for this user (even if it's null)
    fun hasCachedActiveTrip(userId: String): Boolean {
        return cachedActiveTrip.containsKey(userId)
    }

    // Get a single trip by its ID
    suspend fun getTripById(tripId: String): Trip? {
        val snapshot = tripsCollection.document(tripId).get().await()
        val trip = snapshot.toObject(Trip::class.java)
        if (trip != null) cachedTripsById[tripId] = trip
        return trip
    }

    fun getCachedTripById(tripId: String): Trip? {
        return cachedTripsById[tripId]
    }

    // Get all completed trips for user (with photos)
    suspend fun getCompletedTrips(userId: String): List<Trip> {
        val snapshot = tripsCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("active", false)
            .get()
            .await()
        val trips = snapshot.toObjects(Trip::class.java).filter { it.photoCount > 0 }
        cachedUserTrips[userId] = trips
        return trips
    }

    // Returns cached completed trips if available, null if no cache exists
    fun getCachedCompletedTrips(userId: String): List<Trip>? {
        return cachedUserTrips[userId]
    }

    // Increment photo count for a trip
    suspend fun incrementPhotoCount(tripId: String) {
        val trip = tripsCollection.document(tripId).get().await().toObject(Trip::class.java)
        trip?.let {
            tripsCollection.document(tripId).update("photoCount", it.photoCount + 1).await()
        }
    }

    // Returns all trips for a user (for dropdown menu)
    suspend fun getAllTripsForUser(userId: String): List<Trip> {
        return try {
            val snapshot = tripsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Trip::class.java) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Gets trips with at least one photo for multiple users
    suspend fun getTripsWithPhotosForUsers(userIds: List<String>): List<Trip> {
        if (userIds.isEmpty()) return emptyList()

        val allTrips = mutableListOf<Trip>()

        for (batch in userIds.chunked(10)) {
            val snapshot = tripsCollection
                .whereIn("userId", batch)
                .whereGreaterThan("photoCount", 0)
                .get()
                .await()

            allTrips.addAll(snapshot.toObjects(Trip::class.java))
        }

        cachedFeedTrips = allTrips
        return allTrips
    }

    fun getCachedFeedTrips(): List<Trip>? {
        return cachedFeedTrips
    }


    // Reactivates a trip (sets active = true, clears endDate)
    suspend fun reactivateTrip(tripId: String): Boolean {
        return try {
            tripsCollection.document(tripId)
                .update(mapOf("active" to true, "endDate" to ""))
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    // Deactivates a trip (sets active = false, sets endDate to last photo's date)
    suspend fun deactivateTrip(tripId: String, endDate: String): Boolean {
        return try {
            tripsCollection.document(tripId)
                .update(mapOf("active" to false, "endDate" to endDate))
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }

    // Delete trip (for empty trips)
    suspend fun deleteTrip(tripId: String) {
        tripsCollection.document(tripId).delete().await()
    }

    // Updates photo count for a trip
    suspend fun updatePhotoCount(tripId: String, count: Int) {
        tripsCollection.document(tripId).update("photoCount", count).await()
    }

    // Clears all cached data - call after mutations that affect trip state
    fun invalidateCache() {
        cachedUserTrips.clear()
        cachedTripsById.clear()
        cachedActiveTrip.clear()
        cachedFeedTrips = null
    }
}