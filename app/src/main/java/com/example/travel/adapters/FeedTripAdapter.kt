package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.models.Trip

// Adapter for displaying friends' trips in the home feed
class FeedTripAdapter(
    private val trips: List<Pair<Trip, String>>,  // Pair of Trip and username
    private val tripLikes: Map<String, Int>,
    private val onTripClick: (Trip) -> Unit
) : RecyclerView.Adapter<FeedTripAdapter.FeedTripViewHolder>() {

    class FeedTripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val tripName: TextView = view.findViewById(R.id.tripName)
        val tripDate: TextView = view.findViewById(R.id.tripDate)
        val likesCount: TextView = view.findViewById(R.id.likesCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedTripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_trip, parent, false)
        return FeedTripViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedTripViewHolder, position: Int) {
        val (trip, username) = trips[position]

        holder.usernameText.text = "@$username"
        holder.tripName.text = trip.name
        holder.tripDate.text = "${trip.startDate} - ${trip.endDate}"
        val likes = tripLikes[trip.id] ?: 0
        holder.likesCount.text = if (likes == 1) "1 like" else "$likes likes"

        holder.itemView.setOnClickListener {
            onTripClick.invoke(trip)
        }
    }

    override fun getItemCount() = trips.size
}