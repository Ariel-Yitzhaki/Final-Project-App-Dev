package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travel.R
import com.example.travel.models.User
import com.example.travel.utils.setDebouncedClickListener

// Adapter for displaying friends list
class FriendAdapter(
    private val friends: List<User>,
    private val onRemoveClick: (User) -> Unit,
    private val onFriendClick: (User) -> Unit
) : RecyclerView.Adapter<FriendAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profilePicture: ImageView = view.findViewById(R.id.profilePicture)
        val displayNameText: TextView = view.findViewById(R.id.displayNameText)
        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val actionButton: Button = view.findViewById(R.id.actionButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Load friend's profile picture
        if (friend.profilePictureUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(friend.profilePictureUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .into(holder.profilePicture)
        } else {
            holder.profilePicture.setImageResource(R.drawable.ic_profile)
        }
        val friend = friends[position]
        holder.displayNameText.text = friend.displayName
        holder.usernameText.text = "@${friend.username}"
        holder.actionButton.setDebouncedClickListener { onRemoveClick(friend) }
        holder.itemView.setOnClickListener { onFriendClick(friend) }
    }

    override fun getItemCount() = friends.size
}