package com.example.travel.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.models.Trip
import android.text.SpannableString
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.TypefaceSpan
import android.widget.ImageView
import com.example.travel.models.User
import android.widget.ImageButton
import androidx.core.content.res.ResourcesCompat
import com.example.travel.utils.loadProfilePicture

// Adapter for displaying friends' trips in the home feed
class FeedTripAdapter(
    private val onTripClick: (Trip) -> Unit,
    private val onMapClick: (Trip) -> Unit
) : RecyclerView.Adapter<FeedTripAdapter.FeedTripViewHolder>() {

    private var trips: List<Pair<Trip, User>> = emptyList()
    private var tripLikes: Map<String, Int> = emptyMap()

    class FeedTripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profilePicture: ImageView = view.findViewById(R.id.profilePicture)
        val activityText: TextView = view.findViewById(R.id.activityText)
        val tripName: TextView = view.findViewById(R.id.tripName)
        val tripDate: TextView = view.findViewById(R.id.tripDate)
        val likesCount: TextView = view.findViewById(R.id.likesCount)
        val mapButton: ImageButton = view.findViewById(R.id.mapButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedTripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_trip, parent, false)
        return FeedTripViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedTripViewHolder, position: Int) {
        val (trip, user) = trips[position]

        holder.activityText.text = createActivityText(holder.itemView.context, user.username)
        holder.profilePicture.loadProfilePicture(user.profilePictureUrl)
        holder.tripName.text = trip.name
        holder.tripDate.text = trip.startDate


        val likes = tripLikes[trip.id] ?: 0
        holder.likesCount.text = if (likes == 1) "1 like" else "$likes likes"

        holder.itemView.setOnClickListener {
            onTripClick.invoke(trip)
        }

        holder.mapButton.setOnClickListener {
            onMapClick.invoke(trip)
        }
    }

    // Creates "username started a new trip!" with bold username
    private fun createActivityText(context: Context, username: String): SpannableString {
        val text = "$username started a new trip!"
        val spannable = SpannableString(text)
        val typeface = ResourcesCompat.getFont(context, R.font.mont_bold)
        typeface?.let {
            spannable.setSpan(
                TypefaceSpan(it),
                0,
                username.length,
                SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    // Updates adapter data and refreshes the list
    fun updateData(newTrips: List<Pair<Trip, User>>, newTripLikes: Map<String, Int>) {
        trips = newTrips
        tripLikes = newTripLikes
        notifyDataSetChanged()
    }

    override fun getItemCount() = trips.size
}