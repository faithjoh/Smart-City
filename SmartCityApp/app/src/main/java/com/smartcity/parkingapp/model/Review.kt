package com.smartcity.parkingapp.model

import com.google.firebase.Timestamp
import java.io.Serializable

data class Review(
    var id: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val date: String = "",
    val spotId: String = "",
    val timestamp: Timestamp = Timestamp.now()
) : Serializable 