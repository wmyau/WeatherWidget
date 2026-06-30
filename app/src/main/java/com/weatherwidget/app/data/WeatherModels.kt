package com.weatherwidget.app.data

data class DayForecast(
    val date: String,
    val weatherCode: Int,
    val maxTempC: Double,
    val minTempC: Double
)

data class WeatherForecast(
    val latitude: Double,
    val longitude: Double,
    val days: List<DayForecast>
)

// Open-Meteo JSON response shape
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val daily: DailyData
)

data class DailyData(
    val time: List<String>,
    val weathercode: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>
)
