package com.example.travel.data

import com.example.travel.models.Like
import com.example.travel.models.Trip
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await

class LikeRepository {

    companion object {
        val instance = LikeRepository()
    }

    // Cached total likes per trip ID
    private val cachedTripLikes = mutableMapOf<String, Int>()
    private val likesCollection = FirebaseFirestore.getInstance().collection("likes")

    // Toggles like status - returns true if now liked, false if unliked
    suspend fun toggleLike(photoId: String, userId: String): Boolean {
        val likeId = "${photoId}_${userId}"
        val existingLike = likesCollection.document(likeId).get().await()

        return if (existingLike.exists()) {
            // Unlike - remove the document
            likesCollection.document(likeId).delete().await()
            false
        } else {
            // Like - create a new document
            val like = Like(id = likeId, photoId = photoId, userId = userId)
            likesCollection.document(likeId).set(like).await()
            true
        }
    }

    // Gets like count for a photo
    suspend fun getLikeCount(photoId: String): Int {
        val snapshot = likesCollection
            .whereEqualTo("photoId", photoId)
            .get()
            .await()
        return snapshot.size()
    }

    // Checks if user has liked a photo
    suspend fun hasUserLiked(photoId: String, userId: String): Boolean {
        val likeId = "${photoId}_${userId}"
        val doc = likesCollection.document(likeId).get().await()
        return doc.exists()
    }

    // Gets total likes for all photos in a trip
    suspend fun getTotalLikesForTrip(photoIds: List<String>): Int {
        if (photoIds.isEmpty()) return 0
        var total = 0
        for (photoId in photoIds) {
            total += getLikeCount(photoId)
        }
        return total
    }

    // Gets like counts for multiple trips
    suspend fun getLikesForTrips(trips: List<Trip>, photoRepository: PhotoRepository): Map<String, Int> {
        val tripLikes = mutableMapOf<String, Int>()

        // Run all trip-like fetches concurrently instead of one by one
        kotlinx.coroutines.coroutineScope {
            val deferredResults = trips.map { trip ->
                async {
                    val photos = photoRepository.getPhotosForTrip(trip.id)
                    val photoIds = photos.map { it.id }
                    trip.id to getTotalLikesForTrip(photoIds)
                }
            }
            for (deferred in deferredResults) {
                val (tripId, likes) = deferred.await()
                tripLikes[tripId] = likes
                cachedTripLikes[tripId] = likes
            }
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
    }

    // Clears all cached data
    fun invalidateCache(tripId: String? = null) {
        if (tripId != null) {
            cachedTripLikes.remove(tripId)
        } else {
            cachedTripLikes.clear()
        }
    }
}