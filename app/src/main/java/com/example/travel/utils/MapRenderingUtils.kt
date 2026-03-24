package com.example.travel.utils

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.travel.R
import com.example.travel.models.Photo
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

// Shared map rendering logic used by MapFragment and TripMapDialogFragment
object MapRenderingUtils {

    // Creates and adds a custom marker for a photo on the map
    fun addPhotoMarker(
        fragment: Fragment,
        map: GoogleMap,
        photo: Photo,
        photoMarkers: MutableList<Pair<Marker, Photo>>
    ) {
        val position = LatLng(photo.latitude, photo.longitude)
        val size = getMarkerSizeForZoom(map.cameraPosition.zoom)

        Glide.with(fragment)
            .asBitmap()
            .load(photo.imageUrl)
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

    // Draws lines connecting photos in chronological order with direction arrows
    fun drawTravelPath(map: GoogleMap, photos: List<Photo>) {
        if (photos.size < 2) return

        val sortedPhotos = photos.sortedBy { it.timestamp }
        val points = sortedPhotos.map { LatLng(it.latitude, it.longitude) }
        val lineColor = "#51B946".toColorInt()
        val arrowColor = "#BF2C2B".toColorInt()

        // Draw dashed line segment
        val polylineOptions = PolylineOptions()
            .addAll(points)
            .width(4f)
            .color(lineColor)
            .geodesic(true)
            .pattern(listOf(Dash(30f), Gap(20f)))

        map.addPolyline(polylineOptions)

        // Add arrow markers between each segment
        for (i in 0 until sortedPhotos.size - 1) {
            addDirectionArrow(map, points[i], points[i + 1], arrowColor)
        }
    }

    // Updates all marker sizes based on current zoom level
    fun updateMarkerSizes(
        fragment: Fragment,
        map: GoogleMap,
        photoMarkers: List<Pair<Marker, Photo>>
    ) {
        val size = getMarkerSizeForZoom(map.cameraPosition.zoom)
        for ((marker, photo) in photoMarkers) {
            updateMarkerWithSize(fragment, marker, photo, size)
        }
    }

    // Moves camera to fit all photo locations with padding
    fun fitCameraToPhotos(map: GoogleMap, photos: List<Photo>) {
        if (photos.isEmpty()) return

        if (photos.size == 1) {
            val pos = LatLng(photos[0].latitude, photos[0].longitude)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
            return
        }

        val boundsBuilder = LatLngBounds.Builder()
        for (photo in photos) {
            boundsBuilder.include(LatLng(photo.latitude, photo.longitude))
        }
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
    }

    // Shows a popup dialog with the full photo, date, and location
    fun showPhotoDialog(fragment: Fragment, photo: Photo) {
        val dialogView = fragment.layoutInflater.inflate(R.layout.popup_photo, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.dialogPhotoImage)
        val dateText = dialogView.findViewById<TextView>(R.id.dialogPhotoDate)
        val locationText = dialogView.findViewById<TextView>(R.id.dialogPhotoLocation)

        Glide.with(fragment)
            .load(photo.imageUrl)
            .into(imageView)

        dateText.text = photo.date
        locationText.text = GeocodingUtils.getAddressFromCoordinates(
            fragment.requireContext(),
            photo.latitude,
            photo.longitude
        )

        AlertDialog.Builder(fragment.requireContext())
            .setView(dialogView)
            .show()
    }

    // Returns marker size in pixels based on map zoom level
    fun getMarkerSizeForZoom(zoom: Float): Int {
        return when {
            zoom >= 18f -> 300  // Very zoomed in
            zoom >= 16f -> 250  // Zoomed in
            zoom >= 14f -> 200  // Medium
            else -> 100         // Zoomed out
        }
    }

    // Reloads a marker's image at a new size
    private fun updateMarkerWithSize(fragment: Fragment, marker: Marker, photo: Photo, size: Int) {
        Glide.with(fragment)
            .asBitmap()
            .load(photo.imageUrl)
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

    // Adds an arrow marker at the midpoint between two locations
    private fun addDirectionArrow(map: GoogleMap, start: LatLng, end: LatLng, color: Int) {
        val midpoint = com.google.maps.android.SphericalUtil.interpolate(start, end, 0.5)

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

    // Creates a small arrow bitmap pointing upward
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
            moveTo(size / 2f, 0f)       // Top point (tip)
            lineTo(size.toFloat(), size.toFloat()) // Bottom right
            lineTo(size / 2f, size * 0.65f)        // Center
            lineTo(0f, size.toFloat())              // Bottom left
            close()
        }

        canvas.drawPath(path, paint)
        return bitmap
    }

    // Creates a pin-shaped bitmap with a photo inside a white border and a pointer triangle
    private fun createPinWithPhoto(photo: Bitmap, borderWidth: Int, borderColor: Int): Bitmap {
        val pointerHeight = 30

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
        canvas.drawBitmap(photo, borderWidth.toFloat(), borderWidth.toFloat(), null)

        // Draw pointer triangle
        val path = Path()
        path.moveTo(0f, (totalHeight - pointerHeight).toFloat())
        path.lineTo(totalWidth.toFloat(), (totalHeight - pointerHeight).toFloat())
        path.lineTo(totalWidth / 2f, totalHeight.toFloat())
        path.close()
        canvas.drawPath(path, paint)

        return output
    }
}