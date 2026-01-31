package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.models.User

// Adapter for displaying friends list
class FriendAdapter(
    private val friends: List<User>,
    private val onRemoveClick: (User) -> Unit,
    private val onFriendClick: (User) -> Unit
) : RecyclerView.Adapter<FriendAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        val friend = friends[position]
        holder.displayNameText.text = friend.displayName
        holder.usernameText.text = "@${friend.username}"
        holder.actionButton.setOnClickListener { onRemoveClick(friend) }
        holder.itemView.setOnClickListener { onFriendClick(friend) }
    }

    override fun getItemCount() = friends.size
}