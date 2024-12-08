package com.cs407.badgerstudy

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FavoritesAdapter(
    private val favorites: List<Favorite>,
    private val onRemove: (Favorite) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

    class FavoritesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val locationNameTextView: TextView = itemView.findViewById(1)
        val removeButton: Button = itemView.findViewById(2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val context = parent.context
        val itemView = createFavoriteItemView(context)
        return FavoritesViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val favorite = favorites[position]
        holder.locationNameTextView.text = favorite.locationName
        holder.removeButton.setOnClickListener { onRemove(favorite) }
    }

    override fun getItemCount(): Int = favorites.size

    private fun createFavoriteItemView(context: Context): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
        }

        val locationNameTextView = TextView(context).apply {
            id = 1 // Unique ID
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f // Weight to occupy most of the space
            )
        }
        layout.addView(locationNameTextView)

        val removeButton = Button(context).apply {
            id = 2 // Unique ID
            text = "Remove"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(removeButton)

        return layout
    }
}
