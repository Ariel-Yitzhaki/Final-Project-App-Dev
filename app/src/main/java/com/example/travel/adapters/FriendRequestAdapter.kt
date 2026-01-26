package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.models.FriendRequest
import com.example.travel.models.User

// Adapter for displaying pending friend requests
class FriendRequestAdapter(
    private val requests: List<Pair<FriendRequest, User>>,
    private val onAcceptClick: (FriendRequest) -> Unit,
    private val onDeclineClick: (FriendRequest) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val displayNameText: TextView = view.findViewById(R.id.displayNameText)
        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val acceptButton: Button = view.findViewById(R.id.acceptButton)
        val declineButton: Button = view.findViewById(R.id.declineButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (request, sender) = requests[position]
        holder.displayNameText.text = sender.displayName
        holder.usernameText.text = "@${sender.username}"
        holder.acceptButton.setOnClickListener { onAcceptClick(request) }
        holder.declineButton.setOnClickListener { onDeclineClick(request) }
    }

    override fun getItemCount() = requests.size
}