package com.example.travel

import android.graphics.BitmapFactory
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.travel.data.AuthRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.models.Photo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PhotoPreviewActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var photoRepository: PhotoRepository

    private lateinit var photoPreview: ImageView
    private lateinit var locationText: TextView
    private lateinit var uploadButton: Button
    private lateinit var retakeText: TextView
    private lateinit var progressBar: ProgressBar

    private var photoPath: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_preview)

        authRepository = AuthRepository()
        photoRepository = PhotoRepository()

        // Get data from intent
        photoPath = intent.getStringExtra("photoPath") ?: ""
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)

        // Bind views
        photoPreview = findViewById(R.id.photoPreview)
        locationText = findViewById(R.id.locationText)
        uploadButton = findViewById(R.id.uploadButton)
        retakeText = findViewById(R.id.retakeText)
        progressBar = findViewById(R.id.progressBar)

        // Display photo
        val bitmap = BitmapFactory.decodeFile(photoPath)
        photoPreview.setImageBitmap(bitmap)

        // Display location
        loadAddress()

        // Back button - discard and return
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            discardAndFinish()
        }

        // Upload button
        uploadButton.setOnClickListener {
            uploadPhoto()
        }

        // Retake - return to camera
        retakeText.setOnClickListener {
            setResult(RESULT_FIRST_USER)  // Signal to retake
            finish()
        }
    }

    // Convert coordinates to address
    @Suppress("DEPRECATION")
    private fun loadAddress() {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressText = listOfNotNull(
                    address.thoroughfare,
                    address.locality,
                    address.countryName
                ).joinToString(", ")
                locationText.text = addressText.ifEmpty { "$latitude, $longitude" }
            } else {
                locationText.text = "$latitude, $longitude"
            }
        } catch (_: Exception) {
            locationText.text = "$latitude, $longitude"
        }
    }

    private fun uploadPhoto() {
        val userId = authRepository.getCurrentUser()?.uid ?: return

        progressBar.visibility = View.VISIBLE
        uploadButton.isEnabled = false

        val photo = Photo(
            id = UUID.randomUUID().toString(),
            userId = userId,
            localPath = photoPath,
            latitude = latitude,
            longitude = longitude,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            timestamp = System.currentTimeMillis(),
            tripId = "default_trip"
        )

        lifecycleScope.launch {
            try {
                photoRepository.savePhoto(photo)
                Toast.makeText(this@PhotoPreviewActivity, "Photo uploaded!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } catch (_: Exception) {
                Toast.makeText(this@PhotoPreviewActivity, "Upload failed", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                uploadButton.isEnabled = true
            }
        }
    }

    private fun discardAndFinish() {
        // Delete the temp photo file
        try {
            java.io.File(photoPath).delete()
        } catch (_: Exception) {}
        setResult(RESULT_CANCELED)
        finish()
    }
}