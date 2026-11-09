package com.mohallaops.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohallaops.ui.theme.MohallaColors
import com.mohallaops.ui.theme.NumberStyle

enum class Tone(val fg: Color, val bg: Color, val icon: ImageVector) {
    SUCCESS(MohallaColors.Success, MohallaColors.SuccessBg, Icons.Filled.Check),
    WARNING(MohallaColors.Warning, MohallaColors.WarningBg, Icons.Filled.Warning),
    DANGER(MohallaColors.Danger, MohallaColors.DangerBg, Icons.Filled.Close),
    NEUTRAL(MohallaColors.Neutral, MohallaColors.NeutralBg, Icons.Filled.Info),
    INFO(MohallaColors.Info, MohallaColors.InfoBg, Icons.Filled.Info)
}

/** Status is always shown as icon + text, never by colour alone. */
@Composable
fun StatusBadge(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(tone.bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(tone.icon, contentDescription = null, tint = tone.fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = tone.fg)
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MohallaColors.Border)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title, style = MaterialTheme.typography.titleMedium, color = MohallaColors.TextPrimary,
        modifier = modifier.padding(top = 8.dp)
    )
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, tone: Tone? = null) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MohallaColors.Border)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MohallaColors.TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(value, style = NumberStyle, color = tone?.fg ?: MohallaColors.Navy)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MohallaColors.Navy)
    ) { Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MohallaColors.Teal),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = MohallaColors.Teal, textAlign = TextAlign.Center) }
}

@Composable
fun OfflineBanner(offline: Boolean, waiting: Int, modifier: Modifier = Modifier) {
    val (text, tone) = when {
        offline -> "Offline mode — $waiting update(s) waiting to sync" to Tone.WARNING
        waiting > 0 -> "$waiting update(s) waiting to sync" to Tone.WARNING
        else -> "All updates synced" to Tone.SUCCESS
    }
    StatusBadge(text, tone, modifier)
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MohallaColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary, textAlign = TextAlign.Center)
    }
}

/** Panel used for every AI/planner suggestion; makes the "needs human approval" rule visible. */
@Composable
fun AiPanel(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MohallaColors.InfoBg),
        border = BorderStroke(1.dp, MohallaColors.Teal)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusBadge("Suggestion — needs your approval", Tone.INFO)
            Text(title, style = MaterialTheme.typography.titleMedium, color = MohallaColors.Navy)
            content()
        }
    }
}
