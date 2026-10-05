@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.mapspert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mapspert.AppViewModel
import app.mapspert.Screen
import app.mapspert.categoryColor
import app.mapspert.onCategoryColor
import app.mapspert.quiz.Category
import app.mapspert.quiz.Difficulty
import app.mapspert.quiz.QuizMode
import app.mapspert.quiz.Region

@Composable
fun HomeScreen(vm: AppViewModel) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            RetroStripes()
            Spacer(Modifier.height(16.dp))
            Text("MAPSPERT", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Mono("flags / places / codes", alpha = 0.7f)
            Spacer(Modifier.height(4.dp))
            Mono("playing as ${vm.player}", alpha = 0.6f)
        }
        items(Category.values().size) { i ->
            val cat = Category.values()[i]
            val modes = QuizMode.values().filter { it.category == cat }
            Tile(
                modifier = Modifier.fillMaxWidth(),
                color = categoryColor(cat),
                onClick = { vm.navigate(Screen.CategoryHub(cat)) },
            ) {
                val ink = onCategoryColor(cat)
                Text(cat.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ink)
                Text(cat.blurb, style = MaterialTheme.typography.bodyMedium, color = ink)
                Spacer(Modifier.height(6.dp))
                Mono("${modes.size} quizzes", color = ink, alpha = 0.8f)
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.navigate(Screen.Stats) }) { Text("Statistics") }
                OutlinedButton(onClick = { vm.navigate(Screen.Browse) }) { Text("Browse") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { vm.navigate(Screen.Settings) }) { Text("Settings") }
                TextButton(onClick = { vm.navigate(Screen.About) }) { Text("About") }
            }
        }
    }
}

@Composable
fun CategoryHubScreen(vm: AppViewModel, category: Category) {
    val modes = QuizMode.values().filter { it.category == category }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TopBar(category.label) { vm.back() }
        Spacer(Modifier.height(8.dp))
        RegionPicker(vm)
        Spacer(Modifier.height(16.dp))
        modes.forEach { mode ->
            val n = vm.poolSize(mode)
            val ok = vm.playable(mode)
            Tile(
                modifier = Modifier.fillMaxWidth(),
                color = categoryColor(category),
                onClick = { if (ok) vm.navigate(Screen.ModeSetup(mode)) },
            ) {
                val ink = onCategoryColor(category)
                Text(mode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ink)
                Text(mode.blurb, style = MaterialTheme.typography.bodyMedium, color = ink)
                Spacer(Modifier.height(6.dp))
                Mono(if (ok) "$n countries" else "not enough countries here", color = ink, alpha = 0.85f)
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun RegionPicker(vm: AppViewModel) {
    Text("Region", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Region.values().forEach { r ->
            FilterChip(
                selected = vm.region == r,
                onClick = { vm.region = r },
                label = { Text(r.short) },
            )
        }
    }
}

@Composable
fun ModeSetupScreen(vm: AppViewModel, mode: QuizMode) {
    val n = vm.poolSize(mode)
    val perRound = if (vm.questionsPerRound == 0) n else minOf(vm.questionsPerRound, n)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TopBar(mode.title) { vm.back() }
        Spacer(Modifier.height(8.dp))
        Tile(Modifier.fillMaxWidth(), color = categoryColor(mode.category)) {
            val ink = onCategoryColor(mode.category)
            Text(mode.blurb, style = MaterialTheme.typography.bodyLarge, color = ink)
            Spacer(Modifier.height(6.dp))
            Mono("${vm.region.label} | $n countries", color = ink, alpha = 0.85f)
        }
        Spacer(Modifier.height(16.dp))
        Text("Difficulty", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Difficulty.values().forEach { d ->
            FilterChip(
                selected = vm.difficulty == d,
                onClick = { vm.chooseDifficulty(d) },
                label = { Text("${d.label} - ${d.blurb}") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text("Questions per round", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 0).forEach { v ->
                FilterChip(
                    selected = vm.questionsPerRound == v,
                    onClick = { vm.chooseQuestionsPerRound(v) },
                    label = { Text(if (v == 0) "All" else "$v") },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Rounds", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 3, 5).forEach { v ->
                FilterChip(selected = vm.rounds == v, onClick = { vm.chooseRounds(v) }, label = { Text("$v") })
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { vm.startSession(mode) }, modifier = Modifier.fillMaxWidth(), enabled = vm.playable(mode)) {
            Text("Start - ${vm.rounds} x $perRound questions")
        }
        Spacer(Modifier.height(8.dp))
        Mono("these choices are saved as your defaults", alpha = 0.6f)
        Spacer(Modifier.height(24.dp))
    }
}
