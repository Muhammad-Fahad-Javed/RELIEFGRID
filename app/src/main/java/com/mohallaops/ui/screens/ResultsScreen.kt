package com.mohallaops.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohallaops.data.RunResultEntity
import com.mohallaops.domain.S
import com.mohallaops.ui.MainViewModel
import com.mohallaops.ui.components.AppCard
import com.mohallaops.ui.components.StatusBadge
import com.mohallaops.ui.components.Tone
import com.mohallaops.ui.theme.MohallaColors
import com.mohallaops.ui.theme.NumberStyle

@Composable
fun ResultsScreen(vm: MainViewModel) {
    val results by vm.results.collectAsStateWithLifecycle()
    val families by vm.families.collectAsStateWithLifecycle()
    val r1 = results.firstOrNull { it.runLabel == 1 }
    val r2 = results.firstOrNull { it.runLabel == 2 }
    val nTotal = families.count { it.homeStationId == "NORTH" }.takeIf { it > 0 } ?: 2
    val sTotal = families.count { it.homeStationId == "SOUTH" }.takeIf { it > 0 } ?: 4
    val live = families.count { it.status == S.SERVED }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Drill results", style = MaterialTheme.typography.titleLarge, color = MohallaColors.Navy)
            Text("Current drill: $live of ${families.size} families served (confirmed receipts only).", style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary)
        }
        item {
            AppCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Metric", Modifier.weight(1.4f), style = MaterialTheme.typography.labelLarge)
                    Text("First run", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
                    Text("Improved run", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
                }
                Row {
                    StatusBadge(if (r1 != null) "Run 1: recorded" else "Run 1: example values", if (r1 != null) Tone.SUCCESS else Tone.NEUTRAL)
                }
                Row {
                    StatusBadge(if (r2 != null) "Run 2: recorded" else "Run 2: example values", if (r2 != null) Tone.SUCCESS else Tone.NEUTRAL)
                }
                MetricRow("North families served", cell(r1, "$nTotal/$nTotal") { "${it.northServed}/$nTotal" }, cell(r2, "$nTotal/$nTotal") { "${it.northServed}/$nTotal" })
                MetricRow("South families served", cell(r1, "1/$sTotal") { "${it.southServed}/$sTotal" }, cell(r2, "$sTotal/$sTotal") { "${it.southServed}/$sTotal" })
                MetricRow("Families waiting", cell(r1, "3") { "${it.waiting}" }, cell(r2, "0") { "${it.waiting}" })
                MetricRow("Boxes transferred", cell(r1, "0") { "${it.boxesTransferred}" }, cell(r2, "3") { "${it.boxesTransferred}" })
                MetricRow("Avg. time to confirmed handoff", cell(r1, "—") { "${it.avgHandoffMs} ms" }, cell(r2, "—") { "${it.avgHandoffMs} ms" })
                Text(
                    "“Example values” come from the scenario description, not from a run. Press a Demo Run on the Drill tab to record real values. Automated runs finish in milliseconds, so handoff times only become meaningful with real people.",
                    style = MaterialTheme.typography.labelMedium, color = MohallaColors.TextSecondary
                )
            }
        }
        if (r1 != null) item { RunBar("Run 1 families served", r1) }
        if (r2 != null) item { RunBar("Run 2 families served", r2) }
        item {
            AppCard {
                Text("Measured findings", style = MaterialTheme.typography.titleMedium)
                if (r1 != null && r2 != null) {
                    val diff = r2.served - r1.served
                    Text("• Run 2 served $diff ${if (diff == 1) "family" else "families"} ${if (diff >= 0) "more" else "fewer"} than Run 1 (${r1.served} → ${r2.served}).", style = MaterialTheme.typography.bodyMedium)
                    Text("• Families waiting: ${r1.waiting} → ${r2.waiting}.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Boxes moved before the road closed: ${r1.boxesTransferred} → ${r2.boxesTransferred}.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Same starting stock and the same road closure in both runs; the only intended difference is the transfer.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Record both runs to see a before/after comparison.", style = MaterialTheme.typography.bodyMedium, color = MohallaColors.TextSecondary)
                }
                Text(
                    "These lines are computed from recorded results. They are measurements, not AI opinions. The AI after-action review is not included in this build.",
                    style = MaterialTheme.typography.labelMedium, color = MohallaColors.TextSecondary
                )
            }
        }
    }
}

private fun cell(r: RunResultEntity?, example: String, f: (RunResultEntity) -> String) = if (r != null) f(r) else "$example*"

@Composable
private fun MetricRow(label: String, a: String, b: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1.4f), style = MaterialTheme.typography.bodyMedium)
        Text(a, Modifier.weight(1f), style = NumberStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize, fontWeight = FontWeight.SemiBold), textAlign = TextAlign.End)
        Text(b, Modifier.weight(1f), style = NumberStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize, fontWeight = FontWeight.SemiBold), textAlign = TextAlign.End)
    }
}

@Composable
private fun RunBar(title: String, r: RunResultEntity) {
    AppCard {
        Text("$title: ${r.served}/${r.totalFamilies}", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { if (r.totalFamilies == 0) 0f else r.served.toFloat() / r.totalFamilies },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            color = MohallaColors.Teal, trackColor = MohallaColors.Border
        )
    }
}
