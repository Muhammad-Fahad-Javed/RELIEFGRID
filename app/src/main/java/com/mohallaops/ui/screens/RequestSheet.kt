package com.mohallaops.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohallaops.ai.Extraction
import com.mohallaops.domain.Role
import com.mohallaops.ui.MainViewModel
import com.mohallaops.ui.components.AiPanel
import com.mohallaops.ui.components.PrimaryButton
import com.mohallaops.ui.components.SecondaryButton
import com.mohallaops.ui.components.StatusBadge
import com.mohallaops.ui.components.Tone
import com.mohallaops.ui.theme.MohallaColors

/**
 * Request creation + AI understanding in one flow:
 * 1) type or speak the message, 2) "Analyse" fills the fields, 3) the person edits and confirms.
 * Nothing is submitted automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val actor by vm.actor.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var text by remember { mutableStateOf("") }
    var station by remember { mutableStateOf<String?>(null) }
    var qty by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("en") }
    var voiceLang by remember { mutableStateOf("ur-PK") }
    var extraction by remember { mutableStateOf<Extraction?>(null) }
    var analysing by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { text = it }
        }
    }

    fun launchVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, voiceLang)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (voiceLang == "ur-PK") "اپنی درخواست بولیں" else "Say your request")
        }
        try { voiceLauncher.launch(intent) }
        catch (e: ActivityNotFoundException) { vm.toast("Speech recognition is not available on this device. Please type your request.") }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MohallaColors.Background) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Request supplies", style = MaterialTheme.typography.titleLarge, color = MohallaColors.Navy)
            if (actor.role != Role.RESIDENT) {
                StatusBadge("Switch to the Resident demo account to submit", Tone.WARNING)
            }

            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("Your message (Urdu or English)") },
                modifier = Modifier.fillMaxWidth(), minLines = 3
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(voiceLang == "ur-PK", { voiceLang = "ur-PK" }, { Text("Urdu voice") })
                FilterChip(voiceLang == "en-US", { voiceLang = "en-US" }, { Text("English voice") })
            }
            SecondaryButton("Speak your request", { launchVoice() }, Modifier.fillMaxWidth())
            Text(
                "Voice uses your phone's speech recognition. Always check the text before continuing — it can make mistakes.",
                style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary
            )
            PrimaryButton(
                if (analysing) "Analysing…" else "Analyse message",
                onClick = {
                    analysing = true
                    vm.analyse(text) { ex ->
                        analysing = false
                        extraction = ex
                        station = ex.stationId
                        qty = ex.quantity?.toString() ?: ""
                        details = ex.details
                        language = ex.language
                    }
                },
                enabled = text.isNotBlank() && !analysing,
                modifier = Modifier.fillMaxWidth()
            )

            extraction?.let { ex ->
                AiPanel("What we understood (${ex.source})") {
                    Text("Language: ${if (ex.language == "ur") "Urdu" else "English"}", style = MaterialTheme.typography.bodyMedium)
                    StatusBadge(
                        "Confidence ${(ex.confidence * 100).toInt()}% (estimate)",
                        if (ex.confidence >= 0.7f) Tone.SUCCESS else Tone.WARNING
                    )
                    Text(ex.explanation, style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary)
                    if (ex.missing.isNotEmpty()) {
                        StatusBadge("Missing: ${ex.missing.joinToString()}", Tone.WARNING)
                    }
                }
            }

            Text("Confirm or correct the details", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(station == "NORTH", { station = "NORTH" }, { Text("Near North Station") })
                FilterChip(station == "SOUTH", { station = "SOUTH" }, { Text("Near South Station") })
            }
            OutlinedTextField(
                value = qty, onValueChange = { qty = it.filter(Char::isDigit).take(2) },
                label = { Text("Number of boxes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = details, onValueChange = { details = it.take(300) },
                label = { Text("Additional information") }, modifier = Modifier.fillMaxWidth()
            )

            val qtyInt = qty.toIntOrNull()
            val valid = station != null && qtyInt != null && qtyInt in 1..10 && actor.role == Role.RESIDENT
            PrimaryButton(
                "Confirm request",
                onClick = {
                    vm.submitRequest(
                        familyId = actor.familyId.orEmpty(), stationId = station!!, qty = qtyInt!!,
                        raw = (text + if (details.isNotBlank()) " | $details" else "").take(1000),
                        language = language, confidence = extraction?.confidence ?: 1f,
                        onDone = onDismiss
                    )
                },
                enabled = valid, modifier = Modifier.fillMaxWidth()
            )
            SecondaryButton("Cancel", onDismiss, Modifier.fillMaxWidth())
        }
    }
}
