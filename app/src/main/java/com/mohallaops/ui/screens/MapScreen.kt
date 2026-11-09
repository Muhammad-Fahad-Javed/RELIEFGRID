package com.mohallaops.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohallaops.domain.S
import com.mohallaops.ui.MainViewModel
import com.mohallaops.ui.components.AppCard
import com.mohallaops.ui.components.StatusBadge
import com.mohallaops.ui.components.Tone
import com.mohallaops.ui.theme.MohallaColors

/**
 * RELIEFGRID Neighborhood Map.
 * Renders stations, families, road condition, active deliveries,
 * and reported disaster incidents on a real-time reactive canvas.
 */
@Composable
fun MapScreen(vm: MainViewModel) {
    val stations by vm.stations.collectAsStateWithLifecycle()
    val families by vm.families.collectAsStateWithLifecycle()
    val boxes by vm.boxes.collectAsStateWithLifecycle()
    val roads by vm.roads.collectAsStateWithLifecycle()
    val deliveries by vm.deliveries.collectAsStateWithLifecycle()
    val incidents by vm.incidents.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<String?>(null) }
    val roadOpen = roads.firstOrNull()?.open ?: true
    val activeIncidents = incidents.filter { it.status != S.INC_RESOLVED }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Neighborhood map", style = MaterialTheme.typography.titleLarge, color = MohallaColors.Navy)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(if (roadOpen) "Road open" else "Road CLOSED", if (roadOpen) Tone.SUCCESS else Tone.DANGER)
            StatusBadge("${activeIncidents.size} active incidents", if (activeIncidents.isNotEmpty()) Tone.WARNING else Tone.SUCCESS)
            StatusBadge("Simulated map", Tone.NEUTRAL)
        }

        BoxWithConstraints(
            Modifier.fillMaxWidth().aspectRatio(0.85f)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, MohallaColors.Border), RoundedCornerShape(16.dp))
        ) {
            val w = maxWidth
            val h = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                fun pt(x: Float, y: Float) = Offset(size.width * x, size.height * y)
                val byId = stations.associateBy { it.id }

                // spokes from each station to its families
                families.forEach { f ->
                    val st = byId[f.homeStationId] ?: return@forEach
                    drawLine(MohallaColors.Border, pt(st.x, st.y), pt(f.x, f.y), strokeWidth = 3f)
                }

                // active delivery routes (assigned but not yet confirmed)
                deliveries.filter { it.status != S.CONFIRMED && it.status != S.DECLINED && it.status != S.FAILED }.forEach { d ->
                    val st = byId[d.stationId]; val f = families.firstOrNull { it.id == d.familyId }
                    if (st != null && f != null) {
                        drawLine(
                            MohallaColors.Teal, pt(st.x, st.y), pt(f.x, f.y), strokeWidth = 7f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                        )
                    }
                }

                // connecting road
                val n = byId["NORTH"]; val s = byId["SOUTH"]
                if (n != null && s != null) {
                    drawLine(
                        if (roadOpen) MohallaColors.Success else MohallaColors.Danger,
                        pt(n.x, n.y), pt(s.x, s.y), strokeWidth = 12f,
                        pathEffect = if (roadOpen) null else PathEffect.dashPathEffect(floatArrayOf(10f, 22f))
                    )
                }
            }

            // road status label at the midpoint
            Box(Modifier.align(Alignment.Center)) {
                StatusBadge(if (roadOpen) "Road open" else "Road closed ✕", if (roadOpen) Tone.SUCCESS else Tone.DANGER)
            }

            // Stations
            stations.forEach { st ->
                val count = boxes.count { it.ownerStationId == st.id && it.status == S.AVAILABLE }
                Column(
                    Modifier.offset(x = w * st.x - 55.dp, y = h * st.y - 26.dp)
                        .size(110.dp, 52.dp)
                        .background(MohallaColors.Navy, RoundedCornerShape(12.dp))
                        .clickable { selected = st.id }
                        .semantics { contentDescription = "${st.name}, $count boxes available" },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Text(st.name, color = Color.White, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    Text("$count box(es)", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }

            // Families
            families.forEach { f ->
                val served = f.status == S.SERVED
                Column(
                    Modifier.offset(x = w * f.x - 33.dp, y = h * f.y - 22.dp)
                        .size(66.dp, 44.dp)
                        .background(if (served) MohallaColors.SuccessBg else MohallaColors.WarningBg, RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, if (served) MohallaColors.Success else MohallaColors.Warning), RoundedCornerShape(10.dp))
                        .clickable { selected = f.id }
                        .semantics { contentDescription = "${f.label}, ${if (served) "served" else "waiting"}" },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Text(f.id.removePrefix("FAM-"), style = MaterialTheme.typography.labelLarge, color = if (served) MohallaColors.Success else MohallaColors.Warning)
                    Text(if (served) "✓ served" else "… waiting", style = MaterialTheme.typography.labelMedium, color = if (served) MohallaColors.Success else MohallaColors.Warning)
                }
            }

            // Active Incident Pins on Map
            activeIncidents.forEach { inc ->
                val incColor = if (inc.urgency == S.URGENCY_CRITICAL) MohallaColors.Danger else MohallaColors.Warning
                Box(
                    Modifier.offset(x = w * inc.x - 14.dp, y = h * inc.y - 14.dp)
                        .size(28.dp)
                        .background(incColor, CircleShape)
                        .border(BorderStroke(2.dp, Color.White), CircleShape)
                        .clickable { selected = inc.id },
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Text(
            "Tap a station, family, or incident (!) icon to view details. Dashed teal lines represent active deliveries.",
            style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary
        )

        // Selected station card
        val st = stations.firstOrNull { it.id == selected }
        val fam = families.firstOrNull { it.id == selected }
        val inc = incidents.firstOrNull { it.id == selected }

        if (st != null) {
            val owned = boxes.filter { it.ownerStationId == st.id }
            AppCard {
                Text(st.name, style = MaterialTheme.typography.titleMedium)
                Text("Lead: ${st.leadName}", style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary)
                Text(
                    "Available ${owned.count { it.status == S.AVAILABLE }} · Reserved/in transit ${owned.count { it.status == S.RESERVED || it.status == S.PICKED_UP }} · Delivered ${owned.count { it.status == S.DELIVERED }}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (fam != null) {
            val d = deliveries.firstOrNull { it.familyId == fam.id && it.status != S.DECLINED }
            AppCard {
                Text(fam.label, style = MaterialTheme.typography.titleMedium)
                StatusBadge(if (fam.status == S.SERVED) "Served — receipt confirmed" else "Waiting", if (fam.status == S.SERVED) Tone.SUCCESS else Tone.WARNING)
                Text(d?.let { "Delivery ${it.id}: ${deliveryLabel(it.status)}" } ?: "No delivery yet", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (inc != null) {
            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Incident: ${inc.id} (${inc.category})", style = MaterialTheme.typography.titleMedium)
                    StatusBadge(inc.urgency, if (inc.urgency == S.URGENCY_CRITICAL) Tone.DANGER else Tone.WARNING)
                }
                Text("Location: ${inc.locationText}", style = MaterialTheme.typography.bodyMedium, color = MohallaColors.Navy)
                Text(inc.description, style = MaterialTheme.typography.bodyMedium)
                inc.aiSummary?.let {
                    Text("AI Triage: $it", style = MaterialTheme.typography.bodyMedium, color = MohallaColors.Teal)
                }
            }
        }
    }
}
