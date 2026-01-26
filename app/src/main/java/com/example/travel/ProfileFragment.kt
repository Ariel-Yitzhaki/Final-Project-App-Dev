package com.example.travel

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
import com.example.travel.data.AuthRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import kotlinx.coroutines.launch

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
            val trips = tripRepository.getCompletedTrips(userId)

            // Load cover photos for each trip
            progressBar.visibility = View.GONE

            if (trips.isNotEmpty()) {
                emptyText.visibility = View.GONE
                tripsRecyclerView.adapter = TripAdapter(trips) { trip ->
                    // TODO: Open trip detail screen
                }
            }
        }
    }
}