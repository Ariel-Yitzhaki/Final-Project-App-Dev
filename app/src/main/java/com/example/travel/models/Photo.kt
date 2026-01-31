package com.example.travel.models

data class Photo(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",    // Firebase Storage download URL
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val date: String = "",
    val timestamp: Long = 0L,
    val tripId: String = ""
)