package com.smartcity.parkingapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.smartcity.parkingapp.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.google.firebase.firestore.QuerySnapshot

/**
 * User profile management fragment.
 * Displays user information, license plates, and current parking orders.
 * Allows editing profile details, managing license plates and handling payments.
 * 
 * Key features:
 * - View and edit user profile (name, email, license plates)
 * - Add new license plates to user account
 * - View current parking order details
 * - Process payment for active parking orders
 * - Logout from the application
 */
class ProfileFragment : Fragment() {
    
    private val TAG = "ProfileFragment"
    
    // User Information Views
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var licensePlateStatusTextView: TextView
    private lateinit var userNameEditText: EditText
    private lateinit var licensePlateEditText: EditText
    private lateinit var emailUpdateInfoTextView: TextView
    
    // Buttons
    private lateinit var editProfileButton: Button
    private lateinit var saveProfileButton: Button
    private lateinit var cancelEditButton: Button
    private lateinit var updateEmailButton: Button
    private lateinit var payNowButton: Button
    private lateinit var logoutButton: Button
    private lateinit var addPlateButton: Button
    
    // Layouts
    private lateinit var saveCancelLayout: LinearLayout
    private lateinit var orderInfoLayout: LinearLayout
    
    // Order Information Views
    private lateinit var orderLicensePlateText: TextView
    private lateinit var orderEntryTimeText: TextView
    private lateinit var orderExitTimeText: TextView
    private lateinit var orderFeeText: TextView
    private lateinit var noOrderText: TextView
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    // Current User Data
    private var currentUserName = ""
    private var currentUserEmail = ""
    private var currentLicensePlate = ""
    private var currentUserId = ""
    private var currentUser: FirebaseUser? = null
    
