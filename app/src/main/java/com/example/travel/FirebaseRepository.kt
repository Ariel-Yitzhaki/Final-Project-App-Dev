package com.example.travel

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val photosCollection = firestore.collection("photos")
    private val tripsCollection = firestore.collection("trips")

    // Upload image to Storage and return download URL
    suspend fun uploadImage(imageUri: Uri, photoId: String): String {
        val ref = storage.reference.child("photos/$photoId.jpg")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    // Save photo metadata to Firestore
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