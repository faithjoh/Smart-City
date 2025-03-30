package com.smartcity.parkingapp.ui.accessibility

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.smartcity.parkingapp.SmartCityApplication
import com.smartcity.parkingapp.ui.BaseActivity

/**
 * Receives accessibility settings change broadcasts and applies the changes in activities
 */
class AccessibilitySettingsReceiver : BroadcastReceiver() {
    
    private var rootView: View? = null
    private var app: SmartCityApplication? = null
    
    fun initialize(rootView: View, app: SmartCityApplication) {
        this.rootView = rootView
        this.app = app
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == BaseActivity.ACTION_ACCESSIBILITY_SETTINGS_CHANGED) {
            // Application received accessibility settings change broadcast
            app?.let { application ->
                applyAccessibilitySettings(application)
            }
        }
    }
    
    private fun applyAccessibilitySettings(app: SmartCityApplication) {
        // Apply text size
        rootView?.let { view ->
            applyTextSizeToView(view, app.getTextSize())
        }
        
        // High contrast mode requires recreating the activity, handled by the activity itself
    }
    
    private fun applyTextSizeToView(view: View, scale: Float) {
        if (view is TextView) {
            // Fix: Use absolute text size instead of relative scaling based on current size
            val defaultSize = view.textSize / view.paint.density  // Convert to sp
            view.textSize = defaultSize * scale  // Apply scaling factor
        } else if (view is ViewGroup) {
            // Process all child views recursively
            for (i in 0 until view.childCount) {
                applyTextSizeToView(view.getChildAt(i), scale)
            }
        }
    }
} 