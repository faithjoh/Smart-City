package com.smartcity.parkingapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.smartcity.parkingapp.R
import com.smartcity.parkingapp.model.ParkingSpot
import java.io.IOException
import java.util.*

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val parkingSpots = mutableListOf<ParkingSpot>()
    private val parkingMarkers = mutableMapOf<String, Marker>()
    
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var userProfileButton: FloatingActionButton
    
    // London coordinates as default location
    private val defaultLocation = LatLng(51.5074, -0.1278)
    
    // Add property to store parking ID that needs to be shown
    private var pendingParkingSpotId: String? = null
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        
        searchEditText = view.findViewById(R.id.search_edit_text)
        searchButton = view.findViewById(R.id.search_button)
        userProfileButton = view.findViewById(R.id.user_profile_button)
        
        searchButton.setOnClickListener {
            searchByPostcode(searchEditText.text.toString())
        }
        
        userProfileButton.setOnClickListener {
            // Navigate to the User Center (ProfileFragment) using the bottom navigation
            val bottomNavigation = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            bottomNavigation.selectedItemId = R.id.navigation_profile
        }
        
        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check if we were given a parking spot ID to display
        arguments?.getString("parking_spot_id")?.let { parkingId ->
            // Store the ID to show details after map is ready
            pendingParkingSpotId = parkingId
        }
    }
    
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        // Set map style
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        
        // Enable and position controls
        mMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
        }
        
        // Try to adjust zoom controls position
        try {
            // Wait for the map to be laid out
            val mapView = childFragmentManager.findFragmentById(R.id.map)?.view
            mapView?.post {
                // Find and adjust zoom controls
                val zoomControls = mapView.findViewById<View>(0x1)
                zoomControls?.let { 
                    (it.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                        topMargin = 150
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore errors in UI adjustment
        }
        
        mMap.setOnMarkerClickListener(this)
        
        // Set my location button click listener, only locate to user's position when clicked
        mMap.setOnMyLocationButtonClickListener {
            getDeviceLocation()
            false // Return false to let the system handle the event too
        }
        
        // Always move to London position first, as the app's default view
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
        
        // Check location permissions (but don't automatically jump to user's location)
        enableMyLocation()
        
        // Load parking spot data
        loadParkingSpots()
    }
    
    private fun loadParkingSpots() {
        val db = FirebaseFirestore.getInstance()
        
        db.collection("ParkingSpots")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // If we have spots in Firestore, load them
                    parkingSpots.clear()
                    for (document in documents) {
                        val spot = document.toObject(ParkingSpot::class.java)
                        spot.id = document.id
                        parkingSpots.add(spot)
                    }
                    
                    // Update map markers
                    updateMapMarkers()
                } else {
                    // If no spots exist in Firestore, add default ones
                    createDefaultParkingSpots()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load parking data: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // If loading fails, create default spots
                createDefaultParkingSpots()
            }
    }
    
    private fun updateMapMarkers() {
        // Clear existing markers
        mMap.clear()
        parkingMarkers.clear()
        
        // Create parking icon from vector drawable
        val parkingIcon = createParkingMarkerIcon()
        
        // Add new markers
        for (spot in parkingSpots) {
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(LatLng(spot.latitude, spot.longitude))
                    .title(spot.name)
                    .snippet(spot.address)
                    .icon(parkingIcon)
            )
            
            marker?.let {
                parkingMarkers[spot.id] = it
                it.tag = spot.id
            }
        }
        
        // If we have a pending parking spot to show, display it now
        pendingParkingSpotId?.let { parkingId ->
            showParkingSpotDetails(parkingId)
            pendingParkingSpotId = null
        }
    }
    
    private fun createParkingMarkerIcon(): BitmapDescriptor {
        // Try to create a custom bitmap with "P" icon
        return try {
            // Create bitmap for the background
            val background = ContextCompat.getDrawable(requireContext(), R.drawable.parking_marker_bg)
            val backgroundBitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(backgroundBitmap)
            background?.setBounds(0, 0, 96, 96)
            background?.draw(canvas)
            
            // Draw the "P" icon on top
            val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_parking)
            icon?.setBounds(24, 24, 72, 72)
            icon?.draw(canvas)
            
            // Create BitmapDescriptor from the resulting bitmap
            BitmapDescriptorFactory.fromBitmap(backgroundBitmap)
        } catch (e: Exception) {
            // Log the error
            Log.e("MapFragment", "Error creating custom marker: ${e.message}")
            // Fallback to default marker
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
        }
    }
    
    private fun createDefaultParkingSpots() {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()
        
        // London parking spots data
        val parkingSpotsData = listOf(
            // Central London
            mapOf(
                "name" to "NCP Car Park London Shaftesbury",
                "address" to "Selkirk House, Shaftesbury Ave, London W1D 5DN",
                "rating" to 4.3f,
                "price" to 9.5,
                "totalSlots" to 648,
                "openHours" to "24 hours",
                "latitude" to 51.5120,
                "longitude" to -0.1311
            ),
            mapOf(
                "name" to "Q-Park Chinatown",
                "address" to "20 Newport Pl, London WC2H 7PR",
                "rating" to 4.1f,
                "price" to 10.0,
                "totalSlots" to 250,
                "openHours" to "24 hours",
                "latitude" to 51.5119,
                "longitude" to -0.1287
            ),
            mapOf(
                "name" to "NCP Car Park London Covent Garden",
                "address" to "Parker St, London WC2B 5NT",
                "rating" to 4.0f,
                "price" to 9.0,
                "totalSlots" to 330,
                "openHours" to "24 hours",
                "latitude" to 51.5159,
                "longitude" to -0.1241
            ),
            // West London
            mapOf(
                "name" to "QPark Westfield London",
                "address" to "Ariel Way, London W12 7SL",
                "rating" to 4.5f,
                "price" to 6.0,
                "totalSlots" to 4500,
                "openHours" to "6:00-24:00",
                "latitude" to 51.5060,
                "longitude" to -0.2235
            ),
            mapOf(
                "name" to "NCP London Paddington",
                "address" to "Bishops Bridge Rd, London W2 6AA",
                "rating" to 3.9f,
                "price" to 8.0,
                "totalSlots" to 146,
                "openHours" to "24 hours",
                "latitude" to 51.5188,
                "longitude" to -0.1817
            ),
            // North London
            mapOf(
                "name" to "NCP Car Park Camden Town",
                "address" to "Pratt St, London NW1 0LY",
                "rating" to 3.8f,
                "price" to 7.5,
                "totalSlots" to 204,
                "openHours" to "24 hours",
                "latitude" to 51.5379,
                "longitude" to -0.1403
            ),
            mapOf(
                "name" to "Brunswick Square Car Park",
                "address" to "Marchmont St, London WC1N 1AF",
                "rating" to 4.2f,
                "price" to 8.0,
                "totalSlots" to 560,
                "openHours" to "24 hours",
                "latitude" to 51.5242,
                "longitude" to -0.1222
            ),
            // East London
            mapOf(
                "name" to "Stratford Westfield Car Park",
                "address" to "Montfichet Rd, London E20 1EJ",
                "rating" to 4.4f,
                "price" to 5.5,
                "totalSlots" to 4500,
                "openHours" to "7:00-00:00",
                "latitude" to 51.5437,
                "longitude" to -0.0098
            ),
            mapOf(
                "name" to "Minories Car Park",
                "address" to "1 Shorter St, London E1 8LP",
                "rating" to 3.9f,
                "price" to 8.5,
                "totalSlots" to 321,
                "openHours" to "24 hours",
                "latitude" to 51.5112,
                "longitude" to -0.0744
            ),
            // South London
            mapOf(
                "name" to "Q-Park Westminster",
                "address" to "Great College St, London SW1P 3RX",
                "rating" to 4.3f,
                "price" to 8.5,
                "totalSlots" to 230,
                "openHours" to "24 hours",
                "latitude" to 51.4980,
                "longitude" to -0.1261
            ),
            mapOf(
                "name" to "NCP London Bridge",
                "address" to "Kipling St, London SE1 3RU",
                "rating" to 3.8f,
                "price" to 8.0,
                "totalSlots" to 250,
                "openHours" to "24 hours",
                "latitude" to 51.5045,
                "longitude" to -0.0883
            ),
            // Other major areas
            mapOf(
                "name" to "Kensington Olympia Car Park",
                "address" to "Olympia Way, Hammersmith, London W14 8UX",
                "rating" to 4.0f,
                "price" to 7.0,
                "totalSlots" to 380,
                "openHours" to "7:00-23:00",
                "latitude" to 51.4964,
                "longitude" to -0.2104
            ),
            mapOf(
                "name" to "Excel London Car Park",
                "address" to "Royal Victoria Dock, London E16 1XL",
                "rating" to 4.2f,
                "price" to 5.0,
                "totalSlots" to 3070,
                "openHours" to "7:00-22:00",
                "latitude" to 51.5079,
                "longitude" to 0.0288
            ),
            mapOf(
                "name" to "O2 Arena Car Park",
                "address" to "Peninsula Square, London SE10 0DX",
                "rating" to 4.4f,
                "price" to 6.0,
                "totalSlots" to 2800,
                "openHours" to "7:00-1:00",
                "latitude" to 51.5030,
                "longitude" to 0.0032
            ),
            // Additional spots around London
            mapOf(
                "name" to "Brent Cross Shopping Centre",
                "address" to "Prince Charles Dr, London NW4 3FP",
                "rating" to 4.3f,
                "price" to 4.0,
                "totalSlots" to 7500,
                "openHours" to "6:00-0:00",
                "latitude" to 51.5765,
                "longitude" to -0.2255
            ),
            mapOf(
                "name" to "Kings Mall Car Park Hammersmith",
                "address" to "Kings Mall, King St, London W6 9HW",
                "rating" to 3.9f,
                "price" to 6.5,
                "totalSlots" to 700,
                "openHours" to "7:00-23:00",
                "latitude" to 51.4934,
                "longitude" to -0.2273
            ),
            mapOf(
                "name" to "Whiteleys Shopping Centre",
                "address" to "Queensway, London W2 4YN",
                "rating" to 4.1f,
                "price" to 7.0,
                "totalSlots" to 175,
                "openHours" to "7:00-00:00",
                "latitude" to 51.5155,
                "longitude" to -0.1887
            ),
            mapOf(
                "name" to "Canary Wharf Car Park",
                "address" to "Cabot Square, London E14 4QJ",
                "rating" to 4.6f,
                "price" to 9.0,
                "totalSlots" to 2100,
                "openHours" to "24 hours",
                "latitude" to 51.5055,
                "longitude" to -0.0212
            ),
            mapOf(
                "name" to "CitiPark Tower Bridge",
                "address" to "50 Gainsford St, London SE1 2NY",
                "rating" to 4.0f,
                "price" to 8.5,
                "totalSlots" to 460,
                "openHours" to "24 hours",
                "latitude" to 51.5024,
                "longitude" to -0.0733
            ),
            mapOf(
                "name" to "London Waterloo Station Car Park",
                "address" to "39 York Rd, London SE1 7ND",
                "rating" to 3.7f,
                "price" to 8.0,
                "totalSlots" to 127,
                "openHours" to "24 hours",
                "latitude" to 51.5036,
                "longitude" to -0.1136
            )
        )
        
        // Add the parking spots to Firestore
        parkingSpots.clear()
        
        for ((index, spotData) in parkingSpotsData.withIndex()) {
            val docRef = db.collection("ParkingSpots").document("spot_${index + 1}")
            batch.set(docRef, spotData)
            
            // Create local object for immediate display
            val spot = ParkingSpot(
                id = "spot_${index + 1}",
                name = spotData["name"] as String,
                address = spotData["address"] as String,
                rating = spotData["rating"] as Float,
                price = spotData["price"] as Double,
                totalSlots = spotData["totalSlots"] as Int,
                openHours = spotData["openHours"] as String,
                latitude = spotData["latitude"] as Double,
                longitude = spotData["longitude"] as Double
            )
            parkingSpots.add(spot)
        }
        
        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                // Update map markers with the new spots
                updateMapMarkers()
                Toast.makeText(context, "Parking spots added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to add parking spots: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            // No longer automatically calling getDeviceLocation()
            // Let the user manually switch to their location via the location button
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    private fun getDeviceLocation() {
        try {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val locationResult = fusedLocationClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        val lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation.latitude,
                                        lastKnownLocation.longitude
                                    ),
                                    15f
                                )
                            )
                        } else {
                            // If location can't be obtained, use default location
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f)
                            )
                        }
                    } else {
                        // If getting location fails, use default location
                        mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Handle exception
            Toast.makeText(context, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun searchByPostcode(postcode: String) {
        if (postcode.isEmpty()) {
            Toast.makeText(context, "Please enter a postcode", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Ensure we're searching in London, UK
        val searchQuery = if (postcode.lowercase().contains("london")) {
            postcode
        } else {
            "$postcode, London, UK"
        }
        
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            // Use the modern approach with a callback
            @Suppress("DEPRECATION") 
            val addresses = geocoder.getFromLocationName(searchQuery, 1)
            
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val latLng = LatLng(address.latitude, address.longitude)
                
                // Log the found location
                Log.d("MapFragment", "Found location: ${address.getAddressLine(0)}")
                
                // Check if we're still roughly in the London area (40km radius)
                val londonDistance = calculateDistance(
                    defaultLocation.latitude, 
                    defaultLocation.longitude,
                    address.latitude,
                    address.longitude
                )
                
                if (londonDistance <= 40) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    Toast.makeText(context, "Location found: ${address.getAddressLine(0)}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Location outside of London area. Showing default view.", Toast.LENGTH_SHORT).show()
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
                }
            } else {
                Toast.makeText(context, "Location not found for this postcode", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Calculate distance between two points in kilometers using Haversine formula
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Earth radius in kilometers
        
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return r * c // Distance in kilometers
    }
    
    override fun onMarkerClick(marker: Marker): Boolean {
        val spotId = marker.tag as? String ?: return false
        
        // Find corresponding parking spot
        val spot = parkingSpots.find { it.id == spotId } ?: return false
        
        // Show parking details dialog
        val dialog = ParkingDetailsDialog.newInstance(spot)
        dialog.show(parentFragmentManager, "ParkingDetailsDialog")
        
        return true
    }
    
    // Helper method to show parking details dialog
    private fun showParkingDetailsDialog(parkingSpot: ParkingSpot) {
        val dialog = ParkingDetailsDialog.newInstance(parkingSpot)
        dialog.show(parentFragmentManager, "ParkingDetailsDialog")
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(context, "Location permission required to show your location", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Method to show details for a specific parking spot
    fun showParkingSpotDetails(parkingId: String) {
        // Find the parking spot with this ID
        val parkingSpot = parkingSpots.find { it.id == parkingId }
        
        if (parkingSpot != null) {
            // Move camera to this location
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(parkingSpot.latitude, parkingSpot.longitude),
                    15f
                )
            )
            
            // Get the marker for this spot
            val marker = parkingMarkers[parkingId]
            
            // Show the info window for this marker
            marker?.showInfoWindow()
            
            // Open the details dialog for this parking spot
            showParkingDetailsDialog(parkingSpot)
        } else {
            // If spot not found yet, store ID to show later after data loads
            pendingParkingSpotId = parkingId
            
            // If we couldn't find it in our current list, try to load it directly
            if (parkingSpots.isNotEmpty()) {
                loadSpecificParkingSpot(parkingId)
            }
        }
    }
    
    private fun loadSpecificParkingSpot(parkingId: String) {
        val db = FirebaseFirestore.getInstance()
        
        db.collection("ParkingSpots").document(parkingId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Create parking spot from document
                    val spot = document.toObject(ParkingSpot::class.java)
                    spot?.id = document.id
                    
                    if (spot != null && !parkingSpots.any { it.id == spot.id }) {
                        // Add to our list if not already there
                        parkingSpots.add(spot)
                        
                        // Update markers
                        updateMapMarkers()
                    }
                }
            }
    }
} 