    /**
     * Inflates the user center layout for this fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_center, container, false)
    }
    
    /**
     * Initializes UI components and sets up event listeners
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        auth = Firebase.auth
        db = Firebase.firestore
        currentUser = auth.currentUser
        
        // Set title in toolbar
        (activity as? AppCompatActivity)?.supportActionBar?.title = "User Center"
        
        // Initialize user information views
        userNameTextView = view.findViewById(R.id.user_name_text)
        userEmailTextView = view.findViewById(R.id.user_email_text)
        licensePlateStatusTextView = view.findViewById(R.id.license_plate_status_text)
        userNameEditText = view.findViewById(R.id.user_name_edit)
        licensePlateEditText = view.findViewById(R.id.license_plate_edit)
        emailUpdateInfoTextView = view.findViewById(R.id.email_update_info)
        
        // Initialize buttons
        editProfileButton = view.findViewById(R.id.edit_profile_button)
        saveProfileButton = view.findViewById(R.id.save_profile_button)
        cancelEditButton = view.findViewById(R.id.cancel_edit_button)
        updateEmailButton = view.findViewById(R.id.update_email_button)
        payNowButton = view.findViewById(R.id.pay_now_button)
        logoutButton = view.findViewById(R.id.logout_button)
        addPlateButton = view.findViewById(R.id.add_plate_button)
        
        // Force-set logout button to red background, without using XML resource file
        logoutButton.setBackgroundColor(android.graphics.Color.parseColor("#D50000"))
        logoutButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_logout, 0, 0, 0)
        logoutButton.compoundDrawablePadding = 8
        
        // Override any potential theme issues by directly setting text color
        logoutButton.setTextColor(android.graphics.Color.WHITE)
        logoutButton.text = getString(R.string.logout)
        logoutButton.textSize = 16f
        
        // Initialize layouts
        saveCancelLayout = view.findViewById(R.id.save_cancel_layout)
        orderInfoLayout = view.findViewById(R.id.order_info_layout)
        
        // Initialize order information views
        orderLicensePlateText = view.findViewById(R.id.order_license_plate_text)
        orderEntryTimeText = view.findViewById(R.id.order_entry_time_text)
        orderExitTimeText = view.findViewById(R.id.order_exit_time_text)
        orderFeeText = view.findViewById(R.id.order_fee_text)
        noOrderText = view.findViewById(R.id.no_order_text)
        
        // Set up click listeners for profile editing
        editProfileButton.setOnClickListener {
            switchToEditMode(true)
        }
        
        saveProfileButton.setOnClickListener {
            saveUserProfile()
        }
        
        cancelEditButton.setOnClickListener {
            switchToEditMode(false)
        }
        
        updateEmailButton.setOnClickListener {
            showUpdateEmailDialog()
        }
        
        // Set up pay now button click listener
        payNowButton.setOnClickListener {
            handlePayment()
        }

        // Set up logout button click listener
        logoutButton.setOnClickListener {
            auth.signOut()
            // Navigate to login screen
            val intent = Intent(activity, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            activity?.finish()
        }
        
        // Set up add plate button click listener
        addPlateButton.setOnClickListener {
            showAddPlateDialog()
        }
        
        // Load user data and order information
        loadUserData()
        loadCurrentOrder()
    }
    
    /**
     * Shows a dialog for adding a new license plate.
     * Collects user input and passes it to addNewPlate() function.
     */
    private fun showAddPlateDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_plate, null)
        val plateInput = dialogView.findViewById<EditText>(R.id.plateInput)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Add New License Plate")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val newPlate = plateInput.text.toString().trim()
                if (newPlate.isNotEmpty()) {
                    addNewPlate(newPlate)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Adds a new license plate to the user's profile.
     * Checks for duplicates and updates both single and multiple plate fields for compatibility.
     * 
     * @param newPlate The license plate number to add
     */
    private fun addNewPlate(newPlate: String) {
        currentUser?.let { user ->
            db.collection("Users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    // Get existing license plates or create new list
                    val currentPlates = document.get("licensePlates") as? List<String> ?: listOf()
                    
                    // Check if this plate is already in the list
                    if (currentPlates.contains(newPlate)) {
                        Toast.makeText(context, "This license plate is already registered", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    
                    // Add new plate to existing plates
                    val updatedPlates = currentPlates + newPlate
                    
                    // Update only the licensePlates array field, keep original licensePlate for compatibility
                    val updates = hashMapOf<String, Any>(
                        "licensePlates" to updatedPlates
                    )
                    
                    db.collection("Users").document(user.uid)
                        .update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(context, "License plate added successfully", Toast.LENGTH_SHORT).show()
                            // Set this as current license plate for UI display only
                            currentLicensePlate = newPlate
                            // Refresh the display
                            loadUserData()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to add license plate: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to get user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    /**
     * Loads user data from Firebase.
     * Updates the UI with user name, email, and license plates.
     * Handles both single license plate and multiple license plates data formats.
     */
    private fun loadUserData() {
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            currentUserEmail = currentUser.email ?: "No email"
            currentUserId = currentUser.uid
            
            // Fetch user data from Firestore
            db.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // User data found
                        currentUserName = document.getString("name") ?: "User"
                        currentUserEmail = document.getString("email") ?: currentUser.email ?: "No email"
                        
                        // Try to get licensePlates as a list first
                        val plates = document.get("licensePlates") as? List<String>
                        
                        if (plates != null && plates.isNotEmpty()) {
                            // Display multiple plates
                            currentLicensePlate = plates.first() // Set first plate as current
                            
                            // Build a formatted string for multiple license plates with bullets
                            val platesDisplay = plates.joinToString("\n• ", prefix = "• ")
                            licensePlateStatusTextView.text = platesDisplay
                        } else {
                            // Try to get single licensePlate (backwards compatibility)
                            currentLicensePlate = document.getString("licensePlate") ?: ""
                            
                            // Display license plate if available
                            if (currentLicensePlate.isNotEmpty()) {
                                licensePlateStatusTextView.text = "• $currentLicensePlate"
                            } else {
                                licensePlateStatusTextView.text = "No license plate set"
                            }
                        }
                        
                        // Update UI with user data
                        userNameTextView.text = currentUserName
                        userEmailTextView.text = currentUserEmail
                        
                        // Set the edit text values
                        userNameEditText.setText(currentUserName)
                        licensePlateEditText.setText(currentLicensePlate)
                    } else {
                        // User document doesn't exist
                        Log.d(TAG, "No such document")
                        userNameTextView.text = "User"
                        userEmailTextView.text = currentUser.email ?: "No email"
                        licensePlateStatusTextView.text = "No license plate set"
                    }
                }
                .addOnFailureListener { e ->
                    // Error getting user data
                    Log.e(TAG, "Error getting user document", e)
                    Toast.makeText(context, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    
                    // Show fallback data
                    userNameTextView.text = "User"
                    userEmailTextView.text = currentUser.email ?: "No email"
                    licensePlateStatusTextView.text = "No license plate set"
                }
        } else {
            // User not logged in
            Log.d(TAG, "User not logged in")
            userNameTextView.text = "Guest"
            userEmailTextView.text = "Not logged in"
            licensePlateStatusTextView.text = "No license plate set"
            
            // Send user to login
            val intent = Intent(activity, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            activity?.finish()
        }
    }
    
    /**
     * Loads the current active order for the user.
     * Searches for orders associated with the user's license plates.
     * If no orders are found, checks plate recognition records as a fallback.
     */
    private fun loadCurrentOrder() {
        currentUser?.let { user ->
            // Get all license plates for this user
            db.collection("Users").document(user.uid)
                .get()
                .addOnSuccessListener { userDoc ->
                    // Get user's license plates
                    val licensePlates = userDoc.get("licensePlates") as? List<String> 
                        ?: listOf(userDoc.getString("licensePlate") ?: "")
                    
                    // Filter out empty plates
                    val validPlates = licensePlates.filter { it.isNotEmpty() }
                    
                    if (validPlates.isEmpty()) {
                        // No license plates found
                        orderInfoLayout.visibility = View.GONE
                        noOrderText.visibility = View.VISIBLE
                        return@addOnSuccessListener
                    }
                    
                    // Find orders for all user's plates
                    findOrdersForLicensePlates(validPlates)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting user data", e)
                    Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    
                    // Hide order info layout and show "no order" message
                    orderInfoLayout.visibility = View.GONE
                    noOrderText.visibility = View.VISIBLE
                }
        }
    }
    
    /**
     * Finds orders in the database associated with the given license plates.
     * Filters for active or completed but unpaid orders.
     * 
     * @param licensePlates List of license plate numbers to search for
     */
    private fun findOrdersForLicensePlates(licensePlates: List<String>) {
        if (licensePlates.isEmpty()) return
        
        // Find all orders for these license plates
        db.collection("Orders")
            .whereIn("licensePlate", licensePlates)
            .get()
            .addOnSuccessListener { documents ->
                // Filter to include only active or completed but unpaid orders
                val relevantOrders = documents.documents.filter { order ->
                    val status = order.getString("status") ?: ""
                    status in listOf("active", "completed") && status != "paid"
                }
                
                if (relevantOrders.isNotEmpty()) {
                    // Sort orders by entry time (newest first)
                    val sortedOrders = relevantOrders.sortedByDescending { 
                        it.getTimestamp("entryTime")?.seconds ?: 0 
                    }
                    
                    // Use the most recent order
                    updateOrderUI(sortedOrders.first())
                } else {
                    // No relevant orders found, check plate recognition records
                    Log.d(TAG, "No relevant orders found, checking plate recognition")
                    findPlateRecognitionRecords(licensePlates)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error finding orders", e)
                // Try with plate recognition as fallback
                findPlateRecognitionRecords(licensePlates)
            }
    }
    
    /**
     * Searches for plate recognition records as a fallback when no orders are found.
     * Useful for detecting plates that have entered parking but don't have an order yet.
     * 
     * @param licensePlates List of license plate numbers to search for
     */
    private fun findPlateRecognitionRecords(licensePlates: List<String>) {
        if (licensePlates.isEmpty()) {
            // No license plates to search
            orderInfoLayout.visibility = View.GONE
            noOrderText.visibility = View.VISIBLE
            return
        }
        
        // Start with the first license plate
        val firstPlate = licensePlates[0]
        db.collection("PlateRecognition")
            .whereEqualTo("plate_number", firstPlate)
            .get()
            .addOnSuccessListener { result ->
                val allRecords = result.documents.toMutableList()
                
                // If we have more than one plate, check those as well
                if (licensePlates.size > 1) {
                    // Process one additional plate at a time
                    val remainingPlates = licensePlates.subList(1, licensePlates.size)
                    processRemainingPlates(remainingPlates, allRecords)
                } else {
                    // Only had one plate, process results now
                    processPlateRecognitionResults(allRecords)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error finding plate recognition records", e)
                handlePlateRecognitionError(e)
            }
    }
    
    /**
     * Processes remaining license plates recursively to find all plate recognition records.
     * 
     * @param plates Remaining license plates to process
     * @param allRecords Accumulated plate recognition records
     */
    private fun processRemainingPlates(plates: List<String>, allRecords: MutableList<DocumentSnapshot>) {
        if (plates.isEmpty()) {
            // No more plates to process, handle the results
            processPlateRecognitionResults(allRecords)
            return
        }
        
        // Process the next plate
        val nextPlate = plates[0]
        db.collection("PlateRecognition")
            .whereEqualTo("plate_number", nextPlate)
            .get()
            .addOnSuccessListener { result ->
                // Add these documents to our collection
                allRecords.addAll(result.documents)
                
                // Process the rest of the plates
                if (plates.size > 1) {
                    processRemainingPlates(plates.subList(1, plates.size), allRecords)
                } else {
                    // This was the last plate, process results
                    processPlateRecognitionResults(allRecords)
                }
            }
            .addOnFailureListener { e ->
                // Continue with what we have so far
                Log.e(TAG, "Error finding records for plate $nextPlate", e)
                if (plates.size > 1) {
                    processRemainingPlates(plates.subList(1, plates.size), allRecords)
                } else {
                    processPlateRecognitionResults(allRecords)
                }
            }
    }
    
    /**
     * Processes plate recognition results and creates an order if a valid record is found.
     * 
     * @param allRecords List of plate recognition records from all user license plates
     */
    private fun processPlateRecognitionResults(allRecords: List<DocumentSnapshot>) {
        if (allRecords.isNotEmpty()) {
            // Sort by timestamp (newest first)
            val sortedRecords = allRecords.sortedByDescending { 
                it.getTimestamp("timestamp")?.seconds ?: 0 
            }
            
            // Use the most recent record
            createOrderFromPlateRecognition(sortedRecords.first())
        } else {
            // No records found for any license plate
            Log.d(TAG, "No plate recognition records found")
            orderInfoLayout.visibility = View.GONE
            noOrderText.visibility = View.VISIBLE
        }
    }
    
    /**
     * Handles errors that occur when querying plate recognition records.
     * Special handling for Firebase index errors, showing a helpful dialog.
     * 
     * @param e Exception from the failed plate recognition query
     */
    private fun handlePlateRecognitionError(e: Exception) {
        // Check if this is an index error
        val errorMessage = e.message ?: ""
        if (errorMessage.contains("FAILED_PRECONDITION") && errorMessage.contains("index")) {
            // Extract index URL
            val indexUrlRegex = "https://console.firebase.google.com/.*".toRegex()
            val indexUrlMatch = indexUrlRegex.find(errorMessage)
            
            if (indexUrlMatch != null) {
                showCreateIndexDialog(indexUrlMatch.value)
            } else {
                Toast.makeText(context, "Firebase index creation required. Please contact administrator.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Failed to check plate records: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        
        // Hide order info layout and show "no order" message
        orderInfoLayout.visibility = View.GONE
        noOrderText.visibility = View.VISIBLE
    }
    
    /**
     * Creates an order in the database from a plate recognition record.
     * Checks for existing matching orders to avoid duplicates.
     * Updates the UI to display the created order's details.
     * 
     * @param plateRec DocumentSnapshot containing plate recognition data
     */
    private fun createOrderFromPlateRecognition(plateRec: DocumentSnapshot) {
        val currentUser = auth.currentUser ?: return
        
        // Get the data from plate recognition
        val plateNumber = plateRec.getString("plate_number") ?: return
        val timestamp = plateRec.getTimestamp("timestamp") ?: return
        val exitAfterSeconds = plateRec.getLong("exit_after_seconds") ?: 0
        val fee = plateRec.getDouble("fee") ?: 0.0
        val countryIdentifier = plateRec.getString("country_identifier") ?: "Unknown"
        
        // Calculate exit time by adding seconds to timestamp
        val exitTimestamp = com.google.firebase.Timestamp(
            timestamp.seconds + exitAfterSeconds,
            timestamp.nanoseconds
        )
        
        // Create a new order document
        val orderData = hashMapOf(
            "userID" to currentUser.uid,
            "licensePlate" to plateNumber,
            "entryTime" to timestamp,
            "exitTime" to exitTimestamp,
            "fee" to fee,
            "status" to "completed",
            "countryIdentifier" to countryIdentifier
        )
        
        // Check if there's an existing order first to avoid duplicates
        db.collection("Orders")
            .whereEqualTo("licensePlate", plateNumber)
            .whereEqualTo("entryTime", timestamp)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No existing order, create a new one
                    db.collection("Orders")
                        .add(orderData)
                        .addOnSuccessListener { documentRef ->
                            Log.d(TAG, "Order created with ID: ${documentRef.id}")
                            
                            // Now fetch the newly created order
                            documentRef.get()
                                .addOnSuccessListener { order ->
                                    updateOrderUI(order)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error creating order", e)
                            orderInfoLayout.visibility = View.GONE
                            noOrderText.visibility = View.VISIBLE
                        }
                } else {
                    // Order already exists, use that one
                    val existingOrder = documents.documents[0]
                    updateOrderUI(existingOrder)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking for existing order", e)
                
                // Hide order info layout and show "no order" message
                orderInfoLayout.visibility = View.GONE
                noOrderText.visibility = View.VISIBLE
            }
    }
    
    /**
     * Updates the UI to display order information.
     * Formats timestamps and fee information for display.
     * 
     * @param order DocumentSnapshot containing order data
     */
    private fun updateOrderUI(order: DocumentSnapshot) {
        val licensePlate = order.getString("licensePlate") ?: "Unknown"
        val entryTime = order.getTimestamp("entryTime")
        val exitTime = order.getTimestamp("exitTime")
        val fee = order.getDouble("fee") ?: 0.0
        val status = order.getString("status") ?: "active"
        
        val entryString = if (entryTime != null) {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            Instant.ofEpochMilli(entryTime.seconds * 1000)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        } else {
            "Unknown"
        }
        
        val exitString = if (exitTime != null) {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            Instant.ofEpochMilli(exitTime.seconds * 1000)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        } else {
            "Unknown"
        }
        
        // Display payment status along with license plate
        val paymentStatus = if (status == "paid") " (PAID)" else ""
        orderLicensePlateText.text = "License Plate: $licensePlate$paymentStatus"
        
        orderEntryTimeText.text = "Entry Time: $entryString"
        orderExitTimeText.text = "Exit Time: $exitString"
        orderFeeText.text = "Fee: £${"%.2f".format(fee)}"
        
        // Update pay now button based on payment status
        if (status == "paid") {
            // Order is already paid - disable button and change appearance
            payNowButton.isEnabled = false
            payNowButton.text = getString(R.string.already_paid)
            payNowButton.setBackgroundResource(R.drawable.paid_button_background)
            payNowButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_paid, 0, 0, 0)
            payNowButton.compoundDrawablePadding = 8
        } else {
            // Order needs payment - enable button with normal appearance
            payNowButton.isEnabled = true
            payNowButton.text = getString(R.string.pay_now)
            payNowButton.setBackgroundResource(R.drawable.pay_button_background)
            payNowButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pay_now, 0, 0, 0)
            payNowButton.compoundDrawablePadding = 8
        }
        
        orderInfoLayout.visibility = View.VISIBLE
        noOrderText.visibility = View.GONE
    }
    
    /**
     * Switches the UI between view mode and edit mode.
     * Controls visibility of edit fields and buttons.
     * 
     * @param editMode True to enable edit mode, false for view mode
     */
    private fun switchToEditMode(editMode: Boolean) {
        if (editMode) {
            // Switch to edit mode
            userNameTextView.visibility = View.GONE
            licensePlateStatusTextView.visibility = View.GONE
            
            userNameEditText.visibility = View.VISIBLE
            licensePlateEditText.visibility = View.VISIBLE
            
            editProfileButton.visibility = View.GONE
            saveCancelLayout.visibility = View.VISIBLE
            updateEmailButton.visibility = View.VISIBLE
            emailUpdateInfoTextView.visibility = View.VISIBLE
        } else {
            // Switch to view mode
            userNameTextView.visibility = View.VISIBLE
            licensePlateStatusTextView.visibility = View.VISIBLE
            
            userNameEditText.visibility = View.GONE
            licensePlateEditText.visibility = View.GONE
            
            editProfileButton.visibility = View.VISIBLE
            saveCancelLayout.visibility = View.GONE
            updateEmailButton.visibility = View.GONE
            emailUpdateInfoTextView.visibility = View.GONE
            
            // Reset edit text values
            userNameEditText.setText(currentUserName)
            licensePlateEditText.setText(currentLicensePlate)
        }
    }
    
    /**
     * Saves updated user profile information to Firebase.
     * Updates both the name and license plate information if changed.
     * Maintains a list of license plates for the user.
     */
    private fun saveUserProfile() {
        val newName = userNameEditText.text.toString().trim()
        val newLicensePlate = licensePlateEditText.text.toString().trim()
        
        // Validate inputs
        if (newName.isEmpty()) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (newLicensePlate.isEmpty()) {
            Toast.makeText(context, "License plate cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            // Fetch current data first to get existing license plates
            db.collection("Users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    // Create map with updated fields
                    val updates = hashMapOf<String, Any>()
                    
                    // Update name if changed
                    if (newName != currentUserName) {
                        updates["name"] = newName
                    }
                    
                    // Get existing license plates or create new list
                    val existingPlates = document.get("licensePlates") as? List<String> ?: listOf()
                    
                    // Check if this is a different license plate than the current one
                    if (newLicensePlate != currentLicensePlate) {
                        // Check if this plate is already in the list
                        if (!existingPlates.contains(newLicensePlate)) {
                            // Add new plate to existing plates
                            val updatedPlates = existingPlates + newLicensePlate
                            updates["licensePlates"] = updatedPlates
                        }
                        
                        // For backward compatibility, also update single licensePlate field
                        updates["licensePlate"] = newLicensePlate
                    }
                    
                    // Only update if there are changes
                    if (updates.isNotEmpty()) {
                        // Update user data in Firestore
                        db.collection("Users").document(currentUser.uid)
                            .update(updates)
                            .addOnSuccessListener {
                                Log.d(TAG, "User profile updated successfully")
                                
                                // Update local variables
                                currentUserName = newName
                                currentLicensePlate = newLicensePlate
                                
                                // Reload user data to refresh the UI with all plates
                                loadUserData()
                                
                                // Switch back to view mode
                                switchToEditMode(false)
                                
                                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error updating user profile", e)
                                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Nothing changed, just switch back to view mode
                        switchToEditMode(false)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting user document", e)
                    Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    /**
     * Shows a dialog for updating the user's email address.
     * Requires the user to confirm their password for security.
     */
    private fun showUpdateEmailDialog() {
        val currentUser = auth.currentUser ?: return
        
        // Create dialog with input for new email and current password
        val dialogView = layoutInflater.inflate(R.layout.dialog_update_email, null)
        val newEmailEditText = dialogView.findViewById<EditText>(R.id.new_email_edit_text)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.password_edit_text)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Update Email")
            .setView(dialogView)
            .setPositiveButton("Update") { dialog, _ ->
                val newEmail = newEmailEditText.text.toString().trim()
                val password = passwordEditText.text.toString().trim()
                
                if (newEmail.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Reauthenticate user before changing email
                val credential = EmailAuthProvider.getCredential(currentUserEmail, password)
                
                currentUser.reauthenticate(credential)
                    .addOnSuccessListener {
                        // Reauthentication successful, now update email
                        updateEmail(currentUser, newEmail)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Reauthentication failed", e)
                        Toast.makeText(context, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Updates a user's email address after successful reauthentication.
     * Sends verification email and updates the user record in Firestore.
     * 
     * @param currentUser The authenticated Firebase user
     * @param newEmail The new email address to set
     */
    private fun updateEmail(currentUser: FirebaseUser, newEmail: String) {
        currentUser.updateEmail(newEmail)
            .addOnSuccessListener { 
                Log.d(TAG, "Email updated successfully")
                
                // Send verification email
                currentUser.sendEmailVerification()
                    .addOnSuccessListener {
                        Log.d(TAG, "Verification email sent")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to send verification email", e)
                    }
                
                // Update Firestore
                db.collection("Users").document(currentUser.uid)
                    .update("email", newEmail)
                    .addOnSuccessListener {
                        Log.d(TAG, "Email updated in Firestore")
                        
                        // Update local variable and UI
                        currentUserEmail = newEmail
                        userEmailTextView.text = newEmail
                        
                        Toast.makeText(
                            context,
                            "Email updated. Please verify your new email address.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update email in Firestore", e)
                        Toast.makeText(context, "Failed to update email in database", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update email", e)
                Toast.makeText(context, "Failed to update email: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    /**
     * Handles the payment process when the user clicks "Pay Now".
     * Retrieves the user's license plates and finds associated orders to pay.
     */
    private fun handlePayment() {
        val currentUser = auth.currentUser ?: return
        
        // Get all license plates for this user
        db.collection("Users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { userDoc ->
                // Get user's license plates
                val licensePlates = userDoc.get("licensePlates") as? List<String> 
                    ?: listOf(userDoc.getString("licensePlate") ?: "")
                
                // Filter out empty plates
                val validPlates = licensePlates.filter { it.isNotEmpty() }
                
                if (validPlates.isEmpty()) {
                    Toast.makeText(context, "No license plates registered", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                // Find all orders for these license plates
                findOrdersForPayment(validPlates)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting user data", e)
                Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    /**
     * Finds orders eligible for payment associated with the user's license plates.
     * Launches payment activity for the most recent unpaid order if found.
     * Falls back to plate recognition records if no orders are found.
     * 
     * @param licensePlates List of license plate numbers to search for
     */
    private fun findOrdersForPayment(licensePlates: List<String>) {
        // Find all orders for these license plates
        db.collection("Orders")
            .whereIn("licensePlate", licensePlates)
            .get()
            .addOnSuccessListener { documents ->
                // Filter to include only active or completed but unpaid orders
                val relevantOrders = documents.documents.filter { order ->
                    val status = order.getString("status") ?: ""
                    status in listOf("active", "completed") && status != "paid"
                }
                
                if (relevantOrders.isNotEmpty()) {
                    // Sort orders by entry time (newest first)
                    val sortedOrders = relevantOrders.sortedByDescending { 
                        it.getTimestamp("entryTime")?.seconds ?: 0 
                    }
                    
                    // Use the most recent order for payment
                    val latestOrder = sortedOrders.first()
                    val fee = latestOrder.getDouble("fee") ?: 0.0
                    val licensePlate = latestOrder.getString("licensePlate") ?: "--"
                    
                    // Navigate to payment activity
                    val intent = Intent(activity, PaymentActivity::class.java)
                    intent.putExtra("amount", fee)
                    intent.putExtra("licensePlate", licensePlate)
                    intent.putExtra("orderId", latestOrder.id)
                    startActivity(intent)
                } else {
                    // No relevant orders found, check plate recognition
                    checkPlateRecognitionForPayment(licensePlates)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error finding orders", e)
                Toast.makeText(context, "Failed to find orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    /**
     * Searches for plate recognition records when no orders are found for payment.
     * Checks each license plate sequentially and collects the results.
     * 
     * @param licensePlates List of license plate numbers to search for
     */
    private fun checkPlateRecognitionForPayment(licensePlates: List<String>) {
        if (licensePlates.isEmpty()) {
            Toast.makeText(context, "No license plates registered", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Start with the first license plate
        val firstPlate = licensePlates[0]
        db.collection("PlateRecognition")
            .whereEqualTo("plate_number", firstPlate)
            .get()
            .addOnSuccessListener { result ->
                val allRecords = result.documents.toMutableList()
                
                // If we have more than one plate, check those as well
                if (licensePlates.size > 1) {
                    // Process one additional plate at a time
                    val remainingPlates = licensePlates.subList(1, licensePlates.size)
                    processRemainingPlatesForPayment(remainingPlates, allRecords)
                } else {
                    // Only had one plate, process results now
                    processPlateRecognitionForPayment(allRecords)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to query plate recognition records", e)
                Toast.makeText(context, "Failed to check records: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    /**
     * Processes remaining license plates recursively to find all plate recognition records.
     * Used for payment processing as a fallback when no orders are found.
     * 
     * @param plates Remaining license plates to process
     * @param allRecords Accumulated plate recognition records
     */
    private fun processRemainingPlatesForPayment(plates: List<String>, allRecords: MutableList<DocumentSnapshot>) {
        if (plates.isEmpty()) {
            // No more plates to process, handle the results
            processPlateRecognitionForPayment(allRecords)
            return
        }
        
        // Process the next plate
        val nextPlate = plates[0]
        db.collection("PlateRecognition")
            .whereEqualTo("plate_number", nextPlate)
            .get()
            .addOnSuccessListener { result ->
                // Add these documents to our collection
                allRecords.addAll(result.documents)
                
                // Process the rest of the plates
                if (plates.size > 1) {
                    processRemainingPlatesForPayment(plates.subList(1, plates.size), allRecords)
                } else {
                    // This was the last plate, process results
                    processPlateRecognitionForPayment(allRecords)
                }
            }
            .addOnFailureListener { e ->
                // Continue with what we have so far
                Log.e(TAG, "Error finding records for plate $nextPlate", e)
                if (plates.size > 1) {
                    processRemainingPlatesForPayment(plates.subList(1, plates.size), allRecords)
                } else {
                    processPlateRecognitionForPayment(allRecords)
                }
            }
    }
    
    /**
     * Processes plate recognition records to create an order and initiate payment.
     * Creates a new order based on the most recent plate recognition record.
     * 
     * @param allRecords List of plate recognition records from all user license plates
     */
    private fun processPlateRecognitionForPayment(allRecords: List<DocumentSnapshot>) {
        if (allRecords.isNotEmpty()) {
            // Sort by timestamp (newest first)
            val sortedRecords = allRecords.sortedByDescending { 
                it.getTimestamp("timestamp")?.seconds ?: 0 
            }
            
            // Use the most recent record for payment
            val latestRecord = sortedRecords.first()
            val fee = latestRecord.getDouble("fee") ?: 0.0
            val licensePlate = latestRecord.getString("plate_number") ?: return
            
            // Create a new order for this record
            val currentUser = auth.currentUser ?: return
            val timestamp = latestRecord.getTimestamp("timestamp") ?: return
            val exitAfterSeconds = latestRecord.getLong("exit_after_seconds") ?: 0
            val countryIdentifier = latestRecord.getString("country_identifier") ?: "Unknown"
            
            // Calculate exit time
            val exitTimestamp = com.google.firebase.Timestamp(
                timestamp.seconds + exitAfterSeconds,
                timestamp.nanoseconds
            )
            
            // Create order data
            val orderData = hashMapOf(
                "userID" to currentUser.uid,
                "licensePlate" to licensePlate,
                "entryTime" to timestamp,
                "exitTime" to exitTimestamp,
                "fee" to fee,
                "status" to "completed",
                "countryIdentifier" to countryIdentifier
            )
            
            // Add order to Firestore
            db.collection("Orders")
                .add(orderData)
                .addOnSuccessListener { docRef ->
                    // Start payment with the new order
                    val intent = Intent(activity, PaymentActivity::class.java)
                    intent.putExtra("amount", fee)
                    intent.putExtra("licensePlate", licensePlate)
                    intent.putExtra("orderId", docRef.id)
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to create order", e)
                    Toast.makeText(context, "Failed to process payment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "No parking records found for your license plates", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Shows a dialog to help users create a required Firebase index.
     * Provides options to contact an administrator or open the Firebase console directly.
     * 
     * @param indexUrl The URL to the Firebase console for creating the required index
     */
    private fun showCreateIndexDialog(indexUrl: String) {
        val context = context ?: return
        
        AlertDialog.Builder(context)
            .setTitle("Database Index Required")
            .setMessage("The application needs to create an index in Firebase to function properly.\n\nYou can:\n1. Contact the app administrator to create the index\n2. If you have admin privileges, click 'Create Index' to access the Firebase console")
            .setPositiveButton("Create Index") { _, _ ->
                // Create Intent to open browser with the index creation link
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(indexUrl))
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Called when the fragment becomes visible to the user.
     * Reloads user data and current orders to ensure the UI displays current information.
     */
    override fun onResume() {
        super.onResume()
        
        // Reload data when the fragment becomes visible again
        loadUserData()
        loadCurrentOrder()
    }
} 