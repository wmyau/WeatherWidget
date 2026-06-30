package com.weatherwidget.app.worker

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.weatherwidget.app.R
import com.weatherwidget.app.WeatherWidgetProvider
import com.weatherwidget.app.data.WeatherRepository
import com.weatherwidget.app.utils.LocationHelper

class WeatherUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = WeatherRepository()

    override suspend fun doWork(): Result {
        if (!LocationHelper.hasLocationPermission(context)) {
            pushViews(WeatherWidgetProvider.showError(context, context.getString(R.string.no_location_permission)))
            return Result.failure()
        }

        // Prefer the SharedPreferences cache (written when app is in foreground).
        // Fall back to system last-known location (network/passive providers only —
        // GPS is blocked in background on Android 10+ without ACCESS_BACKGROUND_LOCATION).
        // If neither is available, ask the system for a fresh fix (works in foreground).
        val cached = LocationHelper.getCachedLocation(context)
        val lat: Double
        val lon: Double
        val cityName: String

        if (cached != null) {
            lat = cached.first
            lon = cached.second
            cityName = cached.third
        } else {
            val location = LocationHelper.getLastKnownLocation(context)
                ?: LocationHelper.requestSingleUpdate(context)
            if (location == null) {
                pushViews(WeatherWidgetProvider.showError(context, context.getString(R.string.location_unavailable)))
                return Result.retry()
            }
            lat = location.latitude
            lon = location.longitude
            cityName = LocationHelper.getCityName(context, lat, lon)
            LocationHelper.saveLocation(context, lat, lon, cityName)
        }

        val forecast = repository.getForecast(lat, lon)
        if (forecast == null) {
            pushViews(WeatherWidgetProvider.showError(context, context.getString(R.string.weather_unavailable)))
            return Result.retry()
        }

        pushViews(WeatherWidgetProvider.buildViews(context, cityName, forecast.days))
        return Result.success()
    }

    private fun pushViews(views: android.widget.RemoteViews) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = WeatherWidgetProvider.getAllWidgetIds(context)
        if (ids.isNotEmpty()) {
            manager.updateAppWidget(ids, views)
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "WeatherPeriodicUpdate"
    }
}
