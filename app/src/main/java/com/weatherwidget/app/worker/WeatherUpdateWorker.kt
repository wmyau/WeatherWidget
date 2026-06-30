package com.weatherwidget.app.worker

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.weatherwidget.app.R
import com.weatherwidget.app.WeatherWidgetProvider
import com.weatherwidget.app.data.ForecastCache
import com.weatherwidget.app.data.WeatherRepository
import com.weatherwidget.app.utils.LocationHelper

class WeatherUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val lat: Double
        val lon: Double
        val cityName: String

        val manual = LocationHelper.getManualLocation(context)
        if (manual != null) {
            lat = manual.lat
            lon = manual.lon
            cityName = manual.city
        } else {
            if (!LocationHelper.hasLocationPermission(context)) {
                pushViews(WeatherWidgetProvider.showError(context, context.getString(R.string.no_location_permission)))
                return Result.failure()
            }
            val cached = LocationHelper.getCachedLocation(context)
            if (cached != null) {
                lat = cached.lat
                lon = cached.lon
                cityName = cached.city
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
        }

        val forecast = WeatherRepository().getForecast(lat, lon)
        if (forecast == null) {
            // Keep the widget showing last good data rather than an error screen
            val cached = ForecastCache.load(context)
            if (cached != null) {
                pushViews(WeatherWidgetProvider.buildViews(context, cached.first, cached.second.days))
            } else {
                pushViews(WeatherWidgetProvider.showError(context, context.getString(R.string.weather_unavailable)))
            }
            return Result.retry()
        }

        ForecastCache.save(context, cityName, forecast)
        pushViews(WeatherWidgetProvider.buildViews(context, cityName, forecast.days))
        return Result.success()
    }

    private fun pushViews(views: android.widget.RemoteViews) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = WeatherWidgetProvider.getAllWidgetIds(context)
        if (ids.isNotEmpty()) manager.updateAppWidget(ids, views)
    }

    companion object {
        const val PERIODIC_WORK_NAME = "WeatherPeriodicUpdate"
    }
}
