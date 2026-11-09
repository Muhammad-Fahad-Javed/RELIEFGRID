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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohallaops.domain.S
import com.mohallaops.ui.MainViewModel
import com.mohallaops.ui.components.AppCard
import com.mohallaops.ui.components.EmptyState
import com.mohallaops.ui.components.OfflineBanner
import com.mohallaops.ui.components.SecondaryButton
import com.mohallaops.ui.components.SectionHeader
import com.mohallaops.ui.components.StatusBadge
import com.mohallaops.ui.components.Tone
import com.mohallaops.ui.theme.MohallaColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Event type to display tone mapping for the activity timeline. */
private fun eventTone(type: String): Tone = when {
    type.contains("CONFIRMED") || type.contains("SERVED") || type.contains("SYNCED") -> Tone.SUCCESS
    type.contains("REJECTED") || type.contains("DECLINED") || type.contains("FAILED") -> Tone.DANGER
    type.contains("RESET") -> Tone.WARNING
    type.contains("TRANSFER") || type.contains("ASSIGNED") -> Tone.INFO
    else -> Tone.NEUTRAL
}

@Composable
fun ActivityScreen(vm: MainViewModel) {
    val events by vm.events.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
    val dateFmt = SimpleDateFormat("MMM d", Locale.US)
    val waiting = events.count { it.syncStatus == S.SYNC_WAITING }
    val synced = events.count { it.syncStatus == S.SYNCED }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Activity timeline",
                style = MaterialTheme.typography.titleLarge,
                color = MohallaColors.Navy
            )
            Text(
                "${events.size} event(s) · $synced synced · $waiting queued",
                style = MaterialTheme.typography.bodyMedium,
                color = MohallaColors.TextSecondary
            )
        }

        // Sync & exchange controls
        item {
            AppCard {
                OfflineBanner(offline, waiting)
                Text(
                    "Phone-to-phone exchange: copy this device's events log, " +
                    "send it to another phone (via message, share, or any method), " +
                    "and import there. Duplicates are detected and skipped automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MohallaColors.TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        "Copy updates",
                        {
                            scope.launch {
                                clipboard.setText(AnnotatedString(vm.exportEvents()))
                                vm.toast("Updates copied to clipboard (${events.size} events)")
                            }
                        },
                        Modifier.weight(1f)
                    )
                    SecondaryButton(
                        "Import updates",
                        {
                            val text = clipboard.getText()?.text
                            if (text.isNullOrBlank()) vm.toast("Clipboard is empty")
                            else vm.importEvents(text)
                        },
                        Modifier.weight(1f)
                    )
                }
                SecondaryButton(
                    "Sync now",
                    { vm.syncNow() },
                    Modifier.fillMaxWidth(),
                    enabled = waiting > 0 && !offline
                )
                Text(
                    "Note: \"Sync now\" marks queued events as synced locally. " +
                    "No cloud backend is configured in this build (see README).",
                    style = MaterialTheme.typography.labelMedium,
                    color = MohallaColors.TextSecondary
                )
            }
        }

        // Event list section header
        if (events.isNotEmpty()) {
            item { SectionHeader("Events (newest first)") }
        }

        if (events.isEmpty()) {
            item {
                EmptyState(
                    "No activity yet",
                    "Actions in the drill appear here in order. " +
                    "Start by running a demo or manually walking through the request workflow."
                )
            }
        }

        items(events, key = { it.id }) { e ->
            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${dateFmt.format(Date(e.ts))} ${timeFmt.format(Date(e.ts))}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MohallaColors.TextSecondary
                    )
                    StatusBadge(
                        if (e.syncStatus == S.SYNCED) "Synced" else "Queued",
                        if (e.syncStatus == S.SYNCED) Tone.SUCCESS else Tone.WARNING
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusBadge(
                        e.type.replace('_', ' '),
                        eventTone(e.type)
                    )
                }
                Text(e.message, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "By ${e.actor}" + (e.stationId?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = MohallaColors.TextSecondary
                )
            }
        }
    }
}
