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

// Displays photos from a single trip in a vertical scrollable list
class TripDetailFragment : Fragment() {

    private lateinit var photoRepository: PhotoRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var tripNameText: TextView
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var tripId: String = ""
    private var photoAdapter: TripPhotoAdapter? = null
    private var isOwner = false
    private lateinit var tripCleanupManager: TripCleanupManager


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

        photoRepository = PhotoRepository()
        tripRepository = TripRepository()
        // Handles cleanup of photos and their associated data
        tripCleanupManager = TripCleanupManager(tripRepository, photoRepository, LikeRepository())

        // Bind views
        tripNameText = view.findViewById(R.id.tripNameText)
        photosRecyclerView = view.findViewById(R.id.photosRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressBar = view.findViewById(R.id.progressBar)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadTripDetails()
        }

        photosRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        // Create adapter once to avoid "No adapter attached" warning
        photoAdapter = TripPhotoAdapter(
            mutableListOf(),
            mutableMapOf(),
            AuthRepository().getCurrentUser()?.uid ?: "",
            lifecycleScope,
            LikeRepository(),
            isOwner
        ) { photo -> showDeletePhotoDialog(photo) }
        photosRecyclerView.adapter = photoAdapter

        // Back button returns to previous fragment
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadTripDetails()
    }

    // Loads trip info and photos from repositories
    private fun loadTripDetails() {
        if (tripId.isEmpty()) return

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val trip = tripRepository.getTripById(tripId)

            if (trip == null) {
                progressBar.visibility = View.GONE
                tripNameText.text = "Trip unavailable"
                emptyText.text = "This trip is no longer available"
                emptyText.visibility = View.VISIBLE
                return@launch
            }

            tripNameText.text = trip.name
            isOwner = trip.userId == AuthRepository().getCurrentUser()?.uid
            photoAdapter?.setOwner(isOwner)

            val photos = photoRepository.getPhotosForTrip(tripId).sortedByDescending { it.timestamp }

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
        dialogView.findViewById<TextView>(R.id.dialogTitle).text = "Delete Photo"
        dialogView.findViewById<TextView>(R.id.dialogMessage).text =
            "Are you sure you want to delete this photo from the trip?"

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.dialogPositiveButton).apply {
            text = "Delete"
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
            text = "Cancel"
            setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }
}
