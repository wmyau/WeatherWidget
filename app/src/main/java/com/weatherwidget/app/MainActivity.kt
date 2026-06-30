package com.weatherwidget.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.weatherwidget.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: WeatherViewModel by viewModels()

    companion object {
        private const val REQ_LOCATION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvForecast.layoutManager = LinearLayoutManager(this)
        setupListeners()
        observeViewModel()

        if (viewModel.uiState.value is WeatherUiState.Idle) {
            viewModel.loadWeather()
        }
    }

    private fun setupListeners() {
        binding.btnRefresh.setOnClickListener { viewModel.loadWeather() }
        binding.btnGrantPermission.setOnClickListener { requestLocationPermission() }
        binding.btnSearch.setOnClickListener { doSearch() }
        binding.etCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }
        binding.btnUseSearched.setOnClickListener {
            (viewModel.searchState.value as? SearchState.Found)?.let {
                viewModel.confirmSearch(it.result)
            }
        }
        binding.btnUsePhone.setOnClickListener { viewModel.clearManualLocation() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::renderWeatherState) }
                launch { viewModel.searchState.collect(::renderSearchState) }
            }
        }
    }

    private fun renderWeatherState(state: WeatherUiState) {
        when (state) {
            is WeatherUiState.Idle -> {
                binding.tvStatus.text = ""
                binding.btnGrantPermission.visibility =
                    if (hasLocationPermission()) View.GONE else View.VISIBLE
                binding.btnUsePhone.visibility = View.GONE
            }
            is WeatherUiState.Loading -> {
                binding.tvStatus.text = getString(R.string.updating_weather)
                binding.rvForecast.adapter = null
            }
            is WeatherUiState.Success -> {
                binding.tvStatus.text = if (state.fromCache) {
                    getString(R.string.offline_data, state.cityName)
                } else {
                    state.cityName
                }
                binding.rvForecast.adapter = DayForecastAdapter(state.days)
                binding.btnUsePhone.visibility = if (state.isManual) View.VISIBLE else View.GONE
                binding.btnGrantPermission.visibility =
                    if (state.isManual || hasLocationPermission()) View.GONE else View.VISIBLE
            }
            is WeatherUiState.Error -> {
                binding.tvStatus.text = getString(state.messageRes)
                binding.rvForecast.adapter = null
                binding.btnGrantPermission.visibility =
                    if (hasLocationPermission()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun renderSearchState(state: SearchState) {
        when (state) {
            is SearchState.Idle -> {
                binding.tvSearchResult.text = ""
                binding.btnUseSearched.visibility = View.GONE
            }
            is SearchState.Searching -> {
                binding.tvSearchResult.text = getString(R.string.searching)
                binding.btnUseSearched.visibility = View.GONE
            }
            is SearchState.Found -> {
                binding.tvSearchResult.text = state.result.displayName
                binding.btnUseSearched.text = getString(R.string.use_city, state.result.name)
                binding.btnUseSearched.visibility = View.VISIBLE
            }
            is SearchState.NotFound -> {
                binding.tvSearchResult.text = getString(R.string.no_results, state.query)
                binding.btnUseSearched.visibility = View.GONE
            }
        }
    }

    private fun doSearch() {
        val query = binding.etCity.text.toString().trim()
        if (query.isEmpty()) return
        hideKeyboard()
        viewModel.searchCity(query)
    }

    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            REQ_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadWeather()
        }
    }

    private fun hideKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.etCity.windowToken, 0)
    }
}
