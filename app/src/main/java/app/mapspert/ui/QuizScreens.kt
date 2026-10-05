package app.mapspert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mapspert.AppViewModel
import app.mapspert.RoundState
import app.mapspert.Screen
import app.mapspert.Session
import app.mapspert.quiz.Answering
import app.mapspert.quiz.Option
import app.mapspert.quiz.Prompt

@Composable
fun QuizScreen(vm: AppViewModel, session: Session, round: RoundState) {
    if (session.setup.mode.answering == Answering.MAP) {
        MapQuizScreen(vm, session, round)
        return
    }
    val cs = MaterialTheme.colorScheme
    val q = round.current
    val correct = round.answered && round.selected == q.correctIndex
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        QuizHeader(vm, session, round)
        Tile(Modifier.fillMaxWidth()) {
            Text(q.prompt.caption, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            when (val p = q.prompt) {
                is Prompt.Flag -> FlagImage(p.flag, Modifier.fillMaxWidth().height(180.dp))
                is Prompt.Text -> if (p.text.length <= 4 || p.text.startsWith(".")) {
                    Text(p.text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                } else {
                    Text(p.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (q.options.all { it.flag != null }) {
            q.options.chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEachIndexed { col, opt ->
                        val i = row * 2 + col
                        OptionTile(Modifier.weight(1f), opt, optionColor(i, q.correctIndex, round.selected)) { vm.choose(i) }
                    }
                }
            }
        } else {
            q.options.forEachIndexed { i, opt ->
                OptionTile(Modifier.fillMaxWidth(), opt, optionColor(i, q.correctIndex, round.selected)) { vm.choose(i) }
            }
        }
        if (round.answered) {
            Tile(Modifier.fillMaxWidth(), color = if (correct) cs.primaryContainer else cs.errorContainer) {
                Text(
                    if (correct) "Correct" else "Not quite",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (correct) cs.onPrimaryContainer else cs.onErrorContainer,
                )
            }
            Tile(Modifier.fillMaxWidth()) { FactBlock(q.subject) }
            Button(onClick = { vm.next() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (round.index + 1 < round.questions.size) "Next" else "Finish round")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun QuizHeader(vm: AppViewModel, session: Session, round: RoundState) {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.quitSession() }) { Text("Quit") }
            Spacer(Modifier.weight(1f))
            Mono("R${session.roundNumber}/${session.setup.rounds}  ${round.index + 1}/${round.questions.size}  +${round.score}")
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (round.index + if (round.answered) 1 else 0).toFloat() / round.questions.size },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = cs.primary,
            trackColor = cs.surface,
        )
    }
}

@Composable
private fun optionColor(i: Int, correctIndex: Int, selected: Int?): Color {
    val cs = MaterialTheme.colorScheme
    return when {
        selected == null -> cs.surface
        i == correctIndex -> cs.primaryContainer
        i == selected -> cs.errorContainer
        else -> cs.surface
    }
}

@Composable
private fun OptionTile(modifier: Modifier, opt: Option, color: Color, onClick: () -> Unit) {
    Tile(modifier, color = color, onClick = onClick) {
        if (opt.flag != null) {
            FlagImage(opt.flag, Modifier.fillMaxWidth().height(84.dp))
        } else {
            val label = opt.label.orEmpty()
            val mono = label.length <= 3 || label.startsWith(".")
            Text(
                label,
                fontFamily = if (mono) FontFamily.Monospace else null,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
fun RoundSummaryScreen(vm: AppViewModel, session: Session, round: RoundState) {
    val last = session.finished.lastOrNull()
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            RetroStripes()
            Spacer(Modifier.height(16.dp))
            Text("${round.percent}%", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            Text(
                "${round.score} of ${round.questions.size} correct  |  round ${session.roundNumber} of ${session.setup.rounds}",
                style = MaterialTheme.typography.bodyLarge,
            )
            last?.let { Mono(formatSeconds(it.secondsPerQuestion) + " per question", alpha = 0.75f) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.nextRound() }) {
                    Text(if (vm.isLastRound()) "See results" else "Next round")
                }
                OutlinedButton(onClick = { vm.quitSession() }) { Text("Home") }
            }
        }
        if (round.missed.isNotEmpty()) {
            item { Text("To review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(round.missed.toList()) { c ->
                Tile(Modifier.fillMaxWidth(), onClick = { vm.navigate(Screen.Detail(c.alpha2)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlagImage(c.flag, Modifier.width(56.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(c.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun SessionSummaryScreen(vm: AppViewModel, session: Session) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            RetroStripes()
            Spacer(Modifier.height(16.dp))
            Text("${session.percent}%", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            Text(
                "${session.totalCorrect} of ${session.totalQuestions} over ${session.finished.size} rounds",
                style = MaterialTheme.typography.bodyLarge,
            )
            val secs = if (session.totalQuestions == 0) 0.0 else session.totalMillis / 1000.0 / session.totalQuestions
            Mono(formatSeconds(secs) + " per question  |  " + session.setup.mode.title, alpha = 0.75f)
        }
        items(session.finished.toList()) { r ->
            Tile(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${r.percent}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Mono("${r.correct}/${r.questions}   " + formatSeconds(r.secondsPerQuestion) + "/q", alpha = 0.8f)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.replaySession() }) { Text("Play again") }
                OutlinedButton(onClick = { vm.navigate(Screen.Stats) }) { Text("Statistics") }
                TextButton(onClick = { vm.quitSession() }) { Text("Home") }
            }
        }
    }
}
