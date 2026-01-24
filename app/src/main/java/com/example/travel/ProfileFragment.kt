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

class ProfileFragment : Fragment() {

    private lateinit var repository: FirebaseRepository
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

        repository = FirebaseRepository()
        recyclerView = view.findViewById(R.id.photos_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)  // 3 columns

        loadPhotos()
    }

    // Fetches all photos from Firestore and displays them in the grid
    private fun loadPhotos() {
        lifecycleScope.launch {
            val photos = repository.getAllPhotos()
            recyclerView.adapter = PhotoAdapter(photos)
        }
    }
}