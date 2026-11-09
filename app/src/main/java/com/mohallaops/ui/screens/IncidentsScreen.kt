package com.mohallaops.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohallaops.data.IncidentEntity
import com.mohallaops.domain.Role
import com.mohallaops.domain.S
import com.mohallaops.ui.MainViewModel
import com.mohallaops.ui.components.AppCard
import com.mohallaops.ui.components.EmptyState
import com.mohallaops.ui.components.PrimaryButton
import com.mohallaops.ui.components.SecondaryButton
import com.mohallaops.ui.components.StatusBadge
import com.mohallaops.ui.components.Tone
import com.mohallaops.ui.theme.MohallaColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncidentsScreen(vm: MainViewModel, onNewIncident: () -> Unit) {
    val incidents by vm.incidents.collectAsStateWithLifecycle()
    val actor by vm.actor.collectAsStateWithLifecycle()
    var filterUrgency by remember { mutableStateOf<String?>(null) }
    var filterStatus by remember { mutableStateOf("ACTIVE") }

    val filtered = incidents.filter { inc ->
        val matchesStatus = when (filterStatus) {
            "ACTIVE" -> inc.status != S.INC_RESOLVED
            "RESOLVED" -> inc.status == S.INC_RESOLVED
            else -> true
        }
        val matchesUrgency = filterUrgency == null || inc.urgency == filterUrgency
        matchesStatus && matchesUrgency
    }

    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.US)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Emergency Incidents",
                        style = MaterialTheme.typography.titleLarge,
                        color = MohallaColors.Navy
                    )
                    Text(
                        "${incidents.count { it.status != S.INC_RESOLVED }} active incident(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MohallaColors.TextSecondary
                    )
                }
            }
        }

        // Action button to report
        item {
            PrimaryButton(
                "Report New Emergency Incident",
                onClick = onNewIncident,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Status filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("ACTIVE" to "Active", "ALL" to "All", "RESOLVED" to "Resolved")) { (statusKey, label) ->
                    FilterChip(
                        selected = filterStatus == statusKey,
                        onClick = { filterStatus = statusKey },
                        label = { Text(label) }
                    )
                }
            }
        }

        // Urgency filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterUrgency == null,
                        onClick = { filterUrgency = null },
                        label = { Text("All Priorities") }
                    )
                }
                items(listOf(S.URGENCY_CRITICAL, S.URGENCY_HIGH, S.URGENCY_MEDIUM, S.URGENCY_LOW)) { urg ->
                    FilterChip(
                        selected = filterUrgency == urg,
                        onClick = { filterUrgency = if (filterUrgency == urg) null else urg },
                        label = { Text(urg) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    title = "No matching incidents",
                    body = "Reported emergencies will appear here with real-time AI triage recommendations."
                )
            }
        }

        items(filtered, key = { it.id }) { inc ->
            IncidentCard(inc, actor.role, vm, fmt)
        }
    }
}

@Composable
private fun IncidentCard(
    inc: IncidentEntity,
    role: Role,
    vm: MainViewModel,
    fmt: SimpleDateFormat
) {
    val urgencyTone = when (inc.urgency) {
        S.URGENCY_CRITICAL -> Tone.DANGER
        S.URGENCY_HIGH -> Tone.WARNING
        S.URGENCY_MEDIUM -> Tone.INFO
        else -> Tone.NEUTRAL
    }

    val statusTone = when (inc.status) {
        S.INC_RESOLVED -> Tone.SUCCESS
        S.INC_VERIFIED -> Tone.INFO
        else -> Tone.WARNING
    }

    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${inc.id} · ${inc.category}", style = MaterialTheme.typography.titleMedium)
            StatusBadge(inc.urgency, urgencyTone)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge("Status: ${inc.status}", statusTone)
            StatusBadge("Station: ${inc.stationId}", Tone.NEUTRAL)
        }

        Text(inc.description, style = MaterialTheme.typography.bodyLarge)

        Text(
            "Location: ${inc.locationText}",
            style = MaterialTheme.typography.bodyMedium,
            color = MohallaColors.Navy
        )

        inc.aiSummary?.let { summary ->
            Text(
                "AI Recommendation: $summary",
                style = MaterialTheme.typography.bodyMedium,
                color = MohallaColors.Teal
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Reported by: ${inc.reporterName}",
                style = MaterialTheme.typography.labelMedium,
                color = MohallaColors.TextSecondary
            )
            Text(
                fmt.format(Date(inc.createdAt)),
                style = MaterialTheme.typography.labelMedium,
                color = MohallaColors.TextSecondary
            )
        }

        // Action Buttons for Leads / Volunteers
        val canVerify = (role == Role.DRILL_LEADER || role == Role.STATION_LEAD) && inc.status == S.INC_REPORTED
        val canResolve = (role == Role.DRILL_LEADER || role == Role.STATION_LEAD || role == Role.VOLUNTEER) && inc.status != S.INC_RESOLVED

        if (canVerify || canResolve) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canVerify) {
                    SecondaryButton(
                        "Verify Incident",
                        onClick = { vm.verifyIncident(inc.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (canResolve) {
                    PrimaryButton(
                        "Mark Resolved",
                        onClick = { vm.resolveIncident(inc.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
