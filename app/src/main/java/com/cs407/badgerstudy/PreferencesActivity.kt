package com.cs407.badgerstudy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class PreferencesActivity : AppCompatActivity() {
    /*
    TODO highlighting this just so it sticks out!
    TODO firebase is included on this project. to access its functionalities, reference
    TODO FirebaseDatabase.getInstance() to have access to the preferences.

    TODO the path I used is mapped to each user's unique id that they get assigned which is a bunch of gross characters,
    TODO but we only have to ever worry about the current user, so we never actually need to know the uid

    TODO to get CurrentUser id, we use FirebaseAuth.getInstance().currentUser?.uid

    TODO the path that things are stored in is users/$currUser/preferences

    TODO each preference is keyed by the actual text that is on the button. Eg: "Vending Machines" button has key of "Vending Machines" (space included)

    TODO to access these preferences, I believe this code should work. (specifically have the map portion in mind for this)

    preferencesRef.get().addOnSuccessListener { snapshot ->
    val preferences = snapshot.value as? Map<String, Boolean> ?: emptyMap()
    preferences.forEach { (key, value) ->
        println("$key is $value")
    }
}.addOnFailureListener { error ->
    println("Error: ${error.message}")
}       TODO Feel free to ask me questions if something doesn't make sense!
        TODO ChatGPT also has a lot of good reference info for this stuff, it's well documented.
     */
    //CREATES a database instance to grab items from
   private val database = FirebaseDatabase.getInstance()

    //GETS the current user's id for authentication grabs
   private val currUser = FirebaseAuth.getInstance().currentUser?.uid
    //CREATES a reference or grabs reference for user preferences
    private val userPreferencesRef = database.getReference("users/$currUser/preferences")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)


//create color change functionality (referenced generative AI to help with this)

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

            //SETS THE PREFERENCES TO FALSE INITIALLY
            userPreferencesRef.child("${button.text}").setValue(buttonStates[id])

            button.setOnClickListener {
                print("clicked!")
                //toggle the button's clicked state
                buttonStates[id] = !buttonStates[id]!!

                //change the button color and text color based on the state

                //if button is clicked
                if (buttonStates[id] == true) {

                    //change the background color, and update the database values for the button

                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.pressedRed))
                    button.setTextColor(ContextCompat.getColor(this, R.color.pressedWhite))

                    //send the clicked data to the database before going to map section
                    userPreferencesRef.child("${button.text}").setValue(buttonStates[id])
                }
                //if button is not clicked
                else {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.fadedRed))
                    button.setTextColor(ContextCompat.getColor(this, R.color.fadedWhite))
                    //send the clicked data to the database before going to map section
                    userPreferencesRef.child("${button.text}").setValue(buttonStates[id])


                }
            }
        }
        //button that sends user to the next activity
        val doneButton = findViewById<Button>(R.id.doneButton)
        doneButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}





