package com.weatherwidget.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import okhttp3.Request

interface IWeatherRepository {
    suspend fun getForecast(latitude: Double, longitude: Double): WeatherForecast?
}

class WeatherRepository : IWeatherRepository {

    override suspend fun getForecast(latitude: Double, longitude: Double): WeatherForecast? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude" +
                    "&longitude=$longitude" +
                    "&daily=weathercode,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,windspeed_10m_max,uv_index_max" +
                    "&timezone=auto" +
                    "&forecast_days=7"

                val request = Request.Builder().url(url).build()
                HttpClient.okHttp.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string() ?: return@withContext null
                    parseResponse(latitude, longitude, body)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parseResponse(lat: Double, lon: Double, jsonString: String): WeatherForecast? {
        return try {
            val resp = HttpClient.json.decodeFromString<OpenMeteoResponse>(jsonString)
            val days = resp.daily.time.mapIndexed { i, date ->
                DayForecast(
                    date = date,
                    weatherCode = resp.daily.weathercode.getOrElse(i) { 0 },
                    maxTempC = resp.daily.temperature_2m_max.getOrElse(i) { 0.0 },
                    minTempC = resp.daily.temperature_2m_min.getOrElse(i) { 0.0 },
                    precipitationMm = resp.daily.precipitation_sum?.getOrElse(i) { 0.0 } ?: 0.0,
                    precipitationProbability = resp.daily.precipitation_probability_max?.getOrElse(i) { 0 } ?: 0,
                    windspeedKph = resp.daily.windspeed_10m_max?.getOrElse(i) { 0.0 } ?: 0.0,
                    uvIndexMax = resp.daily.uv_index_max?.getOrElse(i) { 0.0 } ?: 0.0
                )
            }
            WeatherForecast(lat, lon, days)
        } catch (_: Exception) {
            null
        }
    }
}
