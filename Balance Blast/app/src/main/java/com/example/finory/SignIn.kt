package com.example.finory

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.finory.ui.MainActivity

class SignIn : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        // Apply edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get references to input fields
        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)

        // Set up click listeners to clear text fields when clicked
        emailInput.setOnClickListener {
            if (emailInput.text.toString() == emailInput.hint.toString() ||
                emailInput.text.toString() == "Enter your email") {
                emailInput.setText("")
            }
        }

        passwordInput.setOnClickListener {
            if (passwordInput.text.toString() == passwordInput.hint.toString() ||
                passwordInput.text.toString() == "Enter your password") {
                passwordInput.setText("")
            }
        }

        // Get reference to the Sign In button
        val loginButton = findViewById<Button>(R.id.login_button)

        // "Don't have an account? Register" link
        val signUpLink = findViewById<TextView>(R.id.sign_up_link)

        loginButton.setOnClickListener {
            if (validateInputs(emailInput, passwordInput)) {
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                // Retrieve stored credentials from SharedPreferences
                val sharedPref = getSharedPreferences("UserCredentials", MODE_PRIVATE)
                val storedEmail = sharedPref.getString("email", "")
                val storedPassword = sharedPref.getString("password", "")

                // Validate credentials
                if (email == storedEmail && password == storedPassword) {
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // If user doesn't have an account, navigate to SignUp
        signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
            finish()
        }
    }

    private fun validateInputs(emailInput: EditText, passwordInput: EditText): Boolean {
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

        // Validate password
        val password = passwordInput.text.toString().trim()
        if (password.isEmpty() || password == "Enter your password" || password == passwordInput.hint.toString()) {
            passwordInput.error = "Password is required"
            isValid = false
        } else {
            passwordInput.error = null
        }

        return isValid
    }
}