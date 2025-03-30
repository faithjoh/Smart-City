package com.smartcity.parkingapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartcity.parkingapp.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    
    private lateinit var welcomeText: TextView
    private lateinit var nearestParkingCard: CardView
    private lateinit var parkingName: TextView
    private lateinit var parkingAddress: TextView
    private lateinit var parkingDistance: TextView
    private lateinit var viewDetailsButton: Button
    private lateinit var tutorialCard: CardView
    private lateinit var dismissTutorialButton: Button
    private lateinit var notificationBadge: TextView
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    // Save the nearest car park id
    private var nearestParkingId: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize view components
        welcomeText = view.findViewById(R.id.welcome_text)
        nearestParkingCard = view.findViewById(R.id.nearest_parking_card)
        parkingName = view.findViewById(R.id.parking_name)
        parkingAddress = view.findViewById(R.id.parking_address)
        parkingDistance = view.findViewById(R.id.parking_distance)
        viewDetailsButton = view.findViewById(R.id.view_details_button)
        tutorialCard = view.findViewById(R.id.tutorial_card)
        dismissTutorialButton = view.findViewById(R.id.dismiss_tutorial_button)
        notificationBadge = view.findViewById(R.id.notification_badge)
        
        // Set up listeners and load data
        setupListeners()
        loadUserData()
        loadNearestParkingSpot()
        loadUnreadNotificationsCount()
        checkTutorialPreference()
    }
    
    private fun setupListeners() {
        // Set up the nearest parking spot card click listener
        viewDetailsButton.setOnClickListener {
            if (nearestParkingId != null) {
                // Navigate to map fragment with specific parking spot ID
                val mainActivity = activity as? MainActivity
                mainActivity?.navigateToMapFragmentWithParkingId(nearestParkingId!!)
            } else {
                Toast.makeText(context, "Unable to retrieve parking information", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Set up tutorial buttons
        view?.findViewById<Button>(R.id.start_tutorial_button)?.setOnClickListener {
            startTutorial()
        }
        
        dismissTutorialButton.setOnClickListener {
            tutorialCard.visibility = View.GONE
            // Save user preference
            saveTutorialDismissed()
        }
        
        // Set up notification view all button
        view?.findViewById<Button>(R.id.view_all_notifications_button)?.setOnClickListener {
            val intent = Intent(context, NotificationsActivity::class.java)
            startActivity(intent)
        }
        
        // Make entire notification preview area clickable to navigate to notifications
        view?.findViewById<View>(R.id.notification_text_1)?.setOnClickListener {
            val intent = Intent(context, NotificationsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadUserData() {
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            // Get user data from Firestore
            db.collection("Users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userName = document.getString("name") ?: "User"
                        welcomeText.text = "Welcome back, $userName"
                    }
                }
                .addOnFailureListener { e ->
                    // Handle error
                    welcomeText.text = "Welcome to Smart City Parking"
                }
        } else {
            welcomeText.text = "Welcome to Smart City Parking"
        }
    }
    
    private fun loadNearestParkingSpot() {
        // In a real app, this would get the user's location and find the closest parking spot
        // For now, we'll just load a sample parking spot from Firestore
        
        db.collection("ParkingSpots")
            .limit(1) // Just get one parking spot for now
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val parkingSpot = documents.documents[0]
                    
                    // Save parking spot ID for later use
                    nearestParkingId = parkingSpot.id
                    
                    parkingName.text = parkingSpot.getString("name") ?: "London Central Parking"
                    parkingAddress.text = parkingSpot.getString("address") ?: "123 Oxford Street, London"
                    
                    // In a real app, we would calculate the distance based on user's location
                    val availableSlots = parkingSpot.getLong("totalSlots")?.toInt() ?: 0
                    parkingDistance.text = "0.5 miles away - $availableSlots spots available"
                    
                    // Make the card visible
                    nearestParkingCard.visibility = View.VISIBLE
                } else {
                    // No parking spots found, hide the card
                    nearestParkingCard.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                // Handle error
                nearestParkingCard.visibility = View.GONE
            }
    }
    
    private fun loadUnreadNotificationsCount() {
        val currentUser = auth.currentUser ?: return
        
        // Query Firestore for unread notifications
        db.collection("Notifications")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { documents ->
                val unreadCount = documents.size()
                
                if (unreadCount > 0) {
                    // Show badge with count
                    notificationBadge.visibility = View.VISIBLE
                    notificationBadge.text = if (unreadCount > 9) "9+" else unreadCount.toString()
                    
                    // Update the preview with the most recent notification
                    if (!documents.isEmpty) {
                        val latestNotification = documents.documents
                            .sortedByDescending { it.getDate("timestamp") }
                            .firstOrNull()
                        
                        latestNotification?.let { notification ->
                            // Update notification preview
                            updateNotificationPreview(notification.getString("title") ?: "",
                                                    notification.getString("message") ?: "",
                                                    notification.getDate("timestamp"))
                        }
                    }
                } else {
                    // Hide badge if no unread notifications
                    notificationBadge.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                // On error, hide badge
                notificationBadge.visibility = View.GONE
            }
    }
    
    private fun updateNotificationPreview(title: String, message: String, timestamp: Date?) {
        // Update the preview notification in Home screen
        val notificationText = view?.findViewById<TextView>(R.id.notification_text_1)
        val notificationTime = view?.findViewById<TextView>(R.id.notification_time_1)
        
        notificationText?.text = message
        
        // Format the time
        timestamp?.let {
            val now = Date()
            val diffInMillis = now.time - it.time
            
            val timeAgo = when {
                diffInMillis < 60 * 1000 -> "Just now"
                diffInMillis < 60 * 60 * 1000 -> "${diffInMillis / (60 * 1000)} minutes ago"
                diffInMillis < 24 * 60 * 60 * 1000 -> "${diffInMillis / (60 * 60 * 1000)} hours ago"
                else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
            }
            
            notificationTime?.text = timeAgo
        }
    }
    
    private fun checkTutorialPreference() {
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            // Check if user has dismissed the tutorial
            db.collection("Users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val tutorialDismissed = document.getBoolean("tutorialDismissed") ?: false
                        tutorialCard.visibility = if (tutorialDismissed) View.GONE else View.VISIBLE
                    } else {
                        tutorialCard.visibility = View.VISIBLE
                    }
                }
                .addOnFailureListener { e ->
                    // Handle error, default to showing tutorial
                    tutorialCard.visibility = View.VISIBLE
                }
        } else {
            // Not logged in, show tutorial
            tutorialCard.visibility = View.VISIBLE
        }
    }
    
    private fun saveTutorialDismissed() {
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            // Save that user has dismissed the tutorial
            db.collection("Users").document(currentUser.uid)
                .update("tutorialDismissed", true)
                .addOnFailureListener { e ->
                    // Handle error
                }
        }
    }
    
    private fun startTutorial() {
        // Start the tutorial activity
        val intent = Intent(context, TutorialActivity::class.java)
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        loadUserData()
        loadNearestParkingSpot()
        loadUnreadNotificationsCount() // Reload notification count when returning to home
    }
} 