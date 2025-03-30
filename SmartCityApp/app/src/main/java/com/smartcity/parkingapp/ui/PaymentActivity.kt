package com.smartcity.parkingapp.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartcity.parkingapp.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.google.firebase.Timestamp
import java.util.UUID

class PaymentActivity : AppCompatActivity() {

    private val TAG = "PaymentActivity"
    
    // Stripe test public key (from README)
    private val stripePublicKey = "pk_test_51R5tjcB0X5aGtngh2WehxbpvGjC8lbscoAtO8ThwAaYXUArhfW52Qs1Qhbljuj6EwIO6q96IIqXtZ7x4hKMSO1pA00n8Y147FM"
    
    private lateinit var amountTextView: TextView
    private lateinit var licensePlateTextView: TextView
    private lateinit var entryTimeTextView: TextView
    private lateinit var exitTimeTextView: TextView
    private lateinit var paymentStatusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cardImageView: ImageView
    private lateinit var confirmPaymentButton: Button
    private lateinit var cancelButton: Button
    
    // Card fields (simulated)
    private lateinit var cardNumberEditText: EditText
    private lateinit var cardExpiryEditText: EditText
    private lateinit var cardCvcEditText: EditText
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private lateinit var paymentLayout: View
    private lateinit var receiptLayout: View
    private lateinit var receiptAmountTextView: TextView
    private lateinit var receiptLicensePlateTextView: TextView
    private lateinit var receiptDateTextView: TextView
    private lateinit var receiptIdTextView: TextView
    private lateinit var receiptCloseButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)
        
        // Initialize views
        amountTextView = findViewById(R.id.amount_text_view)
        licensePlateTextView = findViewById(R.id.license_plate_text_view)
        entryTimeTextView = findViewById(R.id.entry_time_text_view)
        exitTimeTextView = findViewById(R.id.exit_time_text_view)
        paymentStatusTextView = findViewById(R.id.payment_status_text_view)
        progressBar = findViewById(R.id.payment_progress_bar)
        cardImageView = findViewById(R.id.card_image_view)
        confirmPaymentButton = findViewById(R.id.confirm_payment_button)
        cancelButton = findViewById(R.id.cancel_button)
        
        // Initialize card fields
        cardNumberEditText = findViewById(R.id.card_number_edit_text)
        cardExpiryEditText = findViewById(R.id.card_expiry_edit_text)
        cardCvcEditText = findViewById(R.id.card_cvc_edit_text)
        
        // Initialize payment and receipt layouts
        paymentLayout = findViewById(R.id.payment_layout)
        receiptLayout = findViewById(R.id.receipt_layout)
        receiptAmountTextView = findViewById(R.id.receipt_amount_text_view)
        receiptLicensePlateTextView = findViewById(R.id.receipt_license_plate_text_view)
        receiptDateTextView = findViewById(R.id.receipt_date_text_view)
        receiptIdTextView = findViewById(R.id.receipt_id_text_view)
        receiptCloseButton = findViewById(R.id.receipt_close_button)
        
        // Determine view mode
        val viewMode = intent.getStringExtra("VIEW_MODE") ?: "PAYMENT"
        
        if (viewMode == "RECEIPT") {
            // Set title for receipt view
            supportActionBar?.title = "Payment Receipt"
            
            // Show receipt view
            showReceiptView()
            
            // Set up receipt close button
            receiptCloseButton.setOnClickListener {
                finish()
            }
        } else {
            // Set title for payment view
            supportActionBar?.title = "Payment"
            
            // Show payment view
            showPaymentView()
            
            // Get data from intent
            val amount = intent.getDoubleExtra("amount", 0.0)
            val licensePlate = intent.getStringExtra("licensePlate") ?: "--"
            val orderId = intent.getStringExtra("orderId")
            val notificationId = intent.getStringExtra("notificationId")
            
            // Update UI with data
            amountTextView.text = "£${"%.2f".format(amount)}"
            licensePlateTextView.text = licensePlate
            
            // Load order details based on available information
            if (orderId != null) {
                // If we have an order ID, load details from that
                loadOrderDetails(orderId)
            } else if (notificationId != null) {
                // If we have a notification ID, get the timestamps from the notification
                loadTimesFromNotification(notificationId)
            } else {
                // If we don't have either, try to find it by licensePlate
                findOrderByLicensePlate(licensePlate, amount)
            }
            
            // Set up button click listeners
            confirmPaymentButton.setOnClickListener {
                // In a real implementation, we would use Stripe SDK to process the payment
                // with real card details
                processPayment(amount, licensePlate)
            }
            
            cancelButton.setOnClickListener {
                finish()
            }
            
            // Set up card details for demo (in a real app, we would use Stripe SDK)
            setupStripeDemo()
        }
    }
    
    private fun setupStripeDemo() {
        // In a real app, this would initialize the Stripe SDK with our public key
        Log.d(TAG, "Stripe public key: $stripePublicKey")
        
        // For demo purposes, we're using a fixed test card
        cardNumberEditText.setText("4242 4242 4242 4242") // Stripe test card
        cardExpiryEditText.setText("12/25")
        cardCvcEditText.setText("123")
        
        // Disable editing of card fields for the demo
        cardNumberEditText.isEnabled = false
        cardExpiryEditText.isEnabled = false
        cardCvcEditText.isEnabled = false
    }
    
    private fun loadOrderDetails(orderId: String) {
        db.collection("Orders").document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val entryTime = document.getTimestamp("entryTime")
                    val exitTime = document.getTimestamp("exitTime")
                    
                    // Format times
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                    
                    if (entryTime != null) {
                        val entryInstant = Instant.ofEpochMilli(entryTime.seconds * 1000)
                        val formattedEntryTime = entryInstant.atZone(ZoneId.systemDefault()).format(formatter)
                        entryTimeTextView.text = formattedEntryTime
                    } else {
                        // Try to get from intent if not in document
                        val intentEntryTime = intent.getSerializableExtra("entryTime") as? Timestamp
                        if (intentEntryTime != null) {
                            val entryInstant = Instant.ofEpochMilli(intentEntryTime.seconds * 1000)
                            val formattedEntryTime = entryInstant.atZone(ZoneId.systemDefault()).format(formatter)
                            entryTimeTextView.text = formattedEntryTime
                        }
                    }
                    
                    if (exitTime != null) {
                        val exitInstant = Instant.ofEpochMilli(exitTime.seconds * 1000)
                        val formattedExitTime = exitInstant.atZone(ZoneId.systemDefault()).format(formatter)
                        exitTimeTextView.text = formattedExitTime
                    } else {
                        // Try to get from intent if not in document
                        val intentExitTime = intent.getSerializableExtra("exitTime") as? Timestamp
                        if (intentExitTime != null) {
                            val exitInstant = Instant.ofEpochMilli(intentExitTime.seconds * 1000)
                            val formattedExitTime = exitInstant.atZone(ZoneId.systemDefault()).format(formatter)
                            exitTimeTextView.text = formattedExitTime
                        }
                    }
                } else {
                    // If document doesn't exist, try to use intent data
                    getTimesFromIntent()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading order details", e)
                // Try to use intent data as fallback
                getTimesFromIntent()
            }
    }
    
    private fun getTimesFromIntent() {
        // Get entry and exit times from intent if available
        val entryTime = intent.getSerializableExtra("entryTime") as? Timestamp
        val exitTime = intent.getSerializableExtra("exitTime") as? Timestamp
        
        if (entryTime != null || exitTime != null) {
            // Format times
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            
            if (entryTime != null) {
                val entryInstant = Instant.ofEpochMilli(entryTime.seconds * 1000)
                val formattedEntryTime = entryInstant.atZone(ZoneId.systemDefault()).format(formatter)
                entryTimeTextView.text = formattedEntryTime
            }
            
            if (exitTime != null) {
                val exitInstant = Instant.ofEpochMilli(exitTime.seconds * 1000)
                val formattedExitTime = exitInstant.atZone(ZoneId.systemDefault()).format(formatter)
                exitTimeTextView.text = formattedExitTime
            }
        }
    }
    
    private fun findOrderByLicensePlate(licensePlate: String, amount: Double) {
        // Use original license plate format
        // val formattedLicensePlate = licensePlate.replace(" ", "")
        
        Log.d(TAG, "Searching for orders with license plate: $licensePlate")
        
        // Try to find any order with this license plate
        db.collection("Orders")
            .whereEqualTo("licensePlate", licensePlate)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} orders with this license plate")
                
                // If we found multiple records, use a more comprehensive approach
                if (documents.size() > 1) {
                    // Try to get the entry/exit time from intent
                    val intentEntryTime = intent.getSerializableExtra("entryTime") as? Timestamp
                    val intentExitTime = intent.getSerializableExtra("exitTime") as? Timestamp
                    
                    if (intentEntryTime != null && intentExitTime != null) {
                        // Use the updateSimilarRecords method as it has better matching logic
                        updateSimilarRecords(licensePlate, intentEntryTime, intentExitTime, amount)
                        return@addOnSuccessListener
                    }
                }
                
                // Filter in memory to find active or completed orders
                val relevantOrders = documents.documents.filter { order ->
                    val status = order.getString("status") ?: ""
                    status in listOf("active", "completed")
                }
                
                // Check for orders specifically from the web system
                val webOrders = documents.documents.filter { order ->
                    val source = order.getString("source") ?: ""
                    source in listOf("plateRecognition", "web")
                }
                
                // Prioritize orders in this sequence: relevantOrders, webOrders, any order
                val targetOrder = when {
                    relevantOrders.isNotEmpty() -> relevantOrders[0]
                    webOrders.isNotEmpty() -> webOrders[0]
                    documents.size() > 0 -> documents.documents[0]
                    else -> null
                }
                
                if (targetOrder != null) {
                    val orderId = targetOrder.id
                    val entryTime = targetOrder.getTimestamp("entryTime") ?: targetOrder.getTimestamp("timestamp")
                    val exitTime = targetOrder.getTimestamp("exitTime")
                    
                    // If the order doesn't have a userID, associate it with current user
                    val currentUserID = auth.currentUser?.uid
                    if (targetOrder.getString("userID").isNullOrEmpty() && currentUserID != null) {
                        db.collection("Orders").document(orderId)
                            .update("userID", currentUserID)
                            .addOnSuccessListener {
                                Log.d(TAG, "Order updated with current user ID")
                            }
                    }
                    
                    // Create update fields map
                    val updateFields = mutableMapOf<String, Any>(
                        "status" to "paid",
                        "paidAmount" to amount,
                        "paymentDate" to Date(),
                        "paymentMethod" to "stripe",
                        "cardLast4" to "4242"
                    )
                    
                    // For web system records, add additional fields
                    if (targetOrder.getString("source") == "plateRecognition" || targetOrder.getString("source") == "web") {
                        updateFields["isPaid"] = true
                    }
                    
                    // Update the order status to "paid"
                    db.collection("Orders").document(orderId)
                        .update(updateFields)
                        .addOnSuccessListener {
                            Log.d(TAG, "Order updated successfully by license plate")
                            
                            // Find and update any similar records
                            if (entryTime != null) {
                                // If we have both entry and exit times, use them
                                if (exitTime != null) {
                                    updateSimilarRecords(licensePlate, entryTime, exitTime, amount)
                                } else {
                                    // For orders that only have entry time, search for all orders from the same day
                                    findAndUpdateSameDayOrders(licensePlate, entryTime, amount)
                                }
                            } else {
                                showPaymentSuccess()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error updating order by license plate", e)
                            showPaymentError("Failed to update order: ${e.message}")
                        }
                } else {
                    // No matching order found, create a new one as a last resort
                    createAndPayOrder(licensePlate, amount)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error finding order by license plate", e)
                showPaymentError("Error retrieving order information: ${e.message}")
            }
    }
    
    /**
     * Finds and updates all orders for the same license plate from the same day.
     * Used when we only have an entry time but no exit time.
     */
    private fun findAndUpdateSameDayOrders(licensePlate: String, entryTime: Timestamp, amount: Double) {
        db.collection("Orders")
            .whereEqualTo("licensePlate", licensePlate)
            .get()
            .addOnSuccessListener { documents ->
                val sameDayOrders = documents.documents.filter { doc ->
                    val docEntry = doc.getTimestamp("entryTime") ?: doc.getTimestamp("timestamp")
                    docEntry != null && isSameDay(docEntry.seconds * 1000, entryTime.seconds * 1000)
                }
                
                if (sameDayOrders.isEmpty()) {
                    Log.d(TAG, "No same-day orders found for additional updates")
                    showPaymentSuccess()
                    return@addOnSuccessListener
                }
                
                var updatedCount = 0
                val totalToUpdate = sameDayOrders.size
                
                for (doc in sameDayOrders) {
                    val docId = doc.id
                    
                    // Create update fields map
                    val updateFields = mutableMapOf<String, Any>(
                        "status" to "paid",
                        "paidAmount" to amount,
                        "paymentDate" to Date(),
                        "paymentMethod" to "stripe",
                        "cardLast4" to "4242"
                    )
                    
                    // For web system records, add additional fields
                    if (doc.getString("source") == "plateRecognition" || doc.getString("source") == "web") {
                        updateFields["isPaid"] = true
                    }
                    
                    db.collection("Orders").document(docId)
                        .update(updateFields)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully updated same-day order $docId")
                            updatedCount++
                            if (updatedCount >= totalToUpdate) {
                                showPaymentSuccess()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to update same-day order $docId: ${e.message}")
                            updatedCount++
                            if (updatedCount >= totalToUpdate) {
                                showPaymentSuccess()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error finding same-day orders: ${e.message}")
                showPaymentSuccess()
            }
    }
    
    /**
     * Creates a new order and marks it as paid immediately.
     * This is a last resort when no matching order is found.
     */
    private fun createAndPayOrder(licensePlate: String, amount: Double) {
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            showPaymentError("User not logged in")
            return
        }
        
        // Create a new order with the current time
        val now = Date()
        val entryTime = Timestamp(Date(now.time - 3600000)) // 1 hour ago
        val exitTime = Timestamp(now)
        
        val orderData = hashMapOf(
            "licensePlate" to licensePlate,
            "userID" to currentUser.uid,
            "entryTime" to entryTime,
            "exitTime" to exitTime,
            "fee" to amount,
            "status" to "paid",
            "paidAmount" to amount,
            "paymentDate" to now,
            "paymentMethod" to "stripe",
            "cardLast4" to "4242",
            "isPaid" to true,
            "createdAt" to now,
            "updatedAt" to now,
            "source" to "mobile_payment" // Flag to indicate this was created by the mobile payment system
        )
        
        db.collection("Orders")
            .add(orderData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Created and paid new order with ID: ${documentReference.id}")
                showPaymentSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating new order: ${e.message}")
                showPaymentError("Failed to create new order: ${e.message}")
            }
    }
    
    private fun processPayment(amount: Double, licensePlate: String) {
        // Show processing UI
        progressBar.visibility = View.VISIBLE
        paymentStatusTextView.text = "Processing payment..."
        confirmPaymentButton.isEnabled = false
        cancelButton.isEnabled = false
        
        // In a real app, we would use the Stripe SDK to create a payment method and confirm payment
        // For example:
        // 1. Create a PaymentMethod with the card details
        // 2. Send the PaymentMethod ID to our server
        // 3. The server creates a PaymentIntent with Stripe and confirms the payment
        // 4. We handle the result in the client
        
        // For this demo, we'll simulate the payment processing
        simulateStripePaymentProcess(amount, licensePlate)
    }
    
    private fun simulateStripePaymentProcess(amount: Double, licensePlate: String) {
        // Simulate a network request delay
        Handler(Looper.getMainLooper()).postDelayed({
            // This is a simulated payment - in a real app, this would integrate with Stripe API
            
            // Update order status in Firestore
            updateOrderStatus(amount, licensePlate)
            
        }, 2000) // 2-second delay to simulate processing
    }
    
    private fun updateOrderStatus(amount: Double, licensePlate: String) {
        val currentUser = auth.currentUser
        
        // Use original license plate format
        // val formattedLicensePlate = licensePlate.replace(" ", "")
        
        if (currentUser != null) {
            // Try to use the orderId first if it was passed in
            val orderId = intent.getStringExtra("orderId")
            if (orderId != null) {
                updateOrderById(orderId, amount)
                return
            }
            
            // Otherwise find orders without using complex queries
            db.collection("Orders")
                .whereEqualTo("userID", currentUser.uid)
                .whereEqualTo("licensePlate", licensePlate)
                .get()
                .addOnSuccessListener { documents ->
                    // Filter in memory to find active or completed orders
                    val relevantOrders = documents.documents.filter { order ->
                        val status = order.getString("status") ?: ""
                        status in listOf("active", "completed")
                    }
                    
                    if (relevantOrders.isNotEmpty()) {
                        // Update the order status to "paid"
                        val order = relevantOrders[0]
                        val entryTime = order.getTimestamp("entryTime")
                        val exitTime = order.getTimestamp("exitTime")
                        
                        db.collection("Orders").document(order.id)
                            .update(
                                mapOf(
                                    "status" to "paid",
                                    "paidAmount" to amount,
                                    "paymentDate" to Date(),
                                    "paymentMethod" to "stripe", // Add payment method
                                    "cardLast4" to "4242" // Last 4 digits of test card
                                )
                            )
                            .addOnSuccessListener {
                                Log.d(TAG, "Order updated successfully")
                                
                                // Find and update any similar records to ensure consistency
                                if (entryTime != null && exitTime != null) {
                                    updateSimilarRecords(licensePlate, entryTime, exitTime, amount)
                                } else {
                                    showPaymentSuccess()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error updating order", e)
                                showPaymentError("Failed to update order: ${e.message}")
                            }
                    } else {
                        // No matching orders found, try to find by license plate only
                        findOrderByLicensePlate(licensePlate, amount)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting order", e)
                    showPaymentError("Error retrieving order information: ${e.message}")
                }
        } else {
            showPaymentError("User not logged in")
        }
    }
    
    /**
     * Updates the payment status of an order by ID.
     * After successful update, also finds and updates all similar records to ensure consistency.
     * 
     * @param orderId The ID of the order to update
     * @param amount The payment amount
     */
    private fun updateOrderById(orderId: String, amount: Double) {
        db.collection("Orders").document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get license plate and timestamps for finding similar records
                    val licensePlate = document.getString("licensePlate") ?: ""
                    val entryTime = document.getTimestamp("entryTime")
                    val exitTime = document.getTimestamp("exitTime")
                    
                    // Update the order status to "paid"
                    document.reference.update(
                        mapOf(
                            "status" to "paid",
                            "paidAmount" to amount,
                            "paymentDate" to Date(),
                            "paymentMethod" to "stripe",
                            "cardLast4" to "4242"
                        )
                    )
                    .addOnSuccessListener {
                        Log.d(TAG, "Order updated successfully by ID")
                        
                        // Find and update any similar records to ensure consistency
                        if (licensePlate.isNotEmpty() && entryTime != null && exitTime != null) {
                            updateSimilarRecords(licensePlate, entryTime, exitTime, amount)
                        } else {
                            showPaymentSuccess()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating order by ID", e)
                        showPaymentError("Failed to update order: ${e.message}")
                    }
                } else {
                    showPaymentError("Order not found")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting order by ID", e)
                showPaymentError("Error retrieving order: ${e.message}")
            }
    }
    
    /**
     * Finds and updates all records with the same license plate and similar entry/exit times.
     * Ensures payment status consistency across mobile app and web interface.
     * 
     * @param licensePlate The license plate to match
     * @param entryTime The entry timestamp to match
     * @param exitTime The exit timestamp to match
     * @param amount The payment amount
     */
    private fun updateSimilarRecords(
        licensePlate: String, 
        entryTime: com.google.firebase.Timestamp,
        exitTime: com.google.firebase.Timestamp,
        amount: Double
    ) {
        // Use original license plate format
        // val formattedLicensePlate = licensePlate.replace(" ", "")
        
        Log.d(TAG, "Finding similar records for plate: $licensePlate, entry: ${entryTime.seconds}, exit: ${exitTime.seconds}")
        
        // Find ALL orders with this license plate regardless of status
        db.collection("Orders")
            .get()
            .addOnSuccessListener { allDocuments ->
                Log.d(TAG, "Found ${allDocuments.size()} total records in database")
                
                // First filter to get all records with matching license plate
                val licensePlateRecords = allDocuments.documents.filter { doc ->
                    val docLicensePlate = doc.getString("licensePlate") ?: ""
                    docLicensePlate.equals(licensePlate, ignoreCase = true)
                }
                
                Log.d(TAG, "Found ${licensePlateRecords.size} records with license plate: $licensePlate")
                
                // Process all records with increasingly relaxed matching criteria
                
                // Group 1: Records with same entry/exit day (most important for synchronization)
                val sameDayRecords = licensePlateRecords.filter { doc ->
                    val docEntry = doc.getTimestamp("entryTime") ?: doc.getTimestamp("timestamp")
                    val docExit = doc.getTimestamp("exitTime")
                    
                    // Consider records from the same day
                    if (docEntry != null) {
                        val sameEntryDay = isSameDay(docEntry.seconds * 1000, entryTime.seconds * 1000)
                        
                        // If exit time exists, check that too, otherwise just match on entry day
                        if (docExit != null) {
                            val sameExitDay = isSameDay(docExit.seconds * 1000, exitTime.seconds * 1000)
                            return@filter sameEntryDay && sameExitDay
                        }
                        return@filter sameEntryDay
                    }
                    false
                }
                
                // Group 2: Any records specifically tagged as from web or plateRecognition systems
                val webSystemRecords = licensePlateRecords.filter { doc ->
                    val source = doc.getString("source") ?: ""
                    source in listOf("plateRecognition", "web", "web_interface", "anpr_system")
                }
                
                // Group 3: Any incomplete records (active/completed but not paid)
                val incompleteRecords = licensePlateRecords.filter { doc ->
                    val status = doc.getString("status") ?: ""
                    status in listOf("active", "completed", "pending", "") && status != "paid"
                }
                
                // Combine all records without duplicates
                val combinedRecords = LinkedHashSet<com.google.firebase.firestore.DocumentSnapshot>()
                combinedRecords.addAll(sameDayRecords) 
                combinedRecords.addAll(webSystemRecords)
                combinedRecords.addAll(incompleteRecords)
                
                val allRecordsToUpdate = combinedRecords.toList()
                
                Log.d(TAG, "Found ${sameDayRecords.size} records from same day, " +
                          "${webSystemRecords.size} web system records, and " +
                          "${incompleteRecords.size} incomplete records. " +
                          "Total unique records to update: ${allRecordsToUpdate.size}")
                
                if (allRecordsToUpdate.isEmpty()) {
                    Log.d(TAG, "No related records found to update")
                    showPaymentSuccess()
                    return@addOnSuccessListener
                }
                
                // Update all records to maintain consistency
                var updatedCount = 0
                val totalToUpdate = allRecordsToUpdate.size
                
                for (doc in allRecordsToUpdate) {
                    val docId = doc.id
                    val currentStatus = doc.getString("status") ?: ""
                    
                    Log.d(TAG, "Processing record $docId with status $currentStatus")
                    
                    // Create comprehensive update fields
                    val updateFields = mutableMapOf<String, Any>(
                        "status" to "paid",
                        "paidAmount" to amount,
                        "paymentDate" to Date(),
                        "paymentMethod" to "stripe",
                        "cardLast4" to "4242"
                    )
                    
                    // Important: Add all possible fields that different systems might use
                    updateFields["isPaid"] = true
                    updateFields["paid"] = true
                    updateFields["payment_status"] = "paid"
                    updateFields["paymentStatus"] = "paid"
                    updateFields["updatedAt"] = Date()
                    
                    // Update the record
                    db.collection("Orders").document(docId)
                        .update(updateFields)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully updated record $docId to paid status")
                            updatedCount++
                            checkIfComplete(updatedCount, totalToUpdate)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to update record $docId: ${e.message}")
                            updatedCount++
                            checkIfComplete(updatedCount, totalToUpdate)
                        }
                }
                
                // Also update any records in the "PlateRecognition" collection if it exists
                updatePlateRecognitionRecords(licensePlate)
                
                // Special case: if no updates were triggered, we still need to show success
                if (totalToUpdate == 0) {
                    showPaymentSuccess()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error finding records: ${e.message}")
                // Continue with payment success even if finding similar records fails
                showPaymentSuccess()
            }
    }
    
    /**
     * Update records in the PlateRecognition collection to ensure full system consistency
     */
    private fun updatePlateRecognitionRecords(licensePlate: String) {
        db.collection("PlateRecognition")
            .whereEqualTo("plate_number", licensePlate)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "No plate recognition records found for $licensePlate")
                    return@addOnSuccessListener
                }
                
                Log.d(TAG, "Found ${documents.size()} plate recognition records to update")
                
                val batch = db.batch()
                
                for (doc in documents.documents) {
                    val docRef = db.collection("PlateRecognition").document(doc.id)
                    batch.update(docRef, "payment_status", "paid")
                    batch.update(docRef, "isPaid", true)
                }
                
                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully updated all plate recognition records")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating plate recognition records: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error querying plate recognition records: ${e.message}")
            }
    }
    
    /**
     * Checks if all updates are complete and shows payment success.
     */
    private fun checkIfComplete(updatedCount: Int, totalToUpdate: Int) {
        if (updatedCount >= totalToUpdate) {
            Log.d(TAG, "All $updatedCount/$totalToUpdate records updated successfully")
            
            // Add a small delay before finishing to allow Firebase to sync
            Handler(Looper.getMainLooper()).postDelayed({
                showPaymentSuccess()
            }, 500)
        }
    }
    
    /**
     * Checks if two timestamps represent the same day.
     */
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val date1 = Date(timestamp1)
        val date2 = Date(timestamp2)
        
        val cal1 = java.util.Calendar.getInstance()
        val cal2 = java.util.Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.MONTH) == cal2.get(java.util.Calendar.MONTH) &&
               cal1.get(java.util.Calendar.DAY_OF_MONTH) == cal2.get(java.util.Calendar.DAY_OF_MONTH)
    }
    
    private fun showPaymentSuccess() {
        progressBar.visibility = View.GONE
        paymentStatusTextView.text = "Payment successful!"
        paymentStatusTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
        
        // Create success notification
        createPaymentSuccessNotification()
        
        // Show success message
        Toast.makeText(this, "Payment processed successfully", Toast.LENGTH_LONG).show()
        
        // Close activity after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 2000)
    }
    
    /**
     * Creates a payment success notification to inform the user
     * that their payment has been processed successfully.
     */
    private fun createPaymentSuccessNotification() {
        val currentUser = auth.currentUser ?: return
        val licensePlate = licensePlateTextView.text.toString()
        val amount = try {
            amountTextView.text.toString().replace("£", "").toDouble()
        } catch (e: Exception) {
            0.0
        }
        
        // Create a unique payment ID for reference
        val paymentId = "pay_" + UUID.randomUUID().toString().substring(0, 8)
        
        // Create notification data
        val notificationData = hashMapOf(
            "userId" to currentUser.uid,
            "title" to "Payment Successful",
            "message" to "Your payment of ${amountTextView.text} for $licensePlate has been processed successfully.",
            "type" to "PAYMENT_SUCCESS",
            "timestamp" to Timestamp.now(),
            "isRead" to false,
            "data" to mapOf(
                "paymentId" to paymentId,
                "amount" to amount,
                "licensePlate" to licensePlate
            )
        )
        
        // Save to Firestore
        db.collection("Notifications")
            .add(notificationData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Payment success notification created with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating payment success notification: ${e.message}", e)
                // Don't show error to user as this is a background operation
            }
    }
    
    private fun showPaymentError(errorMessage: String) {
        progressBar.visibility = View.GONE
        paymentStatusTextView.text = "Payment failed: $errorMessage"
        paymentStatusTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
        
        // Re-enable buttons
        confirmPaymentButton.isEnabled = true
        cancelButton.isEnabled = true
        
        // Show error message
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }
    
    private fun showPaymentView() {
        paymentLayout.visibility = View.VISIBLE
        receiptLayout.visibility = View.GONE
    }
    
    private fun showReceiptView() {
        paymentLayout.visibility = View.GONE
        receiptLayout.visibility = View.VISIBLE
        
        // Get receipt data from intent
        val paymentId = intent.getStringExtra("paymentId") ?: "Unknown"
        val amount = intent.getDoubleExtra("amount", 0.0)
        val licensePlate = intent.getStringExtra("licensePlate") ?: "Unknown"
        
        // Format current date for receipt
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
        val receiptDate = dateFormat.format(Date())
        
        // Update receipt UI
        receiptAmountTextView.text = "£${"%.2f".format(amount)}"
        receiptLicensePlateTextView.text = licensePlate
        receiptDateTextView.text = receiptDate
        receiptIdTextView.text = "Receipt ID: $paymentId"
    }
    
    private fun loadTimesFromNotification(notificationId: String) {
        // Get notification from Firestore
        db.collection("Notifications")
            .document(notificationId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val data = document.get("data") as? Map<String, Any> ?: mapOf()
                    
                    // Get entry and exit times
                    val entryTime = data["entryTime"] as? Timestamp
                    val exitTime = data["exitTime"] as? Timestamp
                    
                    // Format and display times
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                    
                    if (entryTime != null) {
                        val entryInstant = Instant.ofEpochMilli(entryTime.seconds * 1000)
                        val formattedEntryTime = entryInstant.atZone(ZoneId.systemDefault()).format(formatter)
                        entryTimeTextView.text = formattedEntryTime
                    }
                    
                    if (exitTime != null) {
                        val exitInstant = Instant.ofEpochMilli(exitTime.seconds * 1000)
                        val formattedExitTime = exitInstant.atZone(ZoneId.systemDefault()).format(formatter)
                        exitTimeTextView.text = formattedExitTime
                    }
                    
                    // If we have parkingName in the data, display it
                    val parkingName = data["parkingName"] as? String
                    if (parkingName != null) {
                        // Display parkingName somewhere if needed
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading notification data", e)
            }
    }
} 