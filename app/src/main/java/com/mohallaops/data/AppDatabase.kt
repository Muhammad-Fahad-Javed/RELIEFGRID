package com.mohallaops.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * AppDatabase for RELIEFGRID.
 * Version 2 adds the IncidentEntity table for disaster incident tracking and triage.
 */
@Database(
    entities = [
        StationEntity::class, BoxEntity::class, FamilyEntity::class, RequestEntity::class,
        DeliveryEntity::class, VolunteerEntity::class, RoadEntity::class, InventoryTxEntity::class,
        EventEntity::class, RunResultEntity::class, IncidentEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): DrillDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "mohalla_ops.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
