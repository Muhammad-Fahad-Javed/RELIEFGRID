package com.mohallaops.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
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
    val offline by vm.offline.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var dismissedSuggestion by remember { mutableStateOf(false) }

    val isLeader = actor.role == Role.DRILL_LEADER
    val roadOpen = roads.firstOrNull()?.open ?: true
    val suggestion = Planner.analyse(stations, boxes, families, roadOpen)

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Drill control", style = MaterialTheme.typography.titleLarge, color = MohallaColors.Navy)
            if (!isLeader) StatusBadge("Only the Drill Leader can change the drill. Switch account on Home.", Tone.WARNING)
        }

        item {
            AppCard {
                Text("Automatic demo runs", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Each run resets to the same starting scenario (North 5 boxes / 2 families, South 1 box / 4 families), closes the same road, then serves every family it can.",
                    style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary
                )
                PrimaryButton(
                    "Run 1 — no transfer", enabled = isLeader, modifier = Modifier.fillMaxWidth(),
                    onClick = { confirm = "Reset the drill and run Run 1?" to { vm.runDemo(1, false) } }
                )
                PrimaryButton(
                    "Run 2 — move 3 boxes North → South first", enabled = isLeader, modifier = Modifier.fillMaxWidth(),
                    onClick = { confirm = "Reset the drill and run Run 2?" to { vm.runDemo(2, true) } }
                )
                SecondaryButton("Compare results", onResults, Modifier.fillMaxWidth())
            }
        }

        item {
            SectionHeader("AI planning assistant")
            if (suggestion == null || dismissedSuggestion) {
                AppCard {
                    Text(
                        if (suggestion == null) "No shortfall detected: every waiting family has a box at its own station."
                        else "Suggestion dismissed.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (dismissedSuggestion) SecondaryButton("Show again", { dismissedSuggestion = false })
                }
            } else {
                AiPanel("Problem found") {
                    Text(suggestion.problem, style = MaterialTheme.typography.bodyLarge)
                    Text("Supporting data", style = MaterialTheme.typography.labelLarge)
                    suggestion.evidence.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    Text("Suggested action: ${suggestion.action}", style = MaterialTheme.typography.bodyMedium)
                    Text("Expected effect: ${suggestion.effect}", style = MaterialTheme.typography.bodyMedium)
                    Text("Limitations: ${suggestion.limitations}", style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary)
                    Text(
                        "Counts come from stored records (deterministic). This is practice guidance, not an emergency instruction.",
                        style = MaterialTheme.typography.labelMedium, color = MohallaColors.TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (suggestion.executable) {
                            PrimaryButton(
                                "Accept", enabled = isLeader, modifier = Modifier.weight(1f),
                                onClick = { vm.transfer(suggestion.from!!, suggestion.to!!, suggestion.count) }
                            )
                        }
                        SecondaryButton("Reject", { dismissedSuggestion = true }, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            SectionHeader("Disruptions (simulated)")
            AppCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Road North–South", style = MaterialTheme.typography.titleMedium)
                    StatusBadge(if (roadOpen) "Open" else "Closed", if (roadOpen) Tone.SUCCESS else Tone.DANGER)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Close road", { vm.setRoad(false) }, Modifier.weight(1f), enabled = isLeader && roadOpen)
                    SecondaryButton("Reopen road", { vm.setRoad(true) }, Modifier.weight(1f), enabled = isLeader && !roadOpen)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Internet disconnected", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = offline, onCheckedChange = { vm.setOffline(it) }, enabled = isLeader)
                }
                Text("These are drill conditions only; nothing here affects real roads or services.", style = MaterialTheme.typography.labelMedium, color = MohallaColors.TextSecondary)
            }
        }

        item {
            AppCard {
                Text("Manual steps", style = MaterialTheme.typography.titleMedium)
                SecondaryButton("Transfer 3 boxes North → South", { vm.transfer("NORTH", "SOUTH", 3) }, Modifier.fillMaxWidth(), enabled = isLeader)
                SecondaryButton("Serve every family possible", { vm.fulfil() }, Modifier.fillMaxWidth(), enabled = isLeader)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Record as Run 1", { vm.record(1) }, Modifier.weight(1f), enabled = isLeader)
                    SecondaryButton("Record as Run 2", { vm.record(2) }, Modifier.weight(1f), enabled = isLeader)
                }
                SecondaryButton(
                    "Reset drill", enabled = isLeader, modifier = Modifier.fillMaxWidth(),
                    onClick = { confirm = "Reset everything to the starting scenario?" to { vm.reset() } }
                )
            }
        }

        item {
            SectionHeader("Volunteers")
            AppCard {
                volunteers.forEach { v ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(v.name, style = MaterialTheme.typography.titleMedium)
                            Text("${v.stationId} · ${v.completed} confirmed deliveries", style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary)
                        }
                        StatusBadge(if (v.available) "Available" else "Unavailable", if (v.available) Tone.SUCCESS else Tone.DANGER)
                        Switch(checked = v.available, onCheckedChange = { vm.setVolunteer(v.id, it) }, enabled = isLeader)
                    }
                }
            }
        }
    }

    confirm?.let { (text, action) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Please confirm") },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { action(); confirm = null }) { Text("Yes") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } }
        )
    }
}
