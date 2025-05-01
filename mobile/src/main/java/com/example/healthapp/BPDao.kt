package com.example.healthapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BPDao {
    @Insert
    suspend fun insert(measurement: BPMeasurement)

    @Query("SELECT * FROM bp_measurements WHERE userId = :userId ORDER BY timestamp DESC")
    fun getMeasurementsByUser(userId: String): Flow<List<BPMeasurement>>

    @Query("DELETE FROM bp_measurements WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT * FROM bp_measurements WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForUser(userId: String): BPMeasurement?

}