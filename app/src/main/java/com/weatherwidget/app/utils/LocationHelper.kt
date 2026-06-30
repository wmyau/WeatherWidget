package com.weatherwidget.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    private const val PREFS = "weather_prefs"
    private const val KEY_LAT = "cached_lat"
    private const val KEY_LON = "cached_lon"
    private const val KEY_CITY = "cached_city"
    private const val NO_VALUE = Float.MAX_VALUE

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun saveLocation(context: Context, lat: Double, lon: Double, city: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LON, lon.toFloat())
            .putString(KEY_CITY, city)
            .apply()
    }

    fun getCachedLocation(context: Context): Triple<Double, Double, String>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lat = prefs.getFloat(KEY_LAT, NO_VALUE)
        val lon = prefs.getFloat(KEY_LON, NO_VALUE)
        val city = prefs.getString(KEY_CITY, null)
        if (lat == NO_VALUE || lon == NO_VALUE || city == null) return null
        return Triple(lat.toDouble(), lon.toDouble(), city)
    }

    @Suppress("MissingPermission")
    fun getLastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Skip GPS_PROVIDER: reading it in a background process triggers MONITOR_HIGH_POWER_LOCATION
        // AppOps which is blocked on Android 10+ without ACCESS_BACKGROUND_LOCATION.
        return try {
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("MissingPermission")
    suspend fun requestSingleUpdate(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }
        return withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        lm.removeUpdates(this)
                        if (cont.isActive) cont.resume(loc)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                }
                try {
                    lm.requestLocationUpdates(provider, 0L, 0f, listener)
                    cont.invokeOnCancellation { lm.removeUpdates(listener) }
                } catch (e: Exception) {
                    cont.resume(null)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    suspend fun getCityName(context: Context, latitude: Double, longitude: Double): String {
        // Try Android Geocoder first — absent on Huawei without Google Play Services
        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val city = addresses?.firstOrNull()?.let { addr ->
                    addr.locality ?: addr.subAdminArea ?: addr.adminArea
                }
                if (!city.isNullOrEmpty()) return city
            } catch (_: Exception) {}
        }
        // Fallback: Nominatim reverse geocoding (OpenStreetMap, no key required)
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&format=json")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "WeatherWidget/1.0")
                conn.connectTimeout = 6_000
                conn.readTimeout = 6_000
                val json = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val addr = JSONObject(json).optJSONObject("address")
                addr?.let {
                    it.optString("city").takeIf { s -> s.isNotEmpty() }
                        ?: it.optString("town").takeIf { s -> s.isNotEmpty() }
                        ?: it.optString("village").takeIf { s -> s.isNotEmpty() }
                        ?: it.optString("county").takeIf { s -> s.isNotEmpty() }
                } ?: "Unknown"
            } catch (_: Exception) {
                "Unknown"
            }
        }
    }
}
