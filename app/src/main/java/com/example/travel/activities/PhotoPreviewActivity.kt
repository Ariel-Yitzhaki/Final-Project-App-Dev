package com.example.travel.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.travel.R
import com.example.travel.utils.GeocodingUtils
import com.example.travel.viewmodels.UploadState
import com.example.travel.viewmodels.PhotoPreviewViewModel
import java.io.File

// Loads the screen to display photo and location after taking a picture
class PhotoPreviewActivity : AppCompatActivity() {

    // Survives activity recreation
    private val viewModel: PhotoPreviewViewModel by viewModels()

    private lateinit var photoPreview: ImageView
    private lateinit var locationText: TextView
    private lateinit var uploadButton: Button
    private lateinit var retakeText: TextView
    private lateinit var progressBar: ProgressBar

    private var photoPath: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var tripId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_preview)

        // Handle device back button press
        onBackPressedDispatcher.addCallback(this) {
            discardAndFinish()
        }

        // Get data from intent
        tripId = intent.getStringExtra("tripId") ?: ""
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
        Glide.with(this)
            .load(photoPath)
            .into(photoPreview)

        // Display location
        loadAddress()

        // Back button - discard and return
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            discardAndFinish()
        }

        // Upload button
        uploadButton.setOnClickListener {
            viewModel.uploadPhoto(photoPath, latitude, longitude, tripId)
        }

        // Retake - return to camera
        retakeText.setOnClickListener {
            setResult(RESULT_FIRST_USER)  // Signal to retake
            finish()
        }

        // Observe upload state - re-attaches after recreation
        viewModel.uploadState.observe(this) { state ->
            when (state) {
                is UploadState.Idle -> {
                    progressBar.visibility = View.GONE
                    uploadButton.isEnabled = true
                }

                is UploadState.Uploading -> {
                    progressBar.visibility = View.VISIBLE
                    uploadButton.isEnabled = false
                }

                is UploadState.Success -> {
                    Toast.makeText(this, getString(R.string.toast_photo_uploaded), Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }

                is UploadState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    uploadButton.isEnabled = true
                }
            }
        }
    }

    // Convert coordinates to address
    private fun loadAddress() {
        locationText.text = GeocodingUtils.getAddressFromCoordinates(this, latitude, longitude)
    }

    private fun discardAndFinish() {
        // Delete the temp photo file
        try {
            File(photoPath).delete()
        } catch (_: Exception) {}
        setResult(RESULT_CANCELED)
        finish()
    }
}