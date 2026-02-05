package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travel.R
import com.example.travel.data.LikeRepository
import com.example.travel.models.Photo
import kotlinx.coroutines.launch

// Adapter for displaying trip photos in a vertical scrollable list
class TripPhotoAdapter(
    private val photos: List<Photo>,
    private val addresses: Map<String, String>,
    private val currentUserId: String,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val likeRepository: LikeRepository
) : RecyclerView.Adapter<TripPhotoAdapter.PhotoViewHolder>() {

    private val likeStates = mutableMapOf<String, Boolean>()
    private val likeCounts = mutableMapOf<String, Int>()

    // ViewHolder for each photo item
    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoImage: ImageView = itemView.findViewById(R.id.photoImage)
        val likeButton: ImageButton = itemView.findViewById(R.id.likeButton)
        val likeCountText: TextView = itemView.findViewById(R.id.likeCountText)
        val locationText: TextView = itemView.findViewById(R.id.locationText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]

        // Load photo image
        Glide.with(holder.itemView.context)
            .load(photo.imageUrl)
            .centerCrop()
            .into(holder.photoImage)

        // Shows coordinates for now, need to turn to geocoding later
        // Shows geocoded address or coordinates as fallback
        holder.locationText.text = addresses[photo.id] ?: "%.4f, %.4f"
            .format(photo.latitude, photo.longitude)

        // Like button placeholder
        loadLikeState(holder, photo)
        holder.likeButton.setOnClickListener {
            toggleLike(holder, photo)
        }
    }

    // Loads like state from Firestore or cache
    private fun loadLikeState(holder: PhotoViewHolder, photo: Photo) {
        lifecycleScope.launch {
            val isLiked = likeStates[photo.id] ?: likeRepository.hasUserLiked(photo.id, currentUserId)
            val count = likeCounts[photo.id] ?: likeRepository.getLikeCount(photo.id)

            likeStates[photo.id] = isLiked
            likeCounts[photo.id] = count

            updateLikeUI(holder, isLiked, count)
        }
    }

    // Toggles like state and updates UI
    private fun toggleLike(holder: PhotoViewHolder, photo: Photo) {
        lifecycleScope.launch {
            val nowLiked = likeRepository.toggleLike(photo.id, currentUserId)
            val currentCount = likeCounts[photo.id] ?: 0
            val newCount = if (nowLiked) currentCount + 1 else currentCount - 1

            likeStates[photo.id] = nowLiked
            likeCounts[photo.id] = newCount

            updateLikeUI(holder, nowLiked, newCount)
        }
    }

    // Updates the like button icon and count text
    private fun updateLikeUI(holder: PhotoViewHolder, isLiked: Boolean, count: Int) {
        holder.likeButton.setImageResource(
            if (isLiked) R.drawable.ic_liked else R.drawable.ic_unliked
        )
        holder.likeCountText.text = count.toString()
    }

    override fun getItemCount() = photos.size
}