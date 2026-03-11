package com.example.travel.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travel.data.AuthRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import com.example.travel.models.Photo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Note for self: A sealed class defines all possible subclasses in one file,
// so the compiler ensures every state is handled in `when` blocks.
// Unlike enums, each subclass can hold different data (e.g. Error carries a message)
sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

class PhotoPreviewViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val photoRepository = PhotoRepository()
    private val tripRepository = TripRepository()

    private val _uploadState = MutableLiveData<UploadState>(UploadState.Idle)
    val uploadState: LiveData<UploadState> = _uploadState

    // Runs in viewModelScope so it survives activity recreation
    fun uploadPhoto(photoPath: String, latitude: Double, longitude: Double, tripId: String) {
        // Prevent duplicate uploads if already in progress
        if (_uploadState.value == UploadState.Uploading) return

        val userId = authRepository.getCurrentUser()?.uid ?: return
        _uploadState.value = UploadState.Uploading

        viewModelScope.launch {
            try {
                val photo = Photo(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    imageUrl = "",
                    latitude = latitude,
                    longitude = longitude,
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    timestamp = System.currentTimeMillis(),
                    tripId = tripId
                )

                photoRepository.savePhoto(photo, photoPath)
                if (tripId.isNotEmpty()) {
                    tripRepository.incrementPhotoCount(tripId)
                }
                _uploadState.value = UploadState.Success
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }
}