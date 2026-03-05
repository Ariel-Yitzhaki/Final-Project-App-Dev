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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.travel.data.LikeRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.interfaces.Refresh
import com.example.travel.interfaces.TripEndListener
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.android.material.imageview.ShapeableImageView
import com.bumptech.glide.Glide
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

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
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var profileImage: ShapeableImageView

    // Opens gallery to pick a profile picture
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadProfilePicture(it) }
    }


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
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadProfile()
        }
        profileImage = view.findViewById(R.id.profileImage)

        // Opens gallery to pick a profile picture when clicked
        var isPickerOpen = false
        profileImage.setOnClickListener {
            if (!isPickerOpen) {
                isPickerOpen = true
                pickImageLauncher.launch("image/*")
                it.postDelayed({ isPickerOpen = false }, 1000)
            }
        }

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
                if (it.profilePictureUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(it.profilePictureUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .into(profileImage)
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile)
                }
            }

            // Load completed trips
            val activeTrip = tripRepository.getActiveTrip(userId)
            val completedTrips = tripRepository.getCompletedTrips(userId)

            // Active trip first, then completed trips sorted by date (newest first)
            val allTrips = mutableListOf<Trip>()
            activeTrip?.let {allTrips.add(it)}
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            allTrips.addAll(completedTrips.sortedByDescending {
                try {
                    dateFormat.parse(it.startDate)?.time ?: 0L
                } catch (_: Exception) { 0L }
            })

            // Load cover photos for each trip
            progressBar.visibility = View.GONE

            if (allTrips.isNotEmpty()) {
                emptyText.visibility = View.GONE

                val tripLikes = likeRepository.getLikesForTrips(allTrips, photoRepository)

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
            swipeRefresh.isRefreshing = false
        }
    }

    private fun onEndTripClicked(trip: Trip) {
        lifecycleScope.launch {
            if (trip.photoCount == 0) {
                // Delete empty trip
                tripRepository.deleteTrip(trip.id)
            } else {
                // Mark trip as completed with today's date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val today = dateFormat.format(java.util.Date())
                tripRepository.deactivateTrip(trip.id, today)
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
            if (item.title == "Delete Trip") {
                confirmDeleteTrip(trip)
                true
            } else {
                false
            }
        }
        popup.show()
    }

    // Confirms before deleting a trip
    private fun confirmDeleteTrip(trip: Trip) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm, null)
        dialogView.findViewById<TextView>(R.id.dialogTitle).text = "Delete Trip"
        dialogView.findViewById<TextView>(R.id.dialogMessage).text = "Are you sure you want to delete '${trip.name}'? This cannot be undone."

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogPositiveButton).apply {
            text = "Delete"
            setOnClickListener {
                dialog.dismiss()
                deleteTrip(trip)
            }
        }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogNegativeButton).apply {
            text = "Cancel"
            setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
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

    // Uploads selected image to Firebase Storage and updates user profile
    private fun uploadProfilePicture(imageUri: Uri) {
        val userId = authRepository.getCurrentUser()?.uid ?: return

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Upload image to Storage under profile_pictures folder
                val storageRef = FirebaseStorage.getInstance().reference
                val imageRef = storageRef.child("profile_pictures/$userId.jpg")
                imageRef.putFile(imageUri).await()

                // Get download URL and save to user profile in Firestore
                val downloadUrl = imageRef.downloadUrl.await().toString()
                authRepository.updateProfilePicture(userId, downloadUrl)

                // Display the new profile picture
                Glide.with(requireContext())
                    .load(downloadUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(profileImage)
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Failed to upload picture", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}