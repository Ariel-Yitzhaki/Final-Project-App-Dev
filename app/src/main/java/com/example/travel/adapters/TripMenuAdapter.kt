package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
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
        val card: CardView = itemView.findViewById(R.id.tripMenuCard)
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
            holder.card.setCardBackgroundColor(0xFFE0E0E0.toInt())
            holder.tripName.setTextColor(0xFF757575.toInt())
            holder.activeIndicator.visibility = if (activeTripId == null) View.VISIBLE else View.GONE
            holder.card.setOnClickListener { onItemClick(trip) }
            return
        }

        // "New Trip" option - starts trip name dialog
        if (trip.id.isEmpty()) {
            holder.tripName.text = "New Trip"
            holder.card.setCardBackgroundColor(0x00000000)
            holder.tripName.setTextColor(0xFF2196F3.toInt())
            holder.activeIndicator.visibility = View.GONE
            holder.card.setOnClickListener { onItemClick(trip) }
            return
        }

        // Regular trip - can be reactivated
        holder.tripName.text = trip.name
        holder.card.setCardBackgroundColor(0xFFE3F2FD.toInt())
        holder.tripName.setTextColor(0xFF000000.toInt())
        holder.activeIndicator.visibility = if (trip.id == activeTripId) View.VISIBLE else View.GONE
        holder.card.setOnClickListener { onItemClick(trip) }
    }

    override fun getItemCount(): Int = trips.size
}
