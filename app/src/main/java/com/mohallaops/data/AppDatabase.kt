package com.mohallaops.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version 1 is the first released schema. Future schema changes MUST add a Migration
 * (see README "Database migrations") instead of destructive recreation.
 */
@Database(
    entities = [
        StationEntity::class, BoxEntity::class, FamilyEntity::class, RequestEntity::class,
        DeliveryEntity::class, VolunteerEntity::class, RoadEntity::class, InventoryTxEntity::class,
        EventEntity::class, RunResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): DrillDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "mohalla_ops.db").build()
    }
}
