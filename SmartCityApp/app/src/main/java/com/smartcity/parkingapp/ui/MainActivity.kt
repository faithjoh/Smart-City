package com.smartcity.parkingapp.ui

import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.smartcity.parkingapp.R
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.content.Intent
import com.smartcity.parkingapp.SmartCityApplication
import androidx.coordinatorlayout.widget.CoordinatorLayout

class MainActivity : BaseActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get application instance for accessibility settings
        app = application as SmartCityApplication
        
        // Apply high contrast theme if enabled
        if (app.isHighContrastEnabled()) {
            setTheme(R.style.Theme_SmartCityApp_HighContrast)
        }
        
        setContentView(R.layout.activity_main)
        
        auth = FirebaseAuth.getInstance()
        
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        
        // Set up navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.navigation_map -> {
                    loadFragment(MapFragment())
                    true
                }
                R.id.navigation_payment -> {
                    loadFragment(PaymentFragment())
                    true
                }
                R.id.navigation_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        // Set default fragment
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.navigation_home
        }
        
        // Apply current text size to all views
        applyTextSize(app.getTextSize())
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    /**
     * Shows the accessibility settings dialog
     */
    fun showAccessibilityDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_accessibility, null)
        
        // Get references to dialog views
        val textSizeSeekBar = dialogView.findViewById<SeekBar>(R.id.text_size_seekbar)
        val highContrastSwitch = dialogView.findViewById<SwitchCompat>(R.id.high_contrast_switch)
        val reducedMotionSwitch = dialogView.findViewById<SwitchCompat>(R.id.reduced_motion_switch)
        val dyslexicFontSwitch = dialogView.findViewById<SwitchCompat>(R.id.dyslexic_font_switch)
        
        // Set initial values based on saved preferences
        textSizeSeekBar.progress = getTextSizeProgress(app.getTextSize())
        highContrastSwitch.isChecked = app.isHighContrastEnabled()
        reducedMotionSwitch.isChecked = app.isReducedMotionEnabled()
        dyslexicFontSwitch.isChecked = app.isDyslexicFontEnabled()
        
        // Create and show dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.accessibility_options)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                // Save settings and apply changes
                saveAccessibilitySettings(
                    getTextSizeFromProgress(textSizeSeekBar.progress),
                    highContrastSwitch.isChecked,
                    reducedMotionSwitch.isChecked,
                    dyslexicFontSwitch.isChecked
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            
        dialog.show()
        
        // Set up live preview for text size changes
        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Preview text size change
                val newSize = getTextSizeFromProgress(progress)
                applyTextSize(newSize)
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }
    
    private fun saveAccessibilitySettings(textSize: Float, highContrast: Boolean, 
                                         reducedMotion: Boolean, dyslexicFont: Boolean) {
        // Save settings to preferences
        app.setTextSize(textSize)
        app.setHighContrast(highContrast)
        app.setReducedMotion(reducedMotion)
        app.setDyslexicFont(dyslexicFont)
        
        // Apply text size
        applyTextSize(textSize)
        
        // Apply high contrast (requires activity restart)
        if (highContrast != app.isHighContrastEnabled()) {
            recreate()
        }
        
        // Apply dyslexic font if enabled
        if (dyslexicFont) {
            Toast.makeText(this, R.string.dyslexic_font_enabled, Toast.LENGTH_SHORT).show()
            // Actual font application would require custom TextView or custom font loading
        }
        
        // Show confirmation of changes
        if (highContrast) {
            Toast.makeText(this, R.string.high_contrast_enabled, Toast.LENGTH_SHORT).show()
        }
        
        if (reducedMotion) {
            Toast.makeText(this, R.string.reduced_motion_enabled, Toast.LENGTH_SHORT).show()
        }
        
        // Broadcast settings changed to notify other components
        val settingsChangedIntent = Intent(BaseActivity.ACTION_ACCESSIBILITY_SETTINGS_CHANGED)
        sendBroadcast(settingsChangedIntent)
    }
    
    // Helper function to map text size to seek bar progress (0-3)
    private fun getTextSizeProgress(textSize: Float): Int {
        return when {
            textSize <= SmartCityApplication.TEXT_SIZE_SMALL -> 0
            textSize <= SmartCityApplication.TEXT_SIZE_NORMAL -> 1
            textSize <= SmartCityApplication.TEXT_SIZE_LARGE -> 2
            else -> 3
        }
    }
    
    // Helper function to map seek bar progress (0-3) to text size
    private fun getTextSizeFromProgress(progress: Int): Float {
        return when (progress) {
            0 -> SmartCityApplication.TEXT_SIZE_SMALL
            1 -> SmartCityApplication.TEXT_SIZE_NORMAL
            2 -> SmartCityApplication.TEXT_SIZE_LARGE
            3 -> SmartCityApplication.TEXT_SIZE_EXTRA_LARGE
            else -> SmartCityApplication.TEXT_SIZE_NORMAL
        }
    }
    
    // Apply text size scaling to all text views in the activity
    private fun applyTextSize(scale: Float) {
        val rootView = window.decorView.findViewById<View>(android.R.id.content)
        applyTextSizeToView(rootView, scale)
    }
    
    // Recursively apply text size to all TextView instances
    private fun applyTextSizeToView(view: View, scale: Float) {
        if (view is TextView) {
            val defaultSize = view.textSize / view.paint.density  // 转换为sp
            view.textSize = defaultSize * scale  // 应用缩放比例
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyTextSizeToView(view.getChildAt(i), scale)
            }
        }
    }
    
    // Helper function to check if the device is in night mode
    private fun isNightMode(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == 
                Configuration.UI_MODE_NIGHT_YES
    }
    
    // Check if user is logged in, if not redirect to login page
    private fun checkUserLoggedIn() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in, redirect to login
            val intent = android.content.Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    // Method to navigate to the map fragment
    fun navigateToMapFragment() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_map
    }
    
    // Method to navigate to map fragment and show specific parking spot details
    fun navigateToMapFragmentWithParkingId(parkingId: String) {
        // Navigate to map fragment first
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.selectedItemId = R.id.navigation_map
        
        // Get the current fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        
        // If it's a MapFragment, tell it to show details for this parking spot
        if (currentFragment is MapFragment) {
            currentFragment.showParkingSpotDetails(parkingId)
        } else {
            // Store the ID to be used when the map fragment is created
            val bundle = Bundle()
            bundle.putString("parking_spot_id", parkingId)
            
            val mapFragment = MapFragment()
            mapFragment.arguments = bundle
            
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, mapFragment)
                .commit()
        }
    }
    
    override fun onStart() {
        super.onStart()
        checkUserLoggedIn()
    }
    
    private fun applyAccessibilitySettings() {
        // Apply text size
        applyTextSize(app.getTextSize())
        
        // If high contrast mode changed, need to recreate activity
        if (app.isHighContrastEnabled()) {
            recreate()
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
} 