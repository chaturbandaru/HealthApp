package com.example.healthapp.data

import android.content.Context
import androidx.room.Room

object DatabaseModule {
    private var instance: HealthDatabase? = null

    fun getDatabase(context: Context): HealthDatabase {
        return instance ?: synchronized(this) {
            val newInstance = Room.databaseBuilder(
                context.applicationContext,
                HealthDatabase::class.java,
                HealthDatabase.DATABASE_NAME
            ).build()
            instance = newInstance
            newInstance
        }
    }
}