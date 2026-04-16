package com.example.intellibrief

import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.example.intellibrief.ui.theme.IntelliBriefTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // using savedInstanceState to preserve map pins for each user
        enableEdgeToEdge()
        setContent {
            IntelliBriefTheme {
                Scaffold(
                    topBar = {
                        Surface(
                            color = Color.Black,
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth().height(64.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { finish() }) {
                                    Text("<", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Event Map",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        DisplayMap()
                    }
                }
            }
        }
    }
}

// Google maps
@Composable
fun DisplayMap(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // shared preferences
    val prefs = remember { context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE) }

    // default lat,lon is DC
    var lat by remember { mutableStateOf(prefs.getString("lat", "38.9073") ?: "38.9073") }
    var lon by remember { mutableStateOf(prefs.getString("lon", "-77.0369") ?: "-77.0369") }
    
    // google api key
    val apiKey = BuildConfig.MAPS_API_KEY
    // used to call helper functions in map manager class
    val mapManager = remember { MapManager() }
    val scope = rememberCoroutineScope()
    
    // center point using lat and lon
    val center = LatLng(lat.toDouble(), lon.toDouble())
    // for marker
    val markerState = remember { MarkerState(position = center) }
    // address info or city info
    var addressInfo by remember { mutableStateOf("") }

    // list of saved events
    val savedEvents = remember { mutableStateListOf<GdeltArticle>() }
    // Used pair data structure for an article, location pair
    val markers = remember { mutableStateListOf<Pair<GdeltArticle, LatLng>>() }
    // event id from firebase db
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val fbRef = if (uid != null) "users/$uid/saved_events" else null

    // for zooming
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 10f)
    }

    // Read saved events from Firebase
    LaunchedEffect(fbRef) {
        if (fbRef == null) return@LaunchedEffect
        val eventRef = Firebase.database.getReference(fbRef)
        eventRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                savedEvents.clear()
                snapshot.children
                    .mapNotNull { it.getValue(GdeltArticle::class.java) }
                    .forEach { savedEvents.add(it) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // geocoding in order to get lat, lng for each saved event
    LaunchedEffect(savedEvents.toList()) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val newMarkers = mutableListOf<Pair<GdeltArticle, LatLng>>()
        
        withContext(Dispatchers.IO) {
            savedEvents.forEach { event ->
                if (event.lat != null && event.lon != null) {
                    newMarkers.add(event to LatLng(event.lat, event.lon))
                } else if (event.title.isNotEmpty()) {
                    try {
                        val addresses = geocoder.getFromLocationName(event.title, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val location = LatLng(addresses[0].latitude, addresses[0].longitude)
                            newMarkers.add(event to location)
                        }
                    } catch (e: Exception) {
                        Log.e("MapActivity", "Geocoding failed for: ${event.title}", e)
                    }
                }
            }
        }
        markers.clear()
        markers.addAll(newMarkers)
    }

    // launched effect allows the saved lat, lon to be used on the start of the activity
    LaunchedEffect(Unit) {
        // get the coordinates from local vars (that were updated with new values)
        val initialLat = lat.toDoubleOrNull() ?: 38.91
        val initialLon = lon.toDoubleOrNull() ?: -77.4
        // get city using saved coordinates
        val city = withContext(Dispatchers.IO) {
            mapManager.getCity(initialLat, initialLon, apiKey)
        }
        // show results for saved city
        if (city.isNotEmpty()) {
            addressInfo = city
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapLongClick = { it ->
                markerState.position = it
                val savedLat = it.latitude.toString()
                val savedLon = it.longitude.toString()
                
                // add to shared pref
                prefs.edit {
                    putString("lat", savedLat)
                    putString("lon", savedLon)
                }
                // update local variables
                lat = savedLat
                lon = savedLon

                // scope.launch uses a different thread to run in background
                scope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f))
                    val city = withContext(Dispatchers.IO) {
                        mapManager.getCity(it.latitude, it.longitude, apiKey)
                    }
                    addressInfo = city
                }
            }
        ) {
            // Main selected marker
            Marker(
                state = markerState,
                title = addressInfo,
                snippet = "${markerState.position.latitude}, ${markerState.position.longitude}"
            )

            // Saved events markers
            markers.forEach { (event, position) ->
                Marker(
                    state = MarkerState(position = position),
                    title = event.title,
                    snippet = event.domain,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }
    }
}
