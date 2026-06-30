package com.weatherwidget.app.utils

import com.weatherwidget.app.R

object WeatherIconMapper {

    fun getIconRes(wmoCode: Int): Int = when (wmoCode) {
        0, 1       -> R.drawable.ic_weather_clear_day
        2          -> R.drawable.ic_weather_partly_cloudy_day
        3          -> R.drawable.ic_weather_cloudy
        in 45..48  -> R.drawable.ic_weather_fog
        in 51..57  -> R.drawable.ic_weather_drizzle
        in 61..67  -> R.drawable.ic_weather_rain
        in 71..77  -> R.drawable.ic_weather_snow
        in 80..82  -> R.drawable.ic_weather_rain
        in 85..86  -> R.drawable.ic_weather_snow
        in 95..99  -> R.drawable.ic_weather_thunderstorm
        else       -> R.drawable.ic_weather_cloudy
    }

    fun getIconEmoji(wmoCode: Int): String = when (wmoCode) {
        0, 1       -> "☀"   // ☀ clear
        2          -> "⛅"   // ⛅ partly cloudy
        3          -> "☁"   // ☁ cloudy
        in 45..48  -> "☁"   // ☁ fog
        in 51..57  -> "☂"   // ☂ drizzle
        in 61..67  -> "☂"   // ☂ rain
        in 71..77  -> "❄"   // ❄ snow
        in 80..82  -> "☂"   // ☂ showers
        in 85..86  -> "❄"   // ❄ snow showers
        in 95..99  -> "⚡"   // ⚡ thunderstorm
        else       -> "☁"   // ☁
    }

    fun getDescription(wmoCode: Int): String = when (wmoCode) {
        0       -> "Clear sky"
        1       -> "Mainly clear"
        2       -> "Partly cloudy"
        3       -> "Overcast"
        in 45..48 -> "Fog"
        51, 53  -> "Light drizzle"
        55      -> "Dense drizzle"
        56, 57  -> "Freezing drizzle"
        61, 63  -> "Light rain"
        65      -> "Heavy rain"
        66, 67  -> "Freezing rain"
        71, 73  -> "Light snow"
        75      -> "Heavy snow"
        77      -> "Snow grains"
        80, 81  -> "Rain showers"
        82      -> "Heavy showers"
        85, 86  -> "Snow showers"
        95      -> "Thunderstorm"
        96, 99  -> "Thunderstorm with hail"
        else    -> "Unknown"
    }
}
