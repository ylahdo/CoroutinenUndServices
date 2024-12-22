package com.example.jetpackcompose.api

import android.util.Log
import com.example.jetpackcompose.data.ForecastData
import com.example.jetpackcompose.data.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Die WeatherApiService-Objekt stellt die API-Methoden für das Abrufen von Wetter- und Vorhersagedaten zur Verfügung.
 * Es verwendet Retrofit, um mit der OpenWeatherMap API zu kommunizieren und gibt die abgerufenen Daten zurück.
 * Alle Netzwerkanfragen werden im Hintergrund ausgeführt, um eine Blockierung des UI-Threads zu vermeiden.
 */
object WeatherApiService {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    // OkHttpClient für den HTTP-Verkehr
    private val client = OkHttpClient.Builder().build()

    // Retrofit-Instanz zur API-Kommunikation
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL) // Basis-URL der API
        .client(client) // OkHttpClient für Netzwerkanfragen
        .addConverterFactory(GsonConverterFactory.create()) // Umwandlung der API-Antworten in Kotlin-Datenobjekte
        .build()

    // Erstellen der API-Schnittstelle
    private val api = retrofit.create(WeatherApi::class.java)

    /**
     * Die WeatherApi-Schnittstelle definiert die API-Endpunkte für das Abrufen von Wetter- und Vorhersagedaten.
     */
    interface WeatherApi {
        /**
         * Ruft aktuelle Wetterdaten für eine gegebene Stadt ab.
         * @param city Name der Stadt, für die das Wetter abgefragt wird.
         * @param apiKey API-Schlüssel für die Authentifizierung bei der OpenWeatherMap API.
         * @param units Einheitensystem für die Wetterdaten (Standard: "metric").
         * @return Eine Antwort mit den Wetterdaten.
         */
        @GET("weather")
        suspend fun fetchWeather(
            @Query("q") city: String,
            @Query("appid") apiKey: String,
            @Query("units") units: String = "metric"
        ): retrofit2.Response<WeatherData>

        /**
         * Ruft Wettervorhersagedaten für eine gegebene Stadt ab.
         * @param city Name der Stadt, für die die Wettervorhersage abgefragt wird.
         * @param apiKey API-Schlüssel für die Authentifizierung bei der OpenWeatherMap API.
         * @param units Einheitensystem für die Wettervorhersagedaten (Standard: "metric").
         * @return Eine Antwort mit den Vorhersagedaten.
         */
        @GET("forecast")
        suspend fun fetchForecast(
            @Query("q") city: String,
            @Query("appid") apiKey: String,
            @Query("units") units: String = "metric"
        ): retrofit2.Response<ForecastData>
    }

    /**
     * Holt die aktuellen Wetterdaten für eine gegebene Stadt.
     * @param city Name der Stadt, für die die Wetterdaten abgefragt werden.
     * @param apiKey API-Schlüssel für die Authentifizierung bei der OpenWeatherMap API.
     * @return Die Wetterdaten der Stadt oder null, wenn ein Fehler auftritt.
     */
    suspend fun fetchWeather(city: String, apiKey: String): WeatherData? {
        return try {
            withContext(Dispatchers.Default) {
                val response = api.fetchWeather(city, apiKey)
                if (response.isSuccessful) {
                    response.body() // Gibt die Wetterdaten zurück, wenn die Anfrage erfolgreich war
                } else {
                    Log.e("WeatherApiService", "Failed to fetch data: ${response.code()}")
                    null // Gibt null zurück, wenn die Anfrage nicht erfolgreich war
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherApiService", "Error fetching data: ${e.message}")
            null // Gibt null zurück, wenn ein Fehler auftritt
        }
    }

    /**
     * Holt die Wettervorhersage für eine gegebene Stadt.
     * @param city Name der Stadt, für die die Vorhersage abgefragt wird.
     * @param apiKey API-Schlüssel für die Authentifizierung bei der OpenWeatherMap API.
     * @return Die Vorhersagedaten der Stadt oder null, wenn ein Fehler auftritt.
     */
    suspend fun fetchForecast(city: String, apiKey: String): ForecastData? {
        return try {
            withContext(Dispatchers.IO) { // Verwende Dispatchers.IO für I/O-intensive Operationen
                val response = api.fetchForecast(city, apiKey)
                if (response.isSuccessful) {
                    response.body() // Gibt die Vorhersagedaten zurück, wenn die Anfrage erfolgreich war
                } else {
                    Log.e("WeatherApiService", "Failed to fetch forecast: ${response.code()}")
                    null // Gibt null zurück, wenn die Anfrage nicht erfolgreich war
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherApiService", "Error fetching forecast: ${e.message}")
            null // Gibt null zurück, wenn ein Fehler auftritt
        }
    }
}
