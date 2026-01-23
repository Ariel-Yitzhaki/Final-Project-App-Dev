package com.example.travel

data class Photo(
    val id: String = "",
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val date: String = "",         // Display format: "2024-01-15"
    val timestamp: Long = 0L,      // Full timestamp for sorting/posts
    val tripId: String = ""
)