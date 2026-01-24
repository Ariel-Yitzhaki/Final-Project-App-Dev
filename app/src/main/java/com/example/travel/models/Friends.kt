package com.example.travel.models

// Represents a mutual friendship between two users
data class Friends(
    val id: String = "",
    val user1Id: String = "",  // Alphabetically smaller ID
    val user2Id: String = ""   // Alphabetically larger ID
)