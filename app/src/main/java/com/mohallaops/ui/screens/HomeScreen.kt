package com.mohallaops.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
fun HomeScreen(vm: MainViewModel, onNewRequest: () -> Unit, onOpen: (String) -> Unit) {
    val actor by vm.actor.collectAsStateWithLifecycle()
    val families by vm.families.collectAsStateWithLifecycle()
    val boxes by vm.boxes.collectAsStateWithLifecycle()
    val requests by vm.requests.collectAsStateWithLifecycle()
    val deliveries by vm.deliveries.collectAsStateWithLifecycle()
    val volunteers by vm.volunteers.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()

    val confirmed = deliveries.count { it.status == S.CONFIRMED }
    val pending = requests.count { it.status == S.NEW || it.status == S.NEEDS_REVIEW || it.status == S.APPROVED }
    val waiting = families.count { it.status != S.SERVED }
    val activeVolunteers = volunteers.count { it.available }
    val queued = events.count { it.syncStatus == S.SYNC_WAITING }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = MohallaColors.Navy)
            Text(stringResource(R.string.demo_banner), style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary)
        }
        item {
            SectionHeader("Demo account")
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
        item {
            AppCard {
                Text("Neighborhood flood-relief drill", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge("Drill active", Tone.SUCCESS)
                    StatusBadge(actor.role.label, Tone.INFO)
                }
                OfflineBanner(offline, queued)
                PrimaryButton(
                    "Continue drill",
                    onClick = { onOpen(if (actor.role == Role.DRILL_LEADER) "drill" else "stations") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Families", "${families.size}", Modifier.weight(1f))
                StatCard("Supply boxes", "${boxes.size}", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Confirmed deliveries", "$confirmed", Modifier.weight(1f), Tone.SUCCESS)
                StatCard("Families waiting", "$waiting", Modifier.weight(1f), if (waiting > 0) Tone.WARNING else Tone.SUCCESS)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Pending requests", "$pending", Modifier.weight(1f))
                StatCard("Active volunteers", "$activeVolunteers", Modifier.weight(1f))
            }
        }
        item {
            SectionHeader("Quick actions")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                when (actor.role) {
                    Role.RESIDENT -> {
                        PrimaryButton("Request supplies", onNewRequest, Modifier.fillMaxWidth())
                        SecondaryButton("Track my request", { onOpen("stations") }, Modifier.fillMaxWidth())
                    }
                    Role.VOLUNTEER -> {
                        PrimaryButton("Available deliveries & my assignments", { onOpen("stations") }, Modifier.fillMaxWidth())
                    }
                    Role.STATION_LEAD -> {
                        PrimaryButton("Incoming requests & inventory", { onOpen("stations") }, Modifier.fillMaxWidth())
                    }
                    Role.DRILL_LEADER -> {
                        PrimaryButton("Run or reset the drill", { onOpen("drill") }, Modifier.fillMaxWidth())
                        SecondaryButton("See neighborhood map", { onOpen("map") }, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
