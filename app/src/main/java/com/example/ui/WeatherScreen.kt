package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WeatherResponse
import com.example.viewmodel.CityPreset
import com.example.viewmodel.WeatherUiState
import com.example.viewmodel.WeatherViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.*

// Sophisticated Dark Color Palette
val SophisticatedBg = Color(0xFF1A1C1E)
val SophisticatedSurface = Color(0xFF2D3135)
val SophisticatedContainer = Color(0xFF384954)
val SophisticatedPrimary = Color(0xFFD1E4FF)
val SophisticatedText = Color(0xFFE2E2E6)
val SophisticatedMuted = Color(0xFFC2C7CF)
val SophisticatedSecondary = Color(0xFF90959E)
val SophisticatedBorder = Color(0xFF43474E)
val GlowBlue = Color(0xFF004A77)

data class WeatherInfo(
    val description: String,
    val icon: ImageVector,
    val colorBrush: Brush,
    val glowColor: Color
)

fun getWeatherDetails(code: Int): WeatherInfo {
    val clearBrush = Brush.linearGradient(listOf(Color(0xFFFDC830), Color(0xFFF37335)))
    val cloudBrush = Brush.linearGradient(listOf(Color(0xFF5B86E5), Color(0xFF36D1DC)))
    val rainBrush = Brush.linearGradient(listOf(Color(0xFF2B32B2), Color(0xFF1488CC)))
    val snowBrush = Brush.linearGradient(listOf(Color(0xFF83a4d4), Color(0xFFb6fbff)))
    val stormBrush = Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
    val fogBrush = Brush.linearGradient(listOf(Color(0xFF757F9A), Color(0xFFD7DDE8)))

    return when (code) {
        0 -> WeatherInfo("Clear Sky", Icons.Filled.WbSunny, clearBrush, Color(0xFFF37335))
        1, 2, 3 -> WeatherInfo("Partly Cloudy", Icons.Filled.Cloud, cloudBrush, Color(0xFF5B86E5))
        45, 48 -> WeatherInfo("Foggy", Icons.Filled.Grain, fogBrush, Color(0xFF757F9A))
        51, 53, 55 -> WeatherInfo("Drizzle", Icons.Filled.WaterDrop, rainBrush, Color(0xFF1488CC))
        56, 57 -> WeatherInfo("Freezing Drizzle", Icons.Filled.AcUnit, snowBrush, Color(0xFF83a4d4))
        61, 63, 65 -> WeatherInfo("Rainy", Icons.Filled.Thunderstorm, rainBrush, Color(0xFF1488CC))
        66, 67 -> WeatherInfo("Freezing Rain", Icons.Filled.AcUnit, snowBrush, Color(0xFF83a4d4))
        71, 73, 75 -> WeatherInfo("Snowy", Icons.Filled.AcUnit, snowBrush, Color(0xFFb6fbff))
        77 -> WeatherInfo("Snow Grains", Icons.Filled.AcUnit, snowBrush, Color(0xFFb6fbff))
        80, 81, 82 -> WeatherInfo("Rain Showers", Icons.Filled.Thunderstorm, rainBrush, Color(0xFF1488CC))
        85, 86 -> WeatherInfo("Snow Showers", Icons.Filled.AcUnit, snowBrush, Color(0xFF83a4d4))
        95, 96, 99 -> WeatherInfo("Thunderstorm", Icons.Filled.Thunderstorm, stormBrush, Color(0xFF203A43))
        else -> WeatherInfo("Partly Cloudy", Icons.Filled.Cloud, cloudBrush, Color(0xFF5B86E5))
    }
}

