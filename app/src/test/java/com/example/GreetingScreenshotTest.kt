package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.CurrentWeather
import com.example.data.DailyForecast
import com.example.data.WeatherResponse
import com.example.ui.WeatherContent
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val mockWeather = WeatherResponse(
        latitude = 23.8103,
        longitude = 90.4125,
        current = CurrentWeather(
            time = "2026-06-02T10:00",
            temperature = 28.5,
            weatherCode = 1,
            windSpeed = 12.4
        ),
        daily = DailyForecast(
            time = listOf("2026-06-02", "2026-06-03", "2026-06-04", "2026-06-05", "2026-06-06", "2026-06-07", "2026-06-08"),
            tempMax = listOf(32.0, 31.5, 33.0, 30.0, 29.5, 31.0, 32.5),
            tempMin = listOf(24.0, 23.5, 25.0, 22.0, 21.5, 23.0, 24.5),
            weatherCode = listOf(1, 0, 3, 61, 80, 0, 1)
        )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        WeatherContent(
            weather = mockWeather,
            locationName = "Dhaka Testing",
            isRefreshing = false,
            onRefresh = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
