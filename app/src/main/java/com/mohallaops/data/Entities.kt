package com.mohallaops.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val leadName: String,
    val x: Float,
    val y: Float
)

/** One physical supply box. ownerStationId is the single source of truth for who controls it. */
@Entity(tableName = "boxes")
data class BoxEntity(
    @PrimaryKey val id: String,
    val originStationId: String,
    val ownerStationId: String,
    val status: String,
    val deliveryId: String? = null
)

@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey val id: String,
    val label: String,
    val homeStationId: String,
    val boxesNeeded: Int,
    val status: String,
    val x: Float,
    val y: Float
)

@Entity(tableName = "requests")
data class RequestEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val stationId: String,
    val quantity: Int,
    val rawText: String,
    val language: String,
    val confidence: Float,
    val status: String,
    val createdAt: Long
)

@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey val id: String,
    val requestId: String,
    val familyId: String,
    val stationId: String,
    val volunteerId: String?,
    val boxIds: String, // comma-separated box IDs
    val status: String,
    val failureReason: String? = null,
    val createdAt: Long,
    val deliveredAt: Long? = null
)

@Entity(tableName = "volunteers")
data class VolunteerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val stationId: String,
    val available: Boolean,
    val completed: Int
)

@Entity(tableName = "roads")
data class RoadEntity(
    @PrimaryKey val id: String,
    val stationA: String,
    val stationB: String,
    val open: Boolean
)

/** Auditable inventory history: every ownership change or status change of a box. */
@Entity(tableName = "inventory_tx")
data class InventoryTxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val boxId: String,
    val type: String,
    val fromStationId: String?,
    val toStationId: String?,
    val actor: String
)

@Entity(tableName = "events", indices = [Index(value = ["eventUid"], unique = true)])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventUid: String,
    val ts: Long,
    val type: String,
    val actor: String,
    val message: String,
    val stationId: String?,
    val syncStatus: String
)

@Entity(tableName = "run_results")
data class RunResultEntity(
    @PrimaryKey val runLabel: Int,
    val totalFamilies: Int,
    val served: Int,
    val waiting: Int,
    val northServed: Int,
    val southServed: Int,
    val boxesTransferred: Int,
    val avgHandoffMs: Long,
    val finishedAt: Long
)
