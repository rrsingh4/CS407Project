package com.cs407.badgerstudy

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoritesActivity : AppCompatActivity() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private val favoritesList = mutableListOf<Favorite>()
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Programmatically create the layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Create RecyclerView
        favoritesRecyclerView = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // Weight to occupy most of the screen
            )
            layoutManager = LinearLayoutManager(this@FavoritesActivity)
        }
        rootLayout.addView(favoritesRecyclerView)

        // Create Bottom Navigation
        val bottomNavigationView = BottomNavigationView(this).apply {
            id = R.id.bottom_navigation
            inflateMenu(R.menu.bottom_navigation_menu)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(bottomNavigationView)

        setContentView(rootLayout)

        // Setup Bottom Navigation
        setupBottomNavigation(bottomNavigationView)

        // Load favorites
        loadFavorites()
    }


    private fun setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Navigate to MapActivity
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_favorites -> {
                    // Already in FavoritesActivity, do nothing
                    true
                }
                R.id.nav_settings -> {
                    // Navigate to SettingsActivity
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFavorites() {
        if (userId != null) {
            db.collection("favorites")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    favoritesList.clear()
                    for (document in documents) {
                        val favorite = Favorite(
                            id = document.id,
                            locationName = document.getString("locationName") ?: "",
                            latitude = document.getDouble("latitude") ?: 0.0,
                            longitude = document.getDouble("longitude") ?: 0.0
                        )
                        favoritesList.add(favorite)
                    }
                    favoritesRecyclerView.adapter = FavoritesAdapter(favoritesList) { favorite ->
                        removeFavorite(favorite)
                    }
                }
                .addOnFailureListener {
                    // Handle failure to load favorites
                }
        }
    }

    private fun removeFavorite(favorite: Favorite) {
        db.collection("favorites").document(favorite.id).delete()
            .addOnSuccessListener {
                favoritesList.remove(favorite)
                favoritesRecyclerView.adapter?.notifyDataSetChanged()
            }
    }
}



