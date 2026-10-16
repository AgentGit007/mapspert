package app.mapspert.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import app.mapspert.Kosmos
import app.mapspert.PtMono
import app.mapspert.Unbounded
import app.mapspert.cardPaper
import app.mapspert.panelBorder
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.mapspert.R
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
        GlobeQuizScreen(vm, session, round)
        return
    }
    val cs = MaterialTheme.colorScheme
    val q = round.current
    val correct = round.answered && round.selected == q.correctIndex
    val listState = rememberLazyListState()

    // A new question always starts at the top of the list.
    LaunchedEffect(round.index) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        QuizHeader(vm, session, round)
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            item {
                val number = "№ ${round.index + 1}/${round.questions.size}"
                when (val p = q.prompt) {
                    // Pictures are framed as a postage stamp on a panel.
                    is Prompt.Flag, is Prompt.Outline -> Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(cs.surface)
                            .border(1.5.dp, panelBorder(), MaterialTheme.shapes.large)
                            .padding(vertical = 22.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Stamp(color = cardPaper(), tilt = -2f, contentPadding = 10.dp) {
                            if (p is Prompt.Flag) {
                                FlagImage(p.flag, Modifier.width(210.dp).height(126.dp), countryName = null)
                            } else if (p is Prompt.Outline) {
                                OutlineShape(
                                    shape = vm.map[p.alpha2],
                                    color = Kosmos.Ink,
                                    modifier = Modifier.width(210.dp).height(150.dp),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.width(210.dp)) {
                                Mono(q.prompt.caption, color = Kosmos.Ink, caps = true, modifier = Modifier.weight(1f))
                                Mono(number, color = Kosmos.Ink)
                            }
                        }
                    }
                    // Words and codes go on a ticket.
                    is Prompt.Text -> Ticket(
                        Modifier.fillMaxWidth(),
                        stubLabel = stringResource(R.string.ui_task),
                        stubValue = "%02d".format(round.index + 1),
                    ) {
                        Mono(q.prompt.caption, color = Kosmos.InkSoft, caps = true)
                        Spacer(Modifier.height(8.dp))
                        val code = p.text.length <= 4 || p.text.startsWith(".")
                        Text(
                            p.text,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = if (code) PtMono else Unbounded,
                            color = Kosmos.Ink,
                        )
                    }
                }
            }
            if (q.options.all { it.flag != null }) {
                items(q.options.chunked(2)) { pair ->
                    val base = q.options.indexOf(pair.first())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEachIndexed { col, opt ->
                            val i = base + col
                            OptionTile(Modifier.weight(1f), opt, i, q.correctIndex, round.selected, round.passed) { vm.choose(i) }
                        }
                    }
                }
            } else {
                items(q.options.size) { i ->
                    OptionTile(Modifier.fillMaxWidth(), q.options[i], i, q.correctIndex, round.selected, round.passed) { vm.choose(i) }
                }
            }
            if (round.answered) {
                item {
                    AnimatedVisibility(visible = true, enter = fadeIn(tween(220)) + expandVertically(tween(220))) {
                        FactCard(
                            q.subject,
                            Modifier.fillMaxWidth(),
                            verdict = when {
                                correct -> Verdict.CORRECT
                                round.passed -> Verdict.SKIPPED
                                else -> Verdict.MISSED
                            },
                        )
                    }
                }
            } else {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        // Short label: "I don't know" crowded the row on a narrow phone.
                        Pill(selected = false, label = stringResource(R.string.ui_skip), onClick = { vm.pass() })
                    }
                }
            }
        }
        // The verdict and the Next button stay pinned, so there is never anything to scroll to.
        if (round.answered) {
            Column {
                // A right answer is already shown by the lit option and the stamp; a miss
                // keeps the correct answer pinned so it can never scroll out of view.
                if (!correct) {
                    Card(
                        Modifier.fillMaxWidth(),
                        color = if (round.passed) cs.surface else cs.errorContainer,
                    ) {
                        val ink = if (round.passed) cs.onSurface else cs.onErrorContainer
                        Text(
                            if (round.passed) {
                                "\u2192  Skipped - ${correctLabel(q.options[q.correctIndex], q.subject.name)}"
                            } else {
                                "\u2717  Not quite - ${correctLabel(q.options[q.correctIndex], q.subject.name)}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                PanelButton(
                    text = if (round.index + 1 < round.questions.size) "Next task" else "Finish round",
                    onClick = { vm.next() },
                    modifier = Modifier.fillMaxWidth(),
                    trailing = "→",
                )
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

private fun correctLabel(correct: Option, fallback: String): String = correct.label ?: fallback

@Composable
fun QuizHeader(vm: AppViewModel, session: Session, round: RoundState) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(AppIcon.CLOSE, { vm.quitSession() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    session.setup.mode.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Mono(
                    "round ${session.roundNumber}/${session.totalRounds} · " +
                        "task ${round.index + 1}/${round.questions.size} · ${round.score} right",
                    color = MaterialTheme.colorScheme.onBackground,
                    alpha = 0.72f,
                    caps = true,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        SegmentedProgress(
            total = round.questions.size,
            outcomes = round.outcomes,
            current = round.index,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OptionTile(
    modifier: Modifier,
    opt: Option,
    index: Int,
    correctIndex: Int,
    selected: Int?,
    passed: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val open = selected == null && !passed
    val target = when {
        open -> cs.surface
        index == correctIndex -> cs.primaryContainer
        index == selected -> cs.errorContainer
        else -> cs.surface
    }
    val color by animateColorAsState(target, tween(200), label = "option")
    val ink = when {
        open -> cs.onSurface
        index == correctIndex -> cs.onPrimaryContainer
        index == selected -> cs.onErrorContainer
        else -> cs.onSurface.copy(alpha = 0.7f)
    }
    val mark = when {
        open -> null
        index == correctIndex -> "\u2713"
        index == selected -> "\u2717"
        else -> null
    }
    if (opt.flag != null) {
        Card(modifier, color = color, onClick = { if (open) onClick() }) {
            Column {
                if (mark != null) Text(mark, color = ink, style = MaterialTheme.typography.titleMedium)
                FlagImage(opt.flag, Modifier.fillMaxWidth().height(80.dp))
            }
        }
        return
    }
    // Text answers read like an exam sheet: a lettered key, then the answer.
    val shape = RoundedCornerShape(12.dp)
    val bordered = color == cs.surface
    var m = modifier
        .heightIn(min = 52.dp)
        .clip(shape)
        .background(color)
    if (bordered) m = m.border(1.5.dp, panelBorder(), shape)
    Row(
        m
            .clickable(enabled = open, role = Role.Button) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val lit = !bordered
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (lit) ink else Color.Transparent)
                .border(1.5.dp, if (lit) ink else panelBorder(), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                ('A' + index).toString(),
                fontFamily = PtMono,
                style = MaterialTheme.typography.labelMedium,
                color = if (lit) color else ink,
            )
        }
        Spacer(Modifier.width(12.dp))
        val label = opt.label.orEmpty()
        val code = label.length <= 3 || label.startsWith(".")
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontFamily = if (code) PtMono else Unbounded,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (lit) FontWeight.Bold else FontWeight.Medium,
            color = ink,
        )
        if (mark != null) {
            Spacer(Modifier.width(8.dp))
            Text(mark, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ink)
        }
    }
}

@Composable
fun RoundSummaryScreen(vm: AppViewModel, session: Session, round: RoundState) {
    val last = session.finished.lastOrNull()
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(top = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Mono("round ${session.roundNumber} of ${session.totalRounds}", alpha = 0.55f)
                RollingNumber(round.percent, suffix = "%")
                Text(
                    "${round.score} of ${round.questions.size} correct",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                last?.let { Mono(formatSeconds(it.secondsPerQuestion) + " per question", alpha = 0.55f) }
            }
            if (round.missed.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.ui_to_review)) }
                items(round.missed.toList()) { c ->
                    Card(Modifier.fillMaxWidth(), onClick = { vm.navigate(Screen.Detail(c.alpha2)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FlagImage(c.flag, Modifier.width(52.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall)
                                c.capital?.let { Mono(it, alpha = 0.55f) }
                            }
                        }
                    }
                }
            }
        }
        PanelButton(
            text = if (vm.isLastRound()) "See results" else "Next round",
            onClick = { vm.nextRound() },
            modifier = Modifier.fillMaxWidth(),
            trailing = "→",
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { vm.quitSession() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_back_home)) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun SessionSummaryScreen(vm: AppViewModel, session: Session) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(top = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Mono(session.setup.mode.title.lowercase(), alpha = 0.55f)
                RollingNumber(session.percent, suffix = "%")
                Text(
                    "${session.totalCorrect} of ${session.totalQuestions} over ${session.finished.size} rounds",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                val secs = if (session.totalQuestions == 0) 0.0
                else session.totalMillis / 1000.0 / session.totalQuestions
                Mono(formatSeconds(secs) + " per question", alpha = 0.55f)
            }
            items(session.finished.toList()) { r ->
                Card(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${r.percent}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(64.dp),
                        )
                        Mono("${r.correct}/${r.questions}   " + formatSeconds(r.secondsPerQuestion) + "/q", alpha = 0.7f)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PanelButton(
                text = stringResource(R.string.ui_play_again),
                onClick = { vm.replaySession() },
                modifier = Modifier.weight(1f),
            )
            PanelButton(
                text = stringResource(R.string.ui_statistics),
                onClick = { vm.navigate(Screen.Stats) },
                modifier = Modifier.weight(1f),
                kind = PanelKind.OUTLINE,
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { vm.quitSession() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_back_home)) }
        Spacer(Modifier.height(16.dp))
    }
}
