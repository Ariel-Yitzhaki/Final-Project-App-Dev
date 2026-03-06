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
    private val photos: MutableList<Photo>,
    private val addresses: MutableMap<String, String>,
    private val currentUserId: String,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val likeRepository: LikeRepository,
    private val isOwner: Boolean,
    private val onDeletePhoto: (Photo) -> Unit
) : RecyclerView.Adapter<TripPhotoAdapter.PhotoViewHolder>() {

    private val likeStates = mutableMapOf<String, Boolean>()
    private val likeCounts = mutableMapOf<String, Int>()

    // Tracks photos with a like toggle in progress to prevent double-taps
    private val likePending = mutableSetOf<String>()

    // ViewHolder for each photo item
    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoImage: ImageView = itemView.findViewById(R.id.photoImage)
        val likeButton: ImageButton = itemView.findViewById(R.id.likeButton)
        val likeCountText: TextView = itemView.findViewById(R.id.likeCountText)
        val locationText: TextView = itemView.findViewById(R.id.locationText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val menuButton: ImageButton = itemView.findViewById(R.id.menuButton)
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

        // Shows geocoded address or coordinates as fallback
        holder.locationText.text = addresses[photo.id] ?: "%.4f, %.4f"
            .format(photo.latitude, photo.longitude)

        holder.dateText.text = photo.date

        // Like button placeholder
        loadLikeState(holder, photo)

        holder.likeButton.setOnClickListener {
            toggleLike(holder, photo)
        }

        // Delete button
        if (isOwner) {
            holder.menuButton.visibility = View.VISIBLE
            holder.menuButton.setOnClickListener {
                onDeletePhoto(photo)
            }
        } else {
            holder.menuButton.visibility = View.GONE
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

    // Toggles like state and updates UI (ignores taps while pending)
    private fun toggleLike(holder: PhotoViewHolder, photo: Photo) {
        if (photo.id in likePending) return
        likePending.add(photo.id)

        lifecycleScope.launch {
            try{
                val nowLiked = likeRepository.toggleLike(photo.id, currentUserId)
                val currentCount = likeCounts[photo.id] ?: 0
                val newCount = if (nowLiked) currentCount + 1 else currentCount - 1

                likeStates[photo.id] = nowLiked
                likeCounts[photo.id] = newCount

                updateLikeUI(holder, nowLiked, newCount)
            } finally {
                likePending.remove(photo.id)
            }
        }
    }

    // Updates the like button icon and count text
    private fun updateLikeUI(holder: PhotoViewHolder, isLiked: Boolean, count: Int) {
        holder.likeButton.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
        )
        holder.likeCountText.text = count.toString()
    }

    // Updates photos list and refreshes display
    fun updatePhotos(newPhotos: List<Photo>, newAddresses: Map<String, String>) {
        photos.clear()
        photos.addAll(newPhotos)
        addresses.clear()
        addresses.putAll(newAddresses)
        likeStates.clear()
        likeCounts.clear()
        likePending.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount() = photos.size
}