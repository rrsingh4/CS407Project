package com.cs407.badgerstudy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.android.material.bottomnavigation.BottomNavigationView

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

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        poiInfoBlock = findViewById(R.id.poi_info_block)
        poiNameTextView = findViewById(R.id.poi_name)
        poiInfoBlock.visibility = View.GONE

        // Handle bottom navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_favorites -> {
                    startActivity(Intent(this, FavoritesActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        panToCurrentLocation()

        mMap.setOnPoiClickListener { poi ->
            showPlaceInfo(poi.name, poi.latLng)
        }
    }

    private fun panToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        mMap.isMyLocationEnabled = true
        val fusedLocationProviderClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
    }

    private fun showPlaceInfo(name: String?, latLng: LatLng?) {
        poiInfoBlock.visibility = View.VISIBLE
        poiNameTextView.text = name ?: "Unknown Location"
    }
}


