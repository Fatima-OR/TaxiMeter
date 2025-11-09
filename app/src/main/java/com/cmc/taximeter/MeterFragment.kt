package com.cmc.taximeter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import java.util.*

class MeterFragment : Fragment() {

    private lateinit var tvTotalAPayer: TextView
    private lateinit var tvTempsEcoule: TextView
    private lateinit var tvDistance: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnRestart: Button

    private lateinit var sharedPreferences: android.content.SharedPreferences
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var isRunning = false
    private var isPaused = false
    private var startTime: Long = 0
    private var pausedTime: Long = 0
    private var elapsedTime: Long = 0
    private var totalDistance: Float = 0f
    private var lastLocation: Location? = null

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning && !isPaused) {
                updateTimer()
                updateTotal()
                handler.postDelayed(this, 1000)
            }
        }
    }

    // Tarifs marocains (en Dirhams)
    private val TARIF_BASE = 2.5 // 2.5 Dhs de prise en charge
    private val TARIF_PAR_KM = 1.5 // 1.5 Dhs par km
    private val TARIF_PAR_MINUTE = 0.5 // 0.5 Dhs par minute

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else {
            true
        }

        if (locationGranted && notificationGranted) {
            startMeter()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.permissions_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_meter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("MeterPreferences", Context.MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        initializeViews(view)
        setupLocationCallback()
        loadSavedState()
        setupButtons()

        if (isRunning) {
            updateUIForRunningState()
            if (!isPaused) {
                handler.post(updateRunnable)
                startLocationUpdates()
            }
        }
    }

    private fun initializeViews(view: View) {
        tvTotalAPayer = view.findViewById(R.id.tvTotalAPayer)
        tvTempsEcoule = view.findViewById(R.id.tvTempsEcoule)
        tvDistance = view.findViewById(R.id.tvDistance)
        btnStart = view.findViewById(R.id.btnStart)
        btnStop = view.findViewById(R.id.btnStop)
        btnRestart = view.findViewById(R.id.btnRestart)
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateDistance(location)
                    updateTotal() // Mise à jour en temps réel
                }
            }
        }
    }

    private fun setupButtons() {
        btnStart.setOnClickListener {
            if (!isRunning) {
                checkPermissionsAndStart()
            } else if (isPaused) {
                resumeMeter()
            }
        }

        btnStop.setOnClickListener {
            if (isRunning && !isPaused) {
                pauseMeter()
            }
        }

        btnRestart.setOnClickListener {
            resetMeter()
        }
    }

    private fun checkPermissionsAndStart() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startMeter()
        }
    }

    private fun startMeter() {
        isRunning = true
        isPaused = false
        startTime = System.currentTimeMillis() - elapsedTime

        updateUIForRunningState()
        handler.post(updateRunnable)
        startLocationUpdates()
        startForegroundService()
        saveState()

        Toast.makeText(requireContext(), getString(R.string.course_started), Toast.LENGTH_SHORT).show()
    }

    private fun pauseMeter() {
        isPaused = true
        pausedTime = System.currentTimeMillis()

        handler.removeCallbacks(updateRunnable)
        stopLocationUpdates()

        btnStart.text = getString(R.string.resume)
        btnStart.isEnabled = true

        saveState()
        sendEndCourseNotification()
        stopForegroundService()

        Toast.makeText(requireContext(), getString(R.string.course_stopped), Toast.LENGTH_SHORT).show()
    }

    private fun resumeMeter() {
        isPaused = false
        val pauseDuration = System.currentTimeMillis() - pausedTime
        startTime += pauseDuration

        btnStart.text = getString(R.string.start)

        handler.post(updateRunnable)
        startLocationUpdates()
        startForegroundService()
        saveState()

        Toast.makeText(requireContext(), getString(R.string.course_resumed), Toast.LENGTH_SHORT).show()
    }

    private fun resetMeter() {
        isRunning = false
        isPaused = false
        elapsedTime = 0
        totalDistance = 0f
        lastLocation = null

        tvTotalAPayer.text = "00.00"
        tvTempsEcoule.text = "00:00"
        tvDistance.text = "00.00"

        btnStart.text = getString(R.string.start)
        btnStart.isEnabled = true
        btnStop.isEnabled = false

        handler.removeCallbacks(updateRunnable)
        stopLocationUpdates()
        stopForegroundService()
        saveState()

        Toast.makeText(requireContext(), getString(R.string.meter_reset), Toast.LENGTH_SHORT).show()
    }

    private fun updateUIForRunningState() {
        btnStart.text = if (isPaused) getString(R.string.resume) else getString(R.string.start)
        btnStart.isEnabled = isPaused
        btnStop.isEnabled = !isPaused
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000 // Mise à jour toutes les 3 secondes
            ).apply {
                setMinUpdateIntervalMillis(1000)
                setWaitForAccurateLocation(false)
            }.build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateDistance(newLocation: Location) {
        lastLocation?.let { last ->
            val distance = last.distanceTo(newLocation)
            if (distance < 200 && distance > 0) {
                totalDistance += distance
                tvDistance.text = String.format(Locale.getDefault(), "%.2f", totalDistance /1000)
                saveState()
            }
        }
        lastLocation = newLocation
    }

    private fun updateTimer() {
        elapsedTime = System.currentTimeMillis() - startTime
        val seconds = (elapsedTime / 1000).toInt()
        val minutes = seconds / 60
        val secs = seconds % 60

        tvTempsEcoule.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }

    private fun updateTotal() {
        val distanceKm = totalDistance / 1000
        val minutes = (elapsedTime / 60000).toDouble()

        // Calcul du tarif total en temps réel
        val total = TARIF_BASE + (distanceKm * TARIF_PAR_KM) + (minutes * TARIF_PAR_MINUTE)

        tvTotalAPayer.text = String.format(Locale.getDefault(), "%.2f", total)
    }

    private fun startForegroundService() {
        val intent = Intent(requireContext(), MeterService::class.java)
        intent.action = "START_METER"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun stopForegroundService() {
        val intent = Intent(requireContext(), MeterService::class.java)
        requireContext().stopService(intent)
    }

    private fun sendEndCourseNotification() {
        val intent = Intent(requireContext(), MeterService::class.java)
        intent.action = "END_COURSE"
        intent.putExtra("distance", tvDistance.text.toString())
        intent.putExtra("time", tvTempsEcoule.text.toString())
        intent.putExtra("total", tvTotalAPayer.text.toString())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun saveState() {
        sharedPreferences.edit().apply {
            putBoolean("isRunning", isRunning)
            putBoolean("isPaused", isPaused)
            putLong("startTime", startTime)
            putLong("elapsedTime", elapsedTime)
            putFloat("totalDistance", totalDistance)
            putString("total", tvTotalAPayer.text.toString())
            putString("time", tvTempsEcoule.text.toString())
            putString("distance", tvDistance.text.toString())
            apply()
        }
    }

    private fun loadSavedState() {
        isRunning = sharedPreferences.getBoolean("isRunning", false)
        isPaused = sharedPreferences.getBoolean("isPaused", false)
        startTime = sharedPreferences.getLong("startTime", 0)
        elapsedTime = sharedPreferences.getLong("elapsedTime", 0)
        totalDistance = sharedPreferences.getFloat("totalDistance", 0f)

        tvTotalAPayer.text = sharedPreferences.getString("total", "00.00")
        tvTempsEcoule.text = sharedPreferences.getString("time", "00:00")
        tvDistance.text = sharedPreferences.getString("distance", "00.00")
    }

    override fun onPause() {
        super.onPause()
        if (isRunning) {
            saveState()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSavedState()
        if (isRunning && !isPaused) {
            handler.post(updateRunnable)
            startLocationUpdates()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        stopLocationUpdates()
    }
}