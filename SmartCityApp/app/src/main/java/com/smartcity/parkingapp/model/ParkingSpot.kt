package com.smartcity.parkingapp.model

import java.io.Serializable

data class ParkingSpot(
    var id: String = "",
    val name: String = "",
    val address: String = "",
    val rating: Float = 0f,
    val price: Double = 0.0,
    val totalSlots: Int = 0,
    val openHours: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable 