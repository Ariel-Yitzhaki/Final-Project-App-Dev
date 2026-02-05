package com.example.travel.data

import com.example.travel.models.Like
import com.example.travel.models.Trip
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LikeRepository {

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
        for (trip in trips) {
            val photos = photoRepository.getPhotosForTrip(trip.id)
            val photoIds = photos.map { it.id }
            tripLikes[trip.id] = getTotalLikesForTrip(photoIds)
        }
        return tripLikes
    }

}