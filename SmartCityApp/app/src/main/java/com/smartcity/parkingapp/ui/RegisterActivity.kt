package com.smartcity.parkingapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.smartcity.parkingapp.R
import com.smartcity.parkingapp.SmartCityApplication
import com.smartcity.parkingapp.utils.LogCollector

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var progressBar: ProgressBar
    private lateinit var registerButton: Button
    private lateinit var termsCheckbox: CheckBox
    private lateinit var logCollector: LogCollector
    
    private val TAG = "RegisterActivity"
    private val TIMEOUT_DELAY = 10000L // 10 seconds
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Get LogCollector instance
        logCollector = (application as SmartCityApplication).getLogCollector()
        
        val nameEditText = findViewById<EditText>(R.id.name_edit_text)
        val emailEditText = findViewById<EditText>(R.id.email_edit_text)
        val passwordEditText = findViewById<EditText>(R.id.password_edit_text)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirm_password_edit_text)
        val licensePlateEditText = findViewById<EditText>(R.id.license_plate_edit_text)
        registerButton = findViewById<Button>(R.id.register_button)
        val loginLink = findViewById<TextView>(R.id.login_link)
        progressBar = findViewById(R.id.register_progress)
        termsCheckbox = findViewById(R.id.terms_checkbox)
        val readTermsButton = findViewById<Button>(R.id.read_terms_button)
        
        Log.d(TAG, "Activity created, views initialized")
        
        // Log screen view
        logCollector.logActivity("view_registration_screen")
        
        // Navigate to Login
        loginLink.setOnClickListener {
            Log.d(TAG, "Login link clicked")
            logCollector.logActivity("registration_to_login_navigation")
            finish() // Go back to login screen
        }
        
        // Show terms and conditions dialog
        readTermsButton.setOnClickListener {
            Log.d(TAG, "Read terms button clicked")
            logCollector.logActivity("view_terms_and_conditions")
            showTermsAndConditionsDialog()
        }
        
        // Log when user accepts terms
        termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                logCollector.logActivityWithBoolean("accept_terms_and_conditions", 
                    "data_collection_consent", true)
                Log.d(TAG, "Terms and conditions accepted")
            }
        }
        
        registerButton.setOnClickListener {
            Log.d(TAG, "Register button clicked")
            
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val licensePlate = licensePlateEditText.text.toString().trim()
            
            Log.d(TAG, "Inputs: name=$name, email=$email, password length=${password.length}, license=$licensePlate")
            
            // Validate inputs
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || 
                confirmPassword.isEmpty() || licensePlate.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Validation failed: Empty required fields")
                logCollector.logActivity("registration_validation_failed", 
                    mapOf("reason" to "empty_fields"))
                return@setOnClickListener
            }
            
            // Check if terms and conditions are accepted
            if (!termsCheckbox.isChecked) {
                Toast.makeText(this, R.string.error_accept_terms, Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Validation failed: Terms and conditions not accepted")
                logCollector.logActivity("registration_validation_failed", 
                    mapOf("reason" to "terms_not_accepted"))
                return@setOnClickListener
            }
            
            // Validate email format
            if (!isValidEmail(email)) {
                Toast.makeText(this, R.string.invalid_email_format, Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Validation failed: Invalid email format")
                logCollector.logActivity("registration_validation_failed", 
                    mapOf("reason" to "invalid_email"))
                return@setOnClickListener
            }
            
            if (password != confirmPassword) {
                Toast.makeText(this, R.string.passwords_not_match, Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Validation failed: Passwords don't match")
                logCollector.logActivity("registration_validation_failed", 
                    mapOf("reason" to "passwords_dont_match"))
                return@setOnClickListener
            }
            
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Validation failed: Password too short")
                logCollector.logActivity("registration_validation_failed", 
                    mapOf("reason" to "password_too_short"))
                return@setOnClickListener
            }
            
            // Validate license plate format
            if (!isValidLicensePlate(licensePlate)) {
                Toast.makeText(this, "Please enter a valid license plate", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Validation failed: Invalid license plate format")
                logCollector.logActivity("registration_validation_failed", 
                    mapOf("reason" to "invalid_license_plate"))
                return@setOnClickListener
            }
            
            // Log registration attempt
            logCollector.logActivity("registration_attempt")
            
            // Show toast for debugging
            Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()
            
            // Disable button and show progress
            registerButton.isEnabled = false
            progressBar.visibility = View.VISIBLE
            
            // Set timeout for operation
            val timeoutHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                Log.e(TAG, "Database operation timeout")
                Toast.makeText(
                    this,
                    "Registration timed out. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
                
                // Log timeout
                logCollector.logError("registration_timeout", "Database operation timed out")
                
                // Check if user was created but database failed
                if (auth.currentUser != null) {
                    Log.d(TAG, "User created but DB save timed out. Moving to main activity.")
                    // Navigate to MainActivity anyway
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    resetUI()
                }
            }
            
            // Start timeout timer
            timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_DELAY)
            
            // Create user with email and password
            try {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        try {
                            if (task.isSuccessful) {
                                // User created successfully
                                val userId = auth.currentUser?.uid
                                Log.d(TAG, "User created with ID: $userId")
                                
                                if (userId != null) {
                                    // Create user data
                                    val userMap = hashMapOf(
                                        "id" to userId,
                                        "name" to name,
                                        "email" to email,
                                        "licensePlate" to licensePlate,
                                        "licensePlates" to listOf(licensePlate),
                                        "dataCollectionConsent" to true,
                                        "consentTimestamp" to System.currentTimeMillis()
                                    )
                                    
                                    // Log successful user creation
                                    logCollector.logActivity("user_created", 
                                        mapOf("userId" to userId))
                                    
                                    // Save to Firestore
                                    saveUserToFirestore(userId, userMap, timeoutHandler, timeoutRunnable)
                                } else {
                                    timeoutHandler.removeCallbacks(timeoutRunnable)
                                    Log.e(TAG, "User ID is null after successful registration")
                                    Toast.makeText(this, "Registration error: User ID is null", Toast.LENGTH_LONG).show()
                                    logCollector.logError("registration_error", "User ID is null after successful auth")
                                    resetUI()
                                }
                            } else {
                                // Registration failed
                                timeoutHandler.removeCallbacks(timeoutRunnable)
                                val exception = task.exception
                                Log.e(TAG, "Auth failed: ${exception?.message}", exception)
                                
                                if (exception is FirebaseAuthUserCollisionException) {
                                    // Email already in use
                                    logCollector.logActivity("registration_failed", 
                                        mapOf("reason" to "email_already_exists"))
                                    showEmailExistsDialog(email)
                                } else {
                                    // Other registration error
                                    logCollector.logError("registration_error", 
                                        "Auth failed: ${exception?.message}", 
                                        exception?.stackTraceToString())
                                    
                                    Toast.makeText(
                                        this,
                                        "Registration failed: ${exception?.message ?: "Unknown error"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                resetUI()
                            }
                        } catch (e: Exception) {
                            // Catch any unexpected exceptions
                            timeoutHandler.removeCallbacks(timeoutRunnable)
                            Log.e(TAG, "Unexpected error in registration: ${e.message}", e)
                            Toast.makeText(this, "An unexpected error occurred", Toast.LENGTH_LONG).show()
                            logCollector.logError("registration_unexpected_error", 
                                e.message ?: "Unknown error", 
                                e.stackTraceToString())
                            resetUI()
                        }
                    }
            } catch (e: Exception) {
                // Catch any unexpected exceptions during auth creation
                timeoutHandler.removeCallbacks(timeoutRunnable)
                Log.e(TAG, "Unexpected error during auth creation: ${e.message}", e)
                Toast.makeText(this, "Failed to start registration", Toast.LENGTH_LONG).show()
                logCollector.logError("registration_auth_error", 
                    e.message ?: "Unknown error", 
                    e.stackTraceToString())
                resetUI()
            }
        }
    }
    
    private fun saveUserToFirestore(
        userId: String, 
        userMap: Map<String, Any>,
        timeoutHandler: Handler,
        timeoutRunnable: Runnable
    ) {
        Log.d(TAG, "Saving user data to Firestore for user: $userId")
        
        try {
            // Use Firestore to save user data
            db.collection("Users")
                .document(userId)
                .set(userMap)
                .addOnSuccessListener {
                    // Cancel timeout timer
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    
                    Log.d(TAG, "User data saved successfully to Firestore")
                    Toast.makeText(
                        this@RegisterActivity,
                        R.string.register_success,
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Log successful registration completion
                    logCollector.logActivity("registration_completed", 
                        mapOf("userId" to userId))
                    
                    // Navigate to main screen
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    // Cancel timeout timer
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    
                    Log.e(TAG, "Failed to save user data to Firestore: ${e.message}")
                    Toast.makeText(
                        this@RegisterActivity,
                        "Failed to save user data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Log error
                    logCollector.logError("firestore_save_error", 
                        "Failed to save user data: ${e.message}",
                        e.stackTraceToString())
                    
                    // Try to navigate to main screen anyway
                    // since authentication was successful
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
        } catch (e: Exception) {
            // Handle any exceptions during Firestore operation
            timeoutHandler.removeCallbacks(timeoutRunnable)
            Log.e(TAG, "Error saving user data to Firestore: ${e.message}", e)
            
            // Log error
            logCollector.logError("firestore_exception", 
                "Error saving user data: ${e.message}",
                e.stackTraceToString())
            
            // Try to navigate to main screen anyway
            Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        // Use Android's Patterns class to validate email format
        val pattern = Patterns.EMAIL_ADDRESS
        
        // Additional check for domain - must have at least 2 parts with minimum length
        if (pattern.matcher(email).matches()) {
            val parts = email.split("@")
            if (parts.size == 2) {
                val domain = parts[1]
                val domainParts = domain.split(".")
                
                // Check domain format - must have at least one dot and each part must be valid
                return domainParts.size >= 2 && 
                       domainParts.all { it.isNotEmpty() } && 
                       domainParts.last().length >= 2 // Top level domain must be at least 2 chars
            }
        }
        return false
    }
    
    private fun isValidLicensePlate(licensePlate: String): Boolean {
        // Remove all spaces for validation
        val formattedPlate = licensePlate.replace(" ", "")
        
        // Basic validation: non-empty and appropriate length
        if (formattedPlate.isEmpty() || formattedPlate.length < 5 || formattedPlate.length > 10) {
            Toast.makeText(this, "License plate should be 5-10 characters long", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // License plate format validation
        // Ensure the plate contains both letters and numbers
        val hasLetter = formattedPlate.any { it.isLetter() }
        val hasDigit = formattedPlate.any { it.isDigit() }
        
        if (!hasLetter || !hasDigit) {
            Toast.makeText(this, "License plate must contain both letters and numbers", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check space format: UK license plate format is typically "AA12 BBB" 
        val containsSpace = licensePlate.contains(" ")
        if (!containsSpace) {
            Toast.makeText(this, "Please enter license plate with standard format, e.g.: AA12 BBB", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun resetUI() {
        registerButton.isEnabled = true
        progressBar.visibility = View.GONE
    }
    
    private fun showEmailExistsDialog(email: String) {
        Log.d(TAG, "Showing email exists dialog for: $email")
        AlertDialog.Builder(this)
            .setTitle("Email Already Registered")
            .setMessage("The email address $email is already registered. Would you like to log in instead?")
            .setPositiveButton("Go to Login") { _, _ ->
                // Navigate to login screen
                Log.d(TAG, "User chose to go to login")
                logCollector.logActivity("email_exists_go_to_login")
                finish()
            }
            .setNegativeButton("Try Another Email") { dialog, _ ->
                Log.d(TAG, "User chose to try another email")
                logCollector.logActivity("email_exists_try_another")
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    override fun onStop() {
        super.onStop()
        // Make sure progress bar is hidden if activity is stopped
        progressBar.visibility = View.GONE
        
        // Log activity exit
        logCollector.logActivity("exit_registration_screen")
    }
    
    /**
     * Shows the terms and conditions dialog
     */
    private fun showTermsAndConditionsDialog() {
        Log.d(TAG, "Showing terms and conditions dialog")
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_terms_conditions, null)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Set close button click listener
        dialogView.findViewById<Button>(R.id.close_button).setOnClickListener {
            dialog.dismiss()
            logCollector.logActivity("close_terms_and_conditions")
        }
        
        dialog.show()
    }
} 