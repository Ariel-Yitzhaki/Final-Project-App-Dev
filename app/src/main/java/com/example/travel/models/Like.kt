package com.example.travel.models

// Represents a like on a photo
data class Like (
    val id: String = "",     // Document ID: "{photoId}_{userId}"
    val photoId: String = "",
    val userId: String = ""
)