package com.example.healthapp.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HealthRepository @Inject constructor(
    private val bpDao: BPDao
) {
    fun getBPMeasurements(userId: String): Flow<List<BPMeasurement>> {
        return bpDao.getMeasurementsByUser(userId)
    }

    suspend fun addBPMeasurement(measurement: BPMeasurement) {
        bpDao.insert(measurement)
    }

    suspend fun clearUserData(userId: String) {
        bpDao.deleteAllForUser(userId)
    }
}