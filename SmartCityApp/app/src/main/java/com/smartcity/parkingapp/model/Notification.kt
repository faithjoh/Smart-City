package com.smartcity.parkingapp.model

import java.util.Date

// Notification types
enum class NotificationType {
    INFO,           // General information
    PAYMENT_SUCCESS, // Payment successful
    PAYMENT_DUE,     // Payment reminder for unpaid orders
    PARKING_ENTRY,   // Vehicle entry notification
    PARKING_EXIT,    // Vehicle exit notification
    ALERT,          // Alert notification
    SYSTEM          // System notification
}

// Notification model
data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.INFO,
    val timestamp: Date = Date(),
    var isRead: Boolean = false,
    val data: Map<String, Any> = mapOf() // Flexible data for different notification types
) 