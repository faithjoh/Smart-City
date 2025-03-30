package com.smartcity.parkingapp.ui

import android.content.Context
import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView

/**
 * Utility class containing helper methods for accessibility features
 */
object AccessibilityUtility {

    /**
     * Checks if a screen reader (like TalkBack) is running
     */
    fun isScreenReaderActive(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled
    }

    /**
     * Sets up a view for better screen reader support
     */
    fun enhanceAccessibility(view: View) {
        when (view) {
            is Button -> {
                if (view.contentDescription == null) {
                    view.contentDescription = view.text
                }
            }
            is ImageView -> {
                if (view.contentDescription == null || view.contentDescription.isEmpty()) {
                    // Warning for developers - images should have content descriptions
                    view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            }
            is EditText -> {
                if (view.hint != null && view.contentDescription == null) {
                    view.contentDescription = view.hint
                }
            }
        }
        
        // Make sure view is focusable by screen readers
        view.isFocusable = true
        
        // Process child views if this is a container
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                enhanceAccessibility(view.getChildAt(i))
            }
        }
    }
    
    /**
     * Increases touch target size for better accessibility
     * This helps users with motor impairments to touch buttons more easily
     */
    fun increaseTouchTarget(view: View, extraPadding: Int = 48) {
        view.post {
            val parent = view.parent as? View ?: return@post
            
            // Get the display area of the parent view
            val rect = Rect()
            view.getHitRect(rect)
            
            // Expand touch area
            rect.left -= extraPadding
            rect.top -= extraPadding
            rect.right += extraPadding
            rect.bottom += extraPadding
            
            parent.touchDelegate = TouchDelegate(rect, view)
        }
    }
    
    /**
     * Improves text contrast for readability
     */
    fun improveTextContrast(textView: TextView, isHighContrast: Boolean) {
        if (isHighContrast) {
            textView.setTextColor(android.graphics.Color.WHITE)
            textView.setBackgroundColor(android.graphics.Color.BLACK)
            textView.setShadowLayer(0f, 0f, 0f, 0)
        }
    }
    
    /**
     * Announces a message to screen reader users
     */
    fun announceForAccessibility(view: View, message: String) {
        view.announceForAccessibility(message)
    }
} 