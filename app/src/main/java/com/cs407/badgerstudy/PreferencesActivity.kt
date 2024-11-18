package com.cs407.badgerstudy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)


//TODO finish programming button colors, then actually store the values of the buttons for use in the map.
        //TODO create color change functionality (referenced generative AI to help with this)
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

        //initialize a map to store the click state for each button
        val buttonStates = mutableMapOf<Int, Boolean>().apply {
            buttonIds.forEach { this[it] = false }
        }
        //loop through each button ID, find the button, and set an OnClickListener
        buttonIds.forEach { id ->
            val button = findViewById<Button>(id)
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.fadedRed))
            button.setTextColor(ContextCompat.getColor(this, R.color.fadedWhite))

            button.setOnClickListener {
                print("clicked!")
                //toggle the button's clicked state
                buttonStates[id] = !buttonStates[id]!!

                //change the button color and text color based on the state

                //if button is clicked
                if (buttonStates[id] == true) {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.pressedRed))
                    button.setTextColor(ContextCompat.getColor(this, R.color.pressedWhite))
                }
                //if button is not clicked
                else {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.fadedRed))
                    button.setTextColor(ContextCompat.getColor(this, R.color.fadedWhite))
                }
            }
        }
        val doneButton = findViewById<Button>(R.id.doneButton)
        doneButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}