fun formatDayOfWeek(dateStr: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = format.parse(dateStr) ?: return dateStr
        val dayFormat = SimpleDateFormat("EEEE", Locale.US)
        dayFormat.format(date)
    } catch (e: Exception) {
        dateStr
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Screen selection (simulation for bottom nav)
    var selectedScreen by remember { mutableStateOf("home") }

    // Set up location permission state
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Automatically trigger GPS fetch when permission is granted
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.fetchWeatherWithGPS()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBg)
            .testTag("weather_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.WbSunny,
                            contentDescription = "Weather Icon Logo",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "NexVora",
                            fontWeight = FontWeight.ExtraBold,
                            color = SophisticatedText,
                            modifier = Modifier.testTag("app_title"),
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    // Badge details
                    val isCached = when (val state = uiState) {
                        is WeatherUiState.Success -> state.isFromCache
                        else -> false
                    }
                    if (isCached) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Cached Stats", fontSize = 11.sp, color = SophisticatedMuted) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.CloudOff,
                                    contentDescription = "Offline Cache indicator",
                                    tint = SophisticatedMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SophisticatedSurface,
                                labelColor = SophisticatedMuted
                            ),
                            border = null
                        )
                    } else if (uiState is WeatherUiState.Success) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Live API", fontSize = 11.sp, color = SophisticatedPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Wifi,
                                    contentDescription = "Live Network indicator",
                                    tint = SophisticatedPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SophisticatedSurface,
                                labelColor = SophisticatedPrimary
                            ),
                            border = null
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (locationPermissionsState.allPermissionsGranted) {
                                viewModel.fetchWeatherWithGPS()
                            } else {
                                locationPermissionsState.launchMultiplePermissionRequest()
                            }
                        },
                        modifier = Modifier
                            .testTag("gps_button")
                            .minimumInteractiveComponentSize(),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = SophisticatedSurface,
                            contentColor = SophisticatedPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "GPS Auto Geolocate current weather stats"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SophisticatedBg
                )
            )
        },
        bottomBar = {
            // Sophisticated Bottom Navigation simulation exactly following the premium design parameters
            Column(modifier = Modifier.background(SophisticatedBg)) {
                HorizontalDivider(color = SophisticatedBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home Screen Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (selectedScreen == "home") SophisticatedContainer else Color.Transparent)
                            .clickable { selectedScreen = "home" }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Home Weather Navigation",
                            tint = if (selectedScreen == "home") SophisticatedPrimary else SophisticatedMuted
                        )
                        AnimatedVisibility(visible = selectedScreen == "home") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Home",
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Preset search triggering button
                    IconButton(
                        onClick = {
                            selectedScreen = "search"
                            androidx.compose.material3.SnackbarDuration.Short
                            coroutineScope.run {
                                // Simple toast indicator feedback
                            }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search Preset Locations Nav",
                            tint = if (selectedScreen == "search") SophisticatedPrimary else SophisticatedMuted
                        )
                    }

                    // Settings mockup button
                    IconButton(
                        onClick = {
                            selectedScreen = "settings"
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings Configuration Panel Nav",
                            tint = if (selectedScreen == "settings") SophisticatedPrimary else SophisticatedMuted
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SophisticatedBg)
        ) {
            if (selectedScreen == "settings") {
                // Sleek settings and config description page
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Settings & Config",
                            color = SophisticatedText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    "App Settings",
                                    fontWeight = FontWeight.Bold,
                                    color = SophisticatedPrimary,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Unit Format: Metric (°C, km/h)",
                                    fontSize = 13.sp,
                                    color = SophisticatedText
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Geolocator Priority: Power Balanced Accuracy",
                                    fontSize = 13.sp,
                                    color = SophisticatedText
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Theme: Sophisticated Dark Theme (Always ON)",
                                    fontSize = 13.sp,
                                    color = SophisticatedText
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { selectedScreen = "home" },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Dashboard")
                        }
                    }
                }
            } else if (selectedScreen == "search") {
                // Dynamic Search / Preset Select Area
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        "Select Location",
                        color = SophisticatedText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Pick a city preset instantly. The app will fetch real-time Open-Meteo statistics asynchronously.",
                        fontSize = 13.sp,
                        color = SophisticatedMuted
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(viewModel.presets) { city ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.fetchWeather(city.latitude, city.longitude, city.name)
                                        selectedScreen = "home"
                                    },
                                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                               ) {
                                    Column {
                                        Text(city.name, fontWeight = FontWeight.Bold, color = SophisticatedText)
                                        Text("Lat: ${city.latitude} | Lon: ${city.longitude}", fontSize = 11.sp, color = SophisticatedSecondary)
                                    }
                                    Icon(Icons.Filled.ChevronRight, "select city", tint = SophisticatedPrimary)
                                }
                            }
                        }
                    }
                }
            } else {
                // Preset city navigation bar (Material Chips)
                PresetCitiesRow(
                    presets = viewModel.presets,
                    selectedName = locationName,
                    onCitySelected = { preset ->
                        viewModel.fetchWeather(preset.latitude, preset.longitude, preset.name)
                    }
                )

                // Dynamic Animated Content based on UI State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (val state = uiState) {
                        is WeatherUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.testTag("loading_indicator"),
                                    color = SophisticatedPrimary
                                )
                            }
                        }
                        is WeatherUiState.Success -> {
                            WeatherContent(
                                weather = state.weather,
                                locationName = locationName,
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    val currentResponse = state.weather
                                    viewModel.fetchWeather(
                                        currentResponse.latitude,
                                        currentResponse.longitude,
                                        locationName
                                    )
                                }
                            )
                        }
                        is WeatherUiState.Error -> {
                            ErrorState(
                                message = state.message,
                                onRequestPermission = {
                                    locationPermissionsState.launchMultiplePermissionRequest()
                                },
                                onTryDhaka = {
                                    viewModel.fetchWeather(23.8103, 90.4125, "Dhaka")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PresetCitiesRow(
    presets: List<CityPreset>,
    selectedName: String,
    onCitySelected: (CityPreset) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preset_cities_row")
    ) {
        items(presets) { city ->
            val isSelected = selectedName == city.name
            FilterChip(
                selected = isSelected,
                onClick = { onCitySelected(city) },
                label = { Text(city.name, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SophisticatedSurface,
                    selectedLabelColor = SophisticatedPrimary,
                    containerColor = Color.Transparent,
                    labelColor = SophisticatedMuted
                ),
                border = null,
                modifier = Modifier
                    .testTag("preset_${city.name.lowercase()}")
                    .minimumInteractiveComponentSize()
            )
        }
    }
}

@Composable
fun WeatherContent(
    weather: WeatherResponse,
    locationName: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val current = weather.current
    val daily = weather.daily

    val currentTemp = current?.temperature ?: 0.0
    val windSpeed = current?.windSpeed ?: 0.0
    val weatherCode = current?.weatherCode ?: 0
    val details = getWeatherDetails(weatherCode)

    // Format Current Date
    val sdfDate = SimpleDateFormat("EEEE, MMM dd", Locale.US)
    val formattedDate = sdfDate.format(Date())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("weather_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Minimal High-Fidelity Header Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Geolocated GPS coordinates details pin",
                        tint = SophisticatedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = locationName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedText,
                        modifier = Modifier.testTag("location_title")
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    fontSize = 13.sp,
                    color = SophisticatedMuted
                )
            }
        }

        // Hero Weather Icon Centered with Custom Radial Ambient Blur Glow
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_weather_card"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .drawBehind {
                            // High end radial glowing backdrop behind weather icons creating exceptional visual depth
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(details.glowColor.copy(alpha = 0.4f), Color.Transparent),
                                    radius = size.width / 1.5f
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = details.icon,
                        contentDescription = details.description,
                        tint = if (weatherCode == 0) Color(0xFFFFD54F) else SophisticatedPrimary,
                        modifier = Modifier
                            .size(105.dp)
                            .testTag("weather_main_icon")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Centered large minimal temperature text
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(start = 16.dp) // Offset degree character positioning nicely
                ) {
                    Text(
                        text = "${currentTemp.toInt()}",
                        fontSize = 86.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = SophisticatedText,
                        letterSpacing = (-4).sp,
                        modifier = Modifier.testTag("current_temp")
                    )
                    Text(
                        text = "°",
                        fontSize = 54.sp,
                        color = SophisticatedPrimary,
                        fontWeight = FontWeight.Light
                    )
                }

                Text(
                    text = details.description,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = SophisticatedMuted,
                    modifier = Modifier.testTag("weather_description")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Fetch Daily H / L Temperatures
                val firstDayMax = daily?.tempMax?.firstOrNull()?.toInt() ?: 24
                val firstDayMin = daily?.tempMin?.firstOrNull()?.toInt() ?: 16
                Text(
                    text = "H: ${firstDayMax}°   L: ${firstDayMin}°",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedSecondary
                )
            }
        }

        // Bento Grid Metrics Row block (Wind, Coordinates/Source)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Box 1 (Wind Box)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bento_wind_box"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Air,
                                contentDescription = "Wind indicator vector",
                                tint = SophisticatedMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "WIND",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedMuted,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${windSpeed.toInt()}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "km/h",
                                fontSize = 13.sp,
                                color = SophisticatedSecondary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }

                // Box 2 (GPS coordinates and system caching metadata Bento box)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("bento_gps_box"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Update,
                                contentDescription = "GPS Accuracy tracker icon details",
                                tint = SophisticatedMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "LOCATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedMuted,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Column {
                            Text(
                                text = "Lat: ${String.format(Locale.US, "%.1f", weather.latitude)}°",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedText
                            )
                            Text(
                                text = "Lon: ${String.format(Locale.US, "%.1f", weather.longitude)}°",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedText
                            )
                        }
                    }
                }
            }
        }

        // Daily Forecast styled as a single nested Card with Background `#384954` or `#2D3135`
        if (daily != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forecast_strip_container"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SophisticatedContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = "7 day Calendar Forecast Header",
                                tint = SophisticatedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "7-DAY FORECAST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SophisticatedPrimary,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.testTag("forecast_title")
                            )
                        }

                        // Compact forecasts loop without nested scrolls
                        val size = daily.time.size
                        (0 until size).forEach { index ->
                            val date = daily.time[index]
                            val maxTemp = daily.tempMax.getOrNull(index) ?: 0.0
                            val minTemp = daily.tempMin.getOrNull(index) ?: 0.0
                            val dCode = daily.weatherCode?.getOrNull(index) ?: 0
                            val dayDetails = getWeatherDetails(dCode)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .testTag("forecast_item_$index"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(
                                        text = formatDayOfWeek(date),
                                        fontWeight = FontWeight.Bold,
                                        color = SophisticatedText,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = date,
                                        fontSize = 11.sp,
                                        color = SophisticatedMuted
                                    )
                                }

                                Row(
                                    modifier = Modifier.weight(2f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = dayDetails.icon,
                                        contentDescription = dayDetails.description,
                                        tint = if (dCode == 0) Color(0xFFFFD54F) else SophisticatedPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dayDetails.description,
                                        fontSize = 13.sp,
                                        color = SophisticatedMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "${maxTemp.toInt()}° / ${minTemp.toInt()}°",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SophisticatedText,
                                    modifier = Modifier.weight(1.2f),
                                    textAlign = TextAlign.End,
                                    fontSize = 14.sp
                                )
                            }

                            if (index < size - 1) {
                                HorizontalDivider(color = SophisticatedBg.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }

        // Developer Info Credit Badge Section Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("developer_credit_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SophisticatedSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(SophisticatedBorder, Color.Transparent))
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Developer info credit icon badge",
                            tint = SophisticatedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Credits & App Information",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SophisticatedPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Developed with ❤️ by Prince AR Abdur Rahman, an Independent App Developer passionate about building modern Android applications, productivity tools, and AI-powered experiences.",
                        fontSize = 12.sp,
                        color = SophisticatedMuted,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Published by: NexVora Lab's Ofc\nMission: Build fast, beautiful, privacy-friendly, and user-focused applications accessible to everyone.",
                        fontSize = 12.sp,
                        color = SophisticatedMuted,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = SophisticatedBorder.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Products: NexPlay X, LifeSphere OS, Smart Day Planner X",
                                fontSize = 10.sp,
                                color = SophisticatedSecondary,
                                style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            )
                            Text(
                                text = "© 2026 NexVora Lab's Ofc. All Rights Reserved.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SophisticatedPrimary
                            )
                        }
                        Text(
                            text = "v1.0.0",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = SophisticatedSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRequestPermission: () -> Unit,
    onTryDhaka: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("error_state_container"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = "Error cloud connection icon indicator",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! Weather Load Failed",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = SophisticatedMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = SophisticatedPrimary, contentColor = SophisticatedBg),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("allow_location_button")
        ) {
            Icon(Icons.Filled.MyLocation, "Location permission launcher")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grant Location Permission")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onTryDhaka,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SophisticatedPrimary),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.linearGradient(listOf(SophisticatedPrimary, SophisticatedBorder))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("fallback_dhaka_button")
        ) {
            Text("Show Dhaka Weather (No GPS Needed)")
        }
    }
}
