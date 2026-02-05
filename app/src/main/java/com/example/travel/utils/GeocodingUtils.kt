package com.example.travel.utils

import android.content.Context
import android.location.Geocoder
import java.util.Locale

object GeocodingUtils {

    // Converts coordinates to a readable address string
    @Suppress("DEPRECATION")
    fun getAddressFromCoordinates(context: Context, latitude: Double, longitude: Double): String {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressText = listOfNotNull(
                    address.thoroughfare,
                    address.locality,
                    address.countryName
                ).joinToString(", ")
                if (addressText.isNotEmpty()) {
                    return addressText
                }
            }
        } catch (_: Exception) {}

        return "%.4f, %.4f".format(latitude, longitude)
    }
}