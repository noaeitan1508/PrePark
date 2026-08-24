package com.example.prepark

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object HistoryManager {
    private const val PREFS_NAME = "parking_history_prefs"
    private const val HISTORY_KEY = "history_list"

    fun saveParkingRecord(context: Context, record: ParkingRecord) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()

        // שליפת ההיסטוריה הקיימת מהטלפון
        val json = prefs.getString(HISTORY_KEY, null)
        val type = object : TypeToken<MutableList<ParkingRecord>>() {}.type
        val historyList: MutableList<ParkingRecord> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        // הוספת החניה החדשה להתחלה של הרשימה (כדי שהכי חדשה תהיה למעלה)
        historyList.add(0, record)

        // שמירה על 5 חניות אחרונות בלבד
        if (historyList.size > 5) {
            historyList.removeAt(historyList.size - 1)
        }

        // שמירה חזרה לזיכרון המקומי
        prefs.edit().putString(HISTORY_KEY, gson.toJson(historyList)).apply()
    }

    fun getHistory(context: Context): List<ParkingRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(HISTORY_KEY, null)
        val type = object : TypeToken<MutableList<ParkingRecord>>() {}.type
        return if (json != null) {
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }
}