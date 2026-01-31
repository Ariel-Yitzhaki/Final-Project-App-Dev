package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.models.Trip

// Adapter for displaying trip cards
class TripAdapter(
    private val trips: MutableList<Trip>,
    private val tripLikes: Map<String, Int>,
    // onEndTripClick: called when End Trip button is clicked (active trips only)
    private val onEndTripClick: (Trip) -> Unit,
    // onCardClick: called when the card itself is clicked (to view trip details)
    private val onCardClick: (Trip) -> Unit,
    private val onOptionsClick: (Trip, View) -> Unit,
    private val showOptions: Boolean = true
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tripName: TextView = view.findViewById(R.id.tripName)
        val tripDate: TextView = view.findViewById(R.id.tripDate)
        val likesCount: TextView = view.findViewById(R.id.likesCount)
        val endTripButton: Button = view.findViewById(R.id.endTripButton)
        val optionsButton: ImageButton = view.findViewById(R.id.optionsButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_card, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]

        holder.tripName.text = trip.name

        // Show date range for completed trips, just start date for active card
        if (trip.active) {
            holder.tripDate.text = "Started: ${trip.startDate}"
        } else {
            holder.tripDate.text = "${trip.startDate} - ${trip.endDate}"
        }

        // Show like count (hide for active trips)
        val likes = tripLikes[trip.id] ?: 0
        if (trip.active) {
            holder.likesCount.visibility = View.GONE
        } else {
            holder.likesCount.visibility = View.VISIBLE
            if (likes == 1) holder.likesCount.text = "1 like" else holder.likesCount.text =
                "$likes likes"
        }

        // Show End Trip button only for active trips
        if (trip.active) {
            holder.endTripButton.visibility = View.VISIBLE
            holder.endTripButton.setOnClickListener {
                onEndTripClick.invoke(trip)
            }
        } else {
            holder.endTripButton.visibility = View.GONE
        }
        // Card click
        holder.itemView.setOnClickListener {
            onCardClick.invoke(trip)
        }
        // Options menu click
        holder.optionsButton.setOnClickListener { view ->
            onOptionsClick.invoke(trip, view)
        }

        // Hide options button if not allowed
        holder.optionsButton.visibility = if (showOptions) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = trips.size
}