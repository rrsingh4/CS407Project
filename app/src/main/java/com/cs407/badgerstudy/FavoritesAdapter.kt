package com.cs407.badgerstudy

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoritesAdapter(
    private val favorites: List<Favorite>,
    private val onRemove: (Favorite) -> Unit,
    private val onNavigate: (Favorite) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

    class FavoritesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val locationNameTextView: TextView = itemView.findViewById(R.id.locationNameTextView)
        val removeButton: Button = itemView.findViewById(R.id.removeButton)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite, parent, false)
        return FavoritesViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val favorite = favorites[position]

        // Bind the name of the location to the TextView
        holder.locationNameTextView.text = favorite.locationName

        // Remove button click listener
        holder.removeButton.setOnClickListener {
            onRemove(favorite)
        }

        // Navigate icon click listener
        holder.favoriteIcon.setOnClickListener {
            onNavigate(favorite)
        }
    }

    override fun getItemCount(): Int = favorites.size
}

