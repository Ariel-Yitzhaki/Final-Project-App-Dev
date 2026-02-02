package com.example.travel.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Handles camera operations and photo capture flow
class CameraManager(
    private val activity: AppCompatActivity,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val getActiveTrip: () -> String?  // Returns active trip ID
) {
    private var photoUri: Uri? = null
    private var currentPhotoPath: String = ""

    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var previewLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>

    // Callback for when photo is uploaded successfully
    var onPhotoUploaded: (() -> Unit)? = null

    // Must be called in Activity's onCreate before setContentView
    fun registerLaunchers() {
        // Launches camera and handles result
        takePictureLauncher = activity.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                openPhotoPreview()
            }
        }

        // Launches preview screen and handles result
        previewLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            when (result.resultCode) {
                AppCompatActivity.RESULT_OK -> {
                    onPhotoUploaded?.invoke()
                }
                AppCompatActivity.RESULT_FIRST_USER -> {
                    openCamera()
                }
            }
        }

        // Requests camera permission
        cameraPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                openCamera()
            }
        }
    }

    // Checks permission and opens camera if granted
    fun checkPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Creates temp file and launches camera
    private fun openCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(photoUri)
    }

    // Creates temp image file with timestamp name
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PHOTO_${timestamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    // Gets location and opens preview activity
    private fun openPhotoPreview() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val intent = Intent(activity, PhotoPreviewActivity::class.java).apply {
                    putExtra("photoPath", currentPhotoPath)
                    putExtra("latitude", location?.latitude ?: 0.0)
                    putExtra("longitude", location?.longitude ?: 0.0)
                    putExtra("tripId", getActiveTrip() ?: "")
                }
                previewLauncher.launch(intent)
            }
        }
    }
}