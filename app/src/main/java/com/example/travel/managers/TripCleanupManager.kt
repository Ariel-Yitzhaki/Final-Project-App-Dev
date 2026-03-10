package com.example.travel.managers

import com.example.travel.data.LikeRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class TripCleanupManager(
    private val tripRepository: TripRepository,
    private val photoRepository: PhotoRepository,
    private val likeRepository: LikeRepository
) {
    private val storageRef = FirebaseStorage.getInstance().reference

    // Deletes a trip and all associated data (photos, storage files, likes)
    suspend fun deleteTrip(tripId: String) {
        val photos = photoRepository.getPhotosForTrip(tripId)
        for (photo in photos) {
            deleteLikesForPhoto(photo.id)
            deleteStorageFile(photo.id)
            photoRepository.deletePhoto(photo.id)
        }
        tripRepository.deleteTrip(tripId)
    }

    private suspend fun deleteLikesForPhoto(photoId: String) {
        likeRepository.deleteLikesForPhoto(photoId)
    }

    private suspend fun deleteStorageFile(photoId: String) {
        try {
            storageRef.child("photos/${photoId}.jpg").delete().await()
        } catch (_: Exception) {}
    }

    // Deletes a single photo and all associated data (likes, storage, Firestore)
    suspend fun deletePhoto(photoId: String, tripId: String) {
        deleteLikesForPhoto(photoId)
        deleteStorageFile(photoId)
        photoRepository.deletePhoto(photoId)
        decrementPhotoCount(tripId)
    }

    // Decrements trip's photo count after a photo deletion
    private suspend fun decrementPhotoCount(tripId: String) {
        val trip = tripRepository.getTripById(tripId) ?: return
        val newCount = (trip.photoCount - 1).coerceAtLeast(0)
        tripRepository.updatePhotoCount(tripId, newCount)
    }
}