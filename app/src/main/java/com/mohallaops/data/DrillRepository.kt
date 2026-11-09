package com.mohallaops.data

import androidx.room.withTransaction
import com.mohallaops.domain.Actor
import com.mohallaops.domain.PermissionDeniedException
import com.mohallaops.domain.Role
import com.mohallaops.domain.RuleViolationException
import com.mohallaops.domain.S
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ImportResult(val inserted: Int, val duplicates: Int, val rejected: Int)

/**
 * All business rules live here (not in the UI). Every public operation takes an [Actor] and
 * checks the role, and every multi-step change runs in ONE database transaction.
 */
class DrillRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val dao = db.dao()

    private val _offline = MutableStateFlow(false)
    /** Simulated "internet disconnected" switch. New events are queued as WAITING while true. */
    val offline: StateFlow<Boolean> = _offline

    val stations = dao.stations()
    val boxes = dao.boxes()
    val families = dao.families()
    val requests = dao.requests()
    val deliveries = dao.deliveries()
    val volunteers = dao.volunteers()
    val roads = dao.roads()
    val events = dao.events()
    val inventoryTx = dao.inventoryTx()
    val results = dao.results()

    // ------------------------------------------------------------------ helpers

    private fun need(ok: Boolean, message: String) {
        if (!ok) throw PermissionDeniedException(message)
    }

    private fun rule(ok: Boolean, message: String) {
        if (!ok) throw RuleViolationException(message)
    }

    private fun isLeader(a: Actor) = a.role == Role.DRILL_LEADER
    private fun leadsStation(a: Actor, station: String) =
        a.role == Role.STATION_LEAD && a.stationId == station

    private suspend fun log(
        type: String, actor: String, message: String, stationId: String? = null,
        uid: String = UUID.randomUUID().toString(), ts: Long = clock()
    ) {
        dao.insertEvent(
            EventEntity(
                eventUid = uid, ts = ts, type = type, actor = actor, message = message,
                stationId = stationId,
                syncStatus = if (_offline.value) S.SYNC_WAITING else S.SYNCED
            )
        )
    }

    // ------------------------------------------------------------------ seed / reset

    suspend fun ensureSeeded() {
        if (dao.stationCount() == 0) db.withTransaction {
            seed()
            log("DRILL_CREATED", "System", "Demo drill created: 6 families, North 5 boxes, South 1 box")
        }
    }

    private suspend fun seed() {
        dao.insertStations(
            listOf(
                StationEntity("NORTH", "North Station", "North Lead", 0.5f, 0.13f),
                StationEntity("SOUTH", "South Station", "South Lead", 0.5f, 0.82f)
            )
        )
        dao.insertBoxes(
            (1..5).map { BoxEntity("NORTH-%03d".format(it), "NORTH", "NORTH", S.AVAILABLE) } +
                BoxEntity("SOUTH-001", "SOUTH", "SOUTH", S.AVAILABLE)
        )
        dao.insertFamilies(
            listOf(
                FamilyEntity("FAM-N1", "North family 1", "NORTH", 1, S.WAITING, 0.22f, 0.32f),
                FamilyEntity("FAM-N2", "North family 2", "NORTH", 1, S.WAITING, 0.78f, 0.32f),
                FamilyEntity("FAM-S1", "South family 1", "SOUTH", 1, S.WAITING, 0.12f, 0.66f),
                FamilyEntity("FAM-S2", "South family 2", "SOUTH", 1, S.WAITING, 0.37f, 0.66f),
                FamilyEntity("FAM-S3", "South family 3", "SOUTH", 1, S.WAITING, 0.63f, 0.66f),
                FamilyEntity("FAM-S4", "South family 4", "SOUTH", 1, S.WAITING, 0.88f, 0.66f)
            )
        )
        dao.insertRoad(RoadEntity("ROAD-NS", "NORTH", "SOUTH", true))
        dao.insertVolunteers(
            listOf(
                VolunteerEntity("VOL-A", "Volunteer A", "NORTH", true, 0),
                VolunteerEntity("VOL-B", "Volunteer B", "SOUTH", true, 0),
                VolunteerEntity("VOL-C", "Volunteer C", "SOUTH", true, 0)
            )
        )
    }

    /** Restores the exact starting scenario. Recorded run results are kept for comparison. */
    suspend fun resetDrill(actor: Actor) {
        need(isLeader(actor), "Only the drill leader can reset the drill")
        db.withTransaction {
            dao.clearDeliveriesAndRequestsEtc()
            seed()
            log("DRILL_RESET", actor.name, "Drill reset to the starting scenario")
        }
    }

    private suspend fun DrillDao.clearDeliveriesAndRequestsEtc() {
        clearDeliveries(); clearRequests(); clearBoxes(); clearFamilies()
        clearStations(); clearVolunteers(); clearRoads(); clearTx(); clearEvents()
    }

    // ------------------------------------------------------------------ disruptions

    suspend fun setRoadOpen(actor: Actor, open: Boolean) {
        need(isLeader(actor), "Only the drill leader can change road conditions")
        db.withTransaction {
            dao.setRoadOpen("ROAD-NS", open)
            log(
                if (open) "ROAD_REOPENED" else "ROAD_CLOSED", actor.name,
                if (open) "Simulated: road between North and South reopened"
                else "Simulated: road between North and South closed"
            )
        }
    }

    suspend fun setVolunteerAvailable(actor: Actor, volunteerId: String, available: Boolean) {
        need(isLeader(actor), "Only the drill leader can change volunteer availability")
        db.withTransaction {
            dao.setVolunteerAvailable(volunteerId, available)
            log(
                "VOLUNTEER_AVAILABILITY", actor.name,
                "Volunteer $volunteerId is now ${if (available) "available" else "unavailable"}"
            )
            if (!available) {
                dao.openDeliveriesOf(volunteerId).forEach { d ->
                    val replacement = dao.nextVolunteer(volunteerId)
                    if (replacement != null) {
                        dao.setDeliveryStatusAndVolunteer(d.id, S.D_ASSIGNED, replacement.id)
                        log("DELIVERY_REASSIGNED", actor.name, "Delivery ${d.id} reassigned to ${replacement.id}", d.stationId)
                    } else {
                        dao.setDeliveryStatusAndVolunteer(d.id, S.PENDING_ASSIGNMENT, null)
                        log("DELIVERY_UNASSIGNED", actor.name, "Delivery ${d.id} has no volunteer — needs a replacement", d.stationId)
                    }
                }
            }
        }
    }

    suspend fun reassignDelivery(actor: Actor, deliveryId: String) {
        db.withTransaction {
            val d = dao.getDelivery(deliveryId)
            rule(d != null, "Delivery not found")
            need(isLeader(actor) || leadsStation(actor, d!!.stationId), "Not allowed to reassign this delivery")
            rule(d.status == S.PENDING_ASSIGNMENT, "Delivery does not need a replacement")
            val v = dao.nextVolunteer("")
            rule(v != null, "No available volunteer")
            dao.setDeliveryStatusAndVolunteer(d.id, S.D_ASSIGNED, v!!.id)
            log("DELIVERY_REASSIGNED", actor.name, "Delivery ${d.id} assigned to ${v.id}", d.stationId)
        }
    }

    // ------------------------------------------------------------------ inventory

    /**
     * Moves AVAILABLE boxes from one station to another. Refused if the road is closed,
     * if the actor does not control the source station, or if stock is insufficient.
     * Runs in one transaction: either every box moves or none does.
     */
    suspend fun transferBoxes(actor: Actor, from: String, to: String, count: Int) {
        need(isLeader(actor) || leadsStation(actor, from), "Only the drill leader or the source station lead can transfer boxes")
        rule(count > 0, "Transfer at least one box")
        db.withTransaction {
            val road = dao.roadBetween(from, to)
            rule(road != null && road.open, "Transfer impossible: the road between $from and $to is closed")
            val ids = dao.availableBoxIds(from, count)
            rule(ids.size == count, "$from has only ${ids.size} available box(es)")
            ids.forEach { id ->
                rule(dao.moveBox(id, from, to) == 1, "Box $id could not be moved")
                dao.insertTx(InventoryTxEntity(ts = clock(), boxId = id, type = "TRANSFER", fromStationId = from, toStationId = to, actor = actor.name))
            }
            log("STOCK_TRANSFER", actor.name, "Transferred $count box(es) ${ids.joinToString()} from $from to $to", from)
        }
    }

    /** Reserve one box for a delivery. Enforced in SQL: only the current owner may reserve an AVAILABLE box. */
    suspend fun reserveBox(actor: Actor, boxId: String, deliveryId: String) {
        db.withTransaction {
            val box = dao.getBox(boxId)
            rule(box != null, "Box $boxId does not exist")
            need(isLeader(actor) || leadsStation(actor, box!!.ownerStationId), "Station ${actor.stationId} does not control $boxId")
            rule(dao.reserveBox(boxId, box!!.ownerStationId, deliveryId) == 1, "Box $boxId is not available")
            dao.insertTx(InventoryTxEntity(ts = clock(), boxId = boxId, type = "RESERVE", fromStationId = box.ownerStationId, toStationId = null, actor = actor.name))
        }
    }

    // ------------------------------------------------------------------ requests

    suspend fun submitRequest(
        actor: Actor, familyId: String, stationId: String, quantity: Int,
        rawText: String, language: String, confidence: Float
    ): String {
        need(
            isLeader(actor) || (actor.role == Role.RESIDENT && actor.familyId == familyId),
            "Residents can only submit requests for their own family"
        )
        rule(quantity in 1..10, "Quantity must be between 1 and 10")
        rule(rawText.length <= 1000, "Message is too long")
        return db.withTransaction {
            rule(dao.getFamily(familyId) != null, "Unknown family")
            rule(dao.activeRequestForFamily(familyId) == null, "Possible duplicate: $familyId already has an active request")
            val id = "REQ-" + UUID.randomUUID().toString().take(6).uppercase()
            val status = if (confidence < 0.7f) S.NEEDS_REVIEW else S.NEW
            dao.insertRequest(RequestEntity(id, familyId, stationId, quantity, rawText, language, confidence, status, clock()))
            log("REQUEST_SUBMITTED", actor.name, "$familyId requested $quantity box(es) at $stationId ($id)", stationId)
            id
        }
    }

    suspend fun approveRequest(actor: Actor, requestId: String) {
        db.withTransaction {
            val r = dao.getRequest(requestId); rule(r != null, "Request not found")
            need(isLeader(actor) || leadsStation(actor, r!!.stationId), "Only the lead of ${r!!.stationId} can approve this request")
            rule(r.status == S.NEW || r.status == S.NEEDS_REVIEW, "Request is already ${r.status}")
            dao.setRequestStatus(requestId, S.APPROVED)
            log("REQUEST_APPROVED", actor.name, "$requestId approved", r.stationId)
        }
    }

    suspend fun rejectRequest(actor: Actor, requestId: String) {
        db.withTransaction {
            val r = dao.getRequest(requestId); rule(r != null, "Request not found")
            need(isLeader(actor) || leadsStation(actor, r!!.stationId), "Only the lead of ${r!!.stationId} can reject this request")
            rule(r.status == S.NEW || r.status == S.NEEDS_REVIEW, "Request is already ${r.status}")
            dao.setRequestStatus(requestId, S.REJECTED)
            log("REQUEST_REJECTED", actor.name, "$requestId rejected", r.stationId)
        }
    }

    // ------------------------------------------------------------------ deliveries

    /** Reserves boxes owned by the request's station and assigns a volunteer. All-or-nothing. */
    suspend fun assignDelivery(actor: Actor, requestId: String): String = db.withTransaction {
        val r = dao.getRequest(requestId); rule(r != null, "Request not found")
        need(isLeader(actor) || leadsStation(actor, r!!.stationId), "Only the lead of ${r!!.stationId} can assign this delivery")
        rule(r.status == S.APPROVED, "Request must be approved first")
        val boxIds = dao.availableBoxIds(r.stationId, r.quantity)
        rule(boxIds.size == r.quantity, "No available boxes at ${r.stationId} (needs ${r.quantity}, has ${boxIds.size})")
        val volunteer = dao.nextVolunteer("")
        rule(volunteer != null, "No available volunteer")
        val deliveryId = "DEL-" + UUID.randomUUID().toString().take(6).uppercase()
        boxIds.forEach { reserveBox(actor, it, deliveryId) }
        dao.insertDelivery(DeliveryEntity(deliveryId, requestId, r.familyId, r.stationId, volunteer!!.id, boxIds.joinToString(","), S.D_ASSIGNED, null, clock()))
        dao.setRequestStatus(requestId, S.ASSIGNED)
        log("DELIVERY_ASSIGNED", actor.name, "$deliveryId assigned to ${volunteer.id} with ${boxIds.joinToString()}", r.stationId)
        deliveryId
    }

    private fun ownsDelivery(actor: Actor, d: DeliveryEntity) =
        actor.role == Role.VOLUNTEER && actor.volunteerId != null && actor.volunteerId == d.volunteerId

    suspend fun acceptDelivery(actor: Actor, deliveryId: String) = db.withTransaction {
        val d = dao.getDelivery(deliveryId); rule(d != null, "Delivery not found")
        need(ownsDelivery(actor, d!!), "This delivery is not assigned to you")
        rule(d.status == S.D_ASSIGNED, "Delivery is ${d.status}")
        dao.setDeliveryStatus(deliveryId, S.ACCEPTED)
        log("DELIVERY_ACCEPTED", actor.name, "$deliveryId accepted", d.stationId)
    }

    suspend fun declineDelivery(actor: Actor, deliveryId: String, reason: String) = db.withTransaction {
        val d = dao.getDelivery(deliveryId); rule(d != null, "Delivery not found")
        need(ownsDelivery(actor, d!!), "This delivery is not assigned to you")
        rule(d.status == S.D_ASSIGNED || d.status == S.ACCEPTED, "Delivery is ${d.status}")
        rule(reason.isNotBlank(), "A reason is required")
        dao.declineDelivery(deliveryId, reason.take(200))
        d.boxIds.split(",").forEach { dao.releaseBox(it) }
        dao.setRequestStatus(d.requestId, S.APPROVED)
        log("DELIVERY_DECLINED", actor.name, "$deliveryId declined: ${reason.take(200)}", d.stationId)
    }

    /** The volunteer must scan or type EVERY box ID of the delivery to confirm pickup. */
    suspend fun pickUp(actor: Actor, deliveryId: String, scannedBoxIds: List<String>) = db.withTransaction {
        val d = dao.getDelivery(deliveryId); rule(d != null, "Delivery not found")
        need(ownsDelivery(actor, d!!), "This delivery is not assigned to you")
        rule(d.status == S.ACCEPTED, "Delivery must be accepted before pickup")
        val expected = d.boxIds.split(",").toSet()
        rule(scannedBoxIds.map { it.trim().uppercase() }.toSet() == expected, "Scanned boxes do not match. Expected: ${d.boxIds}")
        expected.forEach {
            dao.setBoxStatus(it, S.PICKED_UP)
            dao.insertTx(InventoryTxEntity(ts = clock(), boxId = it, type = "PICKUP", fromStationId = d.stationId, toStationId = null, actor = actor.name))
        }
        dao.setDeliveryStatus(deliveryId, S.D_PICKED_UP)
        log("DELIVERY_PICKED_UP", actor.name, "$deliveryId picked up ${d.boxIds}", d.stationId)
    }

    /** Volunteer reports the handoff. It is NOT counted as a success until the recipient confirms. */
    suspend fun recordHandoff(actor: Actor, deliveryId: String) = db.withTransaction {
        val d = dao.getDelivery(deliveryId); rule(d != null, "Delivery not found")
        need(ownsDelivery(actor, d!!), "This delivery is not assigned to you")
        rule(d.status == S.D_PICKED_UP, "Delivery must be picked up first")
        dao.markDeliveryDelivered(deliveryId, clock())
        log("DELIVERY_HANDOFF", actor.name, "$deliveryId handed to ${d.familyId}; awaiting recipient confirmation", d.stationId)
    }

    /** Recipient confirmation: the only place where a family becomes SERVED. Idempotent-safe: second call is refused. */
    suspend fun confirmReceipt(actor: Actor, deliveryId: String) = db.withTransaction {
        val d = dao.getDelivery(deliveryId); rule(d != null, "Delivery not found")
        need(isLeader(actor) || (actor.role == Role.RESIDENT && actor.familyId == d!!.familyId), "Only the receiving family can confirm")
        rule(d!!.status == S.D_DELIVERED, "Nothing to confirm (status ${d.status})")
        dao.confirmDelivery(deliveryId)
        d.boxIds.split(",").forEach {
            dao.setBoxStatus(it, S.DELIVERED)
            dao.insertTx(InventoryTxEntity(ts = clock(), boxId = it, type = "DELIVERED", fromStationId = d.stationId, toStationId = null, actor = actor.name))
        }
        dao.setFamilyStatus(d.familyId, S.SERVED)
        dao.setRequestStatus(d.requestId, S.REQ_DELIVERED)
        d.volunteerId?.let { dao.incrementCompleted(it) }
        log("DELIVERY_CONFIRMED", actor.name, "${d.familyId} confirmed receipt of ${d.boxIds}", d.stationId)
    }

    // ------------------------------------------------------------------ run automation (demo)

    /**
     * Drives every still-waiting family through the full workflow using the SAME rule-checked
     * methods a human would use. Families whose station has no stock stay waiting.
     * Returns the number of confirmed deliveries.
     */
    suspend fun fulfilAll(actor: Actor): Int {
        need(isLeader(actor), "Only the drill leader can run the automatic fulfilment")
        var confirmed = 0
        for (fam in dao.familiesNow()) {
            if (fam.status == S.SERVED) continue
            val lead = Actor.lead(fam.homeStationId)
            val resident = Actor.resident(fam.id)
            try {
                val reqId = dao.activeRequestForFamily(fam.id)?.id
                    ?: submitRequest(resident, fam.id, fam.homeStationId, fam.boxesNeeded, "Drill request for ${fam.id}", "en", 1f)
                val req = dao.getRequest(reqId)!!
                if (req.status == S.NEW || req.status == S.NEEDS_REVIEW) approveRequest(lead, reqId)
                if (dao.getRequest(reqId)!!.status == S.APPROVED) assignDelivery(lead, reqId)
                val d = dao.activeDeliveryForRequest(reqId) ?: continue
                val volunteerId = d.volunteerId ?: continue
                val vol = Actor.volunteer(volunteerId)
                if (dao.getDelivery(d.id)!!.status == S.D_ASSIGNED) acceptDelivery(vol, d.id)
                if (dao.getDelivery(d.id)!!.status == S.ACCEPTED) pickUp(vol, d.id, d.boxIds.split(","))
                if (dao.getDelivery(d.id)!!.status == S.D_PICKED_UP) recordHandoff(vol, d.id)
                if (dao.getDelivery(d.id)!!.status == S.D_DELIVERED) { confirmReceipt(resident, d.id); confirmed++ }
            } catch (e: RuleViolationException) {
                log("UNFULFILLED", "System", "${fam.id} not served: ${e.message}", fam.homeStationId)
            }
        }
        return confirmed
    }

    /** Computes the result from real stored records and saves it under the given run label. */
    suspend fun completeRun(actor: Actor, label: Int): RunResultEntity {
        need(isLeader(actor), "Only the drill leader can record results")
        return db.withTransaction {
            val total = dao.familyCount()
            val served = dao.servedCount()
            val handoffs = dao.confirmedDeliveries().map { (it.deliveredAt ?: it.createdAt) - it.createdAt }
            val result = RunResultEntity(
                runLabel = label, totalFamilies = total, served = served, waiting = total - served,
                northServed = dao.servedAt("NORTH"), southServed = dao.servedAt("SOUTH"),
                boxesTransferred = dao.transferCount(),
                avgHandoffMs = if (handoffs.isEmpty()) 0L else handoffs.sum() / handoffs.size,
                finishedAt = clock()
            )
            dao.upsertResult(result)
            log("RUN_RECORDED", actor.name, "Run $label recorded: $served/$total families served, ${total - served} waiting")
            result
        }
    }

    /**
     * One-tap demo run on a freshly reset scenario. Run 1: no transfer. Run 2: move 3 boxes
     * North → South BEFORE the road closes. Both runs then close the same road and fulfil.
     */
    suspend fun runDemo(actor: Actor, label: Int, transferFirst: Boolean): RunResultEntity {
        need(isLeader(actor), "Only the drill leader can run the demo")
        resetDrill(actor)
        log("RUN_STARTED", actor.name, "Run $label started" + if (transferFirst) " with pre-closure transfer" else "")
        if (transferFirst) transferBoxes(actor, "NORTH", "SOUTH", 3)
        setRoadOpen(actor, false)
        fulfilAll(actor)
        return completeRun(actor, label)
    }

    // ------------------------------------------------------------------ offline exchange & sync

    fun setOffline(offline: Boolean) { _offline.value = offline }

    /** Marks queued events as synced. NOTE: no cloud backend is wired in this build (see README). */
    suspend fun syncNow(): Int {
        if (_offline.value) throw RuleViolationException("Still offline — reconnect first")
        return dao.markAllSynced()
    }

    /** Serialises the event log so another phone can import it (clipboard / share / Nearby transport). */
    suspend fun exportEvents(): String {
        val arr = JSONArray()
        dao.eventsNow().forEach {
            arr.put(JSONObject().put("uid", it.eventUid).put("ts", it.ts).put("type", it.type)
                .put("actor", it.actor).put("message", it.message).put("station", it.stationId ?: JSONObject.NULL))
        }
        return JSONObject().put("app", "mohalla-ops").put("v", 1).put("events", arr).toString()
    }

    /** Validates and imports events from another device. Duplicates (same uid) are ignored. */
    suspend fun importEvents(json: String): ImportResult {
        var inserted = 0; var duplicates = 0; var rejected = 0
        val arr = try {
            val root = JSONObject(json)
            if (root.optString("app") != "mohalla-ops") return ImportResult(0, 0, 1)
            root.getJSONArray("events")
        } catch (e: Exception) { return ImportResult(0, 0, 1) }
        db.withTransaction {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i)
                val uid = o?.optString("uid").orEmpty()
                val type = o?.optString("type").orEmpty()
                val msg = o?.optString("message").orEmpty()
                if (o == null || uid.length !in 8..64 || type.isBlank() || type.length > 40 || msg.isBlank() || msg.length > 500 || o.optLong("ts", -1) <= 0) {
                    rejected++; continue
                }
                val row = EventEntity(
                    eventUid = uid, ts = o.getLong("ts"), type = type,
                    actor = o.optString("actor", "Unknown").take(60), message = msg,
                    stationId = o.optString("station").takeIf { it.isNotBlank() && it != "null" },
                    syncStatus = S.SYNC_WAITING // received from a peer; not yet confirmed with the backend
                )
                if (dao.insertEvent(row) == -1L) duplicates++ else inserted++
            }
        }
        return ImportResult(inserted, duplicates, rejected)
    }
}
