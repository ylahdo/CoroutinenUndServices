package com.example.jetpackcompose.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.jetpackcompose.data.ForecastItem
import com.example.jetpackcompose.storage.Keys
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jetpackcompose.viewmodel.WeatherViewModel
import com.example.jetpackcompose.ui.components.SearchBarSample
import com.example.jetpackcompose.ui.components.WeatherCard

/**
 * Composable für die Anzeige der Wettervorhersage.
 * Diese Ansicht zeigt die Wettervorhersage für eine bestimmte Stadt an und ermöglicht das Suchen nach Städten.
 * Außerdem wird eine Fehlermeldung angezeigt, wenn beim Abrufen der Wetterdaten ein Fehler auftritt.
 *
 * @param forecast Eine Liste von `ForecastItem`, die die Wettervorhersage für die kommenden Tage darstellt.
 */
@Composable
fun ForecastWeatherView(forecast: List<ForecastItem>) {
    // Lokaler Kontext für den Zugriff auf die DataStore-Daten
    val context = LocalContext.current

    // State-Variablen für Heimatstadt und API-Schlüssel
    var hometown by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    // ViewModel für den Zugriff auf Wetterdaten
    val weatherViewModel: WeatherViewModel = viewModel()
    val errorMessage by weatherViewModel.errorMessage.collectAsState()

    // Hole Heimatstadt und API-Schlüssel aus dem DataStore
    LaunchedEffect(Unit) {
        context.dataStore.data.collect { preferences ->
            hometown = preferences[Keys.HOMETOWN_KEY] ?: ""
            apiKey = preferences[Keys.API_TOKEN_KEY] ?: ""

            // Wenn Heimatstadt und API-Schlüssel gesetzt sind, lade die Wettervorhersage
            if (hometown.isNotEmpty() && apiKey.isNotEmpty()) {
                weatherViewModel.fetchForecastData(hometown, apiKey)
            }
        }
    }

    // State für die Suchabfrage der Stadt
    val searchQuery = rememberSaveable { mutableStateOf("") }

    // Box für die Suchleiste
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Komponente für die Suchleiste
        SearchBarSample(
            selectedMenu = "Forecast",
            apiKey = apiKey,
            onQueryChanged = { query ->
                searchQuery.value = query
                if (query.isNotEmpty()) {
                    weatherViewModel.fetchForecastData(query, apiKey)
                } else {
                    // Wenn keine Suche eingegeben wurde, lade die Vorhersage für die Heimatstadt
                    if (hometown.isNotEmpty() && apiKey.isNotEmpty()) {
                        weatherViewModel.fetchForecastData(hometown, apiKey)
                    }
                }
            }
        )
    }

    // Anzeige von Fehlermeldungen, falls vorhanden
    errorMessage?.let {
        Text(
            text = it,
            color = Color.Red,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 25.sp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }

    // Container für die Wettervorhersage
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Wenn keine Heimatstadt oder Suchabfrage gesetzt wurde
        if (searchQuery.value.isEmpty() && hometown.isEmpty()) {
            Text(
                text = "Set your hometown in settings",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 24.sp,
                    color = Color.Gray
                ),
                modifier = Modifier.padding(16.dp)
            )
        } else if (forecast.isNotEmpty()) {
            // Anzeige der Stadtname und der Vorhersage-Titel
            Text(
                text = "Forecast for ${searchQuery.value.takeIf { it.isNotEmpty() } ?: hometown}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    color = Color.Black
                ),
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // Anzeige der Wettervorhersage
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(forecast) { forecastItem ->
                    WeatherCard(
                        forecastItem = forecastItem
                    )
                }
            }
        }
    }
}
