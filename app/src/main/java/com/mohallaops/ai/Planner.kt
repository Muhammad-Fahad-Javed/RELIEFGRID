package com.mohallaops.ai

import com.mohallaops.data.BoxEntity
import com.mohallaops.data.FamilyEntity
import com.mohallaops.data.StationEntity
import com.mohallaops.domain.S

data class Suggestion(
    val problem: String,
    val evidence: List<String>,
    val action: String,
    val effect: String,
    val limitations: String,
    val from: String?,
    val to: String?,
    val count: Int
) {
    val executable: Boolean get() = from != null && to != null && count > 0
}

/**
 * Deterministic planner. Every number comes from stored records (never from a language model),
 * so it can't invent boxes, roads or deliveries. A person must accept before anything changes.
 */
object Planner {
    fun analyse(
        stations: List<StationEntity>,
        boxes: List<BoxEntity>,
        families: List<FamilyEntity>,
        roadOpen: Boolean
    ): Suggestion? {
        data class Row(val id: String, val name: String, val demand: Int, val available: Int, val committed: Int) {
            val uncovered get() = (demand - committed - available).coerceAtLeast(0)
            val spare get() = (available - (demand - committed).coerceAtLeast(0)).coerceAtLeast(0)
        }
        val rows = stations.map { st ->
            Row(
                st.id, st.name,
                demand = families.count { it.homeStationId == st.id && it.status != S.SERVED },
                available = boxes.count { it.ownerStationId == st.id && it.status == S.AVAILABLE },
                committed = boxes.count { it.ownerStationId == st.id && (it.status == S.RESERVED || it.status == S.PICKED_UP) }
            )
        }
        val needy = rows.filter { it.uncovered > 0 }.maxByOrNull { it.uncovered } ?: return null
        val donor = rows.filter { it.id != needy.id && it.spare > 0 }.maxByOrNull { it.spare }
        val evidence = rows.map {
            "${it.name}: ${it.demand} family(ies) waiting, ${it.available} available box(es), ${it.committed} already reserved"
        }

        if (donor == null) {
            return Suggestion(
                problem = "${needy.name} is short by ${needy.uncovered} box(es) and no other station has spare stock.",
                evidence = evidence,
                action = "Add stock or reduce demand; no transfer can fix this.",
                effect = "None from a transfer.",
                limitations = "Only recorded stock is considered.",
                from = null, to = null, count = 0
            )
        }
        val move = minOf(needy.uncovered, donor.spare)
        return if (roadOpen) {
            Suggestion(
                problem = "${needy.name} needs ${needy.uncovered} more box(es) than it controls, while ${donor.name} holds $move spare.",
                evidence = evidence,
                action = "Transfer $move box(es) from ${donor.name} to ${needy.name} before the connecting road closes.",
                effect = "Up to $move more family(ies) could be served if the road later closes.",
                limitations = "Valid only while the road is open. Delivery still needs volunteers and recipient confirmation.",
                from = donor.id, to = needy.id, count = move
            )
        } else {
            Suggestion(
                problem = "${needy.name} is short by ${needy.uncovered} box(es) and the connecting road is closed, so ${donor.name}'s spare $move box(es) cannot reach it.",
                evidence = evidence,
                action = "Reopen the road in the drill, or accept that ${needy.uncovered} family(ies) will wait.",
                effect = "Transfers are impossible while the road is closed.",
                limitations = "This is a simulated closure; the lesson is to move stock BEFORE a closure.",
                from = null, to = null, count = 0
            )
        }
    }
}
