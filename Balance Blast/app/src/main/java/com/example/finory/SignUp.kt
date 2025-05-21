package com.example.finory

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignUp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        // Apply edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get references to input fields
        val emailInput = findViewById<EditText>(R.id.email_input)
        val nameInput = findViewById<EditText>(R.id.name_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)

        // Set up click listeners to clear text fields when clicked
        emailInput.setOnClickListener {
            if (emailInput.text.toString() == emailInput.hint.toString() ||
                emailInput.text.toString() == "Enter your email") {
                emailInput.setText("")
            }
        }

        nameInput.setOnClickListener {
            if (nameInput.text.toString() == nameInput.hint.toString() ||
                nameInput.text.toString() == "Enter your name") {
                nameInput.setText("")
            }
        }

        passwordInput.setOnClickListener {
            if (passwordInput.text.toString() == passwordInput.hint.toString() ||
                passwordInput.text.toString() == "Enter your password") {
                passwordInput.setText("")
            }
        }

        // Get reference to the Register button
        val registerButton = findViewById<Button>(R.id.login_button)

        // "Already have an account? Sign In" link
        val signInLink = findViewById<TextView>(R.id.sign_up_link)

        registerButton.setOnClickListener {
            if (validateInputs(emailInput, nameInput, passwordInput)) {
                val email = emailInput.text.toString().trim()
                val username = nameInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                // Save credentials to SharedPreferences
                val sharedPref = getSharedPreferences("UserCredentials", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("email", email)
                    putString("username", username)
                    putString("password", password)
                    apply()
                }

                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                // Navigate to SignIn activity
                startActivity(Intent(this, SignIn::class.java))
                finish()
            }
        }

        // If user already has an account, navigate to SignIn
        signInLink.setOnClickListener {
            startActivity(Intent(this, SignIn::class.java))
            finish()
        }
    }

    private fun validateInputs(emailInput: EditText, nameInput: EditText, passwordInput: EditText): Boolean {
        var isValid = true

        // Validate email
        val email = emailInput.text.toString().trim()
        if (email.isEmpty() || email == "Enter your email" || email == emailInput.hint.toString()) {
            emailInput.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email address"
            isValid = false
        } else {
            emailInput.error = null
        }

        // Validate username
        val username = nameInput.text.toString().trim()
        if (username.isEmpty() || username == "Enter your name" || username == nameInput.hint.toString()) {
            nameInput.error = "Name is required"
            isValid = false
        } else if (username.length < 3) {
            nameInput.error = "Name must be at least 3 characters"
            isValid = false
        } else {
            nameInput.error = null
        }

        // Validate password
        val password = passwordInput.text.toString().trim()
        if (password.isEmpty() || password == "Enter your password" || password == passwordInput.hint.toString()) {
            passwordInput.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordInput.error = "Password must be at least 6 characters"
            isValid = false
        } else if (!password.matches(".*[A-Z].*".toRegex())) {
            passwordInput.error = "Password must contain at least one uppercase letter"
            isValid = false
        } else if (!password.matches(".*[0-9].*".toRegex())) {
            passwordInput.error = "Password must contain at least one number"
            isValid = false
        } else {
            passwordInput.error = null
        }

        return isValid
    }
}