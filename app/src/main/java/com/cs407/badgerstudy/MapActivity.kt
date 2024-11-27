package com.cs407.badgerstudy

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
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
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.libraries.places.api.model.Place

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var poiInfoBlock: View
    private lateinit var poiNameTextView: TextView
    private var currentMarker: Marker? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyB2-kJTZNlA26rq71Pa1aRkTzAtLioKtpo")
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        poiInfoBlock = findViewById(R.id.poi_info_block)
        poiNameTextView = findViewById(R.id.poi_name)
        poiInfoBlock.visibility = View.GONE
        setupSearchAutocomplete()
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

            poiNameTextView.text = poi.name
            poiInfoBlock.visibility = View.VISIBLE


            moveMarker(poi.latLng, poi.name)
        }
    }

    private fun setupSearchAutocomplete() {
        val autocompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val selectedLatLng = place.latLng
                if (selectedLatLng != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, 15f))


                    moveMarker(selectedLatLng, place.name)
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(this@MapActivity, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        })
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

    private fun panToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        mMap.isMyLocationEnabled = true

        // Get the user's current location
        val fusedLocationProviderClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentPosition = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))

                mMap.addCircle(
                    CircleOptions()
                        .center(currentPosition)
                        .radius(80.0)
                        .strokeColor(0xFF0000FF.toInt())
                        .fillColor(0x220000FF)
                        .strokeWidth(4f)
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            panToCurrentLocation()
        }
    }
}