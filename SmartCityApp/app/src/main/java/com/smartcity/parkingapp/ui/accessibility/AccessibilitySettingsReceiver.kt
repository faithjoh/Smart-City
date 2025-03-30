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
            applyTextSize(view, app.getTextSize())
        }
        
        // High contrast mode requires recreating the activity, handled by the activity itself
    }
    
    private fun applyTextSize(view: View, scale: Float) {
        if (view is TextView) {
            // 修复: 使用绝对文本大小而不是基于当前尺寸的相对比例
            val defaultSize = view.textSize / view.paint.density  // 转换为sp
            view.textSize = defaultSize * scale  // 应用缩放比例
        } else if (view is ViewGroup) {
            // Process all child views recursively
            for (i in 0 until view.childCount) {
                applyTextSize(view.getChildAt(i), scale)
            }
        }
    }
} 