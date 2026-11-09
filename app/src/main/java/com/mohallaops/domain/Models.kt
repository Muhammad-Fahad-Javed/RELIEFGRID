package com.mohallaops.domain

enum class Role(val label: String) {
    RESIDENT("Resident"),
    STATION_LEAD("Station Lead"),
    VOLUNTEER("Volunteer"),
    DRILL_LEADER("Drill Leader")
}

/** The person performing an action. Every repository operation receives one and checks it. */
data class Actor(
    val name: String,
    val role: Role,
    val stationId: String? = null,
    val familyId: String? = null,
    val volunteerId: String? = null
) {
    companion object {
        fun lead(stationId: String) = Actor(
            name = stationId.lowercase().replaceFirstChar { it.uppercase() } + " Lead",
            role = Role.STATION_LEAD, stationId = stationId
        )
        fun resident(familyId: String) = Actor("Resident $familyId", Role.RESIDENT, familyId = familyId)
        fun volunteer(volunteerId: String) = Actor("Volunteer $volunteerId", Role.VOLUNTEER, volunteerId = volunteerId)
        val drillLeader = Actor("Drill Leader", Role.DRILL_LEADER)
    }
}

/** Demo accounts used by Demonstration Mode (no real accounts are created). */
object DemoActors {
    val resident = Actor("Resident (South family 1)", Role.RESIDENT, familyId = "FAM-S1")
    val northLead = Actor("North Station Lead", Role.STATION_LEAD, stationId = "NORTH")
    val southLead = Actor("South Station Lead", Role.STATION_LEAD, stationId = "SOUTH")
    val volunteer = Actor("Volunteer A", Role.VOLUNTEER, volunteerId = "VOL-A")
    val leader = Actor("Drill Leader", Role.DRILL_LEADER)
    val all = listOf(leader, northLead, southLead, volunteer, resident)
}

open class DrillException(message: String) : Exception(message)
class PermissionDeniedException(message: String) : DrillException(message)
class RuleViolationException(message: String) : DrillException(message)

/** Status strings stored in the database. */
object S {
    const val AVAILABLE = "AVAILABLE"
    const val RESERVED = "RESERVED"
    const val PICKED_UP = "PICKED_UP"
    const val DELIVERED = "DELIVERED"

    const val WAITING = "WAITING"
    const val SERVED = "SERVED"

    const val NEW = "NEW"
    const val NEEDS_REVIEW = "NEEDS_REVIEW"
    const val APPROVED = "APPROVED"
    const val ASSIGNED = "ASSIGNED"
    const val REJECTED = "REJECTED"
    const val REQ_DELIVERED = "DELIVERED"

    const val PENDING_ASSIGNMENT = "PENDING_ASSIGNMENT"
    const val D_ASSIGNED = "ASSIGNED"
    const val ACCEPTED = "ACCEPTED"
    const val D_PICKED_UP = "PICKED_UP"
    const val D_DELIVERED = "DELIVERED"
    const val CONFIRMED = "CONFIRMED"
    const val DECLINED = "DECLINED"
    const val FAILED = "FAILED"

    const val SYNCED = "SYNCED"
    const val SYNC_WAITING = "WAITING"
}
