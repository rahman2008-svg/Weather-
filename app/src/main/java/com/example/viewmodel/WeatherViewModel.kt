package com.example.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(val weather: WeatherResponse, val isFromCache: Boolean) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

data class CityPreset(val name: String, val latitude: Double, val longitude: Double)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WeatherDatabase.getDatabase(application)
    private val repository = WeatherRepository(db.weatherDao())

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _locationName = MutableStateFlow("Dhaka, Bangladesh")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    val presets = listOf(
        CityPreset("Dhaka", 23.8103, 90.4125),
        CityPreset("New York", 40.7128, -74.0060),
        CityPreset("London", 51.5074, -0.1278),
        CityPreset("Tokyo", 35.6762, 139.6503),
        CityPreset("Paris", 48.8566, 2.3522),
        CityPreset("Sydney", -33.8688, 151.2093)
    )

    init {
        // Automatically fetch room cache first
        viewModelScope.launch {
            val cached = repository.cache.firstOrNull()
            if (cached != null) {
                val response = cached.toResponse()
                _locationName.value = "Last Known Location"
                _uiState.value = WeatherUiState.Success(response, isFromCache = true)
            } else {
                // If cache is empty, load a preset immediately so the app is immediately working
                fetchWeather(23.8103, 90.4125, "Dhaka")
            }
        }
    }

    private fun WeatherCache.toResponse(): WeatherResponse {
        return WeatherResponse(
            latitude = latitude,
            longitude = longitude,
            current = CurrentWeather(
                time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(lastUpdated)),
                temperature = currentTemperature,
                weatherCode = currentWeatherCode,
                windSpeed = currentWindSpeed
            ),
            daily = DailyForecast(
                time = dailyTimes.split(",").filter { it.isNotEmpty() },
                tempMax = dailyTempMax.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toDoubleOrNull() },
                tempMin = dailyTempMin.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toDoubleOrNull() },
                weatherCode = dailyWeatherCodes.split(",").filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
            )
        )
    }

    @SuppressLint("MissingPermission")
    fun fetchWeatherWithGPS() {
        if (!isLocationEnabled()) {
            fetchWeather(23.8103, 90.4125, "Dhaka (Fallback)")
            return
        }

        _uiState.value = WeatherUiState.Loading
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    fetchWeather(location.latitude, location.longitude, "Current GPS Location")
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            fetchWeather(lastLoc.latitude, lastLoc.longitude, "Current GPS Location")
                        } else {
                            fetchWeather(23.8103, 90.4125, "Dhaka (GPS Unavailable)")
                        }
                    }.addOnFailureListener {
                        fetchWeather(23.8103, 90.4125, "Dhaka (GPS Failed)")
                    }
                }
            }.addOnFailureListener {
                fetchWeather(23.8103, 90.4125, "Dhaka (GPS Failed)")
            }
        } catch (e: SecurityException) {
            _uiState.value = WeatherUiState.Error("Location permission denied.")
            // Try loading default
            fetchWeather(23.8103, 90.4125, "Dhaka (Permission Denied)")
        }
    }

    fun fetchWeather(latitude: Double, longitude: Double, name: String) {
        _locationName.value = name
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.refreshWeather(latitude, longitude)
            result.onSuccess { response ->
                _uiState.value = WeatherUiState.Success(response, isFromCache = false)
            }.onFailure { error ->
                val cached = repository.cache.firstOrNull()
                if (cached != null) {
                    _uiState.value = WeatherUiState.Success(cached.toResponse(), isFromCache = true)
                } else {
                    _uiState.value = WeatherUiState.Error(error.localizedMessage ?: "Unknown Network Error")
                }
            }
            _isRefreshing.value = false
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun isOnline(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
