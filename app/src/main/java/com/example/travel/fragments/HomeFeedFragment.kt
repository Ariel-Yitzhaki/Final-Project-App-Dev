package com.example.travel.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.adapters.FeedTripAdapter
import com.example.travel.data.AuthRepository
import com.example.travel.data.FriendsRepository
import com.example.travel.data.LikeRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import com.example.travel.interfaces.Refresh
import com.example.travel.models.Trip
import com.example.travel.models.User
import kotlinx.coroutines.launch

// Displays friends' completed trips in a feed
class HomeFeedFragment : Fragment(), Refresh {

    private lateinit var photoRepository: PhotoRepository
    private lateinit var likeRepository: LikeRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var friendsRepository: FriendsRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var feedRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authRepository = AuthRepository()
        friendsRepository = FriendsRepository()
        tripRepository = TripRepository()
        photoRepository = PhotoRepository()
        likeRepository = LikeRepository()

        feedRecyclerView = view.findViewById(R.id.feedRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressBar = view.findViewById(R.id.progressBar)

        feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadFeed()
    }

    // Loads friends' completed trips sorted by end date
    private fun loadFeed() {
        val currentUserId = authRepository.getCurrentUser()?.uid ?: return

        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE

        lifecycleScope.launch {
            val friendIds = friendsRepository.getFriendIds(currentUserId)

            if (friendIds.isEmpty()) {
                progressBar.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                return@launch
            }

            val trips = tripRepository.getTripsWithPhotosForUsers(friendIds)
                .sortedByDescending { it.endDate }

            val tripsWithUsers = getTripsWithUsers(trips)
            val tripLikes = likeRepository.getLikesForTrips(trips, photoRepository)

            progressBar.visibility = View.GONE
            displayFeed(tripsWithUsers, tripLikes)
        }
    }

    // Maps trips to their owner's username
    private suspend fun getTripsWithUsers(trips: List<Trip>): List<Pair<Trip, User>> {
        return trips.mapNotNull { trip ->
            val user = authRepository.getUserProfile(trip.userId)
            if (user != null) {
                Pair(trip, user)
            } else {
                null
            }
        }
    }

    // Displays feed or empty state
    private fun displayFeed(tripsWithUsers: List<Pair<Trip, User>>, tripLikes: Map<String, Int>) {
        if (tripsWithUsers.isNotEmpty()) {
            feedRecyclerView.adapter = FeedTripAdapter(
                tripsWithUsers,
                tripLikes
            ) { trip ->
                openTripDetail(trip)
            }
        } else {
            emptyText.visibility = View.VISIBLE
        }
    }

    // Opens trip detail view
    private fun openTripDetail(trip: Trip) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, TripDetailFragment.newInstance(trip.id))
            .addToBackStack(null)
            .commit()
    }

    override fun refresh() {
        loadFeed()
    }
}