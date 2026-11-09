package com.mohallaops.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DrillDao {
    // ---- observable lists for the UI ----
    @Query("SELECT * FROM stations ORDER BY id") fun stations(): Flow<List<StationEntity>>
    @Query("SELECT * FROM boxes ORDER BY id") fun boxes(): Flow<List<BoxEntity>>
    @Query("SELECT * FROM families ORDER BY id") fun families(): Flow<List<FamilyEntity>>
    @Query("SELECT * FROM requests ORDER BY createdAt DESC") fun requests(): Flow<List<RequestEntity>>
    @Query("SELECT * FROM deliveries ORDER BY createdAt DESC") fun deliveries(): Flow<List<DeliveryEntity>>
    @Query("SELECT * FROM volunteers ORDER BY id") fun volunteers(): Flow<List<VolunteerEntity>>
    @Query("SELECT * FROM roads ORDER BY id") fun roads(): Flow<List<RoadEntity>>
    @Query("SELECT * FROM events ORDER BY ts DESC, id DESC") fun events(): Flow<List<EventEntity>>
    @Query("SELECT * FROM inventory_tx ORDER BY id DESC") fun inventoryTx(): Flow<List<InventoryTxEntity>>
    @Query("SELECT * FROM run_results ORDER BY runLabel") fun results(): Flow<List<RunResultEntity>>

    // ---- one-shot reads used inside transactions ----
    @Query("SELECT COUNT(*) FROM stations") suspend fun stationCount(): Int
    @Query("SELECT * FROM families ORDER BY id") suspend fun familiesNow(): List<FamilyEntity>
    @Query("SELECT * FROM families WHERE id=:id") suspend fun getFamily(id: String): FamilyEntity?
    @Query("SELECT * FROM requests WHERE id=:id") suspend fun getRequest(id: String): RequestEntity?
    @Query("SELECT * FROM deliveries WHERE id=:id") suspend fun getDelivery(id: String): DeliveryEntity?
    @Query("SELECT * FROM volunteers WHERE id=:id") suspend fun getVolunteer(id: String): VolunteerEntity?
    @Query("SELECT * FROM roads WHERE (stationA=:a AND stationB=:b) OR (stationA=:b AND stationB=:a) LIMIT 1")
    suspend fun roadBetween(a: String, b: String): RoadEntity?
    @Query("SELECT * FROM roads WHERE id=:id") suspend fun getRoad(id: String): RoadEntity?
    @Query("SELECT id FROM boxes WHERE ownerStationId=:station AND status='AVAILABLE' ORDER BY id LIMIT :n")
    suspend fun availableBoxIds(station: String, n: Int): List<String>
    @Query("SELECT * FROM boxes WHERE id=:id") suspend fun getBox(id: String): BoxEntity?
    @Query("SELECT * FROM requests WHERE familyId=:familyId AND status NOT IN ('REJECTED','DELIVERED') LIMIT 1")
    suspend fun activeRequestForFamily(familyId: String): RequestEntity?
    @Query("SELECT * FROM deliveries WHERE requestId=:requestId AND status NOT IN ('DECLINED','FAILED') LIMIT 1")
    suspend fun activeDeliveryForRequest(requestId: String): DeliveryEntity?
    @Query("SELECT * FROM deliveries WHERE volunteerId=:volunteerId AND status IN ('ASSIGNED','ACCEPTED')")
    suspend fun openDeliveriesOf(volunteerId: String): List<DeliveryEntity>
    @Query(
        "SELECT * FROM volunteers v WHERE v.available=1 AND " +
            "(SELECT COUNT(*) FROM deliveries d WHERE d.volunteerId=v.id AND d.status IN ('ASSIGNED','ACCEPTED','PICKED_UP')) < 3 " +
            "AND v.id != :excludeId ORDER BY v.completed ASC, v.id ASC LIMIT 1"
    )
    suspend fun nextVolunteer(excludeId: String): VolunteerEntity?
    @Query("SELECT COUNT(*) FROM families WHERE status='SERVED' AND homeStationId=:station")
    suspend fun servedAt(station: String): Int
    @Query("SELECT COUNT(*) FROM families") suspend fun familyCount(): Int
    @Query("SELECT COUNT(*) FROM families WHERE status='SERVED'") suspend fun servedCount(): Int
    @Query("SELECT COUNT(*) FROM inventory_tx WHERE type='TRANSFER'") suspend fun transferCount(): Int
    @Query("SELECT * FROM deliveries WHERE status='CONFIRMED' AND deliveredAt IS NOT NULL")
    suspend fun confirmedDeliveries(): List<DeliveryEntity>
    @Query("SELECT * FROM events ORDER BY ts, id") suspend fun eventsNow(): List<EventEntity>

    // ---- writes ----
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertStations(v: List<StationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBoxes(v: List<BoxEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFamilies(v: List<FamilyEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertVolunteers(v: List<VolunteerEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRoad(v: RoadEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRequest(v: RequestEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDelivery(v: DeliveryEntity)
    @Insert suspend fun insertTx(v: InventoryTxEntity)
    /** Returns -1 when an event with the same eventUid already exists (duplicate detection). */
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEvent(v: EventEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertResult(v: RunResultEntity)

    /**
     * The enforced ownership rule: a box can be reserved only by the station that currently owns it
     * AND only while it is AVAILABLE. Returns the number of rows changed (1 = success, 0 = refused).
     */
    @Query("UPDATE boxes SET status='RESERVED', deliveryId=:deliveryId WHERE id=:id AND ownerStationId=:station AND status='AVAILABLE'")
    suspend fun reserveBox(id: String, station: String, deliveryId: String): Int
    @Query("UPDATE boxes SET ownerStationId=:to WHERE id=:id AND ownerStationId=:from AND status='AVAILABLE'")
    suspend fun moveBox(id: String, from: String, to: String): Int
    @Query("UPDATE boxes SET status=:status WHERE id=:id") suspend fun setBoxStatus(id: String, status: String)
    @Query("UPDATE boxes SET status='AVAILABLE', deliveryId=NULL WHERE id=:id") suspend fun releaseBox(id: String)
    @Query("UPDATE families SET status=:status WHERE id=:id") suspend fun setFamilyStatus(id: String, status: String)
    @Query("UPDATE requests SET status=:status WHERE id=:id") suspend fun setRequestStatus(id: String, status: String)
    @Query("UPDATE deliveries SET status=:status, volunteerId=:volunteerId WHERE id=:id")
    suspend fun setDeliveryStatusAndVolunteer(id: String, status: String, volunteerId: String?)
    @Query("UPDATE deliveries SET status=:status WHERE id=:id") suspend fun setDeliveryStatus(id: String, status: String)
    @Query("UPDATE deliveries SET status='CONFIRMED' WHERE id=:id") suspend fun confirmDelivery(id: String)
    @Query("UPDATE deliveries SET status='DELIVERED', deliveredAt=:ts WHERE id=:id")
    suspend fun markDeliveryDelivered(id: String, ts: Long)
    @Query("UPDATE deliveries SET status='DECLINED', failureReason=:reason WHERE id=:id")
    suspend fun declineDelivery(id: String, reason: String)
    @Query("UPDATE volunteers SET available=:available WHERE id=:id") suspend fun setVolunteerAvailable(id: String, available: Boolean)
    @Query("UPDATE volunteers SET completed=completed+1 WHERE id=:id") suspend fun incrementCompleted(id: String)
    @Query("UPDATE roads SET open=:open WHERE id=:id") suspend fun setRoadOpen(id: String, open: Boolean)
    @Query("UPDATE events SET syncStatus='SYNCED' WHERE syncStatus='WAITING'") suspend fun markAllSynced(): Int

    @Query("DELETE FROM stations") suspend fun clearStations()
    @Query("DELETE FROM boxes") suspend fun clearBoxes()
    @Query("DELETE FROM families") suspend fun clearFamilies()
    @Query("DELETE FROM requests") suspend fun clearRequests()
    @Query("DELETE FROM deliveries") suspend fun clearDeliveries()
    @Query("DELETE FROM volunteers") suspend fun clearVolunteers()
    @Query("DELETE FROM roads") suspend fun clearRoads()
    @Query("DELETE FROM inventory_tx") suspend fun clearTx()
    @Query("DELETE FROM events") suspend fun clearEvents()
}
