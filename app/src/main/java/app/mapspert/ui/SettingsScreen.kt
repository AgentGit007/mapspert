@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.mapspert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mapspert.AppViewModel
import app.mapspert.ThemeMode
import app.mapspert.quiz.Difficulty

@Composable
fun SettingsScreen(vm: AppViewModel) {
    var name by remember { mutableStateOf(vm.player) }
    var confirmClear by remember { mutableStateOf(false) }
    val players = remember(vm.records, vm.player) { vm.knownPlayers() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TopBar("Settings") { vm.back() }
        Spacer(Modifier.height(12.dp))

        Section("Who is playing")
        Mono("Scores are filed under this name, so a friend's round stays out of your own statistics.", alpha = 0.7f)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; vm.choosePlayer(it) },
            singleLine = true,
            label = { Text("Player name") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (players.size > 1) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                players.forEach { p ->
                    FilterChip(
                        selected = vm.player == p,
                        onClick = { name = p; vm.choosePlayer(p) },
                        label = { Text(p) },
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Section("Default difficulty")
        Difficulty.values().forEach { d ->
            FilterChip(
                selected = vm.difficulty == d,
                onClick = { vm.chooseDifficulty(d) },
                label = { Text("${d.label} - ${d.blurb}") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
        }

        Spacer(Modifier.height(14.dp))
        Section("Questions per round")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 0).forEach { v ->
                FilterChip(
                    selected = vm.questionsPerRound == v,
                    onClick = { vm.chooseQuestionsPerRound(v) },
                    label = { Text(if (v == 0) "All" else "$v") },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Section("Rounds per session")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 3, 5).forEach { v ->
                FilterChip(selected = vm.rounds == v, onClick = { vm.chooseRounds(v) }, label = { Text("$v") })
            }
        }

        Spacer(Modifier.height(14.dp))
        Section("Appearance")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.values().forEach { m ->
                FilterChip(selected = vm.themeMode == m, onClick = { vm.setTheme(m) }, label = { Text(m.label) })
            }
        }

        Spacer(Modifier.height(20.dp))
        Section("Data")
        Mono("Everything is stored on this device only.", alpha = 0.7f)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { confirmClear = true }) { Text("Delete statistics") }
        Spacer(Modifier.height(28.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete statistics?") },
            text = { Text("This removes recorded rounds. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.clearRecords(vm.player); confirmClear = false }) {
                    Text("Delete ${vm.player}'s")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.clearRecords(null); confirmClear = false }) { Text("Delete all") }
            },
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
}
