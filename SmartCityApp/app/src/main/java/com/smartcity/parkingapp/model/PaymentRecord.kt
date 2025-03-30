package com.smartcity.parkingapp.model

import java.util.Date

/**
 * Data class representing a payment record in the parking system.
 * Contains information about a parking order including license plate,
 * entry/exit times, fee amount and payment status.
 */
data class PaymentRecord(
    val orderId: String,
    val licensePlate: String,
    val entryTime: Date,
    val exitTime: Date,
    val fee: Double,
    var isPaid: Boolean  // Changed from val to var to allow updating payment status
) 