package com.example.travel.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.adapters.FriendAdapter
import com.example.travel.adapters.FriendRequestAdapter
import com.example.travel.data.AuthRepository
import com.example.travel.data.FriendsRepository
import com.example.travel.interfaces.Refresh
import com.example.travel.models.FriendRequest
import com.example.travel.models.User
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class FriendsFragment : Fragment(), Refresh {

    private lateinit var authRepository: AuthRepository
    private lateinit var friendsRepository: FriendsRepository
    private lateinit var requestsLabel: TextView
    private lateinit var requestsRecyclerView: RecyclerView
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var requestsAdapter: FriendRequestAdapter
    private lateinit var friendsAdapter: FriendAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authRepository = AuthRepository.instance
        friendsRepository = FriendsRepository.instance

        // Bind views
        requestsLabel = view.findViewById(R.id.requestsLabel)
        requestsRecyclerView = view.findViewById(R.id.requestsRecyclerView)
        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressBar = view.findViewById(R.id.progressBar)

        requestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        friendsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Create adapters once to avoid "No adapter attached" warning
        requestsAdapter = FriendRequestAdapter(
            onAcceptClick = { acceptRequest(it) },
            onDeclineClick = { declineRequest(it) }
        )
        requestsRecyclerView.adapter = requestsAdapter

        friendsAdapter = FriendAdapter(
            onRemoveClick = { friend -> removeFriend(friend) },
            onFriendClick = { friend -> openFriendProfile(friend) }
        )
        friendsRecyclerView.adapter = friendsAdapter

        // Search button opens user search
        view.findViewById<ImageButton>(R.id.searchButton).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, UserSearchFragment())
                .addToBackStack(null)
                .commit()
        }

        loadData()
    }

    private fun loadData() {
        val currentUserId = authRepository.getCurrentUser()?.uid ?: return

        // Show cached data immediately so the screen isn't empty while fetching
        val cachedFriends = friendsRepository.getCachedFriends(currentUserId)
        val cachedRequests = friendsRepository.getCachedRequests(currentUserId)

        if (cachedFriends != null) {
            displayFriends(cachedFriends)
        }
        if (cachedRequests != null) {
            displayRequests(cachedRequests)
        }

        if (cachedFriends == null) {
            progressBar.visibility = View.VISIBLE
        }

        // Fetch fresh data from Firestore in the background
        lifecycleScope.launch {
            val requestsDeferred = async { loadRequestsWithUsers(currentUserId) }
            val friendsDeferred = async { friendsRepository.getFriends(currentUserId) }

            val requestsWithUsers = requestsDeferred.await()
            val friends = friendsDeferred.await()

            // Cache the requests with users for instant display on next visit
            friendsRepository.cacheRequests(currentUserId, requestsWithUsers)
            progressBar.visibility = View.GONE

            displayRequests(requestsWithUsers)
            displayFriends(friends)
        }
    }

    // Fetches pending requests and pairs each with the sender's profile
    private suspend fun loadRequestsWithUsers(userId: String): List<Pair<FriendRequest, User>> {
        val requests = friendsRepository.getPendingRequests(userId)
        return requests.mapNotNull { request ->
            authRepository.getUserProfile(request.senderId)?.let { user ->
                Pair(request, user)
            }
        }
    }

    // Updates the friend requests section visibility and data
    private fun displayRequests(requestsWithUsers: List<Pair<FriendRequest, User>>) {
        if (requestsWithUsers.isNotEmpty()) {
            requestsLabel.visibility = View.VISIBLE
            requestsRecyclerView.visibility = View.VISIBLE
            requestsAdapter.updateData(requestsWithUsers)
        } else {
            requestsLabel.visibility = View.GONE
            requestsRecyclerView.visibility = View.GONE
        }
    }

    // Updates the friends list and empty state
    private fun displayFriends(friends: List<User>) {
        friendsAdapter.updateData(friends)
        emptyText.visibility = if (friends.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun acceptRequest(request: FriendRequest) {
        lifecycleScope.launch {
            val result = friendsRepository.acceptFriendRequest(request)
            result.fold(
                onSuccess = {
                    // Clear stale cache after accepting
                    friendsRepository.invalidateCache()
                    Toast.makeText(requireContext(), getString(R.string.toast_friend_added), Toast.LENGTH_SHORT).show()
                    loadData()
                },
                onFailure = {
                    Toast.makeText(requireContext(), getString(R.string.toast_accept_failed), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun declineRequest(request: FriendRequest) {
        lifecycleScope.launch {
            val result = friendsRepository.declineFriendRequest(request)
            result.fold(
                onSuccess = {
                    // Clear stale cache after declining
                    friendsRepository.invalidateCache()
                    loadData()
                },
                onFailure = {
                    Toast.makeText(requireContext(), getString(R.string.toast_decline_failed), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun removeFriend(friend: User) {
        val currentUserId = authRepository.getCurrentUser()?.uid ?: return

        lifecycleScope.launch {
            val result = friendsRepository.removeFriend(currentUserId, friend.id)
            result.fold(
                onSuccess = {
                    // Clear stale cache after removing
                    friendsRepository.invalidateCache()
                    Toast.makeText(requireContext(), getString(R.string.toast_friend_removed), Toast.LENGTH_SHORT).show()
                    loadData()
                },
                onFailure = {
                    Toast.makeText(requireContext(), getString(R.string.toast_remove_failed), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Opens friend's profile page
    private fun openFriendProfile(friend: User) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FriendProfileFragment.newInstance(friend.id))
            .addToBackStack(null)
            .commit()
    }

    override fun refresh() {
        loadData()
    }
}