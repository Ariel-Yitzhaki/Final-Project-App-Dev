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
import com.example.travel.data.TripRepository
import kotlinx.coroutines.launch
import com.example.travel.models.Trip
import android.widget.PopupMenu
import com.example.travel.data.LikeRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.interfaces.Refresh
import com.example.travel.interfaces.TripEndListener
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment(), Refresh {

    private lateinit var authRepository: AuthRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var photoRepository: PhotoRepository
    private lateinit var likeRepository: LikeRepository
    private var tripEndListener: TripEndListener? = null
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

    // Attaches the listener when fragment connects to activity
    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        if (context is TripEndListener) {
            tripEndListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        tripEndListener = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authRepository = AuthRepository()
        tripRepository = TripRepository()
        photoRepository = PhotoRepository()
        likeRepository = LikeRepository()

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

            // Active trip first, then completed trips sorted by date (newest first)
            val allTrips = mutableListOf<Trip>()
            activeTrip?.let {allTrips.add(it)}
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            allTrips.addAll(completedTrips.sortedByDescending {
                try { dateFormat.parse(it.startDate)?.time ?: 0L } catch (e: Exception) { 0L }
            })

            // Load cover photos for each trip
            progressBar.visibility = View.GONE

            if (allTrips.isNotEmpty()) {
                emptyText.visibility = View.GONE

                val tripLikes = loadTripLikes(allTrips)

                tripsRecyclerView.adapter = TripAdapter(
                    allTrips.toMutableList(),
                    tripLikes,
                    onEndTripClick = { trip -> onEndTripClicked(trip) },
                    onCardClick = { trip -> openTripDetail(trip) },
                    onOptionsClick = { trip, view -> showOptionsMenu(trip, view) }
                )
            } else {
                emptyText.visibility = View.VISIBLE
            }
        }
    }

    // Loads like counts for a list of trips
    private suspend fun loadTripLikes(trips: List<Trip>): Map<String, Int> {
        val tripLikes = mutableMapOf<String, Int>()
        for (trip in trips) {
            val photos = photoRepository.getPhotosForTrip(trip.id)
            val photoIds = photos.map { it.id }
            tripLikes[trip.id] = likeRepository.getTotalLikesForTrip(photoIds)
        }
        return tripLikes
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
            tripEndListener?.onTripEnded()
        }
    }

    // Opens the trip detail view
    private fun openTripDetail(trip: Trip) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, TripDetailFragment.newInstance(trip.id))
            .addToBackStack(null)
            .commit()
    }

    // Shows popup menu with trip options
    private fun showOptionsMenu(trip: Trip, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("Delete Trip")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Delete Trip" -> {
                    confirmDeleteTrip(trip)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // Confirms before deleting a trip
    private fun confirmDeleteTrip(trip: Trip) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Trip")
            .setMessage("Are you sure you want to delete '${trip.name}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTrip(trip)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Deletes trip and its photos
    private fun deleteTrip(trip: Trip) {
        lifecycleScope.launch {
            tripRepository.deleteTrip(trip.id)
            loadProfile()
            tripEndListener?.onTripEnded()
        }
    }

    override fun refresh() {
        loadProfile()
    }
}