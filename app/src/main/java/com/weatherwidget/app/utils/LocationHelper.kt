package com.weatherwidget.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.weatherwidget.app.data.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

data class LocationData(val lat: Double, val lon: Double, val city: String)

object LocationHelper {

    private const val PREFS = "weather_prefs"
    // "_s" suffix distinguishes from legacy Float-typed keys — reading a Float pref as String throws ClassCastException.
    private const val KEY_LAT = "cached_lat_s"
    private const val KEY_LON = "cached_lon_s"
    private const val KEY_CITY = "cached_city"
    private const val KEY_USE_MANUAL = "use_manual_location"
    private const val KEY_MANUAL_LAT = "manual_lat_s"
    private const val KEY_MANUAL_LON = "manual_lon_s"
    private const val KEY_MANUAL_CITY = "manual_city"

    // ── permission ────────────────────────────────────────────────────────────

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    // ── phone-location cache ──────────────────────────────────────────────────

    fun saveLocation(context: Context, lat: Double, lon: Double, city: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAT, lat.toString())
            .putString(KEY_LON, lon.toString())
            .putString(KEY_CITY, city)
            .apply()
    }

    fun getCachedLocation(context: Context): LocationData? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(KEY_LON, null)?.toDoubleOrNull() ?: return null
        val city = prefs.getString(KEY_CITY, null) ?: return null
        return LocationData(lat, lon, city)
    }

    // ── manual location ───────────────────────────────────────────────────────

    fun isManualLocationSet(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_USE_MANUAL, false)

    fun setManualLocation(context: Context, lat: Double, lon: Double, city: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_USE_MANUAL, true)
            .putString(KEY_MANUAL_LAT, lat.toString())
            .putString(KEY_MANUAL_LON, lon.toString())
            .putString(KEY_MANUAL_CITY, city)
            .apply()
    }

    fun getManualLocation(context: Context): LocationData? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_USE_MANUAL, false)) return null
        val lat = prefs.getString(KEY_MANUAL_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(KEY_MANUAL_LON, null)?.toDoubleOrNull() ?: return null
        val city = prefs.getString(KEY_MANUAL_CITY, null) ?: return null
        return LocationData(lat, lon, city)
    }

    fun clearManualLocation(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_USE_MANUAL, false)
            .apply()
    }

    // ── city search (Open-Meteo geocoding) ────────────────────────────────────

    data class GeoResult(
        val name: String,
        val admin1: String?,
        val country: String,
        val lat: Double,
        val lon: Double
    ) {
        val displayName: String get() = buildString {
            append(name)
            if (!admin1.isNullOrEmpty()) append(", $admin1")
            if (country.isNotEmpty()) append(", $country")
        }
    }

    suspend fun searchCity(query: String): List<GeoResult> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=5&language=en&format=json")
                .build()
            val json = HttpClient.okHttp.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext emptyList()
            val arr = JSONObject(json).optJSONArray("results") ?: return@withContext emptyList()
            (0 until arr.length()).map { i ->
                val r = arr.getJSONObject(i)
                GeoResult(
                    name = r.optString("name"),
                    admin1 = r.optString("admin1").takeIf { it.isNotEmpty() },
                    country = r.optString("country"),
                    lat = r.getDouble("latitude"),
                    lon = r.getDouble("longitude")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── GPS helpers ───────────────────────────────────────────────────────────

    @Suppress("MissingPermission")
    fun getLastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (_: Exception) { null }
    }

    @Suppress("MissingPermission")
    suspend fun requestSingleUpdate(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // GPS_PROVIDER requires ACCESS_FINE_LOCATION; we only hold COARSE, so NETWORK only.
        if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return null
        val provider = LocationManager.NETWORK_PROVIDER
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
                } catch (_: Exception) { cont.resume(null) }
            }
        }
    }

    @Suppress("DEPRECATION")
    suspend fun getCityName(context: Context, latitude: Double, longitude: Double): String {
        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val city = addresses?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
                if (!city.isNullOrEmpty()) return city
            } catch (_: Exception) {}
        }
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://nominatim.openstreetmap.org/reverse?lat=$latitude&lon=$longitude&format=json")
                    .header("User-Agent", "WeatherWidget/1.0")
                    .build()
                val json = HttpClient.okHttp.newCall(request).execute().use { it.body?.string() }
                    ?: return@withContext "Unknown"
                val addr = JSONObject(json).optJSONObject("address")
                addr?.let {
                    it.optString("city").takeIf { s -> s.isNotEmpty() }
                        ?: it.optString("town").takeIf { s -> s.isNotEmpty() }
                        ?: it.optString("village").takeIf { s -> s.isNotEmpty() }
                        ?: it.optString("county").takeIf { s -> s.isNotEmpty() }
                } ?: "Unknown"
            } catch (_: Exception) { "Unknown" }
        }
    }
}
