package com.example.travel

data class Photo(
    val id: String = "",
    val localPath: String = "",    // Local file path instead of URL
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val date: String = "",
    val timestamp: Long = 0L,
    val tripId: String = ""
)