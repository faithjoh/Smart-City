package com.smartcity.parkingapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartcity.parkingapp.R
import com.smartcity.parkingapp.adapter.NotificationAdapter
import com.smartcity.parkingapp.model.Notification
import com.smartcity.parkingapp.model.NotificationType
import java.text.SimpleDateFormat
import java.util.*

class NotificationsActivity : AppCompatActivity(), NotificationAdapter.OnNotificationClickListener {

    private val TAG = "NotificationsActivity"
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyView: View
    private lateinit var progressBar: View
    
    private val notifications = mutableListOf<Notification>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        
        // Initialize views
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notifications"
        
        recyclerView = findViewById(R.id.notifications_recycler_view)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        emptyView = findViewById(R.id.empty_view)
        progressBar = findViewById(R.id.progress_bar)
        
        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        notificationAdapter = NotificationAdapter(notifications, this)
        recyclerView.adapter = notificationAdapter
        
        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            loadNotifications()
        }
        
        // Load notifications
        loadNotifications()
        
        // For demo purposes, create some sample notifications if there are none
        createSampleNotificationsIfEmpty()
        
        // Set up automatic refresh every 30 seconds to check for new notifications
        setupAutoRefresh()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun loadNotifications() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showEmptyView(true)
            swipeRefreshLayout.isRefreshing = false
            return
        }
        
        progressBar.visibility = View.VISIBLE
        
        // Use composite index for better performance
        db.collection("Notifications")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                notifications.clear()
                
                if (documents.isEmpty) {
                    showEmptyView(true)
                } else {
                    for (document in documents) {
                        val id = document.id
                        val title = document.getString("title") ?: ""
                        val message = document.getString("message") ?: ""
                        val typeStr = document.getString("type") ?: "INFO"
                        val type = try {
                            NotificationType.valueOf(typeStr)
                        } catch (e: IllegalArgumentException) {
                            NotificationType.INFO
                        }
                        val timestamp = document.getTimestamp("timestamp") ?: Timestamp.now()
                        val isRead = document.getBoolean("isRead") ?: false
                        val data = document.get("data") as? Map<String, Any> ?: mapOf()
                        
                        val notification = Notification(
                            id = id,
                            title = title,
                            message = message,
                            type = type,
                            timestamp = timestamp.toDate(),
                            isRead = isRead,
                            data = data
                        )
                        
                        notifications.add(notification)
                    }
                    
                    showEmptyView(false)
                    notificationAdapter.notifyDataSetChanged()
                }
                
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                // If index error, show specific message with link
                if (e.message?.contains("requires an index") == true) {
                    Toast.makeText(
                        this,
                        "This query requires a Firestore index. Check logs for details.",
                        Toast.LENGTH_LONG
                    ).show()
                    // The logs will contain a direct link to create the required index
                } else {
                    Toast.makeText(this, "Error loading notifications: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                showEmptyView(true)
            }
    }
    
    private fun showEmptyView(show: Boolean) {
        if (show) {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    // Mark notification as read and show full details
    override fun onNotificationClick(notification: Notification, position: Int) {
        // Mark as read in UI and Firestore
        if (!notification.isRead) {
            notification.isRead = true
            notificationAdapter.notifyItemChanged(position)
            
            // Update in Firestore
            markNotificationAsRead(notification.id)
        }
        
        // First show notification details in a dialog
        showNotificationDetailDialog(notification)
    }
    
    private fun showNotificationDetailDialog(notification: Notification) {
        // Create alert dialog to show full notification details
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_notification_detail, null)
        
        // Set up dialog views
        val titleTextView = dialogView.findViewById<TextView>(R.id.detail_title)
        val messageTextView = dialogView.findViewById<TextView>(R.id.detail_message)
        val timeTextView = dialogView.findViewById<TextView>(R.id.detail_time)
        val iconImageView = dialogView.findViewById<ImageView>(R.id.detail_icon)
        val actionButton = dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.action_button)
        
        // Set dialog content
        titleTextView.text = notification.title
        messageTextView.text = notification.message
        timeTextView.text = formatDetailedTime(notification.timestamp)
        
        // Set icon based on notification type
        when (notification.type) {
            NotificationType.PAYMENT_SUCCESS -> {
                iconImageView.setImageResource(R.drawable.ic_info)
                iconImageView.setColorFilter(ContextCompat.getColor(this, R.color.green))
                actionButton.text = "View Receipt"
            }
            NotificationType.PAYMENT_DUE -> {
                iconImageView.setImageResource(R.drawable.ic_alert)
                iconImageView.setColorFilter(ContextCompat.getColor(this, R.color.red))
                actionButton.text = "Pay Now"
            }
            NotificationType.PARKING_ENTRY -> {
                iconImageView.setImageResource(R.drawable.ic_directions)
                iconImageView.setColorFilter(ContextCompat.getColor(this, R.color.blue))
                actionButton.text = "View Details"
            }
            NotificationType.PARKING_EXIT -> {
                iconImageView.setImageResource(R.drawable.ic_directions)
                iconImageView.setColorFilter(ContextCompat.getColor(this, R.color.orange))
                actionButton.text = "View Details"
            }
            else -> {
                iconImageView.setImageResource(R.drawable.ic_info)
                iconImageView.setColorFilter(ContextCompat.getColor(this, R.color.purple))
                actionButton.text = "OK"
            }
        }
        
        // Build dialog
        builder.setView(dialogView)
        val dialog = builder.create()
        
        // Set close button click listener
        dialogView.findViewById<View>(R.id.close_button).setOnClickListener {
            dialog.dismiss()
        }
        
        // Set action button click listener
        actionButton.setOnClickListener {
            dialog.dismiss()
            handleNotificationAction(notification)
        }
        
        // Show dialog
        dialog.show()
    }
    
    private fun handleNotificationAction(notification: Notification) {
        // Handle actions based on notification type
        when (notification.type) {
            NotificationType.PAYMENT_SUCCESS -> {
                // Navigate to receipt view (which is part of PaymentActivity with VIEW_RECEIPT mode)
                val intent = Intent(this, PaymentActivity::class.java)
                intent.putExtra("VIEW_MODE", "RECEIPT")
                intent.putExtra("paymentId", notification.data["paymentId"] as? String)
                intent.putExtra("amount", (notification.data["amount"] as? Double) ?: 0.0)
                intent.putExtra("licensePlate", notification.data["licensePlate"] as? String)
                startActivity(intent)
            }
            NotificationType.PAYMENT_DUE -> {
                // Navigate to payment screen with correct order information
                val intent = Intent(this, PaymentActivity::class.java)
                intent.putExtra("VIEW_MODE", "PAYMENT")
                intent.putExtra("amount", (notification.data["amount"] as? Double) ?: 0.0)
                intent.putExtra("licensePlate", notification.data["licensePlate"] as? String)
                intent.putExtra("orderId", notification.data["paymentId"] as? String)
                
                // Convert and pass parking name if available
                val parkingName = notification.data["parkingName"] as? String
                if (parkingName != null) {
                    intent.putExtra("parkingName", parkingName)
                }
                
                // We'll handle the timestamp data in PaymentActivity by passing the notification itself
                // since we can't easily pass Timestamp objects via Intent
                intent.putExtra("notificationId", notification.id)
                
                startActivity(intent)
            }
            NotificationType.PARKING_ENTRY -> {
                // Show parking entry information dialog instead of navigating to another activity
                showParkingEventDialog(
                    title = "Vehicle Entry Details",
                    licensePlate = notification.data["licensePlate"] as? String ?: "Unknown",
                    parkingName = notification.data["parkingName"] as? String ?: "Unknown",
                    eventTime = notification.timestamp,
                    isEntry = true
                )
            }
            NotificationType.PARKING_EXIT -> {
                // Show parking exit information dialog instead of navigating to another activity
                showParkingEventDialog(
                    title = "Vehicle Exit Details",
                    licensePlate = notification.data["licensePlate"] as? String ?: "Unknown",
                    parkingName = notification.data["parkingName"] as? String ?: "Unknown",
                    eventTime = notification.timestamp,
                    isEntry = false,
                    duration = notification.data["duration"] as? String
                )
            }
            NotificationType.INFO, NotificationType.ALERT, NotificationType.SYSTEM -> {
                // These notification types do not require additional actions
                Toast.makeText(this, "Notification read", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showParkingEventDialog(
        title: String,
        licensePlate: String,
        parkingName: String,
        eventTime: Date,
        isEntry: Boolean,
        duration: String? = null
    ) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(title)
        
        // Format event time
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        val formattedTime = dateFormat.format(eventTime)
        
        // Build message
        val message = StringBuilder()
        message.append("License Plate: $licensePlate\n\n")
        message.append("Location: $parkingName\n\n")
        
        if (isEntry) {
            message.append("Entry Time: $formattedTime")
        } else {
            message.append("Exit Time: $formattedTime")
            if (duration != null) {
                message.append("\n\nParking Duration: $duration")
            }
        }
        
        builder.setMessage(message.toString())
        
        // Set icon based on entry/exit
        val iconResId = if (isEntry) android.R.drawable.ic_menu_directions else android.R.drawable.ic_menu_directions
        val iconColor = if (isEntry) resources.getColor(R.color.blue, theme) else resources.getColor(R.color.orange, theme)
        val iconDrawable = ContextCompat.getDrawable(this, iconResId)
        iconDrawable?.setTint(iconColor)
        builder.setIcon(iconDrawable)
        
        // Add OK button
        builder.setPositiveButton("OK") { dialog, _ -> 
            dialog.dismiss() 
        }
        
        // Show dialog
        builder.create().show()
    }
    
    private fun formatDetailedTime(timestamp: Date): String {
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        return formatter.format(timestamp)
    }
    
    private fun markNotificationAsRead(notificationId: String) {
        val currentUser = auth.currentUser ?: return
        
        db.collection("Notifications")
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener {
                // Successfully marked as read
                // You could send a broadcast here to update any badge counts in other parts of the app
                // For example:
                sendBroadcast(Intent("com.smartcity.parkingapp.NOTIFICATION_READ"))
            }
            .addOnFailureListener { e ->
                // Error updating notification
                Toast.makeText(this, "Error marking notification as read", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun createSampleNotificationsIfEmpty() {
        val currentUser = auth.currentUser ?: return
        
        db.collection("Notifications")
            .whereEqualTo("userId", currentUser.uid)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // First get the user's latest license plate
                    db.collection("Users").document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            // Try to get license plates as a list first
                            val plates = userDoc.get("licensePlates") as? List<String>
                            
                            // Get the most recent license plate (first in the list) or fallback to single field
                            val latestPlate = if (plates != null && plates.isNotEmpty()) {
                                plates.first()
                            } else {
                                userDoc.getString("licensePlate")
                            }
                            
                            // If user has no license plate, don't create sample notifications
                            if (latestPlate.isNullOrEmpty()) {
                                Log.d(TAG, "No license plate found for user, skipping sample notifications")
                                return@addOnSuccessListener
                            }
                            
                            // Format the time strings for entry/exit times
                            val entryTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                            
                            val exitTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                            
                            // Create welcome notification only
                            val notificationsToCreate = listOf(
                                mapOf(
                                    "userId" to currentUser.uid,
                                    "title" to "Welcome to Smart City Parking",
                                    "message" to "Thank you for using our smart parking system. Find the best parking spots near you!",
                                    "type" to "SYSTEM",
                                    "timestamp" to Timestamp.now(),
                                    "isRead" to false,
                                    "data" to mapOf<String, Any>()
                                )
                            )
                            
                            // Batch write to Firestore
                            val batch = db.batch()
                            
                            notificationsToCreate.forEach { notification ->
                                val newNotificationRef = db.collection("Notifications").document()
                                batch.set(newNotificationRef, notification)
                            }
                            
                            batch.commit()
                                .addOnSuccessListener {
                                    // Successfully created sample notifications, reload
                                    loadNotifications()
                                }
                                .addOnFailureListener { e ->
                                    // Error creating sample notifications
                                    Toast.makeText(this, "Error creating sample notifications", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error getting user license plate", e)
                            Toast.makeText(this, "Error creating notifications", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }
    
    private fun setupAutoRefresh() {
        // Create a handler and runnable for periodic refresh
        val handler = Handler(Looper.getMainLooper())
        val refreshRunnable = object : Runnable {
            override fun run() {
                // Only refresh if activity is still in foreground
                if (!isFinishing && !isDestroyed) {
                    Log.d(TAG, "Auto-refreshing notifications")
                    loadNotifications()
                    // Schedule next refresh
                    handler.postDelayed(this, 30000) // 30 seconds
                }
            }
        }
        
        // Start the first refresh cycle
        handler.postDelayed(refreshRunnable, 30000) // 30 seconds
    }
} 