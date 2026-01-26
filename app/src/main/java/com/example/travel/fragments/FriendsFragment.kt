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
import com.example.travel.models.FriendRequest
import com.example.travel.models.User
import kotlinx.coroutines.launch

class FriendsFragment : Fragment(), Refresh {

    private lateinit var authRepository: AuthRepository
    private lateinit var friendsRepository: FriendsRepository

    private lateinit var requestsLabel: TextView
    private lateinit var requestsRecyclerView: RecyclerView
    private lateinit var friendsRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authRepository = AuthRepository()
        friendsRepository = FriendsRepository()

        // Bind views
        requestsLabel = view.findViewById(R.id.requestsLabel)
        requestsRecyclerView = view.findViewById(R.id.requestsRecyclerView)
        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressBar = view.findViewById(R.id.progressBar)

        requestsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        friendsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

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

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Load pending requests
            val requests = friendsRepository.getPendingRequests(currentUserId)
            val requestsWithUsers = requests.mapNotNull { request ->
                authRepository.getUserProfile(request.senderId)?.let { user ->
                    Pair(request, user)
                }
            }

            // Load friends
            val friends = friendsRepository.getFriends(currentUserId)

            progressBar.visibility = View.GONE

            // Show/hide requests section
            if (requestsWithUsers.isNotEmpty()) {
                requestsLabel.visibility = View.VISIBLE
                requestsRecyclerView.visibility = View.VISIBLE
                requestsRecyclerView.adapter = FriendRequestAdapter(
                    requestsWithUsers,
                    onAcceptClick = { acceptRequest(it) },
                    onDeclineClick = { declineRequest(it) }
                )
            } else {
                requestsLabel.visibility = View.GONE
                requestsRecyclerView.visibility = View.GONE
            }

            // Show friends or empty state
            if (friends.isNotEmpty()) {
                emptyText.visibility = View.GONE
                friendsRecyclerView.adapter = FriendAdapter(friends) { friend ->
                    removeFriend(friend)
                }
            } else {
                emptyText.visibility = View.VISIBLE
            }
        }
    }

    private fun acceptRequest(request: FriendRequest) {
        lifecycleScope.launch {
            val result = friendsRepository.acceptFriendRequest(request)
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Friend added!", Toast.LENGTH_SHORT).show()
                    loadData()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Failed to accept", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun declineRequest(request: FriendRequest) {
        lifecycleScope.launch {
            val result = friendsRepository.declineFriendRequest(request)
            result.fold(
                onSuccess = { loadData() },
                onFailure = {
                    Toast.makeText(requireContext(), "Failed to decline", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Friend removed", Toast.LENGTH_SHORT).show()
                    loadData()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Failed to remove", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun refresh() {
        loadData()
    }
}