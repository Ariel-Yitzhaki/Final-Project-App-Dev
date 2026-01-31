package com.example.travel.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.adapters.TripAdapter
import com.example.travel.data.AuthRepository
import com.example.travel.data.LikeRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import com.example.travel.models.Trip
import kotlinx.coroutines.launch

// Displays a friend's profile (read-only)
class FriendProfileFragment : Fragment() {
    private lateinit var authRepository: AuthRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var photoRepository: PhotoRepository
    private lateinit var likeRepository: LikeRepository

    private lateinit var displayNameText: TextView
    private lateinit var usernameText: TextView
    private lateinit var tripsRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar

    private var friendId: String = ""

    companion object {
        fun newInstance(friendId: String): FriendProfileFragment {
            val fragment = FriendProfileFragment()
            fragment.arguments = Bundle().apply {
                putString("friendId", friendId)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friend_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        friendId = arguments?.getString("friendId") ?: return

        authRepository = AuthRepository()
        tripRepository = TripRepository()
        photoRepository = PhotoRepository()
        likeRepository = LikeRepository()

        displayNameText = view.findViewById(R.id.displayNameText)
        usernameText = view.findViewById(R.id.usernameText)
        tripsRecyclerView = view.findViewById(R.id.tripsRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressBar = view.findViewById(R.id.progressBar)

        tripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Back button
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadFriendProfile()
    }

    private fun loadFriendProfile() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Load friend's info
            val user = authRepository.getUserProfile(friendId)
            user?.let {
                displayNameText.text = it.displayName
                usernameText.text = "@${it.username}"
            }

            // Load friend's trips (active + completed)
            val activeTrip = tripRepository.getActiveTrip(friendId)
            val completedTrips = tripRepository.getCompletedTrips(friendId)

            val allTrips = mutableListOf<Trip>()
            activeTrip?.let {allTrips.add(it)}
            allTrips.addAll(completedTrips.sortedByDescending { it.startDate })

            progressBar.visibility = View.GONE

            if (allTrips.isNotEmpty()) {
                emptyText.visibility = View.GONE

                val tripLikes = loadTripLikes(allTrips)

                tripsRecyclerView.adapter = TripAdapter(
                    allTrips.toMutableList(),
                    tripLikes,
                    onEndTripClick = {},
                    onCardClick = {trip -> openTripDetail(trip)},
                    onOptionsClick = {_,_ ->},
                    showOptions = false
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

    // Opens the trip detail view
    private fun openTripDetail(trip: Trip) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, TripDetailFragment.newInstance(trip.id))
            .addToBackStack(null)
            .commit()
    }
}