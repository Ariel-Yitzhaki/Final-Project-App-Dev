package com.example.travel.activities

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.travel.R
import com.example.travel.data.AuthRepository
import com.example.travel.data.PhotoRepository
import com.example.travel.data.TripRepository
import com.example.travel.fragments.FriendsFragment
import com.example.travel.fragments.HomeFeedFragment
import com.example.travel.fragments.MapFragment
import com.example.travel.fragments.ProfileFragment
import com.example.travel.interfaces.Refresh
import com.example.travel.interfaces.TripEndListener
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), TripEndListener {

    private lateinit var tripButton: Button
    private lateinit var fab: FloatingActionButton
    private lateinit var tripManager: TripManager
    private lateinit var cameraManager: CameraManager
    private var currentFragmentTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers before setContentView (for registerLaunchers)
        initializeManagers()

        setContentView(R.layout.activity_main)

        tripButton = findViewById(R.id.tripButton)
        fab = findViewById(R.id.fab_add_picture)

        setupTripButton()
        setupFab()
        setupNavigation()

        // Load home fragment on startup
        if (savedInstanceState == null) {
            switchToFragment(HomeFeedFragment(), "home")
        }

        // Check for active trip
        lifecycleScope.launch {
            tripManager.checkActiveTrip()
        }
    }

    // Initializes TripManager and CameraManager with callbacks
    private fun initializeManagers() {
        val authRepository = AuthRepository()
        val tripRepository = TripRepository()
        val photoRepository = PhotoRepository()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tripManager = TripManager(this, authRepository, tripRepository, photoRepository)
        tripManager.onTripStateChanged = { trip -> updateTripButtonUI(trip) }
        tripManager.onOpenCamera = { cameraManager.checkPermissionAndOpen() }

        cameraManager = CameraManager(this, fusedLocationClient) { tripManager.activeTrip?.id }
        cameraManager.registerLaunchers()
        cameraManager.onPhotoUploaded = {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapFragment(), "map")
                .commit()
            currentFragmentTag = "map"
            updateNavigationIconColors("map")
        }
    }

    // Sets up trip button click listener
    private fun setupTripButton() {
        tripButton.setOnClickListener {
            tripManager.showTripMenu()
        }
    }

    // Sets up FAB click listener
    private fun setupFab() {
        fab.hide()
        fab.setOnClickListener {
            if (tripManager.activeTrip == null) {
                tripManager.promptStartTrip()
            } else {
                cameraManager.checkPermissionAndOpen()
            }
        }
    }

    // Sets up bottom navigation button listeners
    private fun setupNavigation() {
        findViewById<ImageButton>(R.id.nav_home).setOnClickListener {
            switchToFragment(HomeFeedFragment(), "home")
            fab.hide()
        }

        findViewById<ImageButton>(R.id.nav_friends).setOnClickListener {
            switchToFragment(FriendsFragment(), "friends")
            fab.hide()
        }

        findViewById<ImageButton>(R.id.nav_map).setOnClickListener {
            switchToFragment(MapFragment(), "map")
            fab.show()
        }

        findViewById<ImageButton>(R.id.nav_profile).setOnClickListener {
            switchToFragment(ProfileFragment(), "profile")
            fab.hide()
        }
    }

    // Switches to a fragment or refreshes if already showing
    private fun switchToFragment(fragment: Fragment, tag: String) {
        clearBackStack()

        if (currentFragmentTag == tag) {
            val existingFragment = supportFragmentManager.findFragmentByTag(tag)
            (existingFragment as? Refresh)?.refresh()
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit()
            currentFragmentTag = tag
        }

        // Only show trip button on map screen
        tripButton.visibility = if (tag == "map") View.VISIBLE else View.GONE

        lifecycleScope.launch { tripManager.checkActiveTrip() }
        updateNavigationIconColors(tag)
    }

    // Clears all fragments from back stack
    private fun clearBackStack() {
        repeat(supportFragmentManager.backStackEntryCount) {
            supportFragmentManager.popBackStack()
        }
    }

    // Updates navigation icon colors based on selected tab
    private fun updateNavigationIconColors(selectedTag: String) {
        val blackColor = ContextCompat.getColor(this, R.color.black)
        val whiteColor = ContextCompat.getColor(this, R.color.white)

        findViewById<ImageButton>(R.id.nav_home).setColorFilter(
            if (selectedTag == "home") whiteColor else blackColor
        )
        findViewById<ImageButton>(R.id.nav_friends).setColorFilter(
            if (selectedTag == "friends") whiteColor else blackColor
        )
        findViewById<ImageButton>(R.id.nav_map).setColorFilter(
            if (selectedTag == "map") whiteColor else blackColor
        )
        findViewById<ImageButton>(R.id.nav_profile).setColorFilter(
            if (selectedTag == "profile") whiteColor else blackColor
        )
    }

    // Updates trip button text based on trip state
    private fun updateTripButtonUI(trip: com.example.travel.models.Trip?) {
        if (trip != null) {
            tripButton.text = trip.name
            tripButton.isEnabled = true
        } else {
            tripButton.text = "Start Trip"
            tripButton.isEnabled = true
        }
    }

    // Called by ProfileFragment when user ends a trip
    override fun onTripEnded() {
        tripManager.clearActiveTrip()
    }
}