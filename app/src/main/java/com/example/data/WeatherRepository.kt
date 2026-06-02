package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WeatherRepository(
    private val weatherDao: WeatherDao,
    private val weatherService: WeatherService = RetrofitClient.weatherService
) {
    // Expose cached weather from Database
    val cache: Flow<WeatherCache?> = weatherDao.getWeatherCache()

    // Refresh weather from API and store inside database cash
    suspend fun refreshWeather(lat: Double, lon: Double): Result<WeatherResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = weatherService.getForecast(latitude = lat, longitude = lon)
                
                // Construct and insert cache record
                val current = response.current
                val daily = response.daily
                
                if (current != null && daily != null) {
                    val weatherCache = WeatherCache(
                        id = 1,
                        latitude = response.latitude,
                        longitude = response.longitude,
                        lastUpdated = System.currentTimeMillis(),
                        currentTemperature = current.temperature,
                        currentWeatherCode = current.weatherCode,
                        currentWindSpeed = current.windSpeed,
                        dailyTimes = daily.time.joinToString(","),
                        dailyTempMax = daily.tempMax.joinToString(","),
                        dailyTempMin = daily.tempMin.joinToString(","),
                        dailyWeatherCodes = daily.weatherCode?.joinToString(",") ?: ""
                    )
                    weatherDao.insertCache(weatherCache)
                }
                
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
