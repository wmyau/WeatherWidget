package com.weatherwidget.app.data

import kotlinx.serialization.Serializable

@Serializable
data class DayForecast(
    val date: String,
    val weatherCode: Int,
    val maxTempC: Double,
    val minTempC: Double,
    val precipitationMm: Double,
    val precipitationProbability: Int,
    val windspeedKph: Double,
    val uvIndexMax: Double
)

@Serializable
data class WeatherForecast(
    val latitude: Double,
    val longitude: Double,
    val days: List<DayForecast>
)

// Open-Meteo API response shape — optional fields use default null so absent keys are safe
@Serializable
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val daily: DailyData
)

@Serializable
data class DailyData(
    val time: List<String>,
    val weathercode: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_sum: List<Double>? = null,
    val precipitation_probability_max: List<Int>? = null,
    val windspeed_10m_max: List<Double>? = null,
    val uv_index_max: List<Double>? = null
)
