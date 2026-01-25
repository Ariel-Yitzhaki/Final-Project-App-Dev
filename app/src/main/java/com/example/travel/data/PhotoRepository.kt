package com.example.travel.data

import com.example.travel.models.Photo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PhotoRepository {

    private val photosCollection = FirebaseFirestore.getInstance().collection("photos")

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

    // Get all photos
    suspend fun getAllPhotos(): List<Photo> {
        val snapshot = photosCollection.get().await()
        return snapshot.toObjects(Photo::class.java)
    }

    // Get photos for a specific user
    suspend fun getPhotosForUser(userId: String): List<Photo> {
        val snapshot = photosCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return snapshot.toObjects(Photo::class.java)
    }
}