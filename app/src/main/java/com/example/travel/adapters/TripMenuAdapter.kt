package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.tripMenuCard)
        val tripName: TextView = view.findViewById(R.id.tripMenuName)
        val activeIndicator: View = view.findViewById(R.id.tripMenuActive)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_menu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val trip = trips[position]

        when {
            // "None" option - deselect active trip
            trip == null -> {
                holder.tripName.text = "None"
                holder.activeIndicator.visibility = if (activeTripId == null) View.VISIBLE else View.GONE
            }

            // "New Trip" option - starts trip name dialog
            trip.id.isEmpty() -> {
                holder.tripName.text = "New Trip"
                holder.activeIndicator.visibility = View.GONE
            }

            // Regular trip - can be reactivated
            else -> {
                holder.tripName.text = trip.name
                holder.activeIndicator.visibility = if (trip.id == activeTripId) View.VISIBLE else View.GONE
            }
        }

        holder.card.setOnClickListener { onItemClick(trip) }
    }

    override fun getItemCount(): Int = trips.size
}
