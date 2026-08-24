package com.example.prepark

data class ParkingRecord(
    val parkingName: String,
    val durationString: String,
    val totalCost: String,
    val timestamp: Long = System.currentTimeMillis()
)