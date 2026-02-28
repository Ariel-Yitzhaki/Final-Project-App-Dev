package com.example.travel.fragments

import android.Manifest
import android.app.AlertDialog
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
import com.example.travel.activities.MainActivity
import com.example.travel.data.PhotoRepository
import com.example.travel.interfaces.Refresh
import com.example.travel.models.Photo
import com.example.travel.utils.GeocodingUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import android.widget.ImageView
import android.widget.TextView

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
        return inflater.inflate(R.layout.fragment_map, container,
            false)
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

        map.setOnMarkerClickListener { marker ->
            val photo = photoMarkers.find { it.first == marker }?.second
            if (photo != null) {
                showPhotoDialog(photo)
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

            drawTravelPath(photos)

            for (photo in photos) {
                addPhotoMarker(photo)
            }
        }
    }

    // Creates and adds a custom marker for a photo on the map
    private fun addPhotoMarker(photo: Photo) {
        val position = LatLng(photo.latitude, photo.longitude)
        val size = getMarkerSizeForZoom(map.cameraPosition.zoom)

        Glide.with(this)
            .asBitmap()
            .load(photo.imageUrl)
            .override(size, size)
            .centerCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val pinBitmap = createPinWithPhoto(resource, 8,
                        Color.WHITE)

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

    // Draws lines connecting photos in chronological order with direction arrows
    private fun drawTravelPath(photos: List<Photo>) {
        if (photos.size < 2) return

        // Sort by timestamp and create path
        val sortedPhotos = photos.sortedBy { it.timestamp }
        val points = sortedPhotos.map {LatLng(it.latitude, it.longitude)}
        val lineColor = "#51B946".toColorInt()
        val arrowColor = "#BF2C2B".toColorInt()
        // Draw line segment
        val polylineOptions = PolylineOptions()
            .addAll(points)
            .width(4f)
            .color(lineColor)
            .geodesic(true)
            .pattern(listOf(Dash(30f), Gap(20f)))

        map.addPolyline(polylineOptions)

        // Add arrow markers between each segment
        for (i in 0 until sortedPhotos.size - 1) {
            val start = points[i]
            val end = points[i + 1]
            addDirectionArrow(start, end, arrowColor)
        }
    }

    // Adds an arrow marker at the midpoint between two locations
    private fun addDirectionArrow(start: LatLng, end: LatLng, color: Int) {
        val midLat = (start.latitude + end.latitude) / 2
        val midLng = (start.longitude + end.longitude) / 2
        val midpoint = LatLng(midLat, midLng)

        // Use Location class to calculate bearing
        val results = FloatArray(2)
        android.location.Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        val bearing = results[1] // Initial bearing in degrees

        val arrowBitmap = createArrowBitmap(color)

        map.addMarker(
            MarkerOptions()
                .position(midpoint)
                .icon(BitmapDescriptorFactory.fromBitmap(arrowBitmap))
                .rotation(bearing)
                .anchor(0.5f, 0.5f)
                .flat(true)
        )
    }

    // Creates a small arrow bitmap
    private fun createArrowBitmap(color: Int): Bitmap {
        val size = 30
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val path = Path().apply {
            moveTo(size / 2f, 0f) // Top point (tip)
            lineTo(size.toFloat(), size.toFloat()) // Bottom right
            lineTo(size / 2f, size* 0.65f) // Center
            lineTo(0f, size.toFloat()) // Bottom left
            close()
        }

        canvas.drawPath(path, paint)
        return bitmap
    }

    private fun updateMarkerWithSize(marker: Marker, photo: Photo, size: Int) {
        val fragment = this

        Glide.with(fragment)
            .asBitmap()
            .load(photo.imageUrl)
            .override(size, size)
            .centerCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val pinBitmap = createPinWithPhoto(resource, 8,
                        Color.WHITE)

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

        // Draw photo inside frame
        canvas.drawBitmap(photo, borderWidth.toFloat(), borderWidth.toFloat(),
            null)

        // Draw pointer triangle
        val path = Path()
        path.moveTo(0f, (totalHeight - pointerHeight).toFloat())
        path.lineTo(totalWidth.toFloat(), (totalHeight - pointerHeight).toFloat())
        path.lineTo(totalWidth / 2f, totalHeight.toFloat())
        path.close()
        canvas.drawPath(path, paint)

        return output
    }

    private fun getMarkerSizeForZoom(zoom: Float): Int {
        return when {
            zoom >= 18f -> 300  // Very zoomed in
            zoom >= 16f -> 250  // Zoomed in
            zoom >= 14f -> 200  // Medium
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

    // Shows a popup dialog with the full photo
    private fun showPhotoDialog(photo: Photo) {
        val dialogView = layoutInflater.inflate(R.layout.popup_photo, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.dialogPhotoImage)
        val dateText = dialogView.findViewById<TextView>(R.id.dialogPhotoDate)
        val locationText = dialogView.findViewById<TextView>(R.id.dialogPhotoLocation)

        Glide.with(this)
            .load(photo.imageUrl)
            .into(imageView)

        dateText.text = photo.date
        locationText.text = GeocodingUtils.getAddressFromCoordinates(
            requireContext(),
            photo.latitude,
            photo.longitude
        )

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }
}