package com.example.travel.data

import com.example.travel.models.Friends
import com.example.travel.models.FriendRequest
import com.example.travel.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FriendsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val friendsCollection = firestore.collection("friends")
    private val friendRequestsCollection = firestore.collection("friendRequests")

    // Send a friend request
    suspend fun sendFriendRequest(senderId: String, receiverId: String): Result<Unit> {
        return try {
            val id = "${senderId}_${receiverId}"
            val request = FriendRequest(id = id, senderId = senderId, receiverId = receiverId, status = "pending")
            friendRequestsCollection.document(id).set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Accept a friend request - creates friendship and updates request status
    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            friendRequestsCollection.document(request.id).update("status", "accepted").await()

            val (user1, user2) = if (request.senderId < request.receiverId) {
                request.senderId to request.receiverId
            } else {
                request.receiverId to request.senderId
            }
            val friendsId = "${user1}_${user2}"
            val friends = Friends(id = friendsId, user1Id = user1, user2Id = user2)
            friendsCollection.document(friendsId).set(friends).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Decline a friend request
    suspend fun declineFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            friendRequestsCollection.document(request.id).update("status", "declined").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get pending friend requests received by user
    suspend fun getPendingRequests(userId: String): List<FriendRequest> {
        return try {
            friendRequestsCollection
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
                .toObjects(FriendRequest::class.java)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Get all friends for a user
    suspend fun getFriends(userId: String): List<User> {
        return try {
            val asUser1 = friendsCollection
                .whereEqualTo("user1Id", userId)
                .get()
                .await()
                .toObjects(Friends::class.java)

            val asUser2 = friendsCollection
                .whereEqualTo("user2Id", userId)
                .get()
                .await()
                .toObjects(Friends::class.java)

            val friendIds = asUser1.map { it.user2Id } + asUser2.map { it.user1Id }

            friendIds.mapNotNull { getUserProfile(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Remove a friend
    suspend fun removeFriend(currentUserId: String, friendId: String): Result<Unit> {
        return try {
            val (user1, user2) = if (currentUserId < friendId) {
                currentUserId to friendId
            } else {
                friendId to currentUserId
            }
            val friendsId = "${user1}_${user2}"
            friendsCollection.document(friendsId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Search users by username
    suspend fun searchUsersByUsername(query: String): List<User> {
        return try {
            usersCollection
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Check if two users are friends
    suspend fun areFriends(userId1: String, userId2: String): Boolean {
        return try {
            val (user1, user2) = if (userId1 < userId2) userId1 to userId2 else userId2 to userId1
            val friendsId = "${user1}_${user2}"
            friendsCollection.document(friendsId).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    // Check if a pending request exists between two users
    suspend fun getPendingRequestBetween(userId1: String, userId2: String): FriendRequest? {
        return try {
            val sent = friendRequestsCollection.document("${userId1}_${userId2}").get().await()
            if (sent.exists()) {
                return sent.toObject(FriendRequest::class.java)?.takeIf { it.status == "pending" }
            }

            val received = friendRequestsCollection.document("${userId2}_${userId1}").get().await()
            if (received.exists()) {
                return received.toObject(FriendRequest::class.java)?.takeIf { it.status == "pending" }
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    // Helper to get user profile
    private suspend fun getUserProfile(uid: String): User? {
        return try {
            usersCollection.document(uid).get().await().toObject(User::class.java)
        } catch (_: Exception) {
            null
        }
    }
}