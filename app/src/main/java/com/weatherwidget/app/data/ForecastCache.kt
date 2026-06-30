package com.weatherwidget.app.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

object ForecastCache {

    private const val PREFS = "forecast_cache"
    private const val KEY_FORECAST = "forecast_json"
    private const val KEY_CITY = "city_name"

    fun save(context: Context, cityName: String, forecast: WeatherForecast) {
        try {
            val json = HttpClient.json.encodeToString(forecast)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_FORECAST, json)
                .putString(KEY_CITY, cityName)
                .apply()
        } catch (_: Exception) {}
    }

    fun load(context: Context): Pair<String, WeatherForecast>? {
        return try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_FORECAST, null) ?: return null
            val city = prefs.getString(KEY_CITY, null) ?: return null
            val forecast = HttpClient.json.decodeFromString<WeatherForecast>(json)
            Pair(city, forecast)
        } catch (_: Exception) {
            null
        }
    }
}
