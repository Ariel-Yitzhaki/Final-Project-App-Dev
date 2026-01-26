package com.example.travel.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.travel.fragments.FriendsFragment
import com.example.travel.fragments.MapFragment
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
import androidx.fragment.app.Fragment
import com.example.travel.fragments.ProfileFragment
import com.example.travel.fragments.Refresh

class MainActivity : AppCompatActivity() {

    private lateinit var photoRepository: PhotoRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var tripRepository: TripRepository
    private lateinit var tripButton: Button
    private var activeTrip: Trip? = null
    private var photoUri: Uri? = null
    private var currentFragmentTag: String? = null
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
                showTripNameDialog()
            }
        }

        // Load map fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapFragment(), "map")
                .commit()
            currentFragmentTag = "map"
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
            switchToFragment(ProfileFragment(), "profile")
            fab.hide()
        }

        // Friends button - opens friends list
        findViewById<ImageButton>(R.id.nav_friends).setOnClickListener {
            switchToFragment(FriendsFragment(), "friends")
            fab.hide()
        }

        // Home button - opens map
        findViewById<ImageButton>(R.id.nav_home).setOnClickListener {
            switchToFragment(MapFragment(), "map")
            fab.show()
        }
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
        // Using HHmmss format to track order of images in a trip done on the same date
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

    private fun switchToFragment(fragment: Fragment, tag: String) {
        if (currentFragmentTag == tag) {
            // Already on this fragment - refresh the existing fragment
            val existingFragment = supportFragmentManager.findFragmentByTag(tag)
            (existingFragment as? Refresh)?.refresh()
        } else {
            // Switch to new fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit()
            currentFragmentTag = tag
        }
        lifecycleScope.launch { checkActiveTrip() }
    }

    // Start a new trip
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
        updateTripButtonUI()
    }

    private fun showTripNameDialog() {
        val input = EditText(this).apply {
            hint = "Enter trip name"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Name Your Trip")
            .setView(input)
            .setPositiveButton("Start") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        startNewTrip(name)
                    }
                } else {
                    Toast.makeText(this, "Please enter trip name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Prompt user to start trip before taking photo
    private fun promptStartTrip() {
        val input = EditText(this).apply {
            hint = "Enter trip name"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("No Active Trip")
            .setMessage("Start a new trip to take photos?")
            .setPositiveButton("Yes") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch {
                        startNewTrip(name)
                        checkCameraPermissionAndOpen()
                    }
                } else {
                    Toast.makeText(this, "Please enter trip name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}