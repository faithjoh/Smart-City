package com.smartcity.parkingapp.ui

import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smartcity.parkingapp.SmartCityApplication
import com.smartcity.parkingapp.ui.accessibility.AccessibilitySettingsReceiver
import android.content.Context

/**
 * Base activity class containing shared accessibility features
 */
abstract class BaseActivity : AppCompatActivity() {
    
    protected lateinit var app: SmartCityApplication
    private val accessibilityReceiver = AccessibilitySettingsReceiver()
    
    companion object {
        // Define action constant here instead of referencing from FloatingAccessibilityService
        const val ACTION_ACCESSIBILITY_SETTINGS_CHANGED = "com.smartcity.parkingapp.ACTION_ACCESSIBILITY_SETTINGS_CHANGED"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        app = application as SmartCityApplication
        
        // Apply high contrast theme
        if (app.isHighContrastEnabled()) {
            setTheme(com.smartcity.parkingapp.R.style.Theme_SmartCityApp_HighContrast)
        }
        
        super.onCreate(savedInstanceState)
    }
    
    override fun onStart() {
        super.onStart()
        
        // Register accessibility settings change receiver
        val filter = IntentFilter(ACTION_ACCESSIBILITY_SETTINGS_CHANGED)
        registerReceiver(accessibilityReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        
        // Initialize receiver
        val rootView = window.decorView.findViewById<View>(android.R.id.content)
        accessibilityReceiver.initialize(rootView, app)
        
        // Apply current accessibility settings
        applyTextSize(rootView, app.getTextSize())
    }
    
    override fun onStop() {
        super.onStop()
        
        // Unregister receiver
        try {
            unregisterReceiver(accessibilityReceiver)
        } catch (e: Exception) {
            // Ignore exception if not registered
        }
    }
    
    /**
     * Recursively apply text size to all text views
     */
    protected fun applyTextSize(view: View, scale: Float) {
        if (view is TextView) {
            val defaultSize = view.textSize / view.paint.density  // 转换为sp
            view.textSize = defaultSize * scale  // 应用缩放比例
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyTextSize(view.getChildAt(i), scale)
            }
        }
    }
} 