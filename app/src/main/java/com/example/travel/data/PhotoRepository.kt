package com.example.travel.data

import android.net.Uri
import com.example.travel.models.Photo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

class PhotoRepository {

    private val photosCollection = FirebaseFirestore.getInstance().collection("photos")
    private val storageRef = FirebaseStorage.getInstance().reference

    // Upload image to Firebase Storage and save metadata to Firestore
    suspend fun savePhoto(photo: Photo, localPath: String) {
        // Upload image to Storage
        val file = Uri.fromFile(File(localPath))
        val imageRef = storageRef.child("photos/${photo.id}.jpg")
        imageRef.putFile(file).await()

        // Get download URL
        val downloadUrl = imageRef.downloadUrl.await().toString()

        // Save metadata to Firestore with URL
        val photoWithUrl = photo.copy(imageUrl = downloadUrl)
        photosCollection.document(photo.id).set(photoWithUrl).await()

        // Delete local file after successful upload
        try {
            File(localPath).delete()
        } catch (_: Exception) {}
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

    // Returns the most recent photo for a trip (by timestamp), or null if no photos
    suspend fun getLastPhotoForTrip(tripId: String): Photo? {
        return try {
            val snapshot = photosCollection
                .whereEqualTo("tripId", tripId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(Photo::class.java)
        } catch (_: Exception) {
            null
        }
    }
}