package com.example.travel.models

// Friend request with pending/accepted/declined status
data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val status: String = "pending"  // pending, accepted, declined
)