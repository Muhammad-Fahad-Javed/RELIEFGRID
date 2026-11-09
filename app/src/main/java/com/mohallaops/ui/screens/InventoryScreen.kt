package com.mohallaops.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohallaops.data.InventoryTxEntity
import com.mohallaops.ui.MainViewModel
import com.mohallaops.ui.components.AppCard
import com.mohallaops.ui.components.EmptyState
import com.mohallaops.ui.components.SectionHeader
import com.mohallaops.ui.components.StatusBadge
import com.mohallaops.ui.components.Tone
import com.mohallaops.ui.theme.MohallaColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Inventory transaction history screen.
 * Shows every box movement (RESERVE, TRANSFER, PICKUP, DELIVERED) in reverse-chronological order.
 * Data is computed from stored records — no fake or placeholder values.
 */
@Composable
fun InventoryScreen(vm: MainViewModel) {
    val txList by vm.inventoryTx.collectAsStateWithLifecycle()
    val boxes by vm.boxes.collectAsStateWithLifecycle()
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Inventory history",
                style = MaterialTheme.typography.titleLarge,
                color = MohallaColors.Navy
            )
            Text(
                "${boxes.size} box(es) in circulation · ${txList.size} recorded transaction(s)",
                style = MaterialTheme.typography.bodyMedium,
                color = MohallaColors.TextSecondary
            )
        }

        // Box status summary
        item {
            SectionHeader("Current box status")
            AppCard {
                val byStatus = boxes.groupBy { it.status }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    byStatus.forEach { (status, list) ->
                        val tone = when (status) {
                            "AVAILABLE" -> Tone.SUCCESS
                            "DELIVERED" -> Tone.NEUTRAL
                            "RESERVED", "PICKED_UP" -> Tone.INFO
                            else -> Tone.WARNING
                        }
                        StatusBadge("${list.size} ${status.replace('_', ' ').lowercase()}", tone)
                    }
                }
                boxes.sortedBy { it.id }.forEach { b ->
                    val tone = when (b.status) {
                        "AVAILABLE" -> Tone.SUCCESS
                        "DELIVERED" -> Tone.NEUTRAL
                        "RESERVED", "PICKED_UP" -> Tone.INFO
                        else -> Tone.WARNING
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            b.id,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Owner: ${b.ownerStationId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MohallaColors.TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        StatusBadge(b.status.replace('_', ' '), tone)
                    }
                }
            }
        }

        // Transaction log
        item { SectionHeader("Transaction log") }

        if (txList.isEmpty()) {
            item {
                EmptyState(
                    "No transactions yet",
                    "Box movements and deliveries appear here as they happen."
                )
            }
        }

        items(txList, key = { it.id }) { tx ->
            TxCard(tx, fmt)
        }
    }
}

@Composable
private fun TxCard(tx: InventoryTxEntity, fmt: SimpleDateFormat) {
    val (tone, label) = when (tx.type) {
        "RESERVE"  -> Tone.INFO to "Reserved"
        "TRANSFER" -> Tone.WARNING to "Transferred"
        "PICKUP"   -> Tone.INFO to "Picked up"
        "DELIVERED" -> Tone.SUCCESS to "Delivered"
        else       -> Tone.NEUTRAL to tx.type
    }
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                fmt.format(Date(tx.ts)),
                style = MaterialTheme.typography.labelLarge,
                color = MohallaColors.TextSecondary
            )
            StatusBadge(label, tone)
        }
        Text("Box: ${tx.boxId}", style = MaterialTheme.typography.titleMedium)
        val movement = when {
            tx.fromStationId != null && tx.toStationId != null ->
                "From ${tx.fromStationId} → ${tx.toStationId}"
            tx.fromStationId != null ->
                "At ${tx.fromStationId}"
            else -> ""
        }
        if (movement.isNotBlank()) {
            Text(movement, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "By ${tx.actor}",
            style = MaterialTheme.typography.labelMedium,
            color = MohallaColors.TextSecondary
        )
    }
}
