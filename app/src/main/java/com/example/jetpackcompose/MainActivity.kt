package com.example.jetpackcompose

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jetpackcompose.viewmodel.WeatherViewModel
import com.example.jetpackcompose.ui.WeatherApp
import com.example.jetpackcompose.viewmodel.PopupServiceManager

/**
 * MainActivity is the entry point of the application.
 * It initializes the popup service based on the Android version
 * and sets the content of the app using Jetpack Compose.
 */
class MainActivity : ComponentActivity() {

    // Instance of PopupServiceManager to manage the popup service
    private val popupServiceManager = PopupServiceManager(this)

    /**
     * Called when the activity is created.
     * This method sets the content view using Jetpack Compose
     * and handles the popup service based on the Android version.
     *
     * @param savedInstanceState The saved instance state of the activity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the popup service initialization based on the SDK version
        handlePopupService()

        // Set the content of the app using WeatherApp Composable
        setContent {
            // Get the WeatherViewModel instance
            val viewModel: WeatherViewModel = viewModel()
            // Render the WeatherApp Composable
            WeatherApp(viewModel)
        }
    }

    /**
     * Handles the popup service initialization.
     * It requests permission for Android versions Tiramisu (SDK 33) and above,
     * or starts the popup service for older versions.
     */
    private fun handlePopupService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Request permission for popup service on SDK 33 and above
            popupServiceManager.requestPermission()
        } else {
            // Start the popup service for older Android versions
            popupServiceManager.startPopupService()
        }
    }
}
