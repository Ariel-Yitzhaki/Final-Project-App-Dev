package com.example.travel.utils

import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.travel.R
import com.example.travel.fragments.TripDetailFragment
import com.example.travel.fragments.TripMapDialogFragment

// Opens the trip detail view from any fragment
fun Fragment.openTripDetail(tripId: String) {
    parentFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, TripDetailFragment.newInstance(tripId))
        .addToBackStack(null)
        .commit()
}

// Opens the trip map dialog from any fragment
fun Fragment.openTripMap(tripId: String) {
    TripMapDialogFragment.newInstance(tripId)
        .show(parentFragmentManager, "tripMap")
}

// Loads a profile picture into an ImageView with circle crop and default fallback
fun ImageView.loadProfilePicture(url: String) {
    if (url.isNotEmpty()) {
        Glide.with(this.context)
            .load(url)
            .circleCrop()
            .placeholder(R.drawable.ic_profile)
            // Smooth transition so the image fades in instead of popping
            .transition(DrawableTransitionOptions.withCrossFade(200))
            .into(this)
    } else {
        this.setImageResource(R.drawable.ic_profile)
    }
}