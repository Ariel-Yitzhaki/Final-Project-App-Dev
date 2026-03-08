package com.example.travel.utils

import androidx.fragment.app.Fragment
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