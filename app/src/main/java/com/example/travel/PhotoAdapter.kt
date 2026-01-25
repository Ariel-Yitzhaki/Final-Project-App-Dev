package com.example.travel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.models.Photo
import com.bumptech.glide.Glide

// Adapter that binds photo data to RecyclerView grid items
class PhotoAdapter(private val photos: List<Photo>) :
    RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    // Holds reference to each grid item's views
    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.photo_image)
    }

    // Creates new view holder when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    // Binds photo data to view holder - loads image from local path
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]
        Glide.with(holder.itemView.context)
            .load(photo.localPath)
            .centerCrop()
            .into(holder.imageView)
    }

    // Returns total number of photos
    override fun getItemCount() = photos.size
}