package com.weatherwidget.app

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.weatherwidget.app.data.ForecastCache
import com.weatherwidget.app.data.IWeatherRepository
import com.weatherwidget.app.data.WeatherForecast
import com.weatherwidget.app.data.WeatherRepository
import com.weatherwidget.app.utils.LocationData
import com.weatherwidget.app.utils.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(app: Application) : AndroidViewModel(app) {

    // Assigned to the interface so tests can swap in a fake without a custom factory
    private val repository: IWeatherRepository = WeatherRepository()

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Idle)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    fun loadWeather() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            _uiState.value = WeatherUiState.Loading

            val location = resolveLocation(context)
            if (location == null) {
                val cached = ForecastCache.load(context)
                _uiState.value = if (cached != null) {
                    WeatherUiState.Success(cached.first, cached.second.days, fromCache = true)
                } else {
                    WeatherUiState.Error(
                        if (LocationHelper.isManualLocationSet(context) || LocationHelper.hasLocationPermission(context))
                            R.string.location_unavailable
                        else
                            R.string.no_location_permission
                    )
                }
                return@launch
            }

            val forecast = repository.getForecast(location.lat, location.lon)
            if (forecast == null) {
                val cached = ForecastCache.load(context)
                _uiState.value = if (cached != null) {
                    WeatherUiState.Success(cached.first, cached.second.days,
                        isManual = LocationHelper.isManualLocationSet(context),
                        fromCache = true)
                } else {
                    WeatherUiState.Error(R.string.weather_unavailable)
                }
                return@launch
            }

            ForecastCache.save(context, location.city, forecast)
            _uiState.value = WeatherUiState.Success(
                cityName = location.city,
                days = forecast.days,
                isManual = LocationHelper.isManualLocationSet(context)
            )
            pushToWidget(forecast, location.city)
        }
    }

    fun searchCity(query: String) {
        viewModelScope.launch {
            _searchState.value = SearchState.Searching
            val results = LocationHelper.searchCity(query)
            _searchState.value = if (results.isEmpty()) {
                SearchState.NotFound(query)
            } else {
                SearchState.Found(results.first())
            }
        }
    }

    fun confirmSearch(result: LocationHelper.GeoResult) {
        val context = getApplication<Application>()
        LocationHelper.setManualLocation(context, result.lat, result.lon, result.name)
        _searchState.value = SearchState.Idle
        loadWeather()
    }

    fun clearManualLocation() {
        val context = getApplication<Application>()
        LocationHelper.clearManualLocation(context)
        _searchState.value = SearchState.Idle
        loadWeather()
    }

    private suspend fun resolveLocation(context: android.content.Context): LocationData? {
        LocationHelper.getManualLocation(context)?.let { return it }
        if (!LocationHelper.hasLocationPermission(context)) return null
        LocationHelper.getCachedLocation(context)?.let { return it }
        val loc = LocationHelper.getLastKnownLocation(context)
            ?: LocationHelper.requestSingleUpdate(context)
            ?: return null
        val city = LocationHelper.getCityName(context, loc.latitude, loc.longitude)
        LocationHelper.saveLocation(context, loc.latitude, loc.longitude, city)
        return LocationData(loc.latitude, loc.longitude, city)
    }

    private fun pushToWidget(forecast: WeatherForecast, cityName: String) {
        val context = getApplication<Application>()
        val ids = WeatherWidgetProvider.getAllWidgetIds(context)
        if (ids.isEmpty()) return
        val views = WeatherWidgetProvider.buildViews(context, cityName, forecast.days)
        AppWidgetManager.getInstance(context).updateAppWidget(ids, views)
    }
}
