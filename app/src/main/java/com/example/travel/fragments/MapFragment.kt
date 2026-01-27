package com.example.travel.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.travel.R
import com.example.travel.data.AuthRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import com.example.travel.models.Photo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch

// Fragment that displays a Google Map and centers it on user's location
class MapFragment : Fragment(), OnMapReadyCallback, Refresh {

    // GoogleMap object - controls the map display, markers, camera, etc.
    private lateinit var map: GoogleMap

    // Google's location service - gets device location using GPS, WiFi, cell towers
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var photoRepository: PhotoRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var authRepository: AuthRepository
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
        tripRepository = TripRepository()
        authRepository = AuthRepository()

        // Find the map fragment and request the GoogleMap object asynchronously
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Callback triggered when GoogleMap is ready to use - check permissions and enable location
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

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
        map.setOnCameraMoveListener {
            updateMarkerSizes()
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
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        }
    }

    private fun loadPhotosOnMap() {
        lifecycleScope.launch {
            val userId = authRepository.getCurrentUser()?.uid ?: return@launch
            val activeTrip = tripRepository.getActiveTrip(userId)

            // Only load photos for active trip
            val photos = if (activeTrip != null) {
                photoRepository.getAllPhotos().filter { it.tripId == activeTrip.id }
            } else {
                emptyList()
            }

            // Draw travel path first (so it's behind markers)
            drawTravelPath(photos)

            for (photo in photos) {
                val position = LatLng(photo.latitude, photo.longitude)
                val size = getMarkerSizeForZoom(map.cameraPosition.zoom)

                Glide.with(this@MapFragment)
                    .asBitmap()
                    .load(photo.localPath)
                    .override(size, size)
                    .centerCrop()
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val pinBitmap = createPinWithPhoto(resource, 8, Color.WHITE)
                            val markerOptions = MarkerOptions()
                                .position(position)
                                .title(photo.date)
                                .icon(BitmapDescriptorFactory.fromBitmap(pinBitmap))

                            map.addMarker(markerOptions)?.let { marker ->
                                photoMarkers.add(Pair(marker, photo))
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
    }

    // Draws lines connecting photos in chronological order
    private fun drawTravelPath(photos: List<Photo>) {
        if (photos.size < 2) return

        // Sort by timestamp and create path
        val sortedPhotos = photos.sortedBy { it.timestamp }
        val points = sortedPhotos.map { LatLng(it.latitude, it.longitude) }

        val polylineOptions = PolylineOptions()
            .addAll(points)
            .width(8f)
            .color(Color.BLUE)
            .geodesic(true)

        map.addPolyline(polylineOptions)
    }

    private fun updateMarkerWithSize(marker: Marker, photo: Photo, size: Int) {
        Glide.with(this@MapFragment)
            .asBitmap()
            .load(photo.localPath)
            .override(size, size)
            .centerCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val pinBitmap = createPinWithPhoto(resource, 8, Color.WHITE)
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(pinBitmap))
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun createPinWithPhoto(photo: Bitmap, borderWidth: Int, borderColor: Int): Bitmap {
        val pointerHeight = 50
        val totalWidth = photo.width + borderWidth * 2
        val totalHeight = photo.height + borderWidth * 2 + pointerHeight

        val output = createBitmap(totalWidth, totalHeight)
        val canvas = Canvas(output)

        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = borderColor

        // Draw rectangle (frame)
        val rectF = RectF(0f, 0f, totalWidth.toFloat(), (totalHeight - pointerHeight).toFloat())
        canvas.drawRect(rectF, paint)

        // Draw pointer triangle from full width
        val path = Path()
        path.moveTo(0f, (totalHeight - pointerHeight).toFloat())
        path.lineTo(totalWidth.toFloat(), (totalHeight - pointerHeight).toFloat())
        path.lineTo(totalWidth / 2f, totalHeight.toFloat())
        path.close()
        canvas.drawPath(path, paint)

        // Draw photo inside frame
        canvas.drawBitmap(photo, borderWidth.toFloat(), borderWidth.toFloat(), null)

        return output
    }

    private fun getMarkerSizeForZoom(zoom: Float): Int {
        return when {
            zoom >= 18f -> 250  // Very zoomed in
            zoom >= 16f -> 200  // Zoomed in
            zoom >= 14f -> 150  // Medium
            else -> 100         // Zoomed out
        }
    }

    private fun updateMarkerSizes() {
        val size = getMarkerSizeForZoom(map.cameraPosition.zoom)
        for ((marker, photo) in photoMarkers) {
            updateMarkerWithSize(marker, photo, size)
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