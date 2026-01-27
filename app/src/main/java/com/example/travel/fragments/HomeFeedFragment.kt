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
import com.example.travel.data.TripRepository
import com.example.travel.models.Trip
import kotlinx.coroutines.launch

// Displays friends' completed trips in a feed
class HomeFeedFragment : Fragment(), Refresh {

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
            // Get friend IDs
            val friendIds = friendsRepository.getFriendIds(currentUserId)

            if (friendIds.isEmpty()) {
                progressBar.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                return@launch
            }

            // Get friends' completed trips
            val trips = tripRepository.getCompletedTripsForUsers(friendIds)
                .sortedByDescending { it.endDate }

            // Get usernames for each trip
            val tripsWithUsernames = trips.mapNotNull { trip ->
                val user = authRepository.getUserProfile(trip.userId)
                user?.let { Pair(trip, it.username) }
            }

            progressBar.visibility = View.GONE

            if (tripsWithUsernames.isNotEmpty()) {
                feedRecyclerView.adapter = FeedTripAdapter(tripsWithUsernames) { trip ->
                    openTripDetail(trip)
                }
            } else {
                emptyText.visibility = View.VISIBLE
            }
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