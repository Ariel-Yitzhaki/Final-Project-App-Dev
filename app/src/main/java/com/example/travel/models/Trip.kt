package com.example.travel.models

data class Trip(
    val id: String = "", // The trip's id
    val userId: String = "", // The actual user's id
    val name: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isActive: Boolean = false,
    val photoCount: Int = 0
)