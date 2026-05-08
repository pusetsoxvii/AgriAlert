package com.agrialert.app.ui.farmer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.agrialert.app.R
import com.agrialert.app.databinding.ActivityMapPickerBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapPickerBinding
    private lateinit var mMap: GoogleMap
    private var selectedLatLng: LatLng? = null
    private var selectedAddress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnConfirmLocation.setOnClickListener {
            selectedLatLng?.let {
                val intent = Intent()
                intent.putExtra("lat", it.latitude)
                intent.putExtra("lng", it.longitude)
                intent.putExtra("address", selectedAddress)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }

        binding.btnSearch.setOnClickListener { searchLocation() }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation()
                true
            } else false
        }

        binding.fabMyLocation.setOnClickListener { checkLocationPermissionAndGet() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Center on Lesotho by default
        val lesotho = LatLng(-29.6100, 28.2336)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lesotho, 8f))

        mMap.setOnMapClickListener { latLng ->
            updateMarker(latLng)
        }
    }

    private fun searchLocation() {
        val locationName = binding.etSearch.text.toString().trim()
        if (locationName.isEmpty()) return

        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList = geocoder.getFromLocationName(locationName, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)
                updateMarker(latLng)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationPermissionAndGet() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                updateMarker(latLng)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
    }

    private fun updateMarker(latLng: LatLng) {
        mMap.clear()
        selectedAddress = getAddressFromLatLng(latLng)
        mMap.addMarker(MarkerOptions().position(latLng).title(selectedAddress))
        selectedLatLng = latLng
        binding.btnConfirmLocation.isEnabled = true
    }

    private fun getAddressFromLatLng(latLng: LatLng): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address: Address = addresses[0]
                // Construct a readable address string
                val addressParts = mutableListOf<String>()
                for (i in 0..address.maxAddressLineIndex) {
                    addressParts.add(address.getAddressLine(i))
                }
                addressParts.joinToString(", ")
            } else {
                "Pinned Location"
            }
        } catch (e: Exception) {
            "Pinned Location"
        }
    }
}
