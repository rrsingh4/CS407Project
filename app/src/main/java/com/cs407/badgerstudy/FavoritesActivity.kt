package com.cs407.badgerstudy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class FavoritesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // Button to navigate back to MapActivity
        val backToMapButton = findViewById<Button>(R.id.backToMapButton)
        backToMapButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish() // Close FavoritesActivity to prevent it from stacking
        }
    }
}
