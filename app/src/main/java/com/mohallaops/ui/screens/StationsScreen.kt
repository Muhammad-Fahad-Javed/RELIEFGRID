package com.mohallaops.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohallaops.data.DeliveryEntity
import com.mohallaops.data.RequestEntity
import com.mohallaops.domain.Actor
import com.mohallaops.domain.Role
import com.mohallaops.domain.S
import com.mohallaops.ui.MainViewModel
import com.mohallaops.ui.components.AppCard
import com.mohallaops.ui.components.EmptyState
import com.mohallaops.ui.components.PrimaryButton
import com.mohallaops.ui.components.SecondaryButton
import com.mohallaops.ui.components.SectionHeader
import com.mohallaops.ui.components.StatCard
import com.mohallaops.ui.components.StatusBadge
import com.mohallaops.ui.components.Tone
import com.mohallaops.ui.theme.MohallaColors

fun requestTone(status: String) = when (status) {
    S.NEW -> Tone.INFO
    S.NEEDS_REVIEW -> Tone.WARNING
    S.APPROVED -> Tone.INFO
    S.ASSIGNED -> Tone.INFO
    S.REQ_DELIVERED -> Tone.SUCCESS
    S.REJECTED -> Tone.DANGER
    else -> Tone.NEUTRAL
}

fun deliveryTone(status: String) = when (status) {
    S.CONFIRMED -> Tone.SUCCESS
    S.D_DELIVERED -> Tone.WARNING // delivered but NOT yet confirmed by the recipient
    S.DECLINED, S.FAILED -> Tone.DANGER
    S.PENDING_ASSIGNMENT -> Tone.WARNING
    else -> Tone.INFO
}

