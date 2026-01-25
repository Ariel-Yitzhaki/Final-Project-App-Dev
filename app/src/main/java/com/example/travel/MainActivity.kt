package com.example.travel

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ImageButton
import com.example.travel.data.PhotoRepository
import com.example.travel.data.AuthRepository

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var photoRepository: PhotoRepository
    private var photoUri: Uri? = null
    private var currentPhotoPath: String = ""
    private lateinit var authRepository: AuthRepository

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

        // Load map fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapFragment())
                .commit()
        }

        // FAB click listener
        findViewById<FloatingActionButton>(R.id.fab_add_picture).setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        // Profile button - opens gallery view
        findViewById<ImageButton>(R.id.nav_profile).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .commit()
        }

        // Friends button - opens friends list
        findViewById<ImageButton>(R.id.nav_friends).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FriendsFragment())
                .commit()
        }

        // Home button - opens map
        findViewById<ImageButton>(R.id.nav_home).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapFragment())
                .commit()
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
                }
                previewLauncher.launch(intent)
            }
        }
    }
}

