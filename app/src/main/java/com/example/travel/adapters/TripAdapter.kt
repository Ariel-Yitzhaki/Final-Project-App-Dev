package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.models.Trip

// Adapter for displaying trip cards
class TripAdapter(
    private val trips: MutableList<Trip>,
    private val onTripClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tripName: TextView = view.findViewById(R.id.tripName)
        val tripDate: TextView = view.findViewById(R.id.tripDate)
        val endTripButton: Button = view.findViewById(R.id.endTripButton)
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
        holder.tripDate.text = if(trip.active) {
            "Started: ${trip.startDate}"
        } else {
            "${trip.startDate} - ${trip.endDate}"
        }

        // Show End Trip button only for active trips
        if (trip.active) {
            holder.endTripButton.visibility = View.VISIBLE
            holder.endTripButton.setOnClickListener {
                onTripClick.invoke(trip)
            }
        } else {
            holder.endTripButton.visibility = View.GONE
        }
    }

    override fun getItemCount() = trips.size
}