fun deliveryLabel(status: String) = when (status) {
    S.D_DELIVERED -> "DELIVERED — awaiting recipient confirmation"
    S.PENDING_ASSIGNMENT -> "NEEDS A VOLUNTEER"
    else -> status.replace('_', ' ')
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StationsScreen(vm: MainViewModel, onNewRequest: () -> Unit) {
    val actor by vm.actor.collectAsStateWithLifecycle()
    val stations by vm.stations.collectAsStateWithLifecycle()
    val boxes by vm.boxes.collectAsStateWithLifecycle()
    val requests by vm.requests.collectAsStateWithLifecycle()
    val deliveries by vm.deliveries.collectAsStateWithLifecycle()
    var pickupFor by remember { mutableStateOf<DeliveryEntity?>(null) }

    val visibleStations = when (actor.role) {
        Role.DRILL_LEADER -> stations
        Role.STATION_LEAD -> stations.filter { it.id == actor.stationId }
        else -> emptyList()
    }
    val myDeliveries = when (actor.role) {
        Role.RESIDENT -> deliveries.filter { it.familyId == actor.familyId }
        Role.VOLUNTEER -> deliveries.filter { it.volunteerId == actor.volunteerId || it.status == S.PENDING_ASSIGNMENT }
        else -> emptyList()
    }
    val myRequests = if (actor.role == Role.RESIDENT) requests.filter { it.familyId == actor.familyId } else emptyList()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                when (actor.role) {
                    Role.RESIDENT -> "My requests"
                    Role.VOLUNTEER -> "My deliveries"
                    Role.STATION_LEAD -> "My station"
                    Role.DRILL_LEADER -> "All stations"
                },
                style = MaterialTheme.typography.titleLarge, color = MohallaColors.Navy
            )
            Text("Signed in as ${actor.name}", style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary)
        }

        if (actor.role == Role.RESIDENT) {
            item { PrimaryButton("Request supplies", onNewRequest, Modifier.fillMaxWidth()) }
            if (myRequests.isEmpty()) item { EmptyState("No request yet", "Tap “Request supplies” to send one in Urdu or English.") }
            items(myRequests.size) { i -> RequestCard(myRequests[i], actor, vm) }
        }

        items(visibleStations.size) { i ->
          androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val st = visibleStations[i]
            val owned = boxes.filter { it.ownerStationId == st.id }
            val initial = boxes.count { it.originStationId == st.id }
            AppCard {
                Text(st.name, style = MaterialTheme.typography.titleMedium)
                Text("Lead: ${st.leadName}", style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Available", "${owned.count { it.status == S.AVAILABLE }}", Modifier.weight(1f), Tone.SUCCESS)
                    StatCard("Reserved", "${owned.count { it.status == S.RESERVED || it.status == S.PICKED_UP }}", Modifier.weight(1f))
                    StatCard("Delivered", "${owned.count { it.status == S.DELIVERED }}", Modifier.weight(1f))
                }
                Text(
                    "Started with $initial box(es); now controls ${owned.size}.",
                    style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    owned.forEach { b ->
                        StatusBadge(
                            "${b.id} · ${b.status.replace('_', ' ')}",
                            when (b.status) { S.AVAILABLE -> Tone.SUCCESS; S.DELIVERED -> Tone.NEUTRAL; else -> Tone.INFO }
                        )
                    }
                }
            }
            SectionHeader("Requests — ${st.name}")
            val stReq = requests.filter { it.stationId == st.id }
            if (stReq.isEmpty()) EmptyState("No requests", "Requests for this station appear here.")
            stReq.forEach { RequestCard(it, actor, vm) }
            val stDel = deliveries.filter { it.stationId == st.id }
            if (stDel.isNotEmpty()) SectionHeader("Deliveries — ${st.name}")
            stDel.forEach { DeliveryCard(it, actor, vm) { d -> pickupFor = d } }
          }
        }

        if (actor.role == Role.VOLUNTEER || actor.role == Role.RESIDENT) {
            if (actor.role == Role.VOLUNTEER) {
                item { SectionHeader("Assignments") }
                if (myDeliveries.isEmpty()) item { EmptyState("Nothing assigned", "Deliveries assigned to you appear here.") }
            }
            items(myDeliveries.size) { i -> DeliveryCard(myDeliveries[i], actor, vm) { d -> pickupFor = d } }
        }
    }

    pickupFor?.let { d ->
        var input by remember(d.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pickupFor = null },
            title = { Text("Confirm pickup") },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the ID printed on each box (separate with commas). Boxes for this delivery: ${d.boxIds}")
                    OutlinedTextField(input, { input = it }, label = { Text("Box ID(s)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.pickUp(d.id, input.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                    pickupFor = null
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { pickupFor = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RequestCard(r: RequestEntity, actor: Actor, vm: MainViewModel) {
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${r.id} · ${r.familyId}", style = MaterialTheme.typography.titleMedium)
            StatusBadge(r.status.replace('_', ' '), requestTone(r.status))
        }
        Text("${r.quantity} box(es) · ${r.stationId} · ${if (r.language == "ur") "Urdu" else "English"}", style = MaterialTheme.typography.bodyMedium)
        Text(r.rawText, style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary, maxLines = 3)
        if (r.status == S.NEEDS_REVIEW) StatusBadge("Low-confidence reading — please check details", Tone.WARNING)
        val canManage = actor.role == Role.DRILL_LEADER || (actor.role == Role.STATION_LEAD && actor.stationId == r.stationId)
        if (canManage && (r.status == S.NEW || r.status == S.NEEDS_REVIEW)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Approve", { vm.approve(r.id) }, Modifier.weight(1f))
                SecondaryButton("Reject", { vm.reject(r.id) }, Modifier.weight(1f))
            }
        }
        if (canManage && r.status == S.APPROVED) PrimaryButton("Assign delivery", { vm.assign(r.id) }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun DeliveryCard(d: DeliveryEntity, actor: Actor, vm: MainViewModel, onPickup: (DeliveryEntity) -> Unit) {
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${d.id} · ${d.familyId}", style = MaterialTheme.typography.titleMedium)
        }
        StatusBadge(deliveryLabel(d.status), deliveryTone(d.status))
        Text("From ${d.stationId} · boxes ${d.boxIds} · volunteer ${d.volunteerId ?: "none"}", style = MaterialTheme.typography.bodyMedium)
        d.failureReason?.let { Text("Reason: $it", style = MaterialTheme.typography.bodyMedium, color = MohallaColors.Danger) }
        val mine = actor.role == Role.VOLUNTEER && actor.volunteerId == d.volunteerId
        when {
            mine && d.status == S.D_ASSIGNED -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Accept", { vm.accept(d.id) }, Modifier.weight(1f))
                SecondaryButton("Decline", { vm.decline(d.id) }, Modifier.weight(1f))
            }
            mine && d.status == S.ACCEPTED -> PrimaryButton("Enter box ID & pick up", { onPickup(d) }, Modifier.fillMaxWidth())
            mine && d.status == S.D_PICKED_UP -> PrimaryButton("Record handoff", { vm.handoff(d.id) }, Modifier.fillMaxWidth())
            d.status == S.D_DELIVERED && (actor.role == Role.DRILL_LEADER || (actor.role == Role.RESIDENT && actor.familyId == d.familyId)) ->
                PrimaryButton("Confirm I received it", { vm.confirmReceipt(d.id) }, Modifier.fillMaxWidth())
            d.status == S.PENDING_ASSIGNMENT && (actor.role == Role.DRILL_LEADER || (actor.role == Role.STATION_LEAD && actor.stationId == d.stationId)) ->
                PrimaryButton("Choose replacement volunteer", { vm.reassign(d.id) }, Modifier.fillMaxWidth())
        }
    }
}
