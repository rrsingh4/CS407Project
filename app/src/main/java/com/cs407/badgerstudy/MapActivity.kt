package com.cs407.badgerstudy

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.google.firebase.auth.FirebaseAuth

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var poiInfoBlock: View
    private lateinit var poiNameTextView: TextView
    private lateinit var placesClient: PlacesClient
    private var currentMarker: Marker? = null
    private var currentPolyline: Polyline? = null
    private var userLocation: LatLng? = null
    private var favorites : List<Map<String, Any>>? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val MAPS_API_KEY = "AIzaSyDKkP9EHuCGUKAP3f5X4U5syYcXbzDbbho" // Replace with your actual API key
        private const val TAG = "MapActivity"
    }
    private fun fetchUserFavorites(onSuccess: (List<Map<String, Any>>) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val dbRef = FirebaseDatabase.getInstance().getReference("users/$userId/favorites")
        dbRef.get().addOnSuccessListener { snapshot ->
            val favorites = snapshot.children.mapNotNull { it.value as? Map<String, Any> }
            onSuccess(favorites)
        }.addOnFailureListener {
            Log.e(TAG, "Error fetching favorites: ${it.message}")
            Toast.makeText(this, "Failed to load favorites.", Toast.LENGTH_SHORT).show()
        }
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


    private fun fetchUserPreferences(userId: String, onSuccess: (Map<String, Boolean>) -> Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users/$userId/preferences")
        dbRef.get().addOnSuccessListener { snapshot ->
            val preferences = snapshot.value as? Map<String, Boolean> ?: emptyMap()
            onSuccess(preferences)
        }.addOnFailureListener {
            Log.e(TAG, "Error fetching user preferences: ${it.message}")
            Toast.makeText(this, "Failed to load user preferences.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun fetchAndShowFilteredStudyLocations() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid

        // Fetch user preferences
        fetchUserPreferences(userId) { preferences ->
            // Fetch all study locations
            fetchStudyLocations { locations ->
                // Filter locations based on preferences
                val filteredLocations = locations.filter { location ->
                    val environment = location["Environment"]?.split(",")?.map { it.trim() } ?: emptyList()
                    val foodNearby = location["Food Nearby"]?.split(",")?.map { it.trim() } ?: emptyList()
                    val roomType = location["Room Type"]?.split(",")?.map { it.trim() } ?: emptyList()
                    val studyTime = location["Study Time"]?.split(",")?.map { it.trim() } ?: emptyList()

                    // Strict filtering for Environment
                    val selectedEnvironments = mutableListOf<String>()
                    if (preferences["Quiet Study"] == true) selectedEnvironments.add("Quiet Study")
                    if (preferences["Collab Friendly"] == true) selectedEnvironments.add("Collaboration Friendly")
                    if (preferences["Good View"] == true) selectedEnvironments.add("Good view")
                    if (preferences["Cafes"] == true) selectedEnvironments.add("Cafes")

                    // Match at least one environment preference (use `.any` instead of `.all`)
                    val matchesEnvironment = if (selectedEnvironments.isNotEmpty()) {
                        selectedEnvironments.any { environment.contains(it) }
                    } else {
                        true // Allow all environments if none selected
                    }

                    // Loose filtering for other categories
                    val matchesFoodNearby = preferences["Food Trucks"] == true && foodNearby.contains("Food Trucks") ||
                            preferences["Vending Machines"] == true && foodNearby.contains("Vending Machines")

                    val matchesRoomType = preferences["Big Tables"] == true && roomType.contains("Big Tables") ||
                            preferences["Small Tables"] == true && roomType.contains("Small Tables") ||
                            preferences["Private Room"] == true && roomType.contains("Private Rooms")

                    val matchesStudyTime = preferences["Morning"] == true && studyTime.contains("Morning") ||
                            preferences["Afternoon"] == true && studyTime.contains("Afternoon") ||
                            preferences["Night"] == true && studyTime.contains("Night")

                    // Combine strict environment filter with loose filters
                    matchesEnvironment && (matchesFoodNearby || matchesRoomType || matchesStudyTime)
                }

                // Debugging log for filtered locations
                Log.d("FilteredLocations", "Filtered locations: $filteredLocations")

                // Clear existing markers from the map
                mMap.clear()

                // Display filtered locations on the map
                displayStudyLocationsOnMap(filteredLocations)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-fetch preferences and filtered locations when returning to this activity
        fetchAndShowFilteredStudyLocations()
    }


    private fun createCustomMarker(): BitmapDescriptor {
        return try {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.study_mark)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
            BitmapDescriptorFactory.fromBitmap(scaledBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating custom marker: ${e.message}")
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        }
    }



    private fun fetchAndShowStudyLocations() {
        fetchStudyLocations { locations ->
            displayStudyLocationsOnMap(locations)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, MAPS_API_KEY)
        }
        placesClient = Places.createClient(this)

        // Retrieve location details passed via Intent
        val locationName = intent.getStringExtra("location_name")
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        // Initialize map fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize POI information block
        poiInfoBlock = findViewById(R.id.poi_info_scroll)
        poiNameTextView = findViewById(R.id.poi_name)
        poiInfoBlock.visibility = View.GONE

        // Set up search autocomplete
        setupSearchAutocomplete()

        // Set up bottom navigation
        setupBottomNavigation()

        // Handle location details passed via Intent
        if (latitude != 0.0 && longitude != 0.0) {
            val destination = LatLng(latitude, longitude)

            // Postpone moving the map until it is fully initialized in onMapReady
            mapFragment.getMapAsync { googleMap ->
                googleMap.addMarker(MarkerOptions().position(destination).title(locationName))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destination, 15f))
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

        // Pan to the user's current location
        panToCurrentLocation()

        // Fetch and display filtered study locations
        fetchAndShowFilteredStudyLocations()

        // Handle marker clicks dynamically
        mMap.setOnMarkerClickListener { marker ->
            handleMarkerClick(marker)
            marker.showInfoWindow()
            true
        }

        // Handle default POI clicks
        mMap.setOnPoiClickListener { poi ->
            handlePoiClick(poi)
        }

        mMap.setOnMapClickListener {
            // Hide POI block and clear routes
            poiInfoBlock.visibility = View.GONE
            currentPolyline?.remove()
            currentPolyline = null
        }
    }

    private fun handlePoiClick(poi: PointOfInterest) {
        val placeId = poi.placeId

        // Fetch place details using the placeId
        val request = FetchPlaceRequest.newInstance(
            placeId,
            listOf(
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.RATING,
                Place.Field.PHOTO_METADATAS,
                Place.Field.USER_RATINGS_TOTAL,
                Place.Field.OPENING_HOURS,
                Place.Field.LAT_LNG
            )
        )

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place

            // Show the place info
            showPlaceInfo(place)

            // Automatically draw a route to the POI
            if (userLocation != null && place.latLng != null) {
                fetchAndDrawRoute(userLocation!!, place.latLng!!, "walking")
            } else {
                Toast.makeText(this, "Unable to fetch your location or destination.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Error fetching place details: ${exception.message}")
            Toast.makeText(this, "Failed to fetch details for ${poi.name}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun displayStudyLocationsOnMap(locations: List<Map<String, String>>) {
        locations.forEach { location ->
            val coordinates = location["Coordinates"]?.split(", ")?.map { it.trim() } ?: return@forEach
            if (coordinates.size != 2) return@forEach

            val latitude = coordinates[0].toDoubleOrNull() ?: return@forEach
            val longitude = coordinates[1].toDoubleOrNull() ?: return@forEach

            val name = location["Name of spot"] ?: "Unknown Spot"
            val markerOptions = MarkerOptions()
                .position(LatLng(latitude, longitude))
                .title(name)
                .icon(createCustomMarker())

            val marker = mMap.addMarker(markerOptions)
            marker?.tag = "study_location"
        }
    }

    private fun handleMarkerClick(marker: Marker) {
        val markerTitle = marker.title ?: "Unknown Location"
        val markerPosition = marker.position

        Log.d(TAG, "Marker clicked: $markerTitle at $markerPosition")

        // Step 1: Attempt to fetch place details by title (fallback to searchPlaceByName if necessary)
        searchPlaceByName(markerTitle) { place ->
            if (place != null) {
                // Step 2: Show place info in the POI info block
                showPlaceInfo(place)

                // Step 3: Draw a route to the marker's location
                if (userLocation != null && place.latLng != null) {
                    fetchAndDrawRoute(userLocation!!, place.latLng!!, "walking")
                } else {
                    Toast.makeText(
                        this,
                        "User location unavailable or invalid place details.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // If the place couldn't be found
                Toast.makeText(this, "Unable to find details for $markerTitle.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


    private fun searchPlaceByName(placeName: String, callback: (Place?) -> Unit) {
        val url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                "?query=${placeName.replace(" ", "+")}" +
                "&key=$MAPS_API_KEY"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                val jsonObject = JSONObject(response)
                val results = jsonObject.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val firstResult = results.getJSONObject(0)

                    // Extract relevant fields
                    val lat = firstResult.getJSONObject("geometry").getJSONObject("location").getDouble("lat")
                    val lng = firstResult.getJSONObject("geometry").getJSONObject("location").getDouble("lng")
                    val placeName = firstResult.getString("name")
                    val rating = firstResult.optDouble("rating", 0.0).toFloat()
                    val userRatingsTotal = firstResult.optInt("user_ratings_total", 0)
                    val photoReference = if (firstResult.has("photos")) {
                        firstResult.getJSONArray("photos").getJSONObject(0).getString("photo_reference")
                    } else null

                    // Build a simplified Place object with photo reference
                    val place = Place.builder()
                        .setName(placeName)
                        .setLatLng(LatLng(lat, lng))
                        .setRating(rating.toDouble())
                        .setUserRatingsTotal(userRatingsTotal)
                        .setAttributions(photoReference?.let { listOf(it) }) // Convert to List<String> or pass null
                        .build()

                    withContext(Dispatchers.Main) {
                        callback(place)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching place by name: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }


    private fun setupSearchAutocomplete() {
        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as? AutocompleteSupportFragment
        if (autocompleteFragment == null) {
            Log.e(TAG, "Autocomplete fragment is null")
            return
        }

        // Set fields to retrieve
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.RATING,
                Place.Field.PHOTO_METADATAS,
                Place.Field.USER_RATINGS_TOTAL,
                Place.Field.OPENING_HOURS
            )
        )

        // Listener for place selection
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val placeLatLng = place.latLng
                if (placeLatLng == null) {
                    Toast.makeText(this@MapActivity, "Invalid place selected", Toast.LENGTH_SHORT).show()
                    return
                }

                Log.d(TAG, "Place selected: ${place.name}, Location: $placeLatLng")

                // Move the camera to the selected place
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(placeLatLng, 15f))

                // Show detailed info about the place
                showPlaceInfo(place)

                // Draw a route from the user's current location to the selected place
                if (userLocation != null) {
                    fetchAndDrawRoute(userLocation!!, placeLatLng, "walking")
                } else {
                    Toast.makeText(this@MapActivity, "Unable to fetch your location", Toast.LENGTH_SHORT).show()
                }

                // Add a marker for the selected place
                moveMarker(placeLatLng, place.name)
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Log.e(TAG, "Error selecting place: ${status.statusMessage}")
            }
        })

        // Handle search bar clearing by monitoring the AutocompleteFragment's view
        val clearButton = (autocompleteFragment.view as? ViewGroup)?.findViewById<View>(
            resources.getIdentifier("places_autocomplete_clear_button", "id", packageName)
        )
        clearButton?.setOnClickListener {
            clearRouteAndPOI()
        }
    }

    private fun clearRouteAndPOI() {
        currentPolyline?.remove()
        currentPolyline = null
        currentMarker?.remove()
    }



    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Reload map data when returning to the home tab
                    fetchAndShowFilteredStudyLocations()
                    true
                }
                R.id.nav_favorites -> {
                    // Navigate to Favorites and ensure data is refreshed
                    val intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_settings -> {
                    // Navigate to Settings and ensure preferences update
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }


    private fun addGeneralPOIMarker(latLng: LatLng, name: String, placeId: String) {
        val markerOptions = MarkerOptions()
            .position(latLng)
            .title(name)
            .snippet(placeId) // Store Place ID in the snippet
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

        val marker = mMap.addMarker(markerOptions)
        marker?.tag = "general_poi" // Assign tag for general POIs
    }

    private fun searchStudyLocationAndRoute(locationName: String, markerPosition: LatLng) {
        val request = FetchPlaceRequest.newInstance(
            locationName,
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.RATING,
                Place.Field.PHOTO_METADATAS,
                Place.Field.USER_RATINGS_TOTAL,
                Place.Field.OPENING_HOURS
            )
        )

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            showPlaceInfo(place) // Show place details in POI info block
            fetchAndDrawRoute(markerPosition, place.latLng!!, "walking") // Draw route
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Error fetching place details for $locationName: ${exception.message}")
            Toast.makeText(this, "Failed to find $locationName.", Toast.LENGTH_SHORT).show()
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
        findViewById<Button>(R.id.add_to_favorites_button).visibility = View.VISIBLE
        // Get the current day of the week
        val calendar = java.util.Calendar.getInstance()
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)

        // Adjust the index to map Sunday (1) -> 6 and Monday (2) -> 0
        val adjustedIndex = if (dayOfWeek == 1) 6 else dayOfWeek - 2

        val currentDayHours = place.openingHours?.weekdayText?.getOrNull(adjustedIndex)

        findViewById<TextView>(R.id.poi_hours).text =
            currentDayHours ?: "Opening Hours: Not Available"

        val photoImageView = findViewById<ImageView>(R.id.poi_photo)
        val photoReference = place.attributions
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
        place.latLng?.let { setupAddToFavoritesButton(place.name, it) }
    }


    private fun setupAddToFavoritesButton(placeName: String?, placeLatLng: LatLng?) {
        val addToFavoritesButton = findViewById<Button>(R.id.add_to_favorites_button)
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please log in to manage favorites.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val dbRef = FirebaseDatabase.getInstance().getReference("users/$userId/favorites")

        // Fetch current favorites from Firebase
        dbRef.get().addOnSuccessListener { snapshot ->
            var isFavorite = false
            var favoriteKey: String? = null

            // Check if the current place is already a favorite
            for (child in snapshot.children) {
                val name = child.child("name").value as? String
                if (name == placeName) {
                    isFavorite = true
                    favoriteKey = child.key
                    break
                }
            }

            // Update the button text based on the favorite status
            addToFavoritesButton.text = if (isFavorite) "Remove from Favorites" else "Add to Favorites"

            // Set up button click listener
            addToFavoritesButton.setOnClickListener {
                if (isFavorite) {
                    // Remove from favorites
                    favoriteKey?.let {
                        dbRef.child(it).removeValue()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
                                isFavorite = false
                                addToFavoritesButton.text = "Add to Favorites"
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to remove from favorites: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // Add to favorites
                    val favoriteLocation = mapOf(
                        "name" to (placeName ?: "Unknown Location"),
                        "latitude" to (placeLatLng?.latitude ?: 0.0),
                        "longitude" to (placeLatLng?.longitude ?: 0.0)
                    )

                    dbRef.push().setValue(favoriteLocation)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
                            isFavorite = true
                            addToFavoritesButton.text = "Remove from Favorites"
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to add to favorites: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch favorites: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun moveMarker(latLng: LatLng, title: String?) {
        currentMarker?.remove()
        currentMarker = mMap.addMarker(
            MarkerOptions().position(latLng).title(title)
        )
        currentMarker?.showInfoWindow()
    }

    private fun fetchAndDrawRoute(origin: LatLng, destination: LatLng, travelMode: String = "walking") {
        if (origin == destination) {
            Toast.makeText(this, "You're already at the destination!", Toast.LENGTH_SHORT).show()
            return
        }
        val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&mode=$travelMode" +
                "&key=$MAPS_API_KEY"
        Log.d(TAG, "Fetching route with URL: $urlString")

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
            val routes = jsonObject.optJSONArray("routes")
            if (routes != null && routes.length() > 0) {
                val polyline = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")
                val decodedPath = decodePolyline(polyline)

                // Clear the previous polyline if it exists
                currentPolyline?.remove()

                // Draw the new route
                currentPolyline = mMap.addPolyline(
                    PolylineOptions()
                        .addAll(decodedPath)
                        .color(0xFF0000FF.toInt()) // Blue color
                        .width(12f)
                        .geodesic(true) // Smooth route
                )

                // Adjust the camera to show the route
                val boundsBuilder = LatLngBounds.Builder()
                decodedPath.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100)) // Padding of 100
            } else {
                Toast.makeText(this, "No route found to the destination.", Toast.LENGTH_SHORT).show()
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
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
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