package com.example.travel.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.travel.R
import com.example.travel.adapters.TripPhotoAdapter
import com.example.travel.data.AuthRepository
import com.example.travel.data.LikeRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import com.example.travel.managers.TripCleanupManager
import com.example.travel.models.Photo
import com.example.travel.utils.GeocodingUtils
import kotlinx.coroutines.launch
import com.example.travel.utils.loadProfilePicture
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.async

// Displays photos from a single trip in a vertical scrollable list
class TripDetailFragment : Fragment() {

    private lateinit var photoRepository: PhotoRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var tripNameText: TextView
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tripCleanupManager: TripCleanupManager
    private lateinit var profileImage: ShapeableImageView
    private var tripId: String = ""
    private var photoAdapter: TripPhotoAdapter? = null
    private var isOwner = false


    companion object {
        private const val ARG_TRIP_ID = "tripId"

        // Creates a new instance with tripId argument
        fun newInstance(tripId: String): TripDetailFragment {
            return TripDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TRIP_ID, tripId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tripId = arguments?.getString(ARG_TRIP_ID) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoRepository = PhotoRepository.instance
        tripRepository = TripRepository.instance
        authRepository = AuthRepository.instance

        // Handles cleanup of photos and their associated data
        tripCleanupManager = TripCleanupManager(tripRepository, photoRepository, LikeRepository.instance)

        // Bind views
        tripNameText = view.findViewById(R.id.tripNameText)
        profileImage = view.findViewById(R.id.profileImage)
        photosRecyclerView = view.findViewById(R.id.photosRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressBar = view.findViewById(R.id.progressBar)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        // Hide content until data is ready to avoid showing default placeholders
        profileImage.visibility = View.INVISIBLE
        tripNameText.visibility = View.INVISIBLE
        photosRecyclerView.visibility = View.INVISIBLE

        swipeRefresh.setOnRefreshListener {
            loadTripDetails()
        }

        photosRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        // Create adapter once to avoid "No adapter attached" warning
        photoAdapter = TripPhotoAdapter(
            mutableListOf(),
            mutableMapOf(),
            AuthRepository.instance.getCurrentUser()?.uid ?: "",
            lifecycleScope,
            LikeRepository.instance,
            isOwner
        ) { photo -> showDeletePhotoDialog(photo) }
        photosRecyclerView.adapter = photoAdapter

        // Back button returns to previous fragment
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadTripDetails()
    }

    // Loads trip info and photos from repositories asynchronously
    private fun loadTripDetails() {
        if (tripId.isEmpty()) return

        // Show cached data immediately if available
        val cachedTrip = tripRepository.getCachedTripById(tripId)
        val cachedPhotos = photoRepository.getCachedPhotosForTrip(tripId)

        if (cachedTrip != null && cachedPhotos != null) {
            // Content is ready, make everything visible
            profileImage.visibility = View.VISIBLE
            tripNameText.visibility = View.VISIBLE
            photosRecyclerView.visibility = View.VISIBLE

            tripNameText.text = cachedTrip.name
            val cachedOwner = authRepository.getCachedUserProfile(cachedTrip.userId)
            cachedOwner?.let { profileImage.loadProfilePicture(it.profilePictureUrl) }
            isOwner = cachedTrip.userId == authRepository.getCurrentUser()?.uid
            photoAdapter?.setOwner(isOwner)
            if (cachedPhotos.isNotEmpty()) {
                emptyText.visibility = View.GONE
                displayPhotos(cachedPhotos.sortedByDescending { it.timestamp })
            }
        } else {
            progressBar.visibility = View.VISIBLE
        }

        // Fetch fresh data from Firestore in the background
        lifecycleScope.launch {
            val tripDeferred = async { tripRepository.getTripById(tripId) }
            val photosDeferred = async { photoRepository.getPhotosForTrip(tripId) }

            val trip = tripDeferred.await()

            if (trip == null) {
                progressBar.visibility = View.GONE
                tripNameText.text = getString(R.string.trip_unavailable)
                emptyText.text = getString(R.string.trip_no_longer_available)
                emptyText.visibility = View.VISIBLE
                return@launch
            }

            // Content is ready, make everything visible
            profileImage.visibility = View.VISIBLE
            tripNameText.visibility = View.VISIBLE
            photosRecyclerView.visibility = View.VISIBLE

            tripNameText.text = trip.name

            // Load trip owner's profile picture
            val owner = authRepository.getUserProfile(trip.userId)
            owner?.let { profileImage.loadProfilePicture(it.profilePictureUrl) }

            isOwner = trip.userId == authRepository.getCurrentUser()?.uid
            photoAdapter?.setOwner(isOwner)

            val photos = photosDeferred.await().sortedByDescending { it.timestamp }

            progressBar.visibility = View.GONE
            swipeRefresh.isRefreshing = false

            if (photos.isNotEmpty()) {
                emptyText.visibility = View.GONE
                displayPhotos(photos)
            } else {
                emptyText.visibility = View.VISIBLE
            }
        }
    }

    // Updates adapter with photos and their addresses
    private fun displayPhotos(photos: List<Photo>) {
        val addresses = getAddressesForPhotos(photos)
        photoAdapter?.updatePhotos(photos.toMutableList(), addresses.toMutableMap())
    }

    // Converts photo coordinates to readable addresses
    private fun getAddressesForPhotos(photos: List<Photo>): Map<String, String> {
        val addresses = mutableMapOf<String, String>()

        for (photo in photos) {
            addresses[photo.id] = GeocodingUtils.getAddressFromCoordinates(
                requireContext(),
                photo.latitude,
                photo.longitude
            )
        }

        return addresses
    }

    // Shows confirmation dialog before deleting a photo
    private fun showDeletePhotoDialog(photo: Photo) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm, null)
        dialogView.findViewById<TextView>(R.id.dialogTitle).text =
            getString(R.string.dialog_delete_photo_title)
        dialogView.findViewById<TextView>(R.id.dialogMessage).text =
            getString(R.string.dialog_delete_photo_message)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogPositiveButton).apply {
            text = getString(R.string.action_delete)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                resources.getColor(R.color.button_destructive, null)
            )
            setOnClickListener {
                lifecycleScope.launch {
                    tripCleanupManager.deletePhoto(photo.id, tripId)
                    loadTripDetails()
                }
                dialog.dismiss()
            }
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogNegativeButton).apply {
            text = getString(R.string.label_cancel)
            setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }
}
