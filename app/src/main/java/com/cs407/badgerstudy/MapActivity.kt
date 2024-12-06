package com.cs407.badgerstudy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var poiInfoBlock: View
    private lateinit var poiNameTextView: TextView
    private lateinit var placesClient: com.google.android.libraries.places.api.net.PlacesClient
    private var currentMarker: Marker? = null
    private var currentPolyline: Polyline? = null
    private var userLocation: LatLng? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val MAPS_API_KEY = "AIzaSyDKkP9EHuCGUKAP3f5X4U5syYcXbzDbbho" // Replace with your actual API key
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, MAPS_API_KEY)
        }

        placesClient = Places.createClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        poiInfoBlock = findViewById(R.id.poi_info_scroll)
        poiNameTextView = findViewById(R.id.poi_name)
        poiInfoBlock.visibility = View.GONE

        setupSearchAutocomplete()

        // Handle navigation actions
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Stay on this activity
                    true
                }
                R.id.nav_favorites -> {
                    // Handle navigation to favorites
                    val intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_settings -> {
                    // Navigate to SettingsActivity
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMapToolbarEnabled = true
        }
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        panToCurrentLocation()

        mMap.setOnPoiClickListener { poi ->
            fetchPlaceDetails(poi.placeId ?: "", poi.latLng, poi.name)
            fetchAndDrawRoute(userLocation, poi.latLng, "driving")
        }
    }

    private fun setupSearchAutocomplete() {
        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                Place.Field.RATING, Place.Field.PHOTO_METADATAS, Place.Field.USER_RATINGS_TOTAL,
                Place.Field.OPENING_HOURS
            )
        )

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                    showPlaceInfo(place)
                    fetchAndDrawRoute(userLocation, it, "driving")
                    moveMarker(it, place.name)
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(this@MapActivity, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchPlaceDetails(placeId: String, latLng: LatLng?, name: String?) {
        val request = com.google.android.libraries.places.api.net.FetchPlaceRequest.newInstance(
            placeId,
            listOf(
                Place.Field.NAME, Place.Field.LAT_LNG,
                Place.Field.RATING, Place.Field.PHOTO_METADATAS,
                Place.Field.USER_RATINGS_TOTAL, Place.Field.OPENING_HOURS
            )
        )

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            showPlaceInfo(place)
            latLng?.let { moveMarker(it, place.name) }
        }.addOnFailureListener {
            showFallbackPlaceInfo(latLng, name)
        }
    }

    private fun showFallbackPlaceInfo(latLng: LatLng?, name: String?) {
        poiInfoBlock.visibility = View.VISIBLE
        poiNameTextView.text = name ?: "Unknown Location"
        findViewById<TextView>(R.id.poi_rating).text = "Rating: N/A"
        findViewById<TextView>(R.id.poi_user_ratings).text = "User Reviews: 0"
        findViewById<TextView>(R.id.poi_hours).text = "Opening Hours: Not Available"
        findViewById<ImageView>(R.id.poi_photo).setImageResource(R.drawable.placeholder_image)

        latLng?.let { moveMarker(it, name) }
    }

    private fun showPlaceInfo(place: Place) {
        poiInfoBlock.visibility = View.VISIBLE
        poiNameTextView.text = place.name ?: "Unknown Location"

        val ratingTextView = findViewById<TextView>(R.id.poi_rating)
        ratingTextView.text = "Rating: ${place.rating?.toString() ?: "N/A"}"

        val userRatingsTextView = findViewById<TextView>(R.id.poi_user_ratings)
        userRatingsTextView.text = "User Reviews: ${place.userRatingsTotal ?: 0}"

        val hoursTextView = findViewById<TextView>(R.id.poi_hours)
        hoursTextView.text = if (place.openingHours != null) {
            "Opening Hours:\n${place.openingHours?.weekdayText?.joinToString("\n")}"
        } else {
            "Opening Hours: Not Available"
        }

        val photoImageView = findViewById<ImageView>(R.id.poi_photo)
        if (!place.photoMetadatas.isNullOrEmpty()) {
            val photoRequest = FetchPhotoRequest.builder(place.photoMetadatas!![0])
                .setMaxWidth(500)
                .setMaxHeight(500)
                .build()

            placesClient.fetchPhoto(photoRequest)
                .addOnSuccessListener { response ->
                    photoImageView.setImageBitmap(response.bitmap)
                }
                .addOnFailureListener {
                    photoImageView.setImageResource(R.drawable.placeholder_image)
                }
        } else {
            photoImageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    private fun moveMarker(latLng: LatLng, title: String?) {
        currentMarker?.remove()
        currentMarker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
        )
        currentMarker?.showInfoWindow()
    }

    private fun fetchAndDrawRoute(origin: LatLng?, destination: LatLng, travelMode: String) {
        if (origin == null) {
            Toast.makeText(this, "Current location not found", Toast.LENGTH_SHORT).show()
            return
        }

        val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&mode=$travelMode" +
                "&key=$MAPS_API_KEY"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.connect()
                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()
                withContext(Dispatchers.Main) {
                    parseAndDrawRoute(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapActivity, "Error fetching route: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun parseAndDrawRoute(jsonData: String) {
        try {
            val jsonObject = JSONObject(jsonData)
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val polyline = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")
                val decodedPath = decodePolyline(polyline)

                currentPolyline?.remove()
                currentPolyline = mMap.addPolyline(
                    PolylineOptions()
                        .addAll(decodedPath)
                        .color(0xFF0000FF.toInt())
                        .width(10f)
                )
            } else {
                Toast.makeText(this, "No routes found!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error parsing route: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat / 1E5, lng / 1E5)
            poly.add(p)
        }
        return poly
    }

    private fun panToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        mMap.isMyLocationEnabled = true
        val fusedLocationProviderClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                userLocation = currentLatLng
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                mMap.addCircle(
                    CircleOptions()
                        .center(currentLatLng)
                        .radius(80.0)
                        .strokeColor(0xFF0000FF.toInt())
                        .fillColor(0x220000FF)
                        .strokeWidth(4f)
                )
            } ?: run {
                Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
