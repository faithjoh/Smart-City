package com.smartcity.parkingapp.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.smartcity.parkingapp.R
import com.smartcity.parkingapp.model.PaymentRecord
import com.smartcity.parkingapp.ui.PaymentActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Fragment for displaying and managing payment history.
 * Shows a list of payment records associated with the user's license plates.
 */
class PaymentFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noRecordsText: TextView
    private lateinit var adapter: PaymentHistoryAdapter
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val paymentRecords = mutableListOf<PaymentRecord>()
    private val TAG = "PaymentFragment"
    
    /**
     * Custom PaymentHistoryAdapter implementation within the fragment
     * Manages the display of payment records in the RecyclerView
     */
    inner class PaymentHistoryAdapter(
        private val onPayButtonClick: (PaymentRecord) -> Unit
    ) : RecyclerView.Adapter<PaymentHistoryAdapter.PaymentViewHolder>() {
        
        private val records = mutableListOf<PaymentRecord>()
        private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        
        /**
         * ViewHolder for payment record items in the RecyclerView
         */
        inner class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val licensePlate: TextView = itemView.findViewById(R.id.license_plate_text)
            val entryTime: TextView = itemView.findViewById(R.id.entry_time_text)
            val exitTime: TextView = itemView.findViewById(R.id.exit_time_text)
            val fee: TextView = itemView.findViewById(R.id.fee_text)
            val status: TextView = itemView.findViewById(R.id.status_text)
            val payButton: Button = itemView.findViewById(R.id.pay_button)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_payment_history, parent, false)
            return PaymentViewHolder(itemView)
        }
        
        override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
            val record = records[position]
            
            // Format license plate for display (add space between characters if none exists)
            val formattedPlate = if (record.licensePlate.contains(" ")) {
                record.licensePlate
            } else {
                val plate = record.licensePlate
                if (plate.length >= 4) {
                    "${plate.substring(0, 4)} ${plate.substring(4)}"
                } else {
                    plate
                }
            }
            
            holder.licensePlate.text = "License Plate: $formattedPlate"
            
            try {
                holder.entryTime.text = "Entry: ${dateFormat.format(record.entryTime)}"
                holder.exitTime.text = "Exit: ${dateFormat.format(record.exitTime)}"
            } catch (e: Exception) {
                holder.entryTime.text = "Entry: Unknown"
                holder.exitTime.text = "Exit: Unknown"
            }
            
            holder.fee.text = "Fee: £${"%.2f".format(record.fee)}"
            
            if (record.isPaid) {
                holder.status.text = "Status: PAID"
                holder.status.setTextColor(Color.parseColor("#4CAF50")) // Green
                holder.payButton.visibility = View.GONE
            } else {
                holder.status.text = "Status: UNPAID"
                holder.status.setTextColor(Color.parseColor("#F44336")) // Red
                holder.payButton.visibility = View.VISIBLE
                holder.payButton.setOnClickListener {
                    onPayButtonClick(record)
                }
            }
        }
        
        override fun getItemCount() = records.size
        
        /**
         * Updates the adapter with new payment records
         */
        fun updateRecords(newRecords: List<PaymentRecord>) {
            records.clear()
            records.addAll(newRecords)
            notifyDataSetChanged()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        recyclerView = view.findViewById(R.id.payment_history_recycler_view)
        progressBar = view.findViewById(R.id.payment_progress_bar)
        noRecordsText = view.findViewById(R.id.no_payment_records_text)
        
        // Set up recycler view
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PaymentHistoryAdapter { paymentRecord ->
            // Handle payment for unpaid records
            if (!paymentRecord.isPaid) {
                val intent = Intent(activity, PaymentActivity::class.java)
                intent.putExtra("amount", paymentRecord.fee)
                intent.putExtra("licensePlate", paymentRecord.licensePlate)
                intent.putExtra("orderId", paymentRecord.orderId)
                
                // Also pass timestamps to ensure they are displayed in PaymentActivity
                try {
                    intent.putExtra("entryTime", com.google.firebase.Timestamp(paymentRecord.entryTime.time / 1000, 0))
                    intent.putExtra("exitTime", com.google.firebase.Timestamp(paymentRecord.exitTime.time / 1000, 0))
                } catch (e: Exception) {
                    // Ignore these extra information if time conversion fails
                }
                
                startActivity(intent)
            }
        }
        recyclerView.adapter = adapter
        
        // Load payment records
        loadPaymentHistory()
    }
    
    override fun onResume() {
        super.onResume()
        // Reload data when returning to fragment
        loadPaymentHistory()
    }
    
    /**
     * Loads payment history for the current user.
     * First retrieves user's license plates, then queries for orders
     * associated with those plates and the user's ID.
     */
    private fun loadPaymentHistory() {
        val currentUser = auth.currentUser ?: return
        
        progressBar.visibility = View.VISIBLE
        noRecordsText.visibility = View.GONE
        recyclerView.visibility = View.GONE
        paymentRecords.clear()
        
        // First, get the user's license plates
        db.collection("Users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Try to get license plate info in two ways
                    val licensePlatesFromList = document.get("licensePlates") as? List<String> ?: listOf()
                    val singleLicensePlate = document.getString("licensePlate") 
                    
                    val userLicensePlates = if (licensePlatesFromList.isNotEmpty()) {
                        licensePlatesFromList
                    } else if (!singleLicensePlate.isNullOrEmpty()) {
                        listOf(singleLicensePlate)
                    } else {
                        listOf()
                    }
                    
                    if (userLicensePlates.isEmpty()) {
                        showNoRecords("No license plates found for your account")
                        return@addOnSuccessListener
                    }
                    
                    // Normalize license plate numbers (remove spaces)
                    val formattedLicensePlates = userLicensePlates.map { plate -> 
                        plate.replace(" ", "")
                    }
                    
                    // Create batch queries (separate query for each plate)
                    val queries = formattedLicensePlates.map { plate ->
                        db.collection("Orders")
                            .whereEqualTo("licensePlate", plate)
                    }
                    
                    // Directly query orders associated with user ID
                    db.collection("Orders")
                        .whereEqualTo("userID", currentUser.uid)
                        .get()
                        .addOnSuccessListener { userIdDocuments ->
                            // Process results from user ID query
                            processOrderDocuments(userIdDocuments.documents)
                            
                            // Continue querying for each license plate
                            if (queries.isNotEmpty()) {
                                processLicensePlateQueries(queries)
                            } else {
                                updateUI()
                            }
                        }
                        .addOnFailureListener { e ->
                            // Even if user ID query fails, still try license plate queries
                            if (queries.isNotEmpty()) {
                                processLicensePlateQueries(queries)
                            } else {
                                showError("Error loading user orders: ${e.message}")
                            }
                        }
                } else {
                    showNoRecords("User profile not found")
                }
            }
            .addOnFailureListener { e ->
                showError("Error loading user data: ${e.message}")
            }
    }
    
    /**
     * Processes queries for orders associated with license plates.
     * Executes each query and aggregates results.
     * 
     * @param queries List of Firestore queries for license plates
     */
    private fun processLicensePlateQueries(queries: List<Query>) {
        var completedQueries = 0
        val totalQueries = queries.size
        
        for (query in queries) {
            query.get().addOnSuccessListener { documents ->
                // Process results for this license plate
                processOrderDocuments(documents.documents)
                
                // Check if all queries have completed
                completedQueries++
                if (completedQueries >= totalQueries) {
                    // All queries completed, update UI
                    updateUI()
                }
            }.addOnFailureListener { e ->
                // Continue processing even if individual query fails
                completedQueries++
                if (completedQueries >= totalQueries) {
                    updateUI()
                }
            }
        }
    }
    
    /**
     * Processes order documents and adds them to payment records.
     * Extracts order details and creates PaymentRecord objects.
     * Enhanced to better handle duplicate records based on order ID, 
     * or the combination of license plate and entry/exit times.
     * 
     * @param documents List of Firestore document snapshots representing orders
     */
    private fun processOrderDocuments(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        for (document in documents) {
            try {
                val licensePlate = document.getString("licensePlate") ?: "Unknown"
                val entryTime = document.getTimestamp("entryTime")?.toDate() ?: Date()
                val exitTime = document.getTimestamp("exitTime")?.toDate() ?: Date()
                val fee = document.getDouble("fee") ?: 0.0
                val status = document.getString("status") ?: "active"
                val isPaid = status == "paid"
                
                val record = PaymentRecord(
                    orderId = document.id,
                    licensePlate = licensePlate,
                    entryTime = entryTime,
                    exitTime = exitTime,
                    fee = fee,
                    isPaid = isPaid
                )
                
                // Enhanced duplicate detection - check both order ID and time-based matching
                val existingRecordById = paymentRecords.find { it.orderId == record.orderId }
                val existingRecordByDetails = paymentRecords.find { 
                    it.licensePlate == record.licensePlate && 
                    abs(it.entryTime.time - record.entryTime.time) < 1000 && // Allow 1 second difference
                    abs(it.exitTime.time - record.exitTime.time) < 1000
                }
                
                when {
                    // If we found the exact same order ID
                    existingRecordById != null -> {
                        // Update existing record if the new one is paid and the existing one isn't
                        if (record.isPaid && !existingRecordById.isPaid) {
                            existingRecordById.isPaid = true
                        }
                    }
                    
                    // If we found a record with the same license plate and very close times
                    existingRecordByDetails != null -> {
                        // Update existing record if the new one is paid and the existing one isn't
                        if (record.isPaid && !existingRecordByDetails.isPaid) {
                            existingRecordByDetails.isPaid = true
                        }
                    }
                    
                    // No duplicate found, add the new record
                    else -> {
                        paymentRecords.add(record)
                    }
                }
            } catch (e: Exception) {
                // Ignore errors when processing individual documents
            }
        }
    }
    
    /**
     * Updates the UI with the loaded payment records.
     * Sorts records and displays them in the RecyclerView.
     */
    private fun updateUI() {
        progressBar.visibility = View.GONE
        
        if (paymentRecords.isEmpty()) {
            showNoRecords("No payment records found")
            return
        }
        
        // Sort records by entry time (newest to oldest)
        val sortedRecords = paymentRecords.sortedByDescending { it.entryTime }
        
        adapter.updateRecords(sortedRecords)
        recyclerView.visibility = View.VISIBLE
        noRecordsText.visibility = View.GONE
    }
    
    /**
     * Shows a message when no records are found.
     * 
     * @param message The message to display
     */
    private fun showNoRecords(message: String) {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.GONE
        noRecordsText.visibility = View.VISIBLE
        noRecordsText.text = message
    }
    
    /**
     * Shows an error message and updates UI.
     * 
     * @param message The error message to display
     */
    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        showNoRecords("Failed to load payment records")
    }
} 