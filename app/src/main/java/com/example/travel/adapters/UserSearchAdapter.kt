package com.example.travel.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.R
import com.example.travel.models.User
import com.example.travel.utils.loadProfilePicture
import com.example.travel.utils.setDebouncedClickListener

// Adapter for user search results with dynamic button states
class UserSearchAdapter(
    private val onAddClick: (User) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

    private var users: List<User> = emptyList()
    private var statusMap: Map<String, String> = emptyMap() // Maps user ID to status: "friend", "pending", "none"

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profilePicture: ImageView = view.findViewById(R.id.profilePicture)
        val displayNameText: TextView = view.findViewById(R.id.displayNameText)
        val usernameText: TextView = view.findViewById(R.id.usernameText)
        val actionButton: Button = view.findViewById(R.id.actionButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_search, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        val context = holder.itemView.context

        holder.displayNameText.text = user.displayName
        holder.usernameText.text = context.getString(R.string.format_username, user.username)
        holder.profilePicture.loadProfilePicture(user.profilePictureUrl)

        // Set button state based on relationship status
        val status = statusMap[user.id]
        when (status) {
            "friend" -> {
                holder.actionButton.text = context.getString(R.string.label_friends)
                holder.actionButton.isEnabled = false
            }
            "pending" -> {
                holder.actionButton.text = context.getString(R.string.search_status_pending)
                holder.actionButton.isEnabled = false
            }
            else -> {
                holder.actionButton.text = context.getString(R.string.label_add)
                holder.actionButton.isEnabled = true
                holder.actionButton.setDebouncedClickListener { onAddClick(user) }
            }
        }
    }

    // Updates adapter data and refreshes the list
    fun updateData(newUsers: List<User>, newStatusMap: Map<String, String>) {
        users = newUsers
        statusMap = newStatusMap
        notifyDataSetChanged()
    }

    override fun getItemCount() = users.size
}