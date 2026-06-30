package com.weatherwidget.app

import androidx.annotation.StringRes
import com.weatherwidget.app.data.DayForecast
import com.weatherwidget.app.utils.LocationHelper

sealed interface WeatherUiState {
    object Idle : WeatherUiState
    object Loading : WeatherUiState
    data class Success(
        val cityName: String,
        val days: List<DayForecast>,
        val isManual: Boolean = false,
        val fromCache: Boolean = false
    ) : WeatherUiState
    data class Error(@StringRes val messageRes: Int) : WeatherUiState
}

sealed interface SearchState {
    object Idle : SearchState
    object Searching : SearchState
    data class Found(val result: LocationHelper.GeoResult) : SearchState
    data class NotFound(val query: String) : SearchState
}
