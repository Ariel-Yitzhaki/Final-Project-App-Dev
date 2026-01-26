package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.models.Trip

// Adapter for displaying trip cards
class TripAdapter(
    private val trips: List<Trip>,
    private val onTripClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_card, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]
        holder.itemView.setOnClickListener { onTripClick(trip) }
    }

    override fun getItemCount() = trips.size
}