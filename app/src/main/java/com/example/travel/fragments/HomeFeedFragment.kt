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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
import com.example.travel.utils.openTripDetail
import kotlinx.coroutines.launch
import com.example.travel.utils.openTripMap
import kotlinx.coroutines.async
import com.example.travel.utils.parsedStartTime

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
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var feedAdapter: FeedTripAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authRepository = AuthRepository.instance
        friendsRepository = FriendsRepository.instance
        tripRepository = TripRepository.instance
        photoRepository = PhotoRepository.instance
        likeRepository = LikeRepository.instance

        feedRecyclerView = view.findViewById(R.id.feedRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressBar = view.findViewById(R.id.progressBar)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadFeed()
        }

        feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Create adapter once to avoid "No adapter attached" warning
        feedAdapter = FeedTripAdapter(
            onTripClick = { trip -> openTripDetail(trip.id) },
            onMapClick = { trip -> openTripMap(trip.id) }
        )
        feedRecyclerView.adapter = feedAdapter

        loadFeed()
    }

    // Loads friend's trips sorted by end date
    private fun loadFeed() {
        val currentUserId = authRepository.getCurrentUser()?.uid ?: return

        // Show cached feed instantly if available
        val cachedTrips = tripRepository.getCachedFeedTrips()
        val cachedLikes = likeRepository.getCachedLikesForTrips()

        if (cachedTrips != null && cachedTrips.isNotEmpty()) {
            lifecycleScope.launch {
                val tripsWithUsers =
                    getTripsWithUsers(cachedTrips.sortedByDescending { it.parsedStartTime() })
                displayFeed(tripsWithUsers, cachedLikes)
            }
        } else {
            progressBar.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
        }

        // Fetch fresh data from Firestore in the background
        lifecycleScope.launch {
            val friendIds = friendsRepository.getFriendIds(currentUserId)

            if (friendIds.isEmpty()) {
                progressBar.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                return@launch
            }
            val trips = tripRepository.getTripsWithPhotosForUsers(friendIds)
                .sortedByDescending { it.parsedStartTime() }

            val tripsWithUsersDeferred = async { getTripsWithUsers(trips) }
            val tripLikes = likeRepository.fetchTripLikesFromPhotos(trips, photoRepository)
            val tripsWithUsers = tripsWithUsersDeferred.await()

            progressBar.visibility = View.GONE
            displayFeed(tripsWithUsers, tripLikes)
            swipeRefresh.isRefreshing = false
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
            feedAdapter.updateData(tripsWithUsers, tripLikes)
        } else {
            emptyText.visibility = View.VISIBLE
        }
    }

    override fun refresh() {
        loadFeed()
    }
}