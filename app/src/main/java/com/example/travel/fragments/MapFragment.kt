package com.example.travel.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.travel.R
import com.example.travel.activities.MainActivity
import com.example.travel.data.PhotoRepository
import com.example.travel.interfaces.Refresh
import com.example.travel.models.Photo
import com.example.travel.utils.MapRenderingUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.launch

// Fragment that displays a Google Map and centers it on user's location
class MapFragment : Fragment(), OnMapReadyCallback, Refresh {

    // GoogleMap object - controls the map display, markers, camera, etc.
    private lateinit var map: GoogleMap

    // Google's location service - gets device location using GPS, WiFi, cell towers
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var photoRepository: PhotoRepository
    private val photoMarkers = mutableListOf<Pair<Marker, Photo>>()

    // Modern way to request permissions - launches system permission dialog and handles result
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableMyLocation()
        }
    }

    // Creates the fragment's view by inflating the XML layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    // Called after view is created - initializes location client and starts loading the map
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Google's location service
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        photoRepository = PhotoRepository()

        // Find the map fragment and request the GoogleMap object asynchronously
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Callback triggered when GoogleMap is ready to use - check permissions and enable location
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Move My Location button to bottom-right
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.view?.let { mapView ->
            val locationButton = mapView.findViewWithTag("GoogleMapMyLocationButton")
                ?: mapView.findViewById<View>(2)
            locationButton?.let {
                val parent = it.parent as ViewGroup
                parent.removeView(it)
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                params.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
                params.bottomMargin = 32
                params.rightMargin = 16
                (mapView as ViewGroup).addView(it, params)
            }
        }

        // Show photo popup when marker is clicked
        map.setOnMarkerClickListener { marker ->
            val photo = photoMarkers.find { it.first == marker }?.second
            if (photo != null) {
                MapRenderingUtils.showPhotoDialog(this, photo)
            }
            true
        }

        // Check if we already have location permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            // Request permission from user via system dialog
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Update marker sizes when zoom changes
        map.setOnCameraMoveListener {
            MapRenderingUtils.updateMarkerSizes(this, map, photoMarkers)
        }
    }

    // Enables the blue dot on map showing user's location and moves camera there
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true  // Shows blue dot on map
            moveToCurrentLocation()
            loadPhotosOnMap()
        }
    }

    // Gets device's last known location and animates the map camera to that position
    private fun moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // lastLocation is async - addOnSuccessListener handles the result when ready
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    // Move camera to location with zoom level 15 (street level)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng,
                        15f))
                }
            }
        }
    }

    // Loads photos for active trip and displays them on map
    private fun loadPhotosOnMap() {
        lifecycleScope.launch {
            val activeTripId = (activity as? MainActivity)?.getActiveTripId()

            val photos = if (activeTripId != null) {
                photoRepository.getPhotosForTrip(activeTripId)
            } else {
                emptyList()
            }

            MapRenderingUtils.drawTravelPath(map, photos)

            for (photo in photos) {
                MapRenderingUtils.addPhotoMarker(this@MapFragment, map, photo, photoMarkers)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::map.isInitialized) {
            map.setOnCameraMoveListener(null)
        }
        photoMarkers.clear()
    }

    override fun refresh() {
        map.clear()
        photoMarkers.clear()
        loadPhotosOnMap()
    }

}