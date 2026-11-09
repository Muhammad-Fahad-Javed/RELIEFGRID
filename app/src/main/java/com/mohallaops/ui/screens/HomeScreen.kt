package com.mohallaops.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohallaops.R
import com.mohallaops.domain.DemoActors
import com.mohallaops.domain.Role
import com.mohallaops.domain.S
import com.mohallaops.ui.MainViewModel
import com.mohallaops.ui.components.AppCard
import com.mohallaops.ui.components.OfflineBanner
import com.mohallaops.ui.components.PrimaryButton
import com.mohallaops.ui.components.SecondaryButton
import com.mohallaops.ui.components.SectionHeader
import com.mohallaops.ui.components.StatCard
import com.mohallaops.ui.components.StatusBadge
import com.mohallaops.ui.components.Tone
import com.mohallaops.ui.theme.MohallaColors

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onNewRequest: () -> Unit,
    onNewIncident: () -> Unit,
    onOpen: (String) -> Unit
) {
    val actor by vm.actor.collectAsStateWithLifecycle()
    val families by vm.families.collectAsStateWithLifecycle()
    val boxes by vm.boxes.collectAsStateWithLifecycle()
    val requests by vm.requests.collectAsStateWithLifecycle()
    val deliveries by vm.deliveries.collectAsStateWithLifecycle()
    val volunteers by vm.volunteers.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val incidents by vm.incidents.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()

    val confirmed = deliveries.count { it.status == S.CONFIRMED }
    val pending = requests.count { it.status == S.NEW || it.status == S.NEEDS_REVIEW || it.status == S.APPROVED }
    val waiting = families.count { it.status != S.SERVED }
    val served = families.count { it.status == S.SERVED }
    val activeVolunteers = volunteers.count { it.available }
    val queued = events.count { it.syncStatus == S.SYNC_WAITING }
    val activeIncidents = incidents.count { it.status != S.INC_RESOLVED }
    val criticalIncidents = incidents.count { it.status != S.INC_RESOLVED && it.urgency == S.URGENCY_CRITICAL }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: RELIEFGRID branding
        item {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MohallaColors.Navy,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                stringResource(R.string.demo_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = MohallaColors.TextSecondary
            )
        }

        // Active Emergency Alert Card (if any active incidents)
        if (activeIncidents > 0) {
            item {
                AppCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (criticalIncidents > 0) MohallaColors.Danger else MohallaColors.Warning
                            )
                            Text(
                                "Emergency Alerts",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (criticalIncidents > 0) MohallaColors.Danger else MohallaColors.Warning
                            )
                        }
                        StatusBadge(
                            "$activeIncidents active",
                            if (criticalIncidents > 0) Tone.DANGER else Tone.WARNING
                        )
                    }
                    Text(
                        if (criticalIncidents > 0)
                            "$criticalIncidents CRITICAL incident(s) requiring immediate rescue/supplies coordination."
                        else
                            "$activeIncidents reported incident(s) under review.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            "View Incidents",
                            onClick = { onOpen("incidents") },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            "Report Incident",
                            onClick = onNewIncident,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // AI status indicator
        item {
            val aiTone = if (vm.aiConfigured) Tone.SUCCESS else Tone.NEUTRAL
            val aiLabel = if (vm.aiConfigured) "AI: Gemini backend active (triage + extraction)" else "AI: On-device mode (rule engine triage + extraction)"
            StatusBadge(aiLabel, aiTone)
        }

        // Role selector
        item {
            SectionHeader(stringResource(R.string.demo_account_label))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DemoActors.all) { a ->
                    FilterChip(
                        selected = a == actor,
                        onClick = { vm.switchActor(a) },
                        label = { Text(a.name) }
                    )
                }
            }
        }

        // Active drill card
        item {
            AppCard {
                Text(
                    "Neighborhood flood-relief drill",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(stringResource(R.string.drill_active), Tone.SUCCESS)
                    StatusBadge(actor.role.label, Tone.INFO)
                }
                // Progress summary
                if (families.isNotEmpty()) {
                    val pct = (served.toFloat() / families.size * 100).toInt()
                    Text(
                        "$served / ${families.size} families served ($pct%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (waiting == 0) MohallaColors.Success else MohallaColors.TextSecondary
                    )
                }
                OfflineBanner(offline, queued)
                PrimaryButton(
                    stringResource(R.string.continue_drill),
                    onClick = { onOpen(if (actor.role == Role.DRILL_LEADER) "drill" else "stations") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Stats grid — computed from real DB data
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Families", "${families.size}", Modifier.weight(1f))
                StatCard("Supply boxes", "${boxes.size}", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    "Served",
                    "$served",
                    Modifier.weight(1f),
                    if (served > 0) Tone.SUCCESS else null
                )
                StatCard(
                    "Waiting",
                    "$waiting",
                    Modifier.weight(1f),
                    if (waiting > 0) Tone.WARNING else Tone.SUCCESS
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Pending requests", "$pending", Modifier.weight(1f))
                StatCard("Active volunteers", "$activeVolunteers", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    "Confirmed deliveries",
                    "$confirmed",
                    Modifier.weight(1f),
                    if (confirmed > 0) Tone.SUCCESS else null
                )
                StatCard(
                    "Active incidents",
                    "$activeIncidents",
                    Modifier.weight(1f),
                    if (activeIncidents > 0) Tone.WARNING else Tone.SUCCESS
                )
            }
        }

        // Role-specific quick actions
        item {
            SectionHeader(stringResource(R.string.quick_actions))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                // Emergency report accessible to all roles
                SecondaryButton(
                    "Report Emergency Incident",
                    onNewIncident,
                    Modifier.fillMaxWidth()
                )

                when (actor.role) {
                    Role.RESIDENT -> {
                        PrimaryButton(
                            stringResource(R.string.request_supplies),
                            onNewRequest,
                            Modifier.fillMaxWidth()
                        )
                        SecondaryButton(
                            stringResource(R.string.track_my_request),
                            { onOpen("stations") },
                            Modifier.fillMaxWidth()
                        )
                    }
                    Role.VOLUNTEER -> {
                        PrimaryButton(
                            "Available deliveries & my assignments",
                            { onOpen("stations") },
                            Modifier.fillMaxWidth()
                        )
                        SecondaryButton(
                            "Emergency incident triage",
                            { onOpen("incidents") },
                            Modifier.fillMaxWidth()
                        )
                    }
                    Role.STATION_LEAD -> {
                        PrimaryButton(
                            "Incoming requests & inventory",
                            { onOpen("stations") },
                            Modifier.fillMaxWidth()
                        )
                        SecondaryButton(
                            "Verify reported incidents",
                            { onOpen("incidents") },
                            Modifier.fillMaxWidth()
                        )
                        SecondaryButton(
                            "View inventory history",
                            { onOpen("inventory") },
                            Modifier.fillMaxWidth()
                        )
                    }
                    Role.DRILL_LEADER -> {
                        PrimaryButton(
                            stringResource(R.string.run_or_reset_drill),
                            { onOpen("drill") },
                            Modifier.fillMaxWidth()
                        )
                        SecondaryButton(
                            stringResource(R.string.see_neighborhood_map),
                            { onOpen("map") },
                            Modifier.fillMaxWidth()
                        )
                        SecondaryButton(
                            "Incident Command Center",
                            { onOpen("incidents") },
                            Modifier.fillMaxWidth()
                        )
                        SecondaryButton(
                            "Inventory history",
                            { onOpen("inventory") },
                            Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Role guide
        item {
            SectionHeader("Role guide")
            AppCard {
                Text(
                    when (actor.role) {
                        Role.RESIDENT ->
                            "As a Resident: report emergency incidents (flooding, trapped persons, injuries) with AI triage assistance. " +
                            "Submit supply requests in Urdu or English. Track requests on the Stations tab and confirm receipt once delivered."
                        Role.VOLUNTEER ->
                            "As a Volunteer: accept assigned deliveries on the Stations tab. " +
                            "Pick up boxes from the station (enter box IDs). " +
                            "Record the handoff, help resolve reported field incidents, and wait for recipient confirmation."
                        Role.STATION_LEAD ->
                            "As a Station Lead: review and approve requests for your station. " +
                            "Verify reported emergency incidents and coordinate volunteer response. " +
                            "Assign volunteers to deliveries and manage inventory."
                        Role.DRILL_LEADER ->
                            "As Drill Leader: coordinate overall incident response. " +
                            "Run automated demo scenarios (Run 1 and Run 2). " +
                            "Use the AI planner to rebalance supplies and manage road disruptions."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MohallaColors.TextSecondary
                )
            }
        }

        // Data attribution note
        item {
            Text(
                "All statistics above are computed from recorded database actions. No values are fabricated.",
                style = MaterialTheme.typography.labelMedium,
                color = MohallaColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
    }
}
