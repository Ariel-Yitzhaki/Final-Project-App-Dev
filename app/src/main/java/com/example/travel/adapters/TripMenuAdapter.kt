package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.models.Trip

// Adapter for trip selection bottom sheet menu
// Using null because null = "None", first non-null with empty id = "New Trip"
class TripMenuAdapter(
    private val trips: List<Trip?>,
    private val activeTripId: String?,
    private val onItemClick: (Trip?) -> Unit
) : RecyclerView.Adapter<TripMenuAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.tripMenuCard)
        val tripName: TextView = itemView.findViewById(R.id.tripMenuName)
        val activeIndicator: View = itemView.findViewById(R.id.tripMenuActive)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_menu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]

        // "None" option - deselect active trip
        if (trip == null) {
            holder.tripName.text = "None"
            holder.card.setCardBackgroundColor(0xFFF4F4F5.toInt())
            holder.tripName.setTextColor(0xFF71717A.toInt())
            holder.activeIndicator.visibility = if (activeTripId == null) View.VISIBLE else View.GONE
            holder.card.strokeColor = 0x00000000
            holder.card.strokeWidth = 0
            holder.card.foreground = null
        }
        // "New Trip" option - starts trip name dialog
        else if (trip.id.isEmpty()) {
            holder.tripName.text = "New Trip"
            holder.card.setCardBackgroundColor(0x00000000)
            holder.tripName.setTextColor(0xFF000000.toInt())
            holder.activeIndicator.visibility = View.GONE
            holder.card.strokeColor = 0x00000000
            holder.card.strokeWidth = 0
            holder.card.foreground = null
        }
        // Regular trip - can be reactivated
        else {
            holder.tripName.text = trip.name
            holder.activeIndicator.visibility = if (trip.id == activeTripId) View.VISIBLE else View.GONE
            holder.card.setCardBackgroundColor(0xFFF4F4F5.toInt())
            holder.card.strokeColor = if (trip.id == activeTripId) 0xFFC7C7C7.toInt() else 0x00000000
            holder.card.strokeWidth = if (trip.id == activeTripId) 1 else 0
            if (trip.id == activeTripId) {
                // Active trip: gradient overlay fading from black at top to transparent
                holder.card.foreground = holder.itemView.context.getDrawable(R.drawable.bg_active_trip)
                holder.tripName.setTextColor(0xFFFFFFFF.toInt())
            } else {
                // Inactive trip: no overlay
                holder.card.foreground = null
                holder.tripName.setTextColor(0xFF27272A.toInt())
            }
        }
        holder.card.setOnClickListener { onItemClick(trip) }
    }

    override fun getItemCount(): Int = trips.size
}
