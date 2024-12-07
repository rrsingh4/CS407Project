package com.cs407.badgerstudy

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import android.util.Log
import android.widget.TextView
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private val currUser = FirebaseAuth.getInstance().currentUser?.uid
    private val userPreferencesRef = database.getReference("users/$currUser/preferences")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()

        // Logout Button Logic
        val logoutButton = findViewById<Button>(R.id.logout_button)
        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val buttonIds = arrayOf(
            R.id.quiet_study_button,
            R.id.good_view_button,
            R.id.collab_friendly_button,
            R.id.morning_button,
            R.id.afternoon_button,
            R.id.night_button,
            R.id.vending_machine_button,
            R.id.cafes_button,
            R.id.food_truck_button,
            R.id.private_room_button,
            R.id.big_tables_button,
            R.id.small_tables_button
        )

        val buttons = buttonIds.associateWith { findViewById<Button>(it) }

        // Load saved preferences and set default states
        userPreferencesRef.get().addOnSuccessListener { snapshot ->
            val preferences = snapshot.value as? Map<String, Boolean> ?: emptyMap()
            buttons.forEach { (id, button) ->
                val isSelected = preferences[button.text.toString()] == true
                button.backgroundTintList = ContextCompat.getColorStateList(
                    this, if (isSelected) R.color.pressedRed else R.color.fadedRed
                )
            }
        }

        // Set listeners for buttons to toggle their state
        buttons.forEach { (_, button) ->
            button.setOnClickListener {
                val isSelected = button.backgroundTintList == ContextCompat.getColorStateList(this, R.color.pressedRed)
                button.backgroundTintList = ContextCompat.getColorStateList(
                    this, if (!isSelected) R.color.pressedRed else R.color.fadedRed
                )
                userPreferencesRef.child("${button.text}").setValue(!isSelected)
            }
        }
        //text to show miles radius
        val seekBarText = findViewById<TextView>(R.id.progressNum)

        // SeekBar for mile radius preference
        val mileRadiusSeekBar = findViewById<SeekBar>(R.id.seekBar2)
        mileRadiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Save the progress to Firebase
                seekBarText.text = "$progress miles"
                // Save the progress to Firebase
                userPreferencesRef.child("mileRadius").setValue(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // BottomNavigationView Setup
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_favorites -> {
                    Toast.makeText(this, "Favorites clicked!", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    // Stay on current activity
                    true
                }
                else -> false
            }
        } ?: Log.e("SettingsActivity", "BottomNavigationView is null")

        // Save Button Setup
        val saveButton = findViewById<Button>(R.id.createUserButton)
        if (saveButton != null) {
            saveButton.setOnClickListener {
                Toast.makeText(this, "Your new preferences are saved!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("SettingsActivity", "Save Button is null")
        }
    }
}