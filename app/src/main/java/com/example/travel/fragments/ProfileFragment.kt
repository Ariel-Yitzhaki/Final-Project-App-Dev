package com.example.travel.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.activities.LoginActivity
import com.example.travel.adapters.TripAdapter
import com.example.travel.data.AuthRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import kotlinx.coroutines.launch
import com.example.travel.models.Trip

class ProfileFragment : Fragment() {

    private lateinit var authRepository: AuthRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var photoRepository: PhotoRepository

    private lateinit var displayNameText: TextView
    private lateinit var usernameText: TextView
    private lateinit var tripsRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authRepository = AuthRepository()
        tripRepository = TripRepository()
        photoRepository = PhotoRepository()

        // Bind views
        displayNameText = view.findViewById(R.id.displayNameText)
        usernameText = view.findViewById(R.id.usernameText)
        tripsRecyclerView = view.findViewById(R.id.tripsRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressBar = view.findViewById(R.id.progressBar)

        tripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Sign out button
        view.findViewById<Button>(R.id.signOutButton).setOnClickListener {
            authRepository.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        loadProfile()
    }

    private fun loadProfile() {
        val userId = authRepository.getCurrentUser()?.uid ?: return

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Load user info
            val user = authRepository.getUserProfile(userId)
            user?.let {
                displayNameText.text = it.displayName
                usernameText.text = "@${it.username}"
            }

            // Load completed trips
            val activeTrip = tripRepository.getActiveTrip(userId)
            val completedTrips = tripRepository.getCompletedTrips(userId)

            val allTrips = mutableListOf<Trip>()
            activeTrip?.let {allTrips.add(it)}
            allTrips.addAll(completedTrips)

            // Load cover photos for each trip
            progressBar.visibility = View.GONE

            if (allTrips.isNotEmpty()) {
                emptyText.visibility = View.GONE
                tripsRecyclerView.adapter = TripAdapter(allTrips.toMutableList()) { trip ->
                    onEndTripClicked(trip)
                }
            } else {
                emptyText.visibility = View.VISIBLE
            }
        }
    }

    private fun onEndTripClicked(trip: Trip) {
        lifecycleScope.launch {
            if (trip.photoCount == 0) {
                // Delete empty trip
                tripRepository.deleteTrip(trip.id)
            } else {
                // Mark trip as completed with today's date
                val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                val today = dateFormat.format(java.util.Date())
                tripRepository.endTrip(trip.id, today)
            }
            // Refresh the list
            loadProfile()
        }
    }
}