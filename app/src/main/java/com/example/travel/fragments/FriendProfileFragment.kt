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
import com.example.travel.models.User
import com.example.travel.utils.loadProfilePicture
import com.example.travel.utils.openTripDetail
import com.example.travel.utils.openTripMap
import com.example.travel.utils.parsedStartTime
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

// Displays a friend's profile (read-only)
class FriendProfileFragment : Fragment() {
    private lateinit var authRepository: AuthRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var photoRepository: PhotoRepository
    private lateinit var likeRepository: LikeRepository
    private lateinit var profileImage: ShapeableImageView
    private lateinit var displayNameText: TextView
    private lateinit var usernameText: TextView
    private lateinit var tripsRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tripAdapter: TripAdapter

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

        authRepository = AuthRepository.instance
        tripRepository = TripRepository.instance
        photoRepository = PhotoRepository.instance
        likeRepository = LikeRepository.instance

        displayNameText = view.findViewById(R.id.displayNameText)
        usernameText = view.findViewById(R.id.usernameText)
        tripsRecyclerView = view.findViewById(R.id.tripsRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressBar = view.findViewById(R.id.progressBar)
        profileImage = view.findViewById(R.id.profilePicture)

        // Hide profile content until data is ready to avoid showing default placeholders
        profileImage.visibility = View.INVISIBLE
        displayNameText.visibility = View.INVISIBLE
        usernameText.visibility = View.INVISIBLE
        tripsRecyclerView.visibility = View.INVISIBLE

        tripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        tripAdapter = TripAdapter(
            onEndTripClick = {},
            onCardClick = { trip -> openTripDetail(trip.id) },
            onOptionsClick = { _, _ -> },
            onMapClick = { trip -> openTripMap(trip.id) },
            showOptions = false,
            showEndButton = false
        )
        tripsRecyclerView.adapter = tripAdapter

        // Back button
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadFriendProfile()
    }

    private fun loadFriendProfile() {
        // Show cached data immediately if available
        val cachedUser = authRepository.getCachedUserProfile(friendId)
        val cachedActive = tripRepository.getCachedActiveTrip(friendId)
        val cachedCompleted = tripRepository.getCachedCompletedTrips(friendId)
        val cachedLikes = likeRepository.getCachedLikesForTrips()

        if (cachedUser != null && cachedCompleted != null) {
            displayFriendProfile(cachedUser, cachedActive, cachedCompleted, cachedLikes)
        } else {
            progressBar.visibility = View.VISIBLE
        }

        // Fetch fresh data from Firestore in the background
        lifecycleScope.launch {
            val userDeferred = async { authRepository.getUserProfile(friendId) }
            val activeDeferred = async { tripRepository.getActiveTrip(friendId) }
            val completedDeferred = async { tripRepository.getCompletedTrips(friendId) }

            val user = userDeferred.await()
            val activeTrip = activeDeferred.await()
            val completedTrips = completedDeferred.await()

            val allTrips = mutableListOf<Trip>()
            activeTrip?.let { allTrips.add(it) }
            allTrips.addAll(completedTrips)

            val tripLikes = if (allTrips.isNotEmpty()) {
                likeRepository.fetchTripLikesFromPhotos(allTrips, photoRepository)
            } else {
                emptyMap()
            }

            progressBar.visibility = View.GONE

            if (user != null) {
                displayFriendProfile(user, activeTrip, completedTrips, tripLikes)
            } else {
                emptyText.visibility = View.VISIBLE
            }
        }
    }

    // Populates the friend profile UI with user info and sorted trips
    private fun displayFriendProfile(
        user: User,
        activeTrip: Trip?,
        completedTrips: List<Trip>,
        tripLikes: Map<String, Int>
    ) {
        // Content is ready, make everything visible
        profileImage.visibility = View.VISIBLE
        displayNameText.visibility = View.VISIBLE
        usernameText.visibility = View.VISIBLE
        tripsRecyclerView.visibility = View.VISIBLE

        profileImage.loadProfilePicture(user.profilePictureUrl)
        displayNameText.text = user.displayName
        usernameText.text = getString(R.string.format_username, user.username)

        val allTrips = mutableListOf<Trip>()
        activeTrip?.let { allTrips.add(it) }
        allTrips.addAll(completedTrips.sortedByDescending { it.parsedStartTime() })

        if (allTrips.isNotEmpty()) {
            emptyText.visibility = View.GONE
            tripAdapter.updateData(allTrips, tripLikes)
        } else {
            emptyText.visibility = View.VISIBLE
        }
    }
}