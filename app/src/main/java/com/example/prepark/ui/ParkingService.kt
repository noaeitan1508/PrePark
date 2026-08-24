package com.example.prepark

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.Locale

class ParkingService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0
    private var totalOriginalTimeInMillis: Long = 0
    private val hourlyRate = 20.0 // מחיר לשעה לחישוב ההחזר

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "ParkingServiceChannel"

    companion object {
        var isServiceRunning = false
        var currentTimeString = "00:00:00"
        var hasFutureReservation = false
        var futureReservationMillis: Long = 0
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_PARKING" -> {
                isServiceRunning = true
                hasFutureReservation = false
                timeLeftInMillis = intent.getLongExtra("TIME_MILLIS", 7200000)
                totalOriginalTimeInMillis = timeLeftInMillis

                val notification = createNotification("החניה הופעלה, סע בזהירות!")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                startTimer()
            }
            "EXTEND_TIME" -> {
                val minutesToAdd = intent.getLongExtra("MINUTES_TO_ADD", 0)
                val millisToAdd = minutesToAdd * 60 * 1000
                timeLeftInMillis += millisToAdd
                totalOriginalTimeInMillis += millisToAdd

                countDownTimer?.cancel()
                startTimer()
            }
            "STOP_PARKING_EARLY" -> {
                calculateRefundAndStop()
            }
        }
        return START_STICKY
    }

    private fun calculateRefundAndStop() {
        // אם נשארה יותר מחצי שעה, מגיע ללקוח החזר
        if (timeLeftInMillis >= 1800000) {
            val refundProportion = timeLeftInMillis.toDouble() / totalOriginalTimeInMillis.toDouble()
            val refundAmount = refundProportion * (totalOriginalTimeInMillis / 3600000.0) * hourlyRate

            val refundIntent = Intent("REFUND_ISSUED")
            refundIntent.setPackage(packageName)
            refundIntent.putExtra("REFUND_AMOUNT", String.format("%.2f", refundAmount))
            sendBroadcast(refundIntent)
        }
        stopSelf()
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                val hours = (timeLeftInMillis / 1000) / 3600
                val minutes = ((timeLeftInMillis / 1000) % 3600) / 60
                val seconds = (timeLeftInMillis / 1000) % 60

                currentTimeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                updateNotification(currentTimeString)

                val broadcastIntent = Intent("TIMER_UPDATED")
                broadcastIntent.setPackage(packageName)
                broadcastIntent.putExtra("TIME_LEFT", currentTimeString)
                sendBroadcast(broadcastIntent)
            }

            override fun onFinish() {
                isServiceRunning = false
                updateNotification("הזמן נגמר!")
                val stopIntent = Intent("TIMER_STOPPED")
                stopIntent.setPackage(packageName)
                sendBroadcast(stopIntent)
                stopSelf()
            }
        }.start()
    }

    private fun createNotification(timeText: String): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PrePark - חניה פעילה")
            .setContentText("זמן נותר: $timeText")
            .setSmallIcon(R.drawable.prepark_logo)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(timeText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(timeText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Parking Channel", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        countDownTimer?.cancel()
        val stopIntent = Intent("TIMER_STOPPED")
        stopIntent.setPackage(packageName)
        sendBroadcast(stopIntent)
    }
}