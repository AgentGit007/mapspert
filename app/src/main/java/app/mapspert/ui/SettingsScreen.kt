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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.mapspert.R
import app.mapspert.AppViewModel
import app.mapspert.Screen
import app.mapspert.ThemeMode
import app.mapspert.quiz.Difficulty

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(vm.player) }
    var confirmClear by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        message = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
            "Exported."
        }.getOrElse { "Could not write that file." }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        message = runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (text == null) "Could not read that file." else vm.importJson(text, replace = false).message
        }.getOrElse { "Could not read that file." }
        name = vm.player
    }
    val players = remember(vm.records, vm.player) { vm.knownPlayers() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        ScreenHeader(stringResource(R.string.ui_settings), onBack = { vm.back() })
        Spacer(Modifier.height(12.dp))

        Section("Who is playing")
        Mono(stringResource(R.string.ui_scores_are_filed_under_this_name_so_a_friend_s), alpha = 0.7f)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; vm.choosePlayer(it) },
            singleLine = true,
            label = { Text(stringResource(R.string.ui_player_name)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (players.size > 1) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                players.forEach { p ->
                    Pill(selected = vm.player == p, label = p, onClick = { name = p; vm.choosePlayer(p) })
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Section("Default difficulty")
        Difficulty.values().forEach { d ->
            val selected = vm.difficulty == d
            Card(
                Modifier.fillMaxWidth(),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                onClick = { vm.chooseDifficulty(d) },
            ) {
                val ink = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Text(d.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ink)
                Text(d.blurb, style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.8f))
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(14.dp))
        Section("Questions per round")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 0).forEach { v ->
                Pill(
                    selected = vm.questionsPerRound == v,
                    label = if (v == 0) "All" else "$v",
                    onClick = { vm.chooseQuestionsPerRound(v) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Section("Rounds per session")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 3, 5).forEach { v ->
                Pill(selected = vm.rounds == v, label = "$v", onClick = { vm.chooseRounds(v) })
            }
        }

        Spacer(Modifier.height(14.dp))
        Section("Practice")
        Toggle(
            on = vm.practiceWeighting,
            title = stringResource(R.string.ui_focus_on_weak_spots),
            blurb = stringResource(R.string.ui_ask_more_often_about_countries_you_miss_or_hav),
            onChange = { vm.choosePracticeWeighting(it) },
        )
        Spacer(Modifier.height(8.dp))
        Toggle(
            on = vm.keepScreenOn,
            title = stringResource(R.string.ui_keep_the_screen_on),
            blurb = stringResource(R.string.ui_stops_the_display_dimming_in_the_middle_of_a_r),
            onChange = { vm.chooseKeepScreenOn(it) },
        )

        Spacer(Modifier.height(14.dp))
        Section("Appearance")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.values().forEach { m ->
                Pill(selected = vm.themeMode == m, label = m.label, onClick = { vm.chooseTheme(m) })
            }
        }

        Spacer(Modifier.height(20.dp))
        Section("About")
        Card(Modifier.fillMaxWidth(), onClick = { vm.navigate(Screen.About) }) {
            Text(stringResource(R.string.ui_about_mapspert), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.ui_what_the_app_is_where_the_data_comes_from_lice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }

        Spacer(Modifier.height(20.dp))
        Section("Data")
        Mono(stringResource(R.string.ui_everything_is_stored_on_this_device_only), alpha = 0.7f)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = {
                exporter.launch("mapspert-backup-" + fileStamp() + ".json")
            }) { Text(stringResource(R.string.ui_export)) }
            OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                Text(stringResource(R.string.ui_import))
            }
        }
        Spacer(Modifier.height(8.dp))
        Mono(stringResource(R.string.ui_export_writes_a_file_you_keep_import_merges_it), alpha = 0.6f)
        message?.let {
            Spacer(Modifier.height(10.dp))
            Card(Modifier.fillMaxWidth()) {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = { confirmClear = true }) { Text(stringResource(R.string.ui_delete_statistics)) }
        Spacer(Modifier.height(28.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.ui_delete_statistics_2)) },
            text = { Text(stringResource(R.string.ui_this_removes_recorded_rounds_it_cannot_be_undo)) },
            confirmButton = {
                TextButton(onClick = { vm.clearRecords(vm.player); confirmClear = false }) {
                    Text("Delete ${vm.player}'s")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.clearRecords(null); confirmClear = false }) { Text(stringResource(R.string.ui_delete_all)) }
            },
        )
    }
}

@Composable
private fun Section(title: String) = SectionLabel(title)

@Composable
private fun Toggle(on: Boolean, title: String, blurb: String, onChange: (Boolean) -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        onClick = { onChange(!on) },
    ) {
        val ink = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ink)
                Spacer(Modifier.height(2.dp))
                Text(blurb, style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.8f))
            }
            Mono(if (on) "on" else "off", color = ink, alpha = 0.9f)
        }
    }
}

private fun fileStamp(): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
