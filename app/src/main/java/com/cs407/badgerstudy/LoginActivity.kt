package com.cs407.badgerstudy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var database: UserDatabase // Reference to the Room database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize the database
        database = UserDatabase.getDatabase(this)

        // Define UI elements
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val saveButton = findViewById<Button>(R.id.saveButton)

        // Apply button colors and states
        val buttons = arrayOf(loginButton, saveButton)
        buttons.forEach { button ->
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.fadedRed))
            button.setTextColor(ContextCompat.getColor(this, R.color.fadedWhite))

            button.setOnClickListener {
                // Toggle button state colors
                val currentState = button.tag as? Boolean ?: false
                val newState = !currentState
                button.tag = newState

                if (newState) {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.pressedRed))
                    button.setTextColor(ContextCompat.getColor(this, R.color.pressedWhite))
                } else {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.fadedRed))
                    button.setTextColor(ContextCompat.getColor(this, R.color.fadedWhite))
                }
            }
        }

        // Handle login button click
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate credentials using the Room database
            lifecycleScope.launch {
                val user = database.userDao().getUserByUsername(username)
                if (user != null && user.password == password) {
                    // Login successful, navigate to PreferencesActivity
                    val intent = Intent(this@LoginActivity, PreferencesActivity::class.java)
                    intent.putExtra("username", username) // Pass username to the next activity
                    startActivity(intent)
                    finish()
                } else {
                    // Invalid credentials
                    Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handle save button click (save new user)
        saveButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields to save", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUser = database.userDao().getUserByUsername(username)
                if (existingUser == null) {
                    // Save the new user
                    database.userDao().insertUser(User(username = username, password = password))
                    Toast.makeText(this@LoginActivity, "User saved successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@LoginActivity, "Username already exists!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}


