package com.example.finory

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GetStarted : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_get_started)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Get reference to the button
        val login = findViewById<Button>(R.id.SignIn)

        // Set a click listener
        login.setOnClickListener {
            // Navigate to Onboard2
            startActivity(Intent(this, SignIn::class.java))


        }
        // Get reference to the button
        val signup = findViewById<Button>(R.id.SignUp)

        // Set a click listener
        signup.setOnClickListener {
            // Navigate to Onboard2
            startActivity(Intent(this, SignUp::class.java))


        }
    }
}