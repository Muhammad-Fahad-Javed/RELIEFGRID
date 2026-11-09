package com.mohallaops.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohallaops.ai.IncidentClassification
import com.mohallaops.domain.S
import com.mohallaops.ui.MainViewModel
import com.mohallaops.ui.components.AiPanel
import com.mohallaops.ui.components.PrimaryButton
import com.mohallaops.ui.components.SecondaryButton
import com.mohallaops.ui.components.StatusBadge
import com.mohallaops.ui.components.Tone
import com.mohallaops.ui.theme.MohallaColors

/**
 * Incident reporting bottom sheet modal.
 * Supports typed or speech input (Urdu or English), AI Triage assistance,
 * human confirmation of details, and safe fallback.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IncidentSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val actor by vm.actor.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var text by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(S.CAT_FLOOD) }
    var selectedUrgency by remember { mutableStateOf(S.URGENCY_HIGH) }
    var selectedStation by remember { mutableStateOf("NORTH") }
    var voiceLang by remember { mutableStateOf("ur-PK") }
    var classification by remember { mutableStateOf<IncidentClassification?>(null) }
    var triaging by remember { mutableStateOf(false) }

    val categories = listOf(
        S.CAT_FLOOD to "Flood Water",
        S.CAT_ROAD to "Blocked Road",
        S.CAT_MEDICAL to "Medical",
        S.CAT_TRAPPED to "Trapped Persons",
        S.CAT_INFRASTRUCTURE to "Infrastructure",
        S.CAT_OTHER to "Other"
    )

    val urgencies = listOf(
        S.URGENCY_CRITICAL to Tone.DANGER,
        S.URGENCY_HIGH to Tone.WARNING,
        S.URGENCY_MEDIUM to Tone.INFO,
        S.URGENCY_LOW to Tone.NEUTRAL
    )

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let {
                text = it
            }
        }
    }

    fun launchVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceLang)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (voiceLang == "ur-PK") "ایمرجنسی صورتحال بیان کریں" else "Describe the emergency")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            vm.toast("Speech recognition is not available. Please type your report.")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MohallaColors.Background
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Report Emergency / Incident",
                style = MaterialTheme.typography.titleLarge,
                color = MohallaColors.Navy
            )
            Text(
                "Reporting as: ${actor.name} (${actor.role.label})",
                style = MaterialTheme.typography.bodyMedium,
                color = MohallaColors.TextSecondary
            )

            // Voice or text entry
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Describe the situation (Urdu or English)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(voiceLang == "ur-PK", { voiceLang = "ur-PK" }, { Text("Urdu voice") })
                FilterChip(voiceLang == "en-US", { voiceLang = "en-US" }, { Text("English voice") })
            }

            SecondaryButton("Speak emergency description", { launchVoice() }, Modifier.fillMaxWidth())

            // AI Triage Action
            PrimaryButton(
                if (triaging) "Analyzing situation…" else "AI Incident Triage",
                onClick = {
                    triaging = true
                    vm.triageIncident(text) { result ->
                        triaging = false
                        classification = result
                        selectedCategory = result.category
                        selectedUrgency = result.urgency
                        result.stationId?.let { selectedStation = it }
                        if (locationText.isBlank() && result.location != null) {
                            locationText = result.location
                        }
                    }
                },
                enabled = text.isNotBlank() && !triaging,
                modifier = Modifier.fillMaxWidth()
            )

            // AI Triage Results Panel
            classification?.let { c ->
                AiPanel("AI Triage Assessment (${c.source})") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadge("Category: ${c.category}", Tone.INFO)
                        StatusBadge(
                            "Priority: ${c.urgency}",
                            when (c.urgency) {
                                S.URGENCY_CRITICAL -> Tone.DANGER
                                S.URGENCY_HIGH -> Tone.WARNING
                                else -> Tone.INFO
                            }
                        )
                    }
                    Text(c.summary, style = MaterialTheme.typography.bodyMedium)
                    Text("Recommended Action:", style = MaterialTheme.typography.labelLarge)
                    Text(c.recommendedAction, style = MaterialTheme.typography.bodyMedium)
                    if (c.missingInfo.isNotEmpty()) {
                        StatusBadge("Missing details: ${c.missingInfo.joinToString()}", Tone.WARNING)
                    }
                    Text(
                        "AI Advisory: Life-safety decisions require on-ground human verification.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MohallaColors.TextSecondary
                    )
                }
            }

            Text("Verify or Adjust Details", style = MaterialTheme.typography.titleMedium)

            // Category Chips
            Text("Incident Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { (cat, label) ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(label) }
                    )
                }
            }

            // Urgency Selector
            Text("Urgency Level", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                urgencies.forEach { (urg, tone) ->
                    FilterChip(
                        selected = selectedUrgency == urg,
                        onClick = { selectedUrgency = urg },
                        label = { Text(urg) }
                    )
                }
            }

            // Station Selection
            Text("Nearest Relief Station", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selectedStation == "NORTH", { selectedStation = "NORTH" }, { Text("North Station") })
                FilterChip(selectedStation == "SOUTH", { selectedStation = "SOUTH" }, { Text("South Station") })
            }

            // Location Text
            OutlinedTextField(
                value = locationText,
                onValueChange = { locationText = it.take(200) },
                label = { Text("Exact location / Landmark") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            val isValid = text.isNotBlank() && locationText.isNotBlank()

            PrimaryButton(
                "Submit Incident Report",
                onClick = {
                    vm.reportIncident(
                        category = selectedCategory,
                        urgency = selectedUrgency,
                        locationText = locationText,
                        description = text,
                        stationId = selectedStation,
                        aiSummary = classification?.recommendedAction,
                        onDone = onDismiss
                    )
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            )

            SecondaryButton("Cancel", onDismiss, Modifier.fillMaxWidth())
        }
    }
}
