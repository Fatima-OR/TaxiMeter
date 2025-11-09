package com.cmc.taximeter

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*

class TaximeterService : Service() {

    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sharedPreferences: android.content.SharedPreferences

    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var isRunning = false
    private var isPaused = false
    private var lastLocation: Location? = null
    private var totalDistance: Float = 0f

    companion object {
        const val ACTION_UPDATE_TIMER = "com.cmc.taximeter.UPDATE_TIMER"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_DISTANCE = "extra_distance"
        const val EXTRA_COST = "extra_cost"
        const val EXTRA_IS_RUNNING = "extra_is_running"

        const val CHANNEL_ID = "TaximeterChannel"
        const val NOTIFICATION_ID = 1

        private const val PREF_START_TIME = "startTime"
        private const val PREF_ELAPSED_TIME = "elapsedTime"
        private const val PREF_IS_RUNNING = "isRunning"
        private const val PREF_IS_PAUSED = "isPaused"
        private const val PREF_DISTANCE = "distance"
        private const val PREF_LAST_LAT = "lastLat"
        private const val PREF_LAST_LNG = "lastLng"

        // Tarifs par défaut
        const val DEFAULT_BASE_FARE = 2.5f
        const val DEFAULT_PRICE_PER_KM = 1.5f
        const val DEFAULT_PRICE_PER_MINUTE = 0.5f
    }

    inner class LocalBinder : Binder() {
        fun getService(): TaximeterService = this@TaximeterService
    }

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = getSharedPreferences("TaximeterPrefs", Context.MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        restoreState()
        setupLocationCallback()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification(0, 0f, 0.0))
        return START_STICKY
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                if (!isRunning || isPaused) return

