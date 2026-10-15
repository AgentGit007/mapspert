@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.mapspert.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import app.mapspert.Screen
import app.mapspert.quiz.CountryStat
import app.mapspert.quiz.CountryStatsMath
import androidx.compose.ui.res.stringResource
import app.mapspert.R
import app.mapspert.AppViewModel
import app.mapspert.categoryColor
import app.mapspert.quiz.Category
import app.mapspert.quiz.QuizMode
import app.mapspert.quiz.Region
import app.mapspert.quiz.RoundRecord
import app.mapspert.quiz.StatsMath
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private enum class Metric(val label: String) { ACCURACY("Accuracy"), SPEED("Seconds per question") }

@Composable
fun StatsScreen(vm: AppViewModel) {
    val all = vm.records
    val players = remember(all) { StatsMath.players(all).ifEmpty { listOf(vm.player) } }
    var player by remember(players) { mutableStateOf(if (vm.player in players) vm.player else players.first()) }
    var metric by remember { mutableStateOf(Metric.ACCURACY) }
    var categoryFilter by remember { mutableStateOf<Category?>(null) }
    var regionFilter by remember { mutableStateOf<Region?>(null) }

    val mine = remember(all, player, categoryFilter, regionFilter) {
        val cat = categoryFilter
        val reg = regionFilter
        StatsMath.trend(
            all.filter {
                it.player == player &&
                    (cat == null || it.category == cat) &&
                    (reg == null || it.region == reg.name)
            },
        )
    }
    // Only offer regions the player has actually played.
    val playedRegions = remember(all, player) {
        Region.values().filter { r -> all.any { it.player == player && it.region == r.name } }
    }
    val summary = remember(mine) { StatsMath.summarize(mine) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader(stringResource(R.string.ui_statistics), onBack = { vm.back() })
        if (all.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.ui_no_rounds_recorded_yet_play_one_and_your_progr), style = MaterialTheme.typography.bodyLarge)
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (players.size > 1) {
                item {
                    SectionLabel(stringResource(R.string.ui_player))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        players.forEach { p ->
                            Pill(selected = player == p, label = p, onClick = { player = p })
                        }
                    }
                }
            }
            item {
                SectionLabel(stringResource(R.string.ui_category))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Pill(selected = categoryFilter == null, label = stringResource(R.string.ui_all), onClick = { categoryFilter = null })
                    Category.values().forEach { c ->
                        Pill(selected = categoryFilter == c, label = c.label, onClick = { categoryFilter = c })
                    }
                }
            }
            if (playedRegions.size > 1) {
                item {
                    SectionLabel(stringResource(R.string.ui_region))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Pill(selected = regionFilter == null, label = stringResource(R.string.ui_all), onClick = { regionFilter = null })
                        playedRegions.forEach { r ->
                            Pill(selected = regionFilter == r, label = r.short, onClick = { regionFilter = r })
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text("${summary.percent}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Mono(
                        "${summary.rounds} rounds | ${summary.questions} questions | " +
                            formatSeconds(summary.secondsPerQuestion) + "/q | best ${summary.bestPercent}%",
                        alpha = 0.8f,
                    )
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric.values().forEach { m ->
                        Pill(selected = metric == m, label = m.label, onClick = { metric = m })
                    }
                }
            }
            item { TrendChart(mine, metric) }
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { vm.navigate(Screen.KnowledgeGlobe) },
                ) {
                    val ink = MaterialTheme.colorScheme.onSecondaryContainer
                    Text(stringResource(R.string.ui_knowledge_globe),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.ui_every_country_coloured_by_how_well_you_know_it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink.copy(alpha = 0.85f),
                    )
                }
            }
            val own = vm.statsFor(player).values
            val weakest = CountryStatsMath.weakest(own)
            val strongest = CountryStatsMath.strongest(own)
            if (strongest.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.ui_strongest_countries)) }
                items(strongest) { s2 -> CountryRankRow(vm, s2, MaterialTheme.colorScheme.primary) }
            }
            if (weakest.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.ui_weakest_countries)) }
                items(weakest) { s2 -> CountryRankRow(vm, s2, MaterialTheme.colorScheme.error) }
            }
            if (strongest.isEmpty() && weakest.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Mono(stringResource(R.string.ui_answer_a_country_at_least_twice_and_it_will_be),
                            alpha = 0.6f,
                        )
                    }
                }
            }
            item { SectionLabel(stringResource(R.string.ui_by_category)) }
            item {
                val byCat = StatsMath.byCategory(mine)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.values().forEach { c ->
                        val s = byCat[c] ?: return@forEach
                        Card(Modifier.fillMaxWidth(), color = categoryColor(c)) {
                            Text(c.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Mono("${s.percent}%  |  ${s.rounds} rounds  |  " + formatSeconds(s.secondsPerQuestion) + "/q", alpha = 0.85f)
                        }
                    }
                }
            }
            item { SectionLabel(stringResource(R.string.ui_by_quiz)) }
            item {
                val byMode = StatsMath.byMode(mine)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuizMode.values().forEach { m ->
                        val s = byMode[m] ?: return@forEach
                        Card(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(m.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Mono("${s.percent}%  ${s.rounds}r")
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun TrendChart(records: List<RoundRecord>, metric: Metric) {
    val cs = MaterialTheme.colorScheme
    var selected by remember(records, metric) { mutableStateOf<Int?>(null) }
    val values = records.map { if (metric == Metric.ACCURACY) it.percent.toDouble() else it.secondsPerQuestion }
    if (values.isEmpty()) return
    val avg = StatsMath.movingAverage(values, 5)
    val maxV = (values + avg).max().coerceAtLeast(if (metric == Metric.ACCURACY) 100.0 else 1.0)
    val minV = 0.0
    val fmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }

    Card(Modifier.fillMaxWidth()) {
        Text(
            if (metric == Metric.ACCURACY) "Accuracy per round" else "Seconds per question",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(records, metric) {
                    detectTapGestures { p ->
                        if (values.size < 2) { selected = 0; return@detectTapGestures }
                        val step = size.width / (values.size - 1).toFloat()
                        val i = (p.x / step).toInt().coerceIn(0, values.size - 1)
                        val j = (i + 1).coerceAtMost(values.size - 1)
                        selected = if (abs(i * step - p.x) <= abs(j * step - p.x)) i else j
                    }
                },
            ) {
                fun yOf(v: Double): Float {
                    val t = ((v - minV) / (maxV - minV)).toFloat().coerceIn(0f, 1f)
                    return size.height - t * size.height
                }
                // gridlines
                for (f in listOf(0f, 0.5f, 1f)) {
                    val y = size.height * f
                    drawLine(cs.outline.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }
                val step = if (values.size > 1) size.width / (values.size - 1) else size.width
                fun xOf(i: Int) = if (values.size > 1) i * step else size.width / 2

                // moving-average band
                val avgPath = Path()
                avg.forEachIndexed { i, v ->
                    val x = xOf(i); val y = yOf(v)
                    if (i == 0) avgPath.moveTo(x, y) else avgPath.lineTo(x, y)
                }
                drawPath(avgPath, cs.secondary.copy(alpha = 0.7f), style = Stroke(width = 6f))

                // per-round line
                val path = Path()
                values.forEachIndexed { i, v ->
                    val x = xOf(i); val y = yOf(v)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, cs.primary, style = Stroke(width = 3f))
                values.forEachIndexed { i, v ->
                    val c = Offset(xOf(i), yOf(v))
                    val isSel = selected == i
                    drawCircle(if (isSel) cs.tertiary else cs.primary, radius = if (isSel) 8f else 4f, center = c)
                }
                selected?.let { i ->
                    val x = xOf(i)
                    drawLine(cs.tertiary.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 2f)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val i = selected
        if (i != null) {
            val r = records[i]
            Mono(
                fmt.format(Date(r.epochMillis)) + "  |  " + (r.modeEnum?.title ?: r.mode) +
                    "  |  ${r.percent}%  |  " + formatSeconds(r.secondsPerQuestion) + "/q",
                alpha = 0.85f,
            )
        } else {
            Mono(stringResource(R.string.ui_tap_a_point_for_details_thick_line_average_of_), alpha = 0.6f)
        }
        Spacer(Modifier.height(4.dp))
        LegendRow(cs.primary, "per round", cs.secondary, "5-round average")
    }
}

@Composable
private fun LegendRow(c1: Color, l1: String, c2: Color, l2: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Mono("--- $l1", alpha = 0.75f, color = c1)
        Mono("--- $l2", alpha = 0.75f, color = c2)
    }
}

/** One row of the strongest / weakest lists. */
@Composable
private fun CountryRankRow(vm: AppViewModel, stat: CountryStat, accent: Color) {
    val c = runCatching { vm.country(stat.alpha2) }.getOrNull()
    Card(Modifier.fillMaxWidth(), onClick = { c?.let { vm.navigate(Screen.Detail(it.alpha2)) } }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (c != null) {
                FlagImage(c.flag, Modifier.width(44.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(c?.name ?: stat.alpha2, style = MaterialTheme.typography.titleSmall)
                Mono("${stat.seen} attempts  -  " + formatSeconds(stat.secondsPerQuestion) + "/q", alpha = 0.55f)
            }
            Text(
                "${CountryStatsMath.percent(stat)}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
    }
}
