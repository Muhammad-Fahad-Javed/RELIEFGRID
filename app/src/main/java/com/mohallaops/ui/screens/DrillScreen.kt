package com.mohallaops.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohallaops.ai.Planner
import com.mohallaops.domain.Role
import com.mohallaops.domain.S
import com.mohallaops.ui.MainViewModel
import com.mohallaops.ui.components.AiPanel
import com.mohallaops.ui.components.AppCard
import com.mohallaops.ui.components.PrimaryButton
import com.mohallaops.ui.components.SecondaryButton
import com.mohallaops.ui.components.SectionHeader
import com.mohallaops.ui.components.StatusBadge
import com.mohallaops.ui.components.Tone
import com.mohallaops.ui.theme.MohallaColors

@Composable
fun DrillScreen(vm: MainViewModel, onResults: () -> Unit) {
    val actor by vm.actor.collectAsStateWithLifecycle()
    val stations by vm.stations.collectAsStateWithLifecycle()
    val boxes by vm.boxes.collectAsStateWithLifecycle()
    val families by vm.families.collectAsStateWithLifecycle()
    val roads by vm.roads.collectAsStateWithLifecycle()
    val volunteers by vm.volunteers.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var dismissedSuggestion by remember { mutableStateOf(false) }

    val isLeader = actor.role == Role.DRILL_LEADER
    val roadOpen = roads.firstOrNull()?.open ?: true
    val suggestion = Planner.analyse(stations, boxes, families, roadOpen)
    val hasRun1 = results.any { it.runLabel == 1 }
    val hasRun2 = results.any { it.runLabel == 2 }

    // Live summary stats
    val servedCount = families.count { it.status == S.SERVED }
    val waitingCount = families.count { it.status != S.SERVED }
    val availBoxes = boxes.count { it.status == S.AVAILABLE }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Drill control",
                style = MaterialTheme.typography.titleLarge,
                color = MohallaColors.Navy
            )
            if (!isLeader) {
                StatusBadge(
                    "Only the Drill Leader can change the drill. Switch account on Home.",
                    Tone.WARNING
                )
            }
        }

        // Live status summary
        item {
            AppCard {
                Text("Live status", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge("$servedCount served", if (servedCount > 0) Tone.SUCCESS else Tone.NEUTRAL)
                    StatusBadge("$waitingCount waiting", if (waitingCount > 0) Tone.WARNING else Tone.SUCCESS)
                    StatusBadge("$availBoxes boxes available", Tone.INFO)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        if (roadOpen) "Road open" else "Road CLOSED",
                        if (roadOpen) Tone.SUCCESS else Tone.DANGER
                    )
                    StatusBadge(
                        if (offline) "Offline" else "Online",
                        if (offline) Tone.WARNING else Tone.SUCCESS
                    )
                }
                if (hasRun1) StatusBadge("Run 1 recorded", Tone.SUCCESS)
                if (hasRun2) StatusBadge("Run 2 recorded", Tone.SUCCESS)
            }
        }

        // Automated demo runs
        item {
            SectionHeader("Automatic demo runs")
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MohallaColors.Teal)
                    Text("Automated demo runs", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Each run resets to the same starting scenario (North 5 boxes / 2 families, " +
                    "South 1 box / 4 families), closes the same road, then serves every " +
                    "family it can. Results are computed from real recorded actions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MohallaColors.TextSecondary
                )
                PrimaryButton(
                    text = if (hasRun1) "▶  Re-run Run 1 — no transfer" else "▶  Run 1 — no transfer",
                    enabled = isLeader,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        confirm = "Reset the drill and run Run 1 (no pre-transfer)?" to { vm.runDemo(1, false) }
                    }
                )
                PrimaryButton(
                    text = if (hasRun2) "▶  Re-run Run 2 — move 3 boxes North → South" else "▶  Run 2 — move 3 boxes North → South first",
                    enabled = isLeader,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        confirm = "Reset the drill and run Run 2 (3-box pre-transfer)?" to { vm.runDemo(2, true) }
                    }
                )
                SecondaryButton(
                    text = "Compare results",
                    onClick = onResults,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // AI planning assistant
        item {
            SectionHeader("AI planning assistant")
            if (suggestion == null || dismissedSuggestion) {
                AppCard {
                    Text(
                        if (suggestion == null)
                            "No shortfall detected: every waiting family has at least one box available at its own station."
                        else
                            "Suggestion dismissed. Reset the drill or change conditions to trigger a new analysis.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MohallaColors.TextSecondary
                    )
                    if (dismissedSuggestion) {
                        SecondaryButton("Show suggestion again", { dismissedSuggestion = false })
                    }
                }
            } else {
                AiPanel("Problem detected") {
                    Text(suggestion.problem, style = MaterialTheme.typography.bodyLarge)
                    Text("Supporting data", style = MaterialTheme.typography.labelLarge)
                    suggestion.evidence.forEach { ev ->
                        Text("• $ev", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "Suggested action: ${suggestion.action}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Expected effect: ${suggestion.effect}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Limitations: ${suggestion.limitations}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MohallaColors.TextSecondary
                    )
                    Text(
                        "Counts come from stored database records (deterministic). " +
                        "This is practice guidance only — not an emergency instruction.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MohallaColors.TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (suggestion.executable) {
                            PrimaryButton(
                                "Accept — execute transfer",
                                enabled = isLeader,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    vm.transfer(suggestion.from!!, suggestion.to!!, suggestion.count)
                                    dismissedSuggestion = true
                                }
                            )
                        }
                        SecondaryButton(
                            "Dismiss",
                            { dismissedSuggestion = true },
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Disruptions
        item {
            SectionHeader("Disruptions (simulated)")
            AppCard {
                // Road control
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Road North–South", style = MaterialTheme.typography.titleMedium)
                    StatusBadge(
                        if (roadOpen) "Open" else "Closed",
                        if (roadOpen) Tone.SUCCESS else Tone.DANGER
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        "Close road",
                        { vm.setRoad(false) },
                        Modifier.weight(1f),
                        enabled = isLeader && roadOpen
                    )
                    SecondaryButton(
                        "Reopen road",
                        { vm.setRoad(true) },
                        Modifier.weight(1f),
                        enabled = isLeader && !roadOpen
                    )
                }
                // Offline toggle
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Internet disconnected", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = offline,
                        onCheckedChange = { vm.setOffline(it) },
                        enabled = isLeader
                    )
                }
                Text(
                    "These are drill conditions only — nothing here affects real roads or services.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MohallaColors.TextSecondary
                )
            }
        }

        // Manual steps
        item {
            SectionHeader("Manual steps")
            AppCard {
                Text(
                    "Execute individual steps manually to explore the scenario freely.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MohallaColors.TextSecondary
                )
                SecondaryButton(
                    "Transfer 3 boxes North → South",
                    { vm.transfer("NORTH", "SOUTH", 3) },
                    Modifier.fillMaxWidth(),
                    enabled = isLeader && roadOpen
                )
                SecondaryButton(
                    "Serve every family possible",
                    { vm.fulfil() },
                    Modifier.fillMaxWidth(),
                    enabled = isLeader
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        "Record as Run 1",
                        { vm.record(1) },
                        Modifier.weight(1f),
                        enabled = isLeader
                    )
                    SecondaryButton(
                        "Record as Run 2",
                        { vm.record(2) },
                        Modifier.weight(1f),
                        enabled = isLeader
                    )
                }
                SecondaryButton(
                    "Reset drill",
                    enabled = isLeader,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        confirm = "Reset everything to the starting scenario? Run results are preserved." to {
                            vm.reset()
                            dismissedSuggestion = false
                        }
                    }
                )
            }
        }

        // Volunteer management
        item {
            SectionHeader("Volunteers")
            AppCard {
                if (volunteers.isEmpty()) {
                    Text(
                        "No volunteers seeded yet. Reset the drill to restore the starting scenario.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MohallaColors.TextSecondary
                    )
                }
                volunteers.forEach { v ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(v.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${v.stationId} · ${v.completed} confirmed deliveries",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MohallaColors.TextSecondary
                            )
                        }
                        StatusBadge(
                            if (v.available) "Available" else "Unavailable",
                            if (v.available) Tone.SUCCESS else Tone.DANGER
                        )
                        Switch(
                            checked = v.available,
                            onCheckedChange = { vm.setVolunteer(v.id, it) },
                            enabled = isLeader
                        )
                    }
                }
                Text(
                    "Toggling a volunteer unavailable auto-reassigns their active deliveries or marks them pending a replacement.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MohallaColors.TextSecondary
                )
            }
        }
    }

    // Confirmation dialog for destructive actions
    confirm?.let { (text, action) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Please confirm") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { action(); confirm = null }) { Text("Yes, proceed") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Cancel") }
            }
        )
    }
}
