package com.cs407.badgerstudy

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Save Button
        val saveButton = findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            Toast.makeText(this, "Selections have been saved", Toast.LENGTH_SHORT).show()
        }

        // Function to toggle button selection state
        fun toggleButton(button: Button) {
            val currentColor = button.backgroundTintList?.defaultColor
            val selectedColor = ContextCompat.getColor(this, R.color.pressedRed)
            val unselectedColor = ContextCompat.getColor(this, R.color.fadedRed)

            if (currentColor == selectedColor) {
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.fadedRed)
            } else {
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.pressedRed)
            }
        }

        // Environment Buttons (Default: Quiet Study)
        val quietStudyButton = findViewById<Button>(R.id.quiet_study_button)
        val goodViewButton = findViewById<Button>(R.id.good_view_button)
        val collabFriendlyButton = findViewById<Button>(R.id.collab_friendly_button)

        quietStudyButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.pressedRed) // Default selected
        goodViewButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.fadedRed)
        collabFriendlyButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.fadedRed)

        quietStudyButton.setOnClickListener { toggleButton(it as Button) }
        goodViewButton.setOnClickListener { toggleButton(it as Button) }
        collabFriendlyButton.setOnClickListener { toggleButton(it as Button) }

        // Study Time Buttons (Default: Morning)
        val morningButton = findViewById<Button>(R.id.morning_button)
        val afternoonButton = findViewById<Button>(R.id.afternoon_button)
        val nightButton = findViewById<Button>(R.id.night_button)

        morningButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.pressedRed) // Default selected
        afternoonButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.fadedRed)
        nightButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.fadedRed)

        morningButton.setOnClickListener { toggleButton(it as Button) }
        afternoonButton.setOnClickListener { toggleButton(it as Button) }
        nightButton.setOnClickListener { toggleButton(it as Button) }

        // Food Nearby Buttons (Default: Vending Machines)
        val vendingMachineButton = findViewById<Button>(R.id.vending_machine_button)
        val cafesButton = findViewById<Button>(R.id.cafes_button)
        val foodTruckButton = findViewById<Button>(R.id.food_truck_button)

        vendingMachineButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.pressedRed) // Default selected
        cafesButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.fadedRed)
        foodTruckButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.fadedRed)

        vendingMachineButton.setOnClickListener { toggleButton(it as Button) }
        cafesButton.setOnClickListener { toggleButton(it as Button) }
        foodTruckButton.setOnClickListener { toggleButton(it as Button) }

        // Room Type Buttons (Default: Private Room)
        val privateRoomButton = findViewById<Button>(R.id.private_room_button)
        val bigTablesButton = findViewById<Button>(R.id.big_tables_button)
        val smallTablesButton = findViewById<Button>(R.id.small_tables_button)

        privateRoomButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.pressedRed) // Default selected
        bigTablesButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.fadedRed)
        smallTablesButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.fadedRed)

        privateRoomButton.setOnClickListener { toggleButton(it as Button) }
        bigTablesButton.setOnClickListener { toggleButton(it as Button) }
        smallTablesButton.setOnClickListener { toggleButton(it as Button) }

        // Mile Radius SeekBar (Dummy Data)
        val mileRadiusSeekBar = findViewById<SeekBar>(R.id.seekBar2)
        mileRadiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Handle progress changes (you can show progress if required)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Handle start of interaction
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Handle end of interaction
            }
        })
    }
}