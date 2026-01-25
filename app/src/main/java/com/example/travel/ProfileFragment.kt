package com.example.travel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.example.travel.data.PhotoRepository
import com.example.travel.data.AuthRepository
import android.content.Intent
import android.widget.Button

class ProfileFragment : Fragment() {

    private lateinit var photoRepository: PhotoRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    // Called after view is created - sets up RecyclerView and loads photos
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoRepository = PhotoRepository()
        authRepository = AuthRepository()

        view.findViewById<Button>(R.id.signOutButton).setOnClickListener {
            authRepository.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        recyclerView = view.findViewById(R.id.photos_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)  // 3 columns

        loadPhotos()
    }

    // Fetches all photos from Firestore of the user and displays them in the grid
    private fun loadPhotos() {
        val userId = authRepository.getCurrentUser()?.uid ?: return

        lifecycleScope.launch {
            val photos = photoRepository.getPhotosForUser(userId)
            recyclerView.adapter = PhotoAdapter(photos)
        }
    }
}