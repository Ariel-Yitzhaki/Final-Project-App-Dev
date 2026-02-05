package com.example.travel.models

// User profile data stored in Firestore
data class User(
    val id: String = "",
    val username: String = "",      // For login/search
    val displayName: String = "",   // Shown to other users
    val email: String = "",
    val profilePictureUrl: String = ""
)