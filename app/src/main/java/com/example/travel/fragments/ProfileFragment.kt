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
import com.example.travel.models.User
import com.example.travel.utils.loadProfilePicture
import com.example.travel.utils.openTripDetail
import com.example.travel.utils.openTripMap
import kotlinx.coroutines.async

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
    private lateinit var tripAdapter: TripAdapter

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

        authRepository = AuthRepository.instance
        tripRepository = TripRepository.instance
        photoRepository = PhotoRepository.instance
        likeRepository = LikeRepository.instance
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

        // Create adapter once to avoid "No adapter attached" warning
        tripAdapter = TripAdapter(
            onEndTripClick = { trip -> onEndTripClicked(trip) },
            onCardClick = { trip -> openTripDetail(trip.id) },
            onOptionsClick = { trip, _-> showOptionsMenu(trip) },
            onMapClick = { trip -> openTripMap(trip.id) }
        )
        tripsRecyclerView.adapter = tripAdapter

        // Log out button
        view.findViewById<Button>(R.id.logOutButton).setOnClickListener {
            authRepository.logOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        loadProfile()
    }

    private fun loadProfile(excludeTripId: String? = null) {
        val userId = authRepository.getCurrentUser()?.uid ?: return

        // Show cached data immediately so the screen isn't empty while fetching
        val cachedUser = authRepository.getCachedUserProfile(userId)
        val cachedActive = tripRepository.getCachedActiveTrip(userId)
            ?.takeIf { it.id != excludeTripId }
        val cachedCompleted = tripRepository.getCachedCompletedTrips(userId)
            ?.filter { it.id != excludeTripId }
        val cachedLikes = likeRepository.getCachedLikesForTrips()

        if (cachedUser != null && cachedCompleted != null) {
            displayProfile(cachedUser, cachedActive, cachedCompleted, cachedLikes, excludeTripId)
        } else {
            progressBar.visibility = View.VISIBLE
        }

        // Fetch fresh data from Firestore in the background
        lifecycleScope.launch {

            val userDeferred = async { authRepository.getUserProfile(userId) }
            val activeDeferred = async { tripRepository.getActiveTrip(userId) }
            val completedDeferred = async { tripRepository.getCompletedTrips(userId) }

            // Load user info
            val user = userDeferred.await()
            val activeTrip = activeDeferred.await()
                ?.takeIf { it.id != excludeTripId }
            val completedTrips = completedDeferred.await()
                .filter { it.id != excludeTripId }

            // Build full trip list to fetch likes for
            val allTrips = mutableListOf<Trip>()
            activeTrip?.let {allTrips.add(it)}
            allTrips.addAll(completedTrips)

            val tripLikes = if (allTrips.isNotempty()) {
                likeRepository.getLikesForTrips(allTrips, photoRepository)
            } else {
                emptyMap()
            }

            progressBar.visibility = View.GONE
            swipeRefresh.isRefreshing = false

            if (user != null) {
                displayProfile(user, activeTrip, completedTrips, tripLikes, excludeTripId)
            } else {
                emptyText.visibility = View.VISIBLE
            }
        }
    }

    // Populates the profile UI with user info and sorted trips
    private fun displayProfile(
        user: User,
        activeTrip: Trip?,
        completedTrips: List<Trip>,
        tripLikes: Map<String, Int>,
        excludeTripId: String?
    ) {
        displayNameText.text = user.displayName
        usernameText.text = getString(R.string.format_username, user.username)
        profileImage.loadProfilePicture(user.profilePictureUrl)

        val allTrips = mutableListOf<Trip>()
        activeTrip?.let { allTrips.add(it) }
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        allTrips.addAll(completedTrips.sortedByDescending {
            try {
                dateFormat.parse(it.startDate)?.time ?: 0L
            } catch (_: Exception) { 0L }
        })

        if (allTrips.isNotEmpty()) {
            emptyText.visibility = View.GONE
            tripAdapter.updateData(allTrips, tripLikes)
        } else {
            emptyText.visibility = View.VISIBLE
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
        dialogView.findViewById<TextView>(R.id.dialogMessage).text = getString(R.string.dialog_delete_trip_message)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogPositiveButton).apply {
            text = getString(R.string.action_delete)
            setBackgroundColor(0xFFDC2626.toInt())
            setOnClickListener {
                dialog.dismiss()
                confirmDeleteTrip(trip)
            }
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogNegativeButton).apply {
            text = getString(R.string.label_cancel)
            setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }

    // Confirms before deleting a trip
    private fun confirmDeleteTrip(trip: Trip) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm, null)
        dialogView.findViewById<TextView>(R.id.dialogTitle)
            .text = getString(R.string.dialog_delete_trip_title)
        dialogView.findViewById<TextView>(R.id.dialogMessage)
            .text = getString(R.string.dialog_delete_trip_confirm, trip.name)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogPositiveButton).apply {
            text = getString(R.string.action_delete)
            setOnClickListener {
                dialog.dismiss()
                deleteTrip(trip)
            }
        }
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogNegativeButton).apply {
            text = getString(R.string.label_cancel)
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
                Toast.makeText(requireContext(), getString(R.string.toast_upload_picture_failed), Toast.LENGTH_SHORT)
                    .show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}