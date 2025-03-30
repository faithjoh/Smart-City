package com.smartcity.parkingapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartcity.parkingapp.R

class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var licensePlateEditText: TextInputEditText
    private lateinit var registerButton: MaterialButton
    private lateinit var progressBar: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Initialize views
        nameEditText = findViewById(R.id.name_edit_text)
        emailEditText = findViewById(R.id.email_edit_text)
        passwordEditText = findViewById(R.id.password_edit_text)
        licensePlateEditText = findViewById(R.id.license_plate_edit_text)
        registerButton = findViewById(R.id.register_button)
        progressBar = findViewById(R.id.register_progress)
        
        // Set register button click listener
        registerButton.setOnClickListener {
            registerUser()
        }
        
        // Set login link click listener
        findViewById<View>(R.id.login_link).setOnClickListener {
            finish() // Return to login screen
        }
    }
    
    private fun registerUser() {
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val licensePlate = licensePlateEditText.text.toString().trim()
        
        // Validate inputs
        if (name.isEmpty()) {
            nameEditText.error = "Please enter your name"
            nameEditText.requestFocus()
            return
        }
        
        if (email.isEmpty()) {
            emailEditText.error = "Please enter your email"
            emailEditText.requestFocus()
            return
        }
        
        if (password.isEmpty() || password.length < 6) {
            passwordEditText.error = "Password must be at least 6 characters"
            passwordEditText.requestFocus()
            return
        }
        
        if (licensePlate.isEmpty()) {
            licensePlateEditText.error = "Please enter your license plate"
            licensePlateEditText.requestFocus()
            return
        }
        
        // Show progress bar
        progressBar.visibility = View.VISIBLE
        
        // Standardize license plate format
        // val formattedLicensePlate = licensePlate.replace(" ", "")
        
        // Create user with Firebase
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // User created successfully, save user info to database
                    val user = auth.currentUser
                    if (user != null) {
                        val userMap = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "licensePlate" to licensePlate
                        )
                        
                        // Save to Firestore
                        db.collection("Users").document(user.uid)
                            .set(userMap)
                            .addOnSuccessListener { 
                                // Hide progress bar
                                progressBar.visibility = View.GONE
                                
                                // Registration successful, show message and navigate to main screen
                                Toast.makeText(
                                    this,
                                    R.string.register_success,
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                // Go directly to main screen
                                val intent = Intent(this, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                // Hide progress bar
                                progressBar.visibility = View.GONE
                                
                                // Failed to save user data
                                Toast.makeText(
                                    this,
                                    "Failed to save user data: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                } else {
                    // Hide progress bar
                    progressBar.visibility = View.GONE
                    
                    // User creation failed
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
} 