package com.example.healthapp.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BPMeasurement::class],
    version = 1,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun bpDao(): BPDao

    companion object {
        const val DATABASE_NAME = "health_db"
    }
}