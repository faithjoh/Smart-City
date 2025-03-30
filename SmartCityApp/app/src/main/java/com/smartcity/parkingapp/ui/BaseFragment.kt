package com.smartcity.parkingapp.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.smartcity.parkingapp.SmartCityApplication

/**
 * Base Fragment class that handles accessibility features for all fragments
 */
abstract class BaseFragment : Fragment() {

    protected lateinit var app: SmartCityApplication
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = requireActivity().application as SmartCityApplication
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Apply current accessibility settings to the fragment
        applyTextSize(view, app.getTextSize())
        
        // Additional accessibility setup can be added here
    }
    
    /**
     * Apply text size scale to all TextView instances in the view
     */
    protected fun applyTextSize(view: View, scale: Float) {
        if (view is TextView) {
            view.textSize = view.textSize * scale / view.textSize
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyTextSize(view.getChildAt(i), scale)
            }
        }
    }
    
    /**
     * Provides enhanced accessibility descriptions for views
     */
    protected fun setAccessibilityDescriptions(view: View) {
        // Override in subclasses to add specific content descriptions
    }
    
    /**
     * Add focus order for screen readers using nextFocusDown, nextFocusUp, etc.
     */
    protected fun setScreenReaderFocusOrder(views: List<View>) {
        for (i in 0 until views.size - 1) {
            views[i].nextFocusDownId = views[i + 1].id
            views[i + 1].nextFocusUpId = views[i].id
        }
    }
    
    /**
     * Check if high contrast mode is enabled
     */
    protected fun isHighContrastEnabled(): Boolean {
        return app.isHighContrastEnabled()
    }
    
    /**
     * Check if reduced motion is enabled
     */
    protected fun isReducedMotionEnabled(): Boolean {
        return app.isReducedMotionEnabled()
    }
    
    /**
     * Apply special mode for dyslexic users if enabled
     */
    protected fun applyDyslexicFontIfEnabled(view: View) {
        if (app.isDyslexicFontEnabled()) {
            // Ideally, we would load a dyslexic-friendly font and apply it to all TextViews
            // This would require custom font implementation
        }
    }
} 