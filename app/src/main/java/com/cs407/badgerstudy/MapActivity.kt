package com.cs407.badgerstudy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.FirebaseDatabase
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
    private lateinit var placesClient: PlacesClient
    private var currentMarker: Marker? = null
    private var currentPolyline: Polyline? = null
    private var userLocation: LatLng? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val MAPS_API_KEY = "AIzaSyDKkP9EHuCGUKAP3f5X4U5syYcXbzDbbho" // Replace with your actual API key
        private const val TAG = "MapActivity"
    }

    private fun fetchStudyLocations(onSuccess: (List<Map<String, String>>) -> Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference("Study locations")
        dbRef.get().addOnSuccessListener { snapshot ->
            val locations = snapshot.children.mapNotNull { it.value as? Map<String, String> }
            onSuccess(locations)
        }.addOnFailureListener {
            Log.e(TAG, "Error fetching study locations: ${it.message}")
            Toast.makeText(this, "Failed to load study locations.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun displayStudyLocationsOnMap(locations: List<Map<String, String>>) {
        locations.forEach { location ->
            val coordinates = location["Coordinates"]?.split(", ") ?: return@forEach
            val latitude = coordinates[0].toDouble()
            val longitude = coordinates[1].toDouble()

            val name = location["Name of spot"] ?: "Unknown Spot"

            // Create and add the marker with the custom icon
            val markerOptions = MarkerOptions()
                .position(LatLng(latitude, longitude))
                .title(name)
                .snippet("Environment: ${location["Environment"]}")
                .icon(createCustomMarker())

            mMap.addMarker(markerOptions)
        }
    }


    private fun createCustomMarker(): BitmapDescriptor {
        // Load the image from the drawable resource and scale it if needed
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.study_mark)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
    }



    private fun fetchAndShowStudyLocations() {
        fetchStudyLocations { locations ->
            displayStudyLocationsOnMap(locations)
        }
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
        setupBottomNavigation()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMapToolbarEnabled = true
        }
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        panToCurrentLocation()
        fetchAndShowStudyLocations()

        // Handle POI clicks
        mMap.setOnPoiClickListener { poi ->
            Log.d(TAG, "POI clicked: ${poi.name}")
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
                    Log.d(TAG, "Place selected: ${place.name}")
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))
                    showPlaceInfo(place)
                    fetchAndDrawRoute(userLocation, it, "driving")
                    moveMarker(it, place.name)
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Log.e(TAG, "Error selecting place: ${status.statusMessage}")
                Toast.makeText(this@MapActivity, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home, do nothing
                    true
                }
                R.id.nav_favorites -> {
                    // Navigate to Favorites
                    val intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_settings -> {
                    // Navigate to Settings
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchPlaceDetails(placeId: String, latLng: LatLng?, name: String?) {
        if (placeId.isEmpty()) {
            showFallbackPlaceInfo(latLng, name)
            return
        }

        val request = FetchPlaceRequest.newInstance(
            placeId,
            listOf(
                Place.Field.NAME, Place.Field.LAT_LNG,
                Place.Field.RATING, Place.Field.PHOTO_METADATAS,
                Place.Field.USER_RATINGS_TOTAL, Place.Field.OPENING_HOURS
            )
        )

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            Log.d(TAG, "Place details fetched: ${place.name}")
            showPlaceInfo(place)
            latLng?.let { moveMarker(it, place.name) }
        }.addOnFailureListener {
            Log.e(TAG, "Error fetching place details: ${it.message}")
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

        findViewById<TextView>(R.id.poi_rating).text = "Rating: ${place.rating ?: "N/A"}"
        findViewById<TextView>(R.id.poi_user_ratings).text = "User Reviews: ${place.userRatingsTotal ?: 0}"

        // Get the current day of the week
        val calendar = java.util.Calendar.getInstance()
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)

        // Adjust the index to map Sunday (1) -> 6 and Monday (2) -> 0
        val adjustedIndex = if (dayOfWeek == 1) 6 else dayOfWeek - 2

        val currentDayHours = place.openingHours?.weekdayText?.getOrNull(adjustedIndex)

        findViewById<TextView>(R.id.poi_hours).text =
            currentDayHours ?: "Opening Hours: Not Available"

        val photoImageView = findViewById<ImageView>(R.id.poi_photo)
        if (!place.photoMetadatas.isNullOrEmpty()) {
            val photoRequest = FetchPhotoRequest.builder(place.photoMetadatas!![0]).build()
            placesClient.fetchPhoto(photoRequest)
                .addOnSuccessListener { response ->
                    photoImageView.setImageBitmap(response.bitmap)
                }
                .addOnFailureListener {
                    Log.e(TAG, "Error fetching photo: ${it.message}")
                    photoImageView.setImageResource(R.drawable.placeholder_image)
                }
        } else {
            photoImageView.setImageResource(R.drawable.placeholder_image)
        }
    }


    private fun moveMarker(latLng: LatLng, title: String?) {
        currentMarker?.remove()
        currentMarker = mMap.addMarker(
            MarkerOptions().position(latLng).title(title)
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
                    Log.e(TAG, "Error fetching route: ${e.message}")
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
                    PolylineOptions().addAll(decodedPath).color(0xFF0000FF.toInt()).width(10f)
                )
            } else {
                Toast.makeText(this, "No routes found!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing route: ${e.message}")
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
