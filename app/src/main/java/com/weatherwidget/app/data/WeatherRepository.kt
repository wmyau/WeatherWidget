package com.weatherwidget.app.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WeatherRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun getForecast(latitude: Double, longitude: Double): WeatherForecast? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude" +
                    "&longitude=$longitude" +
                    "&daily=weathercode,temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto" +
                    "&forecast_days=7"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    parseResponse(latitude, longitude, body)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseResponse(lat: Double, lon: Double, json: String): WeatherForecast? {
        return try {
            val resp = gson.fromJson(json, OpenMeteoResponse::class.java)
            val days = resp.daily.time.mapIndexed { i, date ->
                DayForecast(
                    date = date,
                    weatherCode = resp.daily.weathercode.getOrElse(i) { 0 },
                    maxTempC = resp.daily.temperature_2m_max.getOrElse(i) { 0.0 },
                    minTempC = resp.daily.temperature_2m_min.getOrElse(i) { 0.0 }
                )
            }
            WeatherForecast(lat, lon, days)
        } catch (e: Exception) {
            null
        }
    }
}
