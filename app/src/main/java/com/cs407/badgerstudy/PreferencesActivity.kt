package com.cs407.badgerstudy

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        val quietStudy: Button = findViewById<Button>(R.id.quiet_study_button)
        val goodView: Button = findViewById<Button>(R.id.good_view_button)
        val collabFriendly: Button = findViewById<Button>(R.id.collab_friendly_button)
        val morning: Button = findViewById<Button>(R.id.morning_button)
        val afternoon: Button = findViewById<Button>(R.id.afternoon_button)
        val night: Button = findViewById<Button>(R.id.night_button)
        val vendingMachines: Button = findViewById<Button>(R.id.vending_machine_button)
        val cafes: Button = findViewById<Button>(R.id.cafes_button)
        val foodTrucks: Button = findViewById<Button>(R.id.food_truck_button)
        val privateRoom: Button = findViewById<Button>(R.id.collab_friendly_button)
        val bigTables: Button = findViewById<Button>(R.id.big_tables_button)
        val smallTables: Button = findViewById<Button>(R.id.small_tables_button)

//TODO finish programming button colors, then actually store the values of the buttons for use in the map.
        quietStudy.setOnClickListener {
            // Set the background tint color programmatically
            quietStudy.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red)
        }
    }











}
