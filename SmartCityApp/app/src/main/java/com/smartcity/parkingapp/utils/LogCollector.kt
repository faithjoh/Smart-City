package com.smartcity.parkingapp.utils

import android.content.Context
import android.location.Location
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * LogCollector is responsible for collecting and storing usage logs,
 * device information, and location data for analytics and service improvement.
 * This class follows GDPR and UK data protection requirements.
 */
class LogCollector private constructor(private val context: Context) {

    private val TAG = "LogCollector"
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val deviceId: String
    
    // Session ID to group logs from the same session
    private val sessionId = UUID.randomUUID().toString()
    
    // Time format for logs
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.UK).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    init {
        // Get device ID using Android ID (requires permission already in manifest)
        deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        Log.d(TAG, "LogCollector initialized with session ID: $sessionId")
    }
    
    /**
     * Logs an activity event without location data
     * @param activity The activity or action being performed
     * @param details Additional details about the activity (optional)
     */
    fun logActivity(activity: String, details: Map<String, String> = mapOf()) {
        val userId = auth.currentUser?.uid
        
        val logData = hashMapOf(
            "userId" to (userId ?: "anonymous"),
            "timestamp" to timeFormat.format(Date()),
            "sessionId" to sessionId,
            "deviceId" to deviceId,
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "osVersion" to "Android ${Build.VERSION.RELEASE}",
            "activityType" to activity
        )
        
        // Add any additional details
        logData.putAll(details)
        
        // Save to Firestore
        firestore.collection("ActivityLogs")
            .add(logData)
            .addOnSuccessListener {
                Log.d(TAG, "Activity log saved: $activity")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving activity log: ${e.message}")
            }
    }
    
    /**
     * Log specific activity with boolean parameter
     * @param activity The activity or action being performed
     * @param paramName The name of the boolean parameter
     * @param paramValue The value of the boolean parameter
     */
    fun logActivityWithBoolean(activity: String, paramName: String, paramValue: Boolean) {
        logActivity(activity, mapOf(paramName to paramValue.toString()))
    }
    
    /**
     * Log specific activity with numeric parameter
     * @param activity The activity or action being performed
     * @param paramName The name of the numeric parameter
     * @param paramValue The value of the numeric parameter
     */
    fun logActivityWithNumber(activity: String, paramName: String, paramValue: Number) {
        logActivity(activity, mapOf(paramName to paramValue.toString()))
    }
    
    /**
     * Logs a location-based activity
     * @param activity The activity or action being performed
     * @param location The location where the activity occurred
     * @param details Additional details about the activity (optional)
     */
    fun logLocationActivity(activity: String, location: Location, details: Map<String, Serializable> = mapOf()) {
        val userId = auth.currentUser?.uid
        
        val logData = hashMapOf<String, Any>(
            "userId" to (userId ?: "anonymous"),
            "timestamp" to timeFormat.format(Date()),
            "sessionId" to sessionId,
            "deviceId" to deviceId,
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "osVersion" to "Android ${Build.VERSION.RELEASE}",
            "activityType" to activity,
            "location" to hashMapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "accuracy" to location.accuracy
            )
        )
        
        // Add any additional details that are Serializable
        details.forEach { (key, value) ->
            logData[key] = value
        }
        
        // Save to Firestore
        firestore.collection("LocationLogs")
            .add(logData)
            .addOnSuccessListener {
                Log.d(TAG, "Location log saved: $activity at ${location.latitude},${location.longitude}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving location log: ${e.message}")
            }
    }
    
    /**
     * Log application errors
     * @param errorType Type of error
     * @param message Error message
     * @param stackTrace Stack trace if available
     */
    fun logError(errorType: String, message: String, stackTrace: String? = null) {
        val userId = auth.currentUser?.uid
        
        val logData = hashMapOf(
            "userId" to (userId ?: "anonymous"),
            "timestamp" to timeFormat.format(Date()),
            "sessionId" to sessionId,
            "deviceId" to deviceId,
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "osVersion" to "Android ${Build.VERSION.RELEASE}",
            "errorType" to errorType,
            "errorMessage" to message
        )
        
        if (stackTrace != null) {
            logData["stackTrace"] = stackTrace
        }
        
        // Save to Firestore
        firestore.collection("ErrorLogs")
            .add(logData)
            .addOnSuccessListener {
                Log.d(TAG, "Error log saved: $errorType - $message")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving error log: ${e.message}")
            }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: LogCollector? = null
        
        fun getInstance(context: Context): LogCollector {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LogCollector(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
} 