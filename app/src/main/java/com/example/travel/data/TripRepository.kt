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

    // Get all trips
    suspend fun getAllTrips(): List<Trip> {
        val snapshot = tripsCollection.get().await()
        return snapshot.toObjects(Trip::class.java)
    }
}