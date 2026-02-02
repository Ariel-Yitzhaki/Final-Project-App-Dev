package com.example.travel.activities

import android.app.AlertDialog
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.adapters.TripMenuAdapter
import com.example.travel.data.AuthRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import com.example.travel.models.Trip
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Handles trip creation, selection, and state management
class TripManager(
    private val activity: AppCompatActivity,
    private val authRepository: AuthRepository,
    private val tripRepository: TripRepository,
    private val photoRepository: PhotoRepository
) {
    var activeTrip: Trip? = null
        private set

    // Callback when trip state changes (for UI updates)
    var onTripStateChanged: ((Trip?) -> Unit)? = null

    // Callback to open camera after starting new trip
    var onOpenCamera: (() -> Unit)? = null

    // Checks if user has an active trip and updates state
    suspend fun checkActiveTrip() {
        val userId = authRepository.getCurrentUser()?.uid ?: return
        activeTrip = tripRepository.getActiveTrip(userId)
        onTripStateChanged?.invoke(activeTrip)
    }

    // Called when trip is ended from ProfileFragment
    fun clearActiveTrip() {
        activeTrip = null
        onTripStateChanged?.invoke(null)
    }

    // Shows bottom sheet menu for trip selection
    fun showTripMenu() {
        val userId = authRepository.getCurrentUser()?.uid ?: return

        activity.lifecycleScope.launch {
            val allTrips = tripRepository.getAllTripsForUser(userId)
            val currentActive = tripRepository.getActiveTrip(userId)

            // Build menu items: None, New Trip, then existing trips sorted by date
            val menuItems = mutableListOf<Trip?>()
            menuItems.add(null) // "None" option
            menuItems.add(Trip()) // Empty trip = "New Trip" option

            // Add existing trips, sorted: active first, then by startDate descending
            val sortedTrips = allTrips.sortedWith(
                compareByDescending<Trip> { it.active }.thenByDescending { it.startDate }
            )
            menuItems.addAll(sortedTrips)

            // Cap at 7 items for display
            val displayItems = menuItems.take(7)

            // Create and show bottom sheet
            val bottomSheet = BottomSheetDialog(activity)
            val view = activity.layoutInflater.inflate(R.layout.bottom_sheet_trip_menu, null)
            bottomSheet.setContentView(view)

            val recycler = view.findViewById<RecyclerView>(R.id.recyclerTrips)
            recycler.layoutManager = LinearLayoutManager(activity)
            recycler.adapter = TripMenuAdapter(displayItems, currentActive?.id) { selectedTrip ->
                bottomSheet.dismiss()
                handleTripSelection(selectedTrip, currentActive)
            }

            bottomSheet.show()
        }
    }

    // Handles user selection from trip menu
    private fun handleTripSelection(selectedTrip: Trip?, currentActiveTrip: Trip?) {
        activity.lifecycleScope.launch {
            if (selectedTrip == null) {
                // Selected "None" - deactivate current trip
                currentActiveTrip?.let { deactivateTrip(it) }
            } else if (selectedTrip.id.isEmpty()) {
                // Selected "New Trip" - deactivate current and show name dialog
                currentActiveTrip?.let { deactivateTrip(it) }
                showTripNameDialog(openCameraAfter = false)
            } else {
                // Selected existing trip - reactivate it
                if (currentActiveTrip != null && currentActiveTrip.id != selectedTrip.id) {
                    deactivateTrip(currentActiveTrip)
                }
                if (selectedTrip.id != currentActiveTrip?.id) {
                    tripRepository.reactivateTrip(selectedTrip.id)
                    activeTrip = selectedTrip.copy(active = true, endDate = "")
                    onTripStateChanged?.invoke(activeTrip)
                }
            }
        }
    }

    // Deactivates a trip, setting endDate to last photo's date
    private suspend fun deactivateTrip(trip: Trip) {
        val lastPhoto = photoRepository.getLastPhotoForTrip(trip.id)
        val endDate = lastPhoto?.date ?: trip.startDate
        tripRepository.deactivateTrip(trip.id, endDate)
        activeTrip = null
        onTripStateChanged?.invoke(null)
    }

    // Shows dialog to name a new trip
    fun showTripNameDialog(openCameraAfter: Boolean = false) {
        val input = EditText(activity).apply {
            hint = "Enter trip name"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(activity)
            .setTitle("Name Your Trip")
            .setView(input)
            .setPositiveButton("Start") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    activity.lifecycleScope.launch {
                        startNewTrip(name)
                        if (openCameraAfter) {
                            onOpenCamera?.invoke()
                        }
                    }
                } else {
                    Toast.makeText(activity, "Please enter trip name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Creates and saves a new trip
    private suspend fun startNewTrip(name: String) {
        val userId = authRepository.getCurrentUser()?.uid ?: return
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())

        val trip = Trip(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name,
            startDate = today,
            active = true,
            photoCount = 0
        )

        tripRepository.saveTrip(trip)
        activeTrip = trip
        onTripStateChanged?.invoke(activeTrip)
    }

    // Prompts user to start trip before taking photo
    fun promptStartTrip() {
        AlertDialog.Builder(activity)
            .setTitle("No Active Trip")
            .setMessage("Start a new trip to take photos?")
            .setPositiveButton("Yes") { _, _ ->
                showTripNameDialog(openCameraAfter = true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}