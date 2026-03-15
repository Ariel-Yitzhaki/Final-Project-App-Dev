package com.example.travel.data

import com.example.travel.models.Like
import com.example.travel.models.Photo
import com.example.travel.models.Trip
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.collections.emptyList

class LikeRepository {

    companion object {
        val instance = LikeRepository()
    }

    // Cached total likes per trip ID
    private val cachedTripLikes = mutableMapOf<String, Int>()
    private val likesCollection = FirebaseFirestore.getInstance().collection("likes")
    private val photosCollection = FirebaseFirestore. getInstance().collection("photos")

    // Toggles like status - returns true if now liked, false if unliked
    suspend fun toggleLike(photoId: String, userId: String): Boolean {
        val likeId = "${photoId}_${userId}"
        val existingLike = likesCollection.document(likeId).get().await()

        if (existingLike.exists()) {
            // Unlike - remove the document
            likesCollection.document(likeId).delete().await()
            photosCollection.document(photoId)
                .update("likeCount", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()
            cachedTripLikes.clear()
            return false
        } else {
            // Like - create a new document
            val like = Like(id = likeId, photoId = photoId, userId = userId)
            likesCollection.document(likeId).set(like).await()
            photosCollection.document(photoId)
                .update("likeCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            cachedTripLikes.clear()
            return true
        }
    }

    // Gets like count for a photo
    suspend fun getLikeCount(photoId: String): Int {
        val doc = photosCollection.document(photoId).get().await()
        return doc.getLong("likeCount")?.toInt() ?: 0
    }

    // Checks if user has liked a photo
    suspend fun hasUserLiked(photoId: String, userId: String): Boolean {
        val likeId = "${photoId}_${userId}"
        val doc = likesCollection.document(likeId).get().await()
        return doc.exists()
    }

    // Gets total likes for all trips
     fun getTotalLikesForTrips(trips: List<Trip>, photos: Map<String, List<Photo>>): Map<String, Int> {
        val tripLikes = mutableMapOf<String, Int>()
        for (trip in trips) {
            val tripPhotos = photos[trip.id] ?: emptyList()
            val total = tripPhotos.sumOf { it.likeCount }
            tripLikes[trip.id] = total
            cachedTripLikes[trip.id] = total
        }
        return tripLikes
    }

    fun getCachedLikesForTrips(): Map<String, Int> {
        return cachedTripLikes.toMap()
    }

    suspend fun deleteLikesForPhoto(photoId: String) {
        val snapshot = likesCollection
            .whereEqualTo("photoId", photoId)
            .get()
            .await()

        for (doc in snapshot.documents) {
            likesCollection.document(doc.id).delete().await()
        }

        try {
            photosCollection.document(photoId).update("likeCount", 0).await()
        } catch (_: Exception) {}
    }

    // Clears all cached data
    fun invalidateCache(tripId: String? = null) {
        if (tripId != null) {
            cachedTripLikes.remove(tripId)
        } else {
            cachedTripLikes.clear()
        }
    }

    suspend fun fetchTripLikesFromPhotos(
        trips: List<Trip>,
        photoRepository: PhotoRepository
    ): Map<String, Int> {
        val photosMap = mutableMapOf<String, List<Photo>>()
        for (trip in trips) {
            photosMap[trip.id] = photoRepository.getPhotosForTrip(trip.id)
        }
        return getTotalLikesForTrips(trips, photosMap)
    }
}