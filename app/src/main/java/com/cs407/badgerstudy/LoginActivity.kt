package com.cs407.badgerstudy

import android.app.AlertDialog
import android.content.Intent
import android.widget.Toast
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        fun showDialog(title: String, message: String) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog when OK is clicked
            }
            builder.create().show()
        }
        // Define UI elements
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val createUserButton = findViewById<Button>(R.id.createUserButton)

        // Apply button colors and states
        val buttons = arrayOf(loginButton, createUserButton)
        buttons.forEach { button ->
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.pressedRed))
            button.setTextColor(ContextCompat.getColor(this, R.color.pressedWhite))

            button.setOnClickListener {
                // Toggle button state colors
                val currentState = button.tag as? Boolean ?: false
                val newState = !currentState
                button.tag = newState

                if (newState) {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.pressedRed))
                    button.setTextColor(ContextCompat.getColor(this, R.color.pressedWhite))
                } else {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.pressedRed))
                    button.setTextColor(ContextCompat.getColor(this, R.color.pressedWhite))
                }
            }
        }

        // Handle login button click
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                showDialog("Login Failed", "Please fill in both fields")
                return@setOnClickListener
            }
            // Simulate login success for demonstration
            //TODO log user in. (separate function)
            auth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener { login ->
                    if (login.isSuccessful) {
                        val user = auth.currentUser
                        //Alert on success
                        showDialog("Login Successful", "Welcome $username!")


                        val intent = Intent(this@LoginActivity, MapActivity::class.java)
                        intent.putExtra("username", username) // Pass username to the next activity
                        startActivity(intent)
                        finish()

                    } else {
                        Log.e("LoginActivity", "Error: ${login.exception?.message}")

                        showDialog("Error Logging In", "Please submit a valid email and password")


                    }
                }
        }

        // Handle save button click (simulated save functionality)
        createUserButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields to save", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Simulate saving the user for demonstration
                //TODO add creating user functionality here
            auth.createUserWithEmailAndPassword(username, password)
                .addOnCompleteListener { login ->
                    if (login.isSuccessful) {
                        val user = auth.currentUser
                        //Alert on success
                        showDialog("Account Created", "Welcome $username!")

                        //move user to preferences screen
                        val intent = Intent(this@LoginActivity, PreferencesActivity::class.java)
                        intent.putExtra("username", username) // Pass username to the next activity
                        startActivity(intent)
                        finish()


                    } else{
                        showDialog("Error Creating Account", "Please submit a valid email")

                    }

                }

        }
    }


}



