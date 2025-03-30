package com.smartcity.parkingapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.smartcity.parkingapp.R

class LoginActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        val emailEditText = findViewById<EditText>(R.id.email_edit_text)
        val passwordEditText = findViewById<EditText>(R.id.password_edit_text)
        val loginButton = findViewById<Button>(R.id.login_button)
        val registerTextView = findViewById<TextView>(R.id.register_text_view)
        val forgotPasswordTextView = findViewById<TextView>(R.id.forgot_password_text_view)
        
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Show progress
            loginButton.isEnabled = false
            
            // Sign in with email and password
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login success
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // Login failed
                        Toast.makeText(
                            this,
                            getString(R.string.login_failed) + ": ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        loginButton.isEnabled = true
                    }
                }
        }
        
        // Forgot password click listener
        forgotPasswordTextView.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Reset Password")
            
            // Set up the input
            val input = EditText(this)
            input.hint = "Enter your email"
            builder.setView(input)
            
            // Set up the buttons
            builder.setPositiveButton("Reset") { dialog, _ ->
                val email = input.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Send password reset email
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", 
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                dialog.dismiss()
            }
            
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            
            builder.show()
        }
        
        // Navigate to register screen
        registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
} 