                locationResult.lastLocation?.let { location ->
                    updateLocation(location)
                }
            }
        }
    }

    private fun updateLocation(newLocation: Location) {
        lastLocation?.let { last ->
            val distance = last.distanceTo(newLocation)
            // Filtrer les petites distances (< 5 mètres) pour éviter les erreurs GPS
            if (distance > 5) {
                totalDistance += distance
                saveState()
            }
        }

        lastLocation = newLocation
        elapsedTime = System.currentTimeMillis() - startTime

        broadcastUpdate()
        updateNotification()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Compteur Taxi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Affiche le compteur du taxi en cours"
                setSound(null, null)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(time: Long, distance: Float, cost: Double): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val hours = (time / 3600000).toInt()
        val minutes = ((time % 3600000) / 60000).toInt()
        val seconds = ((time % 60000) / 1000).toInt()

        val timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        val distanceText = String.format("%.2f km", distance / 1000)
        val costText = String.format("%.2f DH", cost)

        val statusText = if (isRunning && !isPaused) "En course" else if (isPaused) "En pause" else "Arrêté"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚕 $statusText")
            .setContentText("⏱️ $timeText | 📍 $distanceText | 💰 $costText")
            .setSmallIcon(R.drawable.ic_speedometer)
            .setContentIntent(pendingIntent)
            .setOngoing(isRunning)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("⏱️ Temps: $timeText\n📍 Distance: $distanceText\n💰 Total: $costText"))
            .build()
    }

    fun startTimer() {
        if (isRunning && !isPaused) return

        if (isPaused) {
            // Reprendre après une pause
            val pausedDuration = System.currentTimeMillis() - (startTime + elapsedTime)
            startTime = System.currentTimeMillis() - elapsedTime
            isPaused = false
        } else {
            // Nouveau démarrage
            startTime = System.currentTimeMillis()
            elapsedTime = 0
            totalDistance = 0f
            lastLocation = null
        }

        isRunning = true
        saveState()
        startLocationUpdates()
        broadcastUpdate()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 2000 // Mise à jour toutes les 2 secondes
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 5f // Minimum 5 mètres de déplacement
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    fun pauseTimer() {
        if (!isRunning || isPaused) return

        isPaused = true
        elapsedTime = System.currentTimeMillis() - startTime
        fusedLocationClient.removeLocationUpdates(locationCallback)
        saveState()
        broadcastUpdate()
        updateNotification()
    }

    fun stopTimer() {
        if (!isRunning) return

        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Calculer le coût final avant de réinitialiser
        val finalCost = calculateCost()

        // Envoyer notification de fin de course
        sendEndTripNotification(elapsedTime, totalDistance, finalCost)

        isRunning = false
        isPaused = false
        saveState()
        broadcastUpdate()
        updateNotification()
    }

    fun resetTimer() {
        stopTimer()
        startTime = 0
        elapsedTime = 0
        totalDistance = 0f
        lastLocation = null
        saveState()
        broadcastUpdate()
        updateNotification()
    }

    fun getElapsedTime(): Long = elapsedTime

    fun getDistance(): Float = totalDistance

    fun isTimerRunning(): Boolean = isRunning && !isPaused

    fun isTimerPaused(): Boolean = isPaused

    private fun calculateCost(): Double {
        val baseFare = sharedPreferences.getFloat("baseFare", DEFAULT_BASE_FARE).toDouble()
        val pricePerKm = sharedPreferences.getFloat("pricePerKm", DEFAULT_PRICE_PER_KM).toDouble()
        val pricePerMinute = sharedPreferences.getFloat("pricePerMinute", DEFAULT_PRICE_PER_MINUTE).toDouble()

        val distanceInKm = totalDistance / 1000.0
        val timeInMinutes = elapsedTime / 60000.0

        return baseFare + (distanceInKm * pricePerKm) + (timeInMinutes * pricePerMinute)
    }

    private fun broadcastUpdate() {
        val intent = Intent(ACTION_UPDATE_TIMER).apply {
            putExtra(EXTRA_TIME, elapsedTime)
            putExtra(EXTRA_DISTANCE, totalDistance)
            putExtra(EXTRA_COST, calculateCost())
            putExtra(EXTRA_IS_RUNNING, isRunning && !isPaused)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun updateNotification() {
        val notification = createNotification(elapsedTime, totalDistance, calculateCost())
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendEndTripNotification(time: Long, distance: Float, cost: Double) {
        val hours = (time / 3600000).toInt()
        val minutes = ((time % 3600000) / 60000).toInt()
        val seconds = ((time % 60000) / 1000).toInt()

        val timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        val distanceText = String.format("%.2f km", distance / 1000)
        val costText = String.format("%.2f DH", cost)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("✅ Course terminée")
            .setContentText("Total: $costText")
            .setSmallIcon(R.drawable.ic_speedometer)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("⏱️ Durée: $timeText\n📍 Distance: $distanceText\n💰 Total à payer: $costText"))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun saveState() {
        sharedPreferences.edit().apply {
            putLong(PREF_START_TIME, startTime)
            putLong(PREF_ELAPSED_TIME, elapsedTime)
            putBoolean(PREF_IS_RUNNING, isRunning)
            putBoolean(PREF_IS_PAUSED, isPaused)
            putFloat(PREF_DISTANCE, totalDistance)
            lastLocation?.let {
                putFloat(PREF_LAST_LAT, it.latitude.toFloat())
                putFloat(PREF_LAST_LNG, it.longitude.toFloat())
            }
            apply()
        }
    }

    private fun restoreState() {
        startTime = sharedPreferences.getLong(PREF_START_TIME, 0)
        elapsedTime = sharedPreferences.getLong(PREF_ELAPSED_TIME, 0)
        isRunning = sharedPreferences.getBoolean(PREF_IS_RUNNING, false)
        isPaused = sharedPreferences.getBoolean(PREF_IS_PAUSED, false)
        totalDistance = sharedPreferences.getFloat(PREF_DISTANCE, 0f)

        val lastLat = sharedPreferences.getFloat(PREF_LAST_LAT, 0f)
        val lastLng = sharedPreferences.getFloat(PREF_LAST_LNG, 0f)

        if (lastLat != 0f && lastLng != 0f) {
            lastLocation = Location("").apply {
                latitude = lastLat.toDouble()
                longitude = lastLng.toDouble()
            }
        }

        if (isRunning && !isPaused) {
            startLocationUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        saveState()
    }
}