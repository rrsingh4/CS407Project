package com.cs407.badgerstudy

data class Favorite(
    val id: String = "",
    val locationName: String = "",     // Name of the location
    val latitude: Double = 0.0,        // Latitude of the location
    val longitude: Double = 0.0        // Longitude of the location
)
