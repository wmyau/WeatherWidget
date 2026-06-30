package com.weatherwidget.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.weatherwidget.app.data.WeatherRepository
import com.weatherwidget.app.databinding.ActivityMainBinding
import com.weatherwidget.app.utils.LocationHelper
import com.weatherwidget.app.worker.WeatherUpdateWorker
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val REQ_LOCATION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRefresh.setOnClickListener { triggerUpdate() }
        binding.btnGrantPermission.setOnClickListener { requestLocationPermission() }

        if (hasLocationPermission()) {
            binding.btnGrantPermission.isEnabled = false
            triggerUpdate()
        }
    }

    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQ_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                binding.tvStatus.text = getString(R.string.permission_granted)
                binding.btnGrantPermission.isEnabled = false
                triggerUpdate()
            } else {
                binding.tvStatus.text = getString(R.string.permission_denied)
            }
        }
    }

    private fun triggerUpdate() {
        binding.tvStatus.text = getString(R.string.updating_weather)
        binding.tvWeatherResult.text = ""

        lifecycleScope.launch {
            val location = LocationHelper.getLastKnownLocation(this@MainActivity)
                ?: LocationHelper.requestSingleUpdate(this@MainActivity)

            if (location == null) {
                binding.tvStatus.text = "Could not get location"
                return@launch
            }

            val lat = location.latitude
            val lon = location.longitude
            binding.tvStatus.text = "Got location: %.4f, %.4f".format(lat, lon)

            val cityName = LocationHelper.getCityName(this@MainActivity, lat, lon)
            LocationHelper.saveLocation(this@MainActivity, lat, lon, cityName)

            binding.tvStatus.text = "Fetching weather for $cityName…"

            val forecast = WeatherRepository().getForecast(lat, lon)
            if (forecast == null) {
                binding.tvStatus.text = "Weather fetch failed — check network"
                return@launch
            }

            binding.tvStatus.text = "Weather Widget — $cityName"
            binding.tvWeatherResult.text = forecast.days.joinToString("\n") { day ->
                "%-6s  %3d° / %3d°".format(
                    day.date.takeLast(5),
                    day.maxTempC.toInt(),
                    day.minTempC.toInt()
                )
            }

            val manager = AppWidgetManager.getInstance(this@MainActivity)
            val ids = WeatherWidgetProvider.getAllWidgetIds(this@MainActivity)
            binding.tvStatus.text = "Weather Widget — $cityName (widget IDs: ${ids.toList()})"

            if (ids.isNotEmpty()) {
                // First: send ACTION_APPWIDGET_UPDATE from our process so the launcher
                // treats the widget as properly initialised (onUpdate path).
                sendBroadcast(Intent(this@MainActivity, WeatherWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                })
                // Then push the actual weather views directly.
                val views = WeatherWidgetProvider.buildViews(this@MainActivity, cityName, forecast.days)
                manager.updateAppWidget(ids, views)
            }

            // Also queue a background worker so periodic updates keep working.
            WorkManager.getInstance(this@MainActivity)
                .enqueue(OneTimeWorkRequestBuilder<WeatherUpdateWorker>().build())
        }
    }
}
