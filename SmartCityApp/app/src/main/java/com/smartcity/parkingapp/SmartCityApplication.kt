package com.smartcity.parkingapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.smartcity.parkingapp.utils.LogCollector

class SmartCityApplication : Application() {

    companion object {
        private const val PREFS_NAME = "smart_city_prefs"
        private const val KEY_TEXT_SIZE = "text_size"
        private const val KEY_HIGH_CONTRAST = "high_contrast"
        private const val KEY_REDUCED_MOTION = "reduced_motion"
        private const val KEY_DYSLEXIC_FONT = "dyslexic_font"
        
        // Default accessibility values
        const val DEFAULT_TEXT_SIZE = 1.0f  // Normal text size scale
        const val DEFAULT_HIGH_CONTRAST = false
        const val DEFAULT_REDUCED_MOTION = false
        const val DEFAULT_DYSLEXIC_FONT = false
        
        // Text size scales
        const val TEXT_SIZE_SMALL = 0.85f
        const val TEXT_SIZE_NORMAL = 1.0f
        const val TEXT_SIZE_LARGE = 1.2f
        const val TEXT_SIZE_EXTRA_LARGE = 1.5f
    }
    
    private lateinit var preferences: SharedPreferences
    private lateinit var logCollector: LogCollector
    
    private val TAG = "SmartCityApplication"

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            
            // Initialize Firestore
            val db = FirebaseFirestore.getInstance()
            
            // Firestore includes built-in caching, no need for additional persistence setup
            
            preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // Initialize LogCollector
            logCollector = LogCollector.getInstance(this)
            
            Log.d("SmartCityApp", "Application initialized with accessibility settings")
            
            // Log application start
            logCollector.logActivity("app_start", mapOf("version" to BuildConfig.VERSION_NAME))
            
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
        }
    }
    
    /**
     * Get the LogCollector instance for logging user activities
     */
    fun getLogCollector(): LogCollector {
        return logCollector
    }
    
    // Accessibility preference getters and setters
    fun getTextSize(): Float = preferences.getFloat(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
    
    fun setTextSize(size: Float) {
        preferences.edit().putFloat(KEY_TEXT_SIZE, size).apply()
    }
    
    fun isHighContrastEnabled(): Boolean = preferences.getBoolean(KEY_HIGH_CONTRAST, DEFAULT_HIGH_CONTRAST)
    
    fun setHighContrast(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HIGH_CONTRAST, enabled).apply()
    }
    
    fun isReducedMotionEnabled(): Boolean = preferences.getBoolean(KEY_REDUCED_MOTION, DEFAULT_REDUCED_MOTION)
    
    fun setReducedMotion(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_REDUCED_MOTION, enabled).apply()
    }
    
    fun isDyslexicFontEnabled(): Boolean = preferences.getBoolean(KEY_DYSLEXIC_FONT, DEFAULT_DYSLEXIC_FONT)
    
    fun setDyslexicFont(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DYSLEXIC_FONT, enabled).apply()
    }
} 