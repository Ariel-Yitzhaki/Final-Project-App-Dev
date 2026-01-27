package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travel.R
import com.example.travel.models.Photo

// Adapter for displaying trip photos in a vertical scrollable list
class TripPhotoAdapter(
    private val photos: List<Photo>
) : RecyclerView.Adapter<TripPhotoAdapter.PhotoViewHolder>() {

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
            .load(photo.localPath)
            .centerCrop()
            .into(holder.photoImage)

        // Set image height to 4:5 aspect ratio based on screen width
        holder.photoImage.post {
            val width = holder.photoImage.width
            val height = (width * 1.25).toInt()
            holder.photoImage.layoutParams.height = height
            holder.photoImage.requestLayout()
        }

        // Shows coordinates for now, need to turn to geocoding later
        holder.locationText.text = "%.4f, %.4f".format(photo.latitude, photo.longitude)

        // Like button placeholder - need to add the functionality later
        holder.likeCountText.text = "0"
    }

    override fun getItemCount() = photos.size
}