package com.smartcity.parkingapp.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smartcity.parkingapp.R
import com.smartcity.parkingapp.adapter.TutorialPagerAdapter
import com.smartcity.parkingapp.databinding.ActivityTutorialBinding

/**
 * Tutorial Activity that guides new users through the features of Smart City Parking app.
 * Shows a series of screens with information about key app features.
 */
class TutorialActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button
    private lateinit var btnSkip: Button
    private lateinit var btnFinish: Button
    private lateinit var tutorialHeading: TextView
    private lateinit var tutorialDescription: TextView
    private lateinit var tutorialImage: ImageView
    private lateinit var binding: ActivityTutorialBinding
    
    // Tutorial content - titles, descriptions and indicators for each step
    private val tutorialTitles = arrayOf(
        "Welcome to Smart City Parking",
        "Find Available Parking Spaces",
        "Pay for Parking Easily",
        "Get Notifications",
        "All Set!"
    )
    
    private val tutorialDescriptions = arrayOf(
        "Smart City Parking helps you find and pay for parking spaces in the city. Let's see how it works!",
        "Use the map to find available parking spaces near you. Green markers indicate available spaces, while red markers show occupied spaces.",
        "Pay for your parking directly through the app using your saved payment methods. No need for cash or parking meters!",
        "Receive notifications about your parking status, payment reminders, and available spaces near your favorite locations.",
        "You're all set! Start using Smart City Parking to make your parking experience easier and more convenient."
    )
    
    private var currentPosition = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupAccessibilityFeatures()
        
        // Initialize views
        viewPager = binding.tutorialPager
        tabLayout = binding.tabIndicator
        btnNext = binding.btnNext
        btnPrevious = binding.btnPrevious
        btnSkip = binding.btnSkip
        btnFinish = binding.btnFinish
        tutorialHeading = binding.tutorialHeading
        tutorialDescription = binding.tutorialDescription
        tutorialImage = binding.tutorialImage
        
        // Set up view pager
        setupViewPager()
        
        // Set up buttons
        setupButtons()
        
        // Update UI for first page
        updateUI(0)
    }
    
    private fun setupViewPager() {
        // Create and set adapter
        val adapter = TutorialPagerAdapter(this, tutorialTitles.size)
        viewPager.adapter = adapter
        
        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { _, _ ->
            // No configuration needed for the tabs, they're just indicators
        }.attach()
        
        // Listen for page changes
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
                updateUI(position)
            }
        })
    }
    
    private fun setupButtons() {
        btnNext.setOnClickListener {
            if (currentPosition < tutorialTitles.size - 1) {
                viewPager.currentItem = currentPosition + 1
            }
        }
        
        btnPrevious.setOnClickListener {
            if (currentPosition > 0) {
                viewPager.currentItem = currentPosition - 1
            }
        }
        
        btnSkip.setOnClickListener {
            finishTutorial()
        }
        
        btnFinish.setOnClickListener {
            finishTutorial()
        }
    }
    
    private fun updateUI(position: Int) {
        // Update text content
        tutorialHeading.text = tutorialTitles[position]
        tutorialDescription.text = tutorialDescriptions[position]
        
        // Update image based on position with our custom vector drawables
        when (position) {
            0 -> tutorialImage.setImageResource(R.drawable.tutorial_welcome_icon)
            1 -> tutorialImage.setImageResource(R.drawable.tutorial_map_icon)
            2 -> tutorialImage.setImageResource(R.drawable.tutorial_payment_icon)
            3 -> tutorialImage.setImageResource(R.drawable.tutorial_notification_icon)
            4 -> tutorialImage.setImageResource(R.drawable.tutorial_complete_icon)
        }
        
        // Update button visibility
        btnPrevious.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
        btnSkip.visibility = if (position < tutorialTitles.size - 1) View.VISIBLE else View.GONE
        btnNext.visibility = if (position < tutorialTitles.size - 1) View.VISIBLE else View.GONE
        btnFinish.visibility = if (position == tutorialTitles.size - 1) View.VISIBLE else View.GONE
    }
    
    private fun finishTutorial() {
        // Return to the main activity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun setupAccessibilityFeatures() {
        // Set up text size increase button
        binding.btnIncreaseText.setOnClickListener {
            increaseTextSize()
        }
        
        // Set up high contrast mode button
        binding.btnHighContrast.setOnClickListener {
            toggleHighContrastMode()
        }
    }
    
    private fun increaseTextSize() {
        // Toggle between normal and large text
        val currentSize = binding.tutorialHeading.textSize
        val scaleFactor = if (currentSize > 30f) 0.75f else 1.25f
        
        // Scale heading text
        binding.tutorialHeading.textSize *= scaleFactor
        
        // Scale description text
        binding.tutorialDescription.textSize *= scaleFactor
        
        // Scale button text
        binding.btnPrevious.textSize = binding.btnPrevious.textSize * scaleFactor
        binding.btnNext.textSize = binding.btnNext.textSize * scaleFactor
        binding.btnSkip.textSize = binding.btnSkip.textSize * scaleFactor
        binding.btnFinish.textSize = binding.btnFinish.textSize * scaleFactor
        
        // Announce the change for screen readers
        val message = if (scaleFactor > 1) getString(R.string.text_size_increased) else getString(R.string.text_size_normal)
        binding.root.announceForAccessibility(message)
    }
    
    private fun toggleHighContrastMode() {
        val isHighContrast = binding.tutorialHeading.currentTextColor == Color.WHITE
        
        if (isHighContrast) {
            // Restore normal contrast
            binding.root.setBackgroundColor(Color.parseColor("#F8FDF8"))
            binding.tutorialHeading.setTextColor(Color.parseColor("#2E7D32"))
            binding.tutorialDescription.setTextColor(Color.parseColor("#555555"))
            binding.btnNext.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            binding.btnFinish.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        } else {
            // Set high contrast
            binding.root.setBackgroundColor(Color.BLACK)
            binding.tutorialHeading.setTextColor(Color.WHITE)
            binding.tutorialDescription.setTextColor(Color.WHITE)
            binding.btnNext.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            binding.btnFinish.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            binding.btnNext.setTextColor(Color.BLACK)
            binding.btnFinish.setTextColor(Color.BLACK)
        }
        
        // Announce the change for screen readers
        val message = if (isHighContrast) getString(R.string.high_contrast_disabled) else getString(R.string.high_contrast_enabled)
        binding.root.announceForAccessibility(message)
    }
} 