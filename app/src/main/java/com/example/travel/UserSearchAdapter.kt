package com.example.travel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travel.models.User

// Adapter for user search results with dynamic button states
class UserSearchAdapter(
    private val users: List<User>,
    private val statusMap: Map<String, String>,  // Maps user ID to status: "friend", "pending", "none"
    private val onAddClick: (User) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        holder.displayNameText.text = user.displayName
        holder.usernameText.text = "@${user.username}"

        // Set button state based on relationship status
        when (statusMap[user.id]) {
            "friend" -> {
                holder.actionButton.text = "Friends"
                holder.actionButton.isEnabled = false
            }
            "pending" -> {
                holder.actionButton.text = "Pending"
                holder.actionButton.isEnabled = false
            }
            else -> {
                holder.actionButton.text = "Add"
                holder.actionButton.isEnabled = true
                holder.actionButton.setOnClickListener { onAddClick(user) }
            }
        }
    }

    override fun getItemCount() = users.size
}