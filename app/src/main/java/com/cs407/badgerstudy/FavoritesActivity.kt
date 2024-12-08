package com.cs407.badgerstudy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.cs407.badgerstudy.FavoritesAdapter

class FavoritesActivity : AppCompatActivity() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var backToMapButton: Button
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

        // Create Back to Map Button
        backToMapButton = Button(this).apply {
            text = "Back to Map"
            setOnClickListener {
                val intent = Intent(this@FavoritesActivity, MapActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        rootLayout.addView(backToMapButton)

        setContentView(rootLayout)

        // Load favorites
        loadFavorites()
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



