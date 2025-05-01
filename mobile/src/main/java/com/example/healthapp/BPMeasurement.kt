package com.example.healthapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "bp_measurements")
data class BPMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,  // Links to UserProfile.name
    val systolic: Int,
    val diastolic: Int,
    val timestamp: Long = System.currentTimeMillis()
)