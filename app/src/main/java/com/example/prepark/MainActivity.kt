package com.example.prepark

import android.app.AlarmManager
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity() {

    private lateinit var miniPlayerLayout: View
    private lateinit var tvMiniTimer: TextView
    private lateinit var navController: NavController

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateMiniPlayerTimer()
            timerHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        miniPlayerLayout = findViewById(R.id.miniPlayerLayout)
        tvMiniTimer = findViewById(R.id.tvMiniTimer)
        val btnCancelParking = findViewById<TextView>(R.id.btnCancelParking)
        val btnExtendParking = findViewById<TextView>(R.id.btnExtendParking)

        // שינוי לצבע הכחול הכהה (כמו פנגו)
        miniPlayerLayout.setBackgroundColor(Color.parseColor("#154C9A"))

        // --- 1. לוגיקה לכפתור הארכה ---
        btnExtendParking.setOnClickListener {
            val activePref = getSharedPreferences("ActiveBooking", Context.MODE_PRIVATE)
            val endTime = activePref.getLong("endTime", 0L)

            if (endTime != 0L) {
                val remainingMinutes = ((endTime - System.currentTimeMillis()) / (1000 * 60)).toInt()

                // אפשר להאריך רק אם נשארה חצי שעה או פחות
                if (remainingMinutes <= 30) {
                    val newEndTime = endTime + (30 * 60 * 1000) // הוספת 30 דקות (במילישניות)
                    activePref.edit().putLong("endTime", newEndTime).apply()
                    Toast.makeText(this, "החניה הוארכה ב-30 דקות ⏳", Toast.LENGTH_SHORT).show()

                    // עדכון התראות קיימות עם הזמן החדש
                    activePref.edit().putBoolean("alarmScheduled", false).apply()
                    updateMiniPlayerTimer()
                } else {
                    Toast.makeText(this, "ניתן להאריך חניה רק ב-30 הדקות האחרונות", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // --- 2. לוגיקה לכפתור ביטול (סימולציית תמחור דינמי ופנגו) ---
        btnCancelParking.setOnClickListener {
            val activePref = getSharedPreferences("ActiveBooking", Context.MODE_PRIVATE)
            val endTime = activePref.getLong("endTime", 0L)
            val remainingMinutes = ((endTime - System.currentTimeMillis()) / (1000 * 60)).toInt()

            if (remainingMinutes >= 30) {
                // הלקוח יצא מוקדם - יש זיכוי!
                val refundAmount = (remainingMinutes / 60.0) * 40.0 // תעריף דמה

                android.app.AlertDialog.Builder(this)
                    .setTitle("סיום חניה מוקדם")
                    .setMessage("יצאת $remainingMinutes דקות מוקדם יותר!\n\nעל פי התמחור הדינמי זוכית ב-₪${String.format("%.2f", refundAmount)}.\n\nהפרטים הועברו לשרתי פנגו (Pango API) לסליקה סופית.")
                    .setPositiveButton("אישור") { _, _ ->
                        cancelAlarm()
                        activePref.edit().clear().apply()
                        updateNotification(0L, true)
                        updateMiniPlayerTimer()
                        Toast.makeText(this, "חניה הסתיימה", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("חזור", null)
                    .show()
            } else {
                // הלקוח יצא רגיל - חיוב רגיל
                android.app.AlertDialog.Builder(this)
                    .setTitle("סיום חניה")
                    .setMessage("החניה תסתיים כעת והחיוב יועבר לשרתי פנגו.")
                    .setPositiveButton("אישור") { _, _ ->
                        cancelAlarm()
                        activePref.edit().clear().apply()
                        updateNotification(0L, true)
                        updateMiniPlayerTimer()
                        Toast.makeText(this, "חניה הסתיימה", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("חזור", null)
                    .show()
            }
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        timerHandler.post(timerRunnable)
    }

    private fun updateMiniPlayerTimer() {
        val activePref = getSharedPreferences("ActiveBooking", Context.MODE_PRIVATE)
        val isActivated = activePref.getBoolean("isActivated", false)
        val endTime = activePref.getLong("endTime", 0L)

        if (isActivated && endTime > 0L) {

            val alarmScheduled = activePref.getBoolean("alarmScheduled", false)
            if (!alarmScheduled) {
                scheduleAlarm(endTime)
                activePref.edit().putBoolean("alarmScheduled", true).apply()
            }

            val now = System.currentTimeMillis()
            if (now <= endTime) {
                miniPlayerLayout.visibility = View.VISIBLE
                val diff = endTime - now
                val seconds = (diff / 1000) % 60
                val minutes = (diff / (1000 * 60)) % 60
                val hours = (diff / (1000 * 60 * 60)) % 24

                val timeString = String.format("זמן חניה נותר: %02d:%02d:%02d", hours, minutes, seconds)
                tvMiniTimer.text = timeString
                tvMiniTimer.setTextColor(Color.WHITE)

                updateNotification(endTime, false)
            } else {
                miniPlayerLayout.visibility = View.GONE
                activePref.edit().clear().apply()
                updateNotification(0L, true)
                cancelAlarm()

                try {
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
                    ringtone.play()
                } catch (e: Exception) {}

                Toast.makeText(this, "זמן החניה שלך הסתיים!", Toast.LENGTH_LONG).show()
            }
        } else {
            miniPlayerLayout.visibility = View.GONE
            updateNotification(0L, true)
        }
    }

    private fun scheduleAlarm(endTime: Long) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, ParkingReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelAlarm() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, ParkingReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateNotification(endTime: Long, isFinished: Boolean) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "parking_timer_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "זמן חניה", NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            if (isFinished) {
                notificationManager.cancel(1)
                return
            }

            val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentTitle("זמן חניה נותר:")
                .setOngoing(true)
                .setOnlyAlertOnce(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setUsesChronometer(true)
                builder.setChronometerCountDown(true)
                builder.setWhen(endTime)
                builder.setContentText("PrePark - חניה פעילה")
            } else {
                builder.setContentText("PrePark - חניה פעילה")
            }

            notificationManager.notify(1, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showPersonalAreaDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_personal_area)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnCancelActive = dialog.findViewById<Button>(R.id.btnCancelActive)
        val btnActivateFuture = dialog.findViewById<Button>(R.id.btnActivateFuture)
        val btnParkingHistory = dialog.findViewById<Button>(R.id.btnParkingHistory)
        val btnCarDetails = dialog.findViewById<Button>(R.id.btnCarDetails)
        val btnSavedCredit = dialog.findViewById<Button>(R.id.btnSavedCredit)

        val btnCloseDialog = dialog.findViewById<ImageView>(R.id.btnCloseDialog)
        btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        val activePref = getSharedPreferences("ActiveBooking", Context.MODE_PRIVATE)
        val isActivated = activePref.getBoolean("isActivated", false)
        val endTime = activePref.getLong("endTime", 0L)
        val startTime = activePref.getLong("startTime", 0L)

        if (endTime > System.currentTimeMillis()) {
            if (isActivated) {
                btnCancelActive.visibility = View.VISIBLE
                btnActivateFuture?.visibility = View.GONE
            } else {
                btnCancelActive.visibility = View.VISIBLE
                btnActivateFuture?.visibility = View.VISIBLE
            }
        } else {
            btnCancelActive.visibility = View.GONE
            btnActivateFuture?.visibility = View.GONE
        }

        // חניה עתידית - לוקח את משך הזמן ומתחיל אותו מעכשיו!
        btnActivateFuture?.setOnClickListener {
            val originalDuration = endTime - startTime
            val newStartTime = System.currentTimeMillis()
            val newEndTime = newStartTime + originalDuration

            activePref.edit()
                .putBoolean("isActivated", true)
                .putLong("startTime", newStartTime)
                .putLong("endTime", newEndTime)
                .apply()

            updateMiniPlayerTimer()
            Toast.makeText(this, "החניה העתידית הופעלה כעת! השעון מתחיל לרוץ.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnCancelActive.setOnClickListener {
            cancelAlarm()
            activePref.edit().clear().apply()
            updateNotification(0L, true)
            updateMiniPlayerTimer()
            Toast.makeText(this, "החניה בוטלה בהצלחה.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnParkingHistory.setOnClickListener {
            dialog.dismiss()
            try { navController.navigate(R.id.historyFragment) } catch (e: Exception) {}
        }

        btnCarDetails.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("פרטי רכב שמורים")
                .setMessage("מספר רכב: 123-45-678\nסוג: קיה פיקנטו (שחור)\n\n* ניתן לעדכן פרטים אלו דרך שירות הלקוחות.")
                .setPositiveButton("סגור", null)
                .show()
        }

        btnSavedCredit.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("אמצעי תשלום")
                .setMessage("כרטיס אשראי: ****-****-****-1234\nתוקף: 12/28\nסטטוס: פעיל ומאובטח")
                .setPositiveButton("סגור", null)
                .show()
        }

        // הפעלת כפתור דיווחי נהגים בזמן אמת
        val btnOpenReports = dialog.findViewById<Button>(R.id.openReportsButton)
        btnOpenReports?.setOnClickListener {
            val activePref = getSharedPreferences("ActiveBooking", Context.MODE_PRIVATE)
            val currentParking = activePref.getString("parkingName", "")

            if (currentParking.isNullOrEmpty()) {
                Toast.makeText(this@MainActivity, "יש להזמין חניה קודם כדי לראות ולשתף דיווחים \uD83D\uDE97", Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(this@MainActivity, ReportsActivity::class.java)
                startActivity(intent)
            }
        }
        dialog.show()
    }


    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }

    override fun onResume() {
        super.onResume()
        updateMiniPlayerTimer()
    }
}