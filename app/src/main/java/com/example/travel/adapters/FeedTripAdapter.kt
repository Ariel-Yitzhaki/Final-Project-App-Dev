package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.models.Trip
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.widget.ImageView
import com.example.travel.models.User
import com.bumptech.glide.Glide

// Adapter for displaying friends' trips in the home feed
class FeedTripAdapter(
    private val trips: List<Pair<Trip, User>>,  // Pair of Trip and username
    private val tripLikes: Map<String, Int>,
    private val onTripClick: (Trip) -> Unit
) : RecyclerView.Adapter<FeedTripAdapter.FeedTripViewHolder>() {

    class FeedTripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profilePicture: ImageView = view.findViewById(R.id.profilePicture)
        val activityText: TextView = view.findViewById(R.id.activityText)
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
        val (trip, user) = trips[position]

        holder.activityText.text = createActivityText(user.username)
        if (user.profilePictureUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.profilePictureUrl)
                .circleCrop()
                .into(holder.profilePicture)
        }

        holder.tripName.text = trip.name
        holder.tripDate.text = "${trip.startDate} - ${trip.endDate}"
        val likes = tripLikes[trip.id] ?: 0
        holder.likesCount.text = if (likes == 1) "1 like" else "$likes likes"

        holder.itemView.setOnClickListener {
            onTripClick.invoke(trip)
        }
    }

    // Creates "username started a new trip!" with bold username
    private fun createActivityText(username: String): SpannableString {
        val text = "$username started a new trip!"
        val spannable = SpannableString(text)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            username.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    override fun getItemCount() = trips.size
}