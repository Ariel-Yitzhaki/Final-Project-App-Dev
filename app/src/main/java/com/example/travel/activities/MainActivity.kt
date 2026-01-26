package com.example.travel.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.travel.fragments.FriendsFragment
import com.example.travel.fragments.MapFragment
import com.example.travel.activities.PhotoPreviewActivity
import com.example.travel.fragments.ProfileFragment
import com.example.travel.R
import com.example.travel.data.AuthRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import com.example.travel.models.Trip
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var photoRepository: PhotoRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var tripButton: Button
    private var activeTrip: Trip? = null
    private var photoUri: Uri? = null
    private var currentPhotoPath: String = ""
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var fab: FloatingActionButton

    // Launches camera and handles result
    // Launches camera and handles result
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            openPhotoPreview()
        }
    }

    // Launches preview screen and handles result
    private val previewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                // Photo uploaded, refresh map
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, MapFragment())
                    .commit()
            }
            RESULT_FIRST_USER -> {
                // Retake requested
                openCamera()
            }
            // RESULT_CANCELED - do nothing, photo discarded
        }
    }

    // Requests camera permission
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        photoRepository = PhotoRepository()
        authRepository = AuthRepository()
        tripRepository = TripRepository()
        tripButton = findViewById(R.id.tripButton)

        // Check for active trip on startup
        lifecycleScope.launch {
            checkActiveTrip()
        }

// Trip button click
        tripButton.setOnClickListener {
            if (activeTrip == null) {
                startNewTrip()
            }
        }

        // Load map fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapFragment())
                .commit()
        }

        // FAB click listener
        fab = findViewById(R.id.fab_add_picture)
        fab.setOnClickListener {
            if (activeTrip == null) {
                promptStartTrip()
            } else {
                checkCameraPermissionAndOpen()
            }
        }

        // Profile button - opens gallery view
        findViewById<ImageButton>(R.id.nav_profile).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .commit()
            fab.show()
        }

        // Friends button - opens friends list
        findViewById<ImageButton>(R.id.nav_friends).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FriendsFragment())
                .commit()
            fab.hide()        }

        // Home button - opens map
        findViewById<ImageButton>(R.id.nav_home).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapFragment())
                .commit()
            fab.hide()        }
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(photoUri)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PHOTO_${timestamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun openPhotoPreview() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val intent = Intent(this, PhotoPreviewActivity::class.java).apply {
                    putExtra("photoPath", currentPhotoPath)
                    putExtra("latitude", location?.latitude ?: 0.0)
                    putExtra("longitude", location?.longitude ?: 0.0)
                    putExtra("tripId", activeTrip?.id ?: "")
                }
                previewLauncher.launch(intent)
            }
        }
    }

    // Check if user has an active trip and update UI
    private suspend fun checkActiveTrip() {
        val userId = authRepository.getCurrentUser()?.uid ?: return
        activeTrip = tripRepository.getActiveTrip(userId)
        updateTripButtonUI()
    }

    // Update button text based on trip state
    private fun updateTripButtonUI() {
        if (activeTrip != null) {
            tripButton.text = "TRAVELING"
            tripButton.isEnabled = false
        } else {
            tripButton.text = "Start Trip"
            tripButton.isEnabled = true
        }
    }

    // Start a new trip
    private fun startNewTrip() {
        val userId = authRepository.getCurrentUser()?.uid ?: return
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())

        val trip = Trip(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = "Trip - $today",
            startDate = today,
            isActive = true,
            photoCount = 0
        )

        lifecycleScope.launch {
            tripRepository.saveTrip(trip)
            activeTrip = trip
            updateTripButtonUI()
        }
    }

    // Prompt user to start trip before taking photo
    private fun promptStartTrip() {
        AlertDialog.Builder(this)
            .setTitle("No Active Trip")
            .setMessage("Start a new trip to take photos?")
            .setPositiveButton("Yes") { _, _ ->
                startNewTrip()
                checkCameraPermissionAndOpen()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}