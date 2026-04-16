package com.example.intellibrief

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MapManager {
    private val client = OkHttpClient()

    // using Google geocoding api to get city from lat, lng
     fun getCity(lat: Double, lon: Double, apiKey: String): String {
        val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lon&key=$apiKey"
        val request = Request.Builder().url(url).build()
        
        return try {
            val response = client.newCall(request).execute()
            val jsonData = response.body?.string()
            val jsonObject = JSONObject(jsonData ?: "")
            val results = jsonObject.getJSONArray("results")
            
            if (results.length() > 0) {
                val addressComponents = results.getJSONObject(0).getJSONArray("address_components")
                var city = ""
                for (i in 0 until addressComponents.length()) {
                    val component = addressComponents.getJSONObject(i)
                    val types = component.getJSONArray("types")
                    for (j in 0 until types.length()) {
                        if (types.getString(j) == "locality") {
                            city = component.getString("long_name")
                            break
                        }
                    }
                    if (city.isNotEmpty()) break
                }
                city
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e("NewsManager", "Geocoding failed", e)
            ""
        }
    }
}
