package com.example.travel.models

data class Trip(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val city: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isActive: Boolean = false,
    val photoCount: Int = 0
)