package com.cmc.taximeter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MeterService : Service() {

    private val CHANNEL_ID = "MeterServiceChannel"
    private val END_CHANNEL_ID = "MeterEndChannel"
    private val NOTIFICATION_ID = 1
    private val END_NOTIFICATION_ID = 2

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            "START_METER" -> {
                startForegroundService()
            }
            "END_COURSE" -> {
                val distance = intent.getStringExtra("distance") ?: "0.00"
                val time = intent.getStringExtra("time") ?: "00:00"
                val total = intent.getStringExtra("total") ?: "0.00"
                showEndCourseNotification(distance, time, total)
                stopForeground(true)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.meter_running))
            .setContentText(getString(R.string.meter_running_message))
            .setSmallIcon(R.drawable.ic_speedometer)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun showEndCourseNotification(distance: String, time: String, total: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val summaryText = getString(R.string.course_summary, distance, time, total)

        val notification = NotificationCompat.Builder(this, END_CHANNEL_ID)
            .setContentTitle(getString(R.string.course_finished))
            .setContentText(summaryText)
            .setSmallIcon(R.drawable.ic_verified)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(END_NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal pour le service en cours
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Meter Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )

            // Canal pour la notification de fin de course
            val endChannel = NotificationChannel(
                END_CHANNEL_ID,
                "Course Finished Channel",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(endChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }
}