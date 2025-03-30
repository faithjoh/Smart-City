package com.smartcity.parkingapp.ui

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.smartcity.parkingapp.R
import com.smartcity.parkingapp.model.ParkingSpot
import com.smartcity.parkingapp.model.Review
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParkingDetailsDialog : DialogFragment() {
    
    private lateinit var parkingSpot: ParkingSpot
    private lateinit var reviewsAdapter: ReviewAdapter
    private val reviews = mutableListOf<Review>()
    
    // Firebase
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    // UI Components
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var newReviewRating: RatingBar
    private lateinit var newReviewComment: TextInputEditText
    private lateinit var submitReviewButton: Button

    /**
     * Custom ReviewAdapter implementation within the dialog
     */
    inner class ReviewAdapter(private val reviewsList: MutableList<Review>) : 
        RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {
        
        private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        
        inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userName: TextView = itemView.findViewById(R.id.review_user_name)
            val reviewDate: TextView = itemView.findViewById(R.id.review_date)
            val rating: RatingBar = itemView.findViewById(R.id.review_rating)
            val comment: TextView = itemView.findViewById(R.id.review_comment)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
            Log.d("ReviewAdapter", "Creating new ViewHolder for position with viewType: $viewType")
            
            try {
                // Try to inflate the review item layout
                val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_review, parent, false)
                
                // Log successful inflation
                Log.d("ReviewAdapter", "Successfully inflated item_review layout")
                
                return ReviewViewHolder(itemView)
            } catch (e: Exception) {
                // Log error and try a fallback approach
                Log.e("ReviewAdapter", "Error inflating item_review layout: ${e.message}", e)
                
                // Create a TextView as fallback
                val textView = TextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    text = "Error loading review"
                    setPadding(20, 20, 20, 20)
                }
                
                // Create an empty view with the TextView
                val fallbackView = LinearLayout(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    orientation = LinearLayout.VERTICAL
                    addView(textView)
                }
                
                // Create a ViewHolder with fallback view
                return ReviewViewHolder(fallbackView)
            }
        }
        
        override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
            try {
                // Check if index is out of bounds
                if (position < 0 || position >= reviewsList.size) {
                    Log.e("ReviewAdapter", "Attempted to bind view holder at invalid position: $position, list size: ${reviewsList.size}")
                    // Display an error message instead of crashing
                    holder.userName.text = "Error"
                    holder.reviewDate.text = ""
                    holder.rating.rating = 0f
                    holder.comment.text = "Data loading error"
                    return
                }
                
                val currentReview = reviewsList[position]
                
                // Log each review being displayed
                Log.d("ReviewAdapter", "Binding review at position $position: ${currentReview.id}, ${currentReview.userName}, ${currentReview.comment}")
                
                // Handle special display for empty reviews
                if (currentReview.id == "empty") {
                    holder.userName.text = currentReview.userName
                    holder.reviewDate.visibility = View.GONE
                    holder.rating.visibility = View.GONE
                    holder.comment.text = currentReview.comment
                    return
                } else {
                    holder.reviewDate.visibility = View.VISIBLE
                    holder.rating.visibility = View.VISIBLE
                }
                
                // Set user name with fallback
                holder.userName.text = if (currentReview.userName.isNotEmpty()) {
                    currentReview.userName
                } else {
                    "Anonymous User"
                }
                
                // Format the date with error handling
                var formattedDate = "Unknown date"
                try {
                    if (currentReview.date.isNotEmpty()) {
                        // First try to use the date string field
                        formattedDate = currentReview.date
                    } else {
                        // Fall back to timestamp if available
                        val date = Date(currentReview.timestamp.seconds * 1000)
                        formattedDate = dateFormat.format(date)
                    }
                } catch (e: Exception) {
                    Log.e("ReviewAdapter", "Error formatting date: ${e.message}")
                }
                
                // Set rating and handle invalid values
                holder.rating.rating = when {
                    currentReview.rating < 0 -> 0f
                    currentReview.rating > 5 -> 5f
                    else -> currentReview.rating
                }
                
                // Set date and comment with fallbacks
                holder.reviewDate.text = formattedDate
                holder.comment.text = if (currentReview.comment.isNotEmpty()) {
                    currentReview.comment
                } else {
                    "(No comment provided)"
                }
            } catch (e: Exception) {
                // Fallback for any unexpected errors
                Log.e("ReviewAdapter", "Error displaying review: ${e.message}", e)
                holder.userName.text = "User"
                holder.reviewDate.text = "Unknown date"
                holder.rating.rating = 0f
                holder.comment.text = "Error displaying review"
            }
        }
        
        override fun getItemCount() = reviewsList.size
        
        fun updateReviews(newReviews: List<Review>) {
            try {
                Log.d("ReviewAdapter", "Updating reviews. Old size: ${reviewsList.size}, New size: ${newReviews.size}")
                
                // Create a temporary list to avoid directly modifying adapter data
                val tempList = ArrayList<Review>(newReviews)
                
                // Disable notifications before cleaning and updating to avoid intermediate state errors
                reviewsRecyclerView.post {
                    // Safely update on the main thread
                    reviewsList.clear()
                    reviewsList.addAll(tempList)
                    
                    Log.d("ReviewAdapter", "Reviews list updated to size: ${reviewsList.size}")
                    notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("ReviewAdapter", "Error updating reviews: ${e.message}", e)
            }
        }
    }
    
    companion object {
        private const val ARG_PARKING_SPOT = "arg_parking_spot"
        
        fun newInstance(parkingSpot: ParkingSpot): ParkingDetailsDialog {
            val fragment = ParkingDetailsDialog()
            val args = Bundle()
            args.putSerializable(ARG_PARKING_SPOT, parkingSpot)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.DialogTheme)
        
        // Set dialog window width and height
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        
        arguments?.let {
            @Suppress("DEPRECATION")
            parkingSpot = it.getSerializable(ARG_PARKING_SPOT) as ParkingSpot
            
            // Modern approach would be:
            // parkingSpot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //     it.getSerializable(ARG_PARKING_SPOT, ParkingSpot::class.java)!!
            // } else {
            //     @Suppress("DEPRECATION")
            //     it.getSerializable(ARG_PARKING_SPOT) as ParkingSpot
            // }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_parking_details, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val nameTextView = view.findViewById<TextView>(R.id.parking_name)
        val addressTextView = view.findViewById<TextView>(R.id.parking_address)
        val priceTextView = view.findViewById<TextView>(R.id.parking_price)
        val slotsTextView = view.findViewById<TextView>(R.id.parking_slots)
        val hoursTextView = view.findViewById<TextView>(R.id.parking_hours)
        val ratingBar = view.findViewById<RatingBar>(R.id.parking_rating)
        
        val navigateButton = view.findViewById<Button>(R.id.navigate_button)
        val closeButton = view.findViewById<View>(R.id.close_button)
        
        // Initialize review components
        reviewsRecyclerView = view.findViewById(R.id.reviews_recycler_view)
        newReviewRating = view.findViewById(R.id.new_review_rating)
        newReviewComment = view.findViewById(R.id.new_review_comment)
        submitReviewButton = view.findViewById(R.id.submit_review_button)
        
        // Set up RecyclerView with additional configuration
        reviewsAdapter = ReviewAdapter(reviews)
        reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reviewsAdapter
            setHasFixedSize(false)  // Allow dynamic sizing
            isNestedScrollingEnabled = true  // Enable nested scrolling
            visibility = View.VISIBLE  // Ensure visibility
            
            // Add extra padding and spacing for better visibility
            setPadding(8, 8, 8, 8)
            clipToPadding = false
            
            // Add item decoration for spacing between items
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.top = 8
                    outRect.bottom = 8
                }
            })
        }
        
        // Set parking spot details
        nameTextView.text = parkingSpot.name
        addressTextView.text = parkingSpot.address
        priceTextView.text = "£${parkingSpot.price}/hour"
        slotsTextView.text = "${parkingSpot.totalSlots} spaces"
        hoursTextView.text = parkingSpot.openHours
        ratingBar.rating = parkingSpot.rating
        
        // Set button click listeners
        navigateButton.setOnClickListener {
            openNavigation(parkingSpot.latitude, parkingSpot.longitude)
        }
        
        // Set close button click listener
        closeButton.setOnClickListener {
            dismiss()
        }
        
        // Set submit review button click listener
        submitReviewButton.setOnClickListener {
            submitReview()
        }
        
        // Load reviews
        loadReviews()
    }
    
    /**
     * Log the current state of reviews and UI components for debugging purposes
     */
    private fun logReviewsAndUiState() {
        Log.d("ParkingDetailsDialog", "Current reviews count: ${reviews.size}")
        reviews.forEachIndexed { index, review ->
            Log.d("ParkingDetailsDialog", "Review[$index]: id=${review.id}, userId=${review.userId}, spotId=${review.spotId}, rating=${review.rating}")
        }
        
        val recyclerViewVisible = reviewsRecyclerView.visibility == View.VISIBLE
        val adapterItemCount = reviewsAdapter.itemCount
        
        Log.d("ParkingDetailsDialog", "UI State: RecyclerView visible=$recyclerViewVisible, adapter item count=$adapterItemCount")
    }
    
    private fun loadReviews() {
        // Clear the existing reviews list first
        reviews.clear()
        
        Log.d("ParkingDetailsDialog", "Loading reviews for parking spot: id=${parkingSpot.id}, name=${parkingSpot.name}")
        
        // Query Firestore for reviews with matching spotId
        db.collection("Reviews")
            .whereEqualTo("spotId", parkingSpot.id)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("ParkingDetailsDialog", "Retrieved ${documents.size()} reviews for spotId=${parkingSpot.id}")
                
                if (documents.isEmpty) {
                    Log.d("ParkingDetailsDialog", "No reviews found for this parking spot")
                    
                    // Try loading all reviews to see what's available for debugging
                    logAllReviewsForDebugging()
                } else {
                    Log.d("ParkingDetailsDialog", "Processing ${documents.size()} reviews")
                    
                    // Process each document
                    for (document in documents) {
                        try {
                            val reviewData = document.data
                            Log.d("ParkingDetailsDialog", "Review data: $reviewData")
                            
                            val review = document.toObject(Review::class.java).copy(id = document.id)
                            Log.d("ParkingDetailsDialog", "Converted to Review object: $review")
                            reviews.add(review)
                        } catch (e: Exception) {
                            Log.e("ParkingDetailsDialog", "Error converting document to Review: ${e.message}", e)
                        }
                    }
                    
                    // Sort reviews by timestamp (newest first)
                    reviews.sortByDescending { it.timestamp }
                    
                    // Update UI with retrieved reviews
                    updateReviewsList()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ParkingDetailsDialog", "Error getting reviews: ${exception.message}", exception)
                Toast.makeText(context, "Failed to load reviews", Toast.LENGTH_SHORT).show()
            }
    }
    
    /**
     * For debugging purposes - logs all reviews in the database to check their structure and spotId values
     */
    private fun logAllReviewsForDebugging() {
        db.collection("Reviews")
            .get()
            .addOnSuccessListener { documents ->
                Log.d("ParkingDetailsDialog", "DEBUG: Total reviews in database: ${documents.size()}")
                
                if (documents.isEmpty) {
                    Log.d("ParkingDetailsDialog", "DEBUG: No reviews found in the entire database")
                } else {
                    Log.d("ParkingDetailsDialog", "DEBUG: Listing all reviews to find potential matches:")
                    for (document in documents) {
                        val spotId = document.getString("spotId") ?: "null"
                        Log.d("ParkingDetailsDialog", "DEBUG: Review[${document.id}] has spotId=$spotId (looking for ${parkingSpot.id})")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ParkingDetailsDialog", "DEBUG: Error listing all reviews: ${exception.message}", exception)
            }
    }
    
    /**
     * Updates the reviews list in the UI
     */
    private fun updateReviewsList() {
        Log.d("ParkingDetailsDialog", "Updating reviews list with ${reviews.size} reviews")
        
        // Notify adapter of data change
        reviewsAdapter.notifyDataSetChanged()
        
        // Ensure recyclerview is visible and configured correctly
        reviewsRecyclerView.visibility = if (reviews.isEmpty()) View.GONE else View.VISIBLE
        
        // Log the final state
        logReviewsAndUiState()
    }
    
    private fun submitReview() {
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            Toast.makeText(context, "You must be logged in to submit a review", Toast.LENGTH_SHORT).show()
            return
        }
        
        val rating = newReviewRating.rating
        val comment = newReviewComment.text.toString().trim()
        
        if (rating == 0f) {
            Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (comment.isEmpty()) {
            Toast.makeText(context, "Please write a comment", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Disable submit button to prevent multiple submissions
        submitReviewButton.isEnabled = false
        
        // Log the review data being submitted
        Log.d("ParkingDetailsDialog", "Submitting review for spot: ${parkingSpot.id}, rating: $rating, comment: $comment")
        
        // Get user name from Firestore
        db.collection("Users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val userName = if (document.exists()) {
                    document.getString("name") ?: "User"
                } else {
                    "User"
                }
                
                // Create current date string
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val currentDate = dateFormat.format(Date())
                val timestamp = Timestamp.now()
                
                // Log user and date information
                Log.d("ParkingDetailsDialog", "Creating review as user: $userName, date: $currentDate")
                
                // Create review data with consistent field names
                val reviewData = hashMapOf(
                    "userId" to currentUser.uid,
                    "userName" to userName,
                    "rating" to rating,
                    "comment" to comment,
                    "spotId" to parkingSpot.id,  // Make sure this matches the query field
                    "date" to currentDate,
                    "timestamp" to timestamp
                )
                
                // Log the complete review data before saving
                Log.d("ParkingDetailsDialog", "Review data to save: $reviewData")
                
                // Save to Firestore
                db.collection("Reviews")
                    .add(reviewData)
                    .addOnSuccessListener { documentReference ->
                        // Get the new document ID
                        val newReviewId = documentReference.id
                        Log.d("ParkingDetailsDialog", "Review saved successfully with ID: $newReviewId")
                        
                        // Add the new review to the local list directly
                        val newReview = Review(
                            id = newReviewId,
                            userId = currentUser.uid,
                            userName = userName,
                            rating = rating,
                            comment = comment,
                            spotId = parkingSpot.id,
                            date = currentDate,
                            timestamp = timestamp
                        )
                        
                        // Clear form
                        newReviewRating.rating = 0f
                        newReviewComment.setText("")
                        Toast.makeText(context, "Review submitted successfully", Toast.LENGTH_SHORT).show()
                        
                        // Re-enable submit button
                        submitReviewButton.isEnabled = true

                        // Delay reloading to ensure UI updates first
                        reviewsRecyclerView.post {
                            // Safely load all comments
                            loadReviews()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ParkingDetailsDialog", "Failed to submit review: ${e.message}", e)
                        Toast.makeText(context, "Failed to submit review: ${e.message}", Toast.LENGTH_SHORT).show()
                        
                        // Re-enable submit button
                        submitReviewButton.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ParkingDetailsDialog", "Failed to get user data: ${e.message}", e)
                Toast.makeText(context, "Failed to get user data: ${e.message}", Toast.LENGTH_SHORT).show()
                submitReviewButton.isEnabled = true
            }
    }
    
    private fun openNavigation(latitude: Double, longitude: Double) {
        val navigationUri = Uri.parse("google.navigation:q=$latitude,$longitude")
        val mapIntent = Intent(Intent.ACTION_VIEW, navigationUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        
        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapIntent)
        }
    }
} 