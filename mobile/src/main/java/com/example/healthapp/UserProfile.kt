package com.example.healthapp

import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserProfile(
    // Basic info
    val name: String,
    val dateOfBirth: String,
    val weight: Float,
    val height: Float,
    val gender: String,
    val smokes: Boolean,
    val drinks: Boolean,
    val hasDailyActivity: Boolean,
    // Dynamic health data
    val bpReadings: List<BPMeasurement> = emptyList() // Store history
) : Parcelable

@Parcelize
data class BPMeasurement(
    val systolic: Int,
    val diastolic: Int,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable