package com.example.travel.fragments

import android.location.Geocoder
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
import com.example.travel.R
import com.example.travel.adapters.TripPhotoAdapter
import com.example.travel.data.AuthRepository
import com.example.travel.data.LikeRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import com.example.travel.models.Photo
import kotlinx.coroutines.launch
import java.util.Locale

// Displays photos from a single trip in a vertical scrollable list
class TripDetailFragment : Fragment() {

    private lateinit var photoRepository: PhotoRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var tripNameText: TextView
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar
    private var tripId: String = ""

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

        // Bind views
        tripNameText = view.findViewById(R.id.tripNameText)
        photosRecyclerView = view.findViewById(R.id.photosRecyclerView)
        emptyText = view.findViewById(R.id.emptyText)
        progressBar = view.findViewById(R.id.progressBar)

        photosRecyclerView.layoutManager = LinearLayoutManager(requireContext())

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

            val photos = photoRepository.getPhotosForTrip(tripId).sortedBy { it.timestamp }

            progressBar.visibility = View.GONE

            if (photos.isNotEmpty()) {
                emptyText.visibility = View.GONE
                displayPhotos(photos)
            } else {
                emptyText.visibility = View.VISIBLE
            }
        }
    }

    // Sets up adapter with photos and their addresses
    private fun displayPhotos(photos: List<Photo>) {
        val addresses = getAddressesForPhotos(photos)
        val currentUserId = AuthRepository().getCurrentUser()?.uid ?: ""
        val likeRepository = LikeRepository()

        photosRecyclerView.adapter = TripPhotoAdapter(
            photos,
            addresses,
            currentUserId,
            lifecycleScope,
            likeRepository
        )
    }

    // Converts photo coordinates to readable addresses
    @Suppress("DEPRECATION")
    private fun getAddressesForPhotos(photos: List<Photo>): Map<String, String> {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses = mutableMapOf<String, String>()

        for (photo in photos) {
            try {
                val results = geocoder.getFromLocation(photo.latitude, photo.longitude, 1)
                val address = results?.firstOrNull()
                if (address != null) {
                    addresses[photo.id] = address.locality ?: address.subAdminArea ?: address.adminArea ?: "%.4f, %.4f".format(photo.latitude, photo.longitude)
                } else {
                    addresses[photo.id] = "%.4f, %.4f".format(photo.latitude, photo.longitude)
                }
            } catch (_: Exception) {
                addresses[photo.id] = "%.4f, %.4f".format(photo.latitude, photo.longitude)
            }
        }
        return addresses
    }
}
