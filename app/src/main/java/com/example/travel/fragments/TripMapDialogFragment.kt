package com.example.travel.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.travel.R
import com.example.travel.data.PhotoRepository
import com.example.travel.models.Photo
import com.example.travel.utils.MapRenderingUtils
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.launch

// Full-screen dialog that displays a trip's photos on a Google Map
class TripMapDialogFragment : DialogFragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var photoRepository: PhotoRepository
    private val photoMarkers = mutableListOf<Pair<Marker, Photo>>()
    private var tripId: String = ""

    companion object {
        private const val ARG_TRIP_ID = "tripId"

        fun newInstance(tripId: String): TripMapDialogFragment {
            return TripMapDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TRIP_ID, tripId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        tripId = arguments?.getString(ARG_TRIP_ID) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_map_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoRepository = PhotoRepository()
        val mapFragment = childFragmentManager.findFragmentById(R.id.dialogMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Callback triggered when GoogleMap is ready to use
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Show photo popup when marker is clicked
        map.setOnMarkerClickListener { marker ->
            val photo = photoMarkers.find { it.first == marker }?.second
            if (photo != null) {
                MapRenderingUtils.showPhotoDialog(this, photo)
            }
            true
        }

        // Update marker sizes when zoom changes
        map.setOnCameraMoveListener {
            MapRenderingUtils.updateMarkerSizes(this, map, photoMarkers)
        }

        loadPhotosOnMap()
    }

    // Loads photos for the trip and displays them on the map
    private fun loadPhotosOnMap() {
        if (tripId.isEmpty()) return

        lifecycleScope.launch {
            val photos = photoRepository.getPhotosForTrip(tripId)
            if (photos.isEmpty()) return@launch

            MapRenderingUtils.drawTravelPath(map, photos)

            for (photo in photos) {
                MapRenderingUtils.addPhotoMarker(
                    this@TripMapDialogFragment,
                    map,
                    photo,
                    photoMarkers
                )
            }
            MapRenderingUtils.fitCameraToPhotos(map, photos)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::map.isInitialized) {
            map.setOnCameraMoveListener(null)
        }
        photoMarkers.clear()
    }
}
