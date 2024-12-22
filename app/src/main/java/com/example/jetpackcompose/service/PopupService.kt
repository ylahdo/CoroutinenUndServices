package com.example.jetpackcompose.service

import android.app.*
import android.content.*
import android.os.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent
import androidx.core.content.ContextCompat
import com.example.jetpackcompose.MainActivity
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.jetpackcompose.ui.views.dataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Der PopupService ist ein Service, der im Hintergrund läuft und regelmäßig Benachrichtigungen anzeigt.
 * Die Häufigkeit der Benachrichtigungen wird durch die in den Einstellungen gespeicherte Timer-Option bestimmt.
 * Der Service startet im Vordergrund, um sicherzustellen, dass er auch bei Einschränkungen im Hintergrund weiterläuft.
 */
class PopupService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var delayMillis: Long = -1L
    private var i = 0
    private val dataStore by lazy { applicationContext.dataStore }
    private var isNotificationEnabled: Boolean = false

    // Empfänger für die Aktualisierung der Timer-Option
    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val newTimerOption = intent?.getStringExtra("timer_option") ?: "Deactivated"
            updateTimerOption(newTimerOption)
        }
    }

    /**
     * Wird beim Start des Services aufgerufen.
     * Initialisiert den Timer und registriert den BroadcastReceiver für Updates.
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel() // Erstellt den Notification Channel für Benachrichtigungen
        startForegroundService() // Startet den Service im Vordergrund
        registerUpdateReceiver() // Registriert den BroadcastReceiver für Timer-Option-Updates
        initializeTimerFromSettings() // Initialisiert den Timer basierend auf den gespeicherten Einstellungen
    }

    /**
     * Wird beim Zerstören des Services aufgerufen.
     * Entfernt den Handler und meldet den Receiver ab.
     */
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(showNotificationRunnable)
        unregisterReceiver(updateReceiver)
    }

    /**
     * Wird beim Starten des Services aufgerufen.
     * Wenn ein Timer-Delay gesetzt ist, wird der Timer erneut gestartet.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (delayMillis != -1L) {
            handler.removeCallbacks(showNotificationRunnable)
            handler.post(showNotificationRunnable)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Runnable, das regelmäßig die Benachrichtigung anzeigt
    private val showNotificationRunnable = object : Runnable {
        override fun run() {
            if (isNotificationEnabled) {
                sendNotification("Hello World $i") // Sendet eine Benachrichtigung
                i++
            }
            handler.postDelayed(this, delayMillis) // Wiederholt das Runnable nach dem festgelegten Intervall
        }
    }

    /**
     * Aktualisiert die Timer-Option und startet oder stoppt den Timer basierend auf der Option.
     */
    private fun updateTimerOption(option: String) {
        delayMillis = timerOptionToMillis(option) // Umwandlung der Option in Millisekunden
        isNotificationEnabled = delayMillis != -1L
        handler.removeCallbacks(showNotificationRunnable)

        // Wenn der Timer deaktiviert wurde, stoppe den Service
        if (delayMillis == -1L) {
            stopSelf()
        } else {
            handler.postDelayed(showNotificationRunnable, delayMillis)
        }
    }

    /**
     * Holt die Timer-Option aus den gespeicherten Einstellungen.
     */
    private suspend fun fetchTimerOptionFromSettings(): String {
        val key = stringPreferencesKey("timer_option_key")
        val timerOption = dataStore.data.map { preferences ->
            preferences[key] ?: "Deactivated"
        }.first()

        return timerOption
    }

    /**
     * Registriert einen Receiver für Updates der Timer-Option.
     */
    private fun registerUpdateReceiver() {
        ContextCompat.registerReceiver(
            this,
            updateReceiver,
            IntentFilter("com.example.jetpackcompose.UPDATE_TIMER"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /**
     * Konvertiert die Timer-Option in Millisekunden.
     */
    private fun timerOptionToMillis(option: String): Long {
        return when (option) {
            "10s" -> 10_000L
            "30s" -> 30_000L
            "60s" -> 60_000L
            "30 min" -> 30 * 60 * 1000L
            "60 min" -> 60 * 60 * 1000L
            else -> -1L
        }
    }

    /**
     * Initialisiert den Timer basierend auf der gespeicherten Timer-Option.
     */
    private fun initializeTimerFromSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            val timerOption = fetchTimerOptionFromSettings()
            delayMillis = timerOptionToMillis(timerOption)

            if (delayMillis != -1L) {
                isNotificationEnabled = true
                handler.post(showNotificationRunnable)
            }
        }
    }

    /**
     * Startet den Service im Vordergrund mit einer Benachrichtigung.
     */
    private fun startForegroundService() {
        val notification = getForegroundNotification()
        startForeground(1, notification)
    }

    /**
     * Erstellt die Benachrichtigungs-Channel für den Service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "popup_service_channel",
                "Popup Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from Popup Service"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Sendet eine Benachrichtigung mit einer angegebenen Nachricht.
     */
    private fun sendNotification(message: String) {
        if (ActivityCompat.checkSelfPermission(
                this@PopupService,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(this)
        val notification = getNotification(message)
        notificationManager.notify(1, notification)
    }

    /**
     * Erstellt eine Benachrichtigung mit dem angegebenen Text.
     */
    private fun getNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "popup_service_channel")
            .setContentTitle("Popup Service")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    /**
     * Holt die Benachrichtigungs-Channel für den Vordergrund-Notification.
     */
    private fun getForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "popup_service_channel")
            .setContentTitle("Popup Service Running")
            .setContentText("The service is running in the background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
