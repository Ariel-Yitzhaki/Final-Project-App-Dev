package com.example.travel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.data.AuthRepository
import com.example.travel.data.FriendsRepository
import com.example.travel.models.User
import kotlinx.coroutines.launch

class UserSearchFragment : Fragment() {

    private lateinit var authRepository: AuthRepository
    private lateinit var friendsRepository: FriendsRepository

    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar

    private var currentUserId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authRepository = AuthRepository()
        friendsRepository = FriendsRepository()
        currentUserId = authRepository.getCurrentUser()?.uid ?: ""

        // Bind views
        searchInput = view.findViewById(R.id.searchInput)
        searchButton = view.findViewById(R.id.searchButton)
        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressBar = view.findViewById(R.id.progressBar)

        resultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                searchUsers(query)
            }
        }
    }

    private fun searchUsers(query: String) {
        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE

        lifecycleScope.launch {
            // Search for users
            val users = friendsRepository.searchUsersByUsername(query)
                .filter { it.id != currentUserId }  // Exclude self

            if (users.isEmpty()) {
                progressBar.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                resultsRecyclerView.adapter = null
                return@launch
            }

            // Check relationship status for each user
            val statusMap = mutableMapOf<String, String>()
            for (user in users) {
                statusMap[user.id] = when {
                    friendsRepository.areFriends(currentUserId, user.id) -> "friend"
                    friendsRepository.getPendingRequestBetween(currentUserId, user.id) != null -> "pending"
                    else -> "none"
                }
            }

            progressBar.visibility = View.GONE

            resultsRecyclerView.adapter = UserSearchAdapter(users, statusMap) { user ->
                sendFriendRequest(user)
            }
        }
    }

    private fun sendFriendRequest(user: User) {
        lifecycleScope.launch {
            val result = friendsRepository.sendFriendRequest(currentUserId, user.id)
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Request sent!", Toast.LENGTH_SHORT).show()
                    // Refresh search results to update button state
                    val query = searchInput.text.toString().trim()
                    if (query.isNotEmpty()) {
                        searchUsers(query)
                    }
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Failed to send request", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}