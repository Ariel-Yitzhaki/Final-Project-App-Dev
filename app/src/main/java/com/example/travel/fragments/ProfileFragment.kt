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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.travel.data.LikeRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.interfaces.Refresh
import com.example.travel.interfaces.TripEndListener
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.android.material.imageview.ShapeableImageView
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.travel.utils.setDebouncedClickListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.example.travel.managers.TripCleanupManager
import com.example.travel.utils.loadProfilePicture
import com.example.travel.utils.openTripDetail
import com.example.travel.utils.openTripMap

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
    private lateinit var tripCleanupManager: TripCleanupManager

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
        tripCleanupManager = TripCleanupManager(tripRepository, photoRepository, likeRepository)

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

        // Opens gallery to pick a profile picture
        profileImage.setDebouncedClickListener {
            pickImageLauncher.launch("image/*")
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

    private fun loadProfile(excludeTripId: String? = null) {
        val userId = authRepository.getCurrentUser()?.uid ?: return
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Load user info
            val user = authRepository.getUserProfile(userId)
            user?.let {
                displayNameText.text = it.displayName
                usernameText.text = "@${it.username}"
                profileImage.loadProfilePicture(it.profilePictureUrl)
            }

            // Load completed trips
            val activeTrip = tripRepository.getActiveTrip(userId)
                ?.takeIf { it.id != excludeTripId }
            val completedTrips = tripRepository.getCompletedTrips(userId)
                .filter {it.id != excludeTripId}

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
                    onCardClick = { trip -> openTripDetail(trip.id) },
                    onOptionsClick = { trip, _-> showOptionsMenu(trip) },
                    onMapClick = { trip -> openTripMap(trip.id) }
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
                tripCleanupManager.deleteTrip(trip.id)
                // Refresh the list
                loadProfile(excludeTripId = trip.id)
            } else {
                // Mark trip as completed with today's date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val today = dateFormat.format(java.util.Date())
                tripRepository.deactivateTrip(trip.id, today)
                // Refresh the list
                loadProfile()
            }
            tripEndListener?.onTripEnded()
        }
    }

    // Shows popup menu with trip options
    private fun showOptionsMenu(trip: Trip) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm, null)
        dialogView.findViewById<TextView>(R.id.dialogTitle).text = trip.name
        dialogView.findViewById<TextView>(R.id.dialogMessage).text = "Delete This Trip?"

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogPositiveButton).apply {
            text = "Delete"
            setBackgroundColor(0xFFDC2626.toInt())
            setOnClickListener {
                dialog.dismiss()
                confirmDeleteTrip(trip)
            }
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogNegativeButton).apply {
            text = "Cancel"
            setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
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
            tripCleanupManager.deleteTrip(trip.id)
            loadProfile(excludeTripId = trip.id)
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
                profileImage.loadProfilePicture(downloadUrl)
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Failed to upload picture", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}