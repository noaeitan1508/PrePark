package com.example.prepark

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager

class ParkingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        // 1. מוחק את החניה מהזיכרון כדי שלא תופיע כ"פעילה"
        val activePref = context.getSharedPreferences("ActiveBooking", Context.MODE_PRIVATE)
        activePref.edit().clear().apply()

        // שעון רץ לאחור כאשר גוללים את המסך למטה בהתראות של הטלפון
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(1)

        //  משאיר את התהליך פעיל מספיק זמן כדי להשמיע את הצלצול עד הסוף
        val pendingResult = goAsync()
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val mediaPlayer = MediaPlayer.create(context, alarmUri)
            if (mediaPlayer != null) {
                // כשהצלצול מסתיים הוא סוגר את התהליך בצורה מסודרת
                mediaPlayer.setOnCompletionListener {
                    it.release()
                    pendingResult.finish()
                }
                mediaPlayer.start()
            } else {
                pendingResult.finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            pendingResult.finish()
        }
    }
}