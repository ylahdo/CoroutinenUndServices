package com.example.jetpackcompose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpackcompose.api.WeatherApiService
import com.example.jetpackcompose.data.ForecastItem
import com.example.jetpackcompose.data.WeatherData
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel für die Wetterdaten der Anwendung.
 * Verwaltet den Zustand der aktuellen Wetterdaten, der Wettervorhersage und Fehlernachrichten.
 */
class WeatherViewModel : ViewModel() {

    // MutableStateFlow für die aktuellen Wetterdaten, initial null
    private val _currentWeather = MutableStateFlow<WeatherData?>(null)

    /**
     * StateFlow für die aktuellen Wetterdaten.
     * Wird von der UI verwendet, um Änderungen an den aktuellen Wetterdaten zu beobachten.
     */
    val currentWeather: StateFlow<WeatherData?> = _currentWeather

    // MutableStateFlow für die Wettervorhersage, initial eine leere Liste
    private val _forecast = MutableStateFlow<List<ForecastItem>>(emptyList())

    /**
     * StateFlow für die Wettervorhersage.
     * Wird von der UI verwendet, um Änderungen an der Wettervorhersage zu beobachten.
     */
    val forecast: StateFlow<List<ForecastItem>> = _forecast

    // MutableStateFlow für die URL des Wetter-Icons, initial null
    private val _iconUrl = MutableStateFlow<String?>(null)

    /**
     * StateFlow für die URL des Wetter-Icons.
     * Wird von der UI verwendet, um das Wetter-Icon zu laden und darzustellen.
     */
    val iconUrl: StateFlow<String?> get() = _iconUrl

    // MutableStateFlow für die Fehlermeldung, initial null
    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * StateFlow für die Fehlermeldung.
     * Wird von der UI verwendet, um Fehlernachrichten anzuzeigen.
     */
    val errorMessage: StateFlow<String?> get() = _errorMessage

    /**
     * Lädt die aktuellen Wetterdaten für eine angegebene Stadt unter Verwendung eines API-Schlüssels.
     * Wenn die Daten erfolgreich abgerufen wurden, werden sie im ViewModel gespeichert.
     * Andernfalls wird eine Fehlermeldung gesetzt.
     *
     * @param city Der Name der Stadt, für die das Wetter abgerufen werden soll.
     * @param apiKey Der API-Schlüssel für den Wetterdienst.
     */
    fun fetchWeatherData(city: String, apiKey: String) {
        viewModelScope.launch {
            try {
                val weatherResponse = WeatherApiService.fetchWeather(city, apiKey)
                if (weatherResponse != null) {
                    _currentWeather.value = weatherResponse
                    // Wetter-Icon URL abrufen
                    fetchWeatherIcon(weatherResponse.weather.firstOrNull()?.icon.orEmpty())
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Failed to fetch weather. Please check your API key or city name."
                }
            } catch (e: Exception) {
                _errorMessage.value = "An error occurred: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Lädt die Wettervorhersagedaten für eine angegebene Stadt unter Verwendung eines API-Schlüssels.
     * Wenn die Vorhersagedaten erfolgreich abgerufen wurden, werden sie im ViewModel gespeichert.
     * Andernfalls wird eine Fehlermeldung gesetzt.
     *
     * @param city Der Name der Stadt, für die die Wettervorhersage abgerufen werden soll.
     * @param apiKey Der API-Schlüssel für den Wetterdienst.
     */
    fun fetchForecastData(city: String, apiKey: String) {
        viewModelScope.launch {
            try {
                val forecastResponse = WeatherApiService.fetchForecast(city, apiKey)
                if (forecastResponse != null) {
                    _forecast.value = forecastResponse.list
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Failed to fetch forecast. Please check your API key or city name."
                }
            } catch (e: Exception) {
                _errorMessage.value = "An error occurred while fetching the forecast: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Lädt die URL für das Wetter-Icon basierend auf der übergebenen Icon-ID.
     * Wenn die Icon-ID nicht leer ist, wird die URL im StateFlow gesetzt.
     *
     * @param iconId Die Icon-ID, die vom Wetterdienst zurückgegeben wird.
     */
    private fun fetchWeatherIcon(iconId: String) {
        if (iconId.isNotEmpty()) {
            // Setzt die URL für das Wetter-Icon
            _iconUrl.value = "https://openweathermap.org/img/wn/$iconId@2x.png"
        }
    }
}
