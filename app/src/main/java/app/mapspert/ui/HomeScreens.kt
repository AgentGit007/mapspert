@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.mapspert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import app.mapspert.Kosmos
import app.mapspert.LocalIsDarkTheme
import app.mapspert.panelBorder
import kotlin.math.roundToInt
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import app.mapspert.butterColor
import app.mapspert.quiz.CountryStatsMath
import app.mapspert.quiz.Mastery
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import app.mapspert.quiz.GlobeCamera
import app.mapspert.quiz.GlobeMath
import androidx.compose.ui.res.stringResource
import app.mapspert.R
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
    val cats = Category.values().toList()
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Mono(
                        stringResource(R.string.ui_tagline),
                        color = if (LocalIsDarkTheme.current) Kosmos.Ochre else Color(0xFFA4483A),
                        caps = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.ui_mapspert).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    RetroRule()
                    Spacer(Modifier.height(8.dp))
                    Mono(
                        stringResource(R.string.ui_operator, vm.player),
                        color = MaterialTheme.colorScheme.onBackground,
                        alpha = 0.7f,
                        caps = true,
                    )
                }
                IconBadge(AppIcon.BROWSE, { vm.navigate(Screen.Browse) })
                Spacer(Modifier.size(8.dp))
                IconBadge(AppIcon.STATS, { vm.navigate(Screen.Stats) })
                Spacer(Modifier.size(8.dp))
                IconBadge(AppIcon.SETTINGS, { vm.navigate(Screen.Settings) })
            }
            Spacer(Modifier.height(14.dp))
            HomeGlobe(vm)
        }
        vm.resumable?.let { saved ->
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { vm.resumeSaved() },
                ) {
                    val ink = MaterialTheme.colorScheme.onSecondaryContainer
                    Text(stringResource(R.string.ui_resume_round).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    Mono(
                        "question ${saved.index + 1} of ${saved.subjects.size}",
                        color = ink,
                        alpha = 0.8f,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(selected = true, label = stringResource(R.string.ui_continue), onClick = { vm.resumeSaved() })
                        Pill(selected = false, label = stringResource(R.string.ui_discard), onClick = { vm.discardSaved() })
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(2.dp))
            SectionLabel(stringResource(R.string.ui_missions))
            // The first category gets a full-width stamp, the rest share rows of two.
            CategoryStamp(vm, cats.first(), 1, wide = true, modifier = Modifier.fillMaxWidth())
        }
        items(cats.drop(1).chunked(2).size) { row ->
            val pair = cats.drop(1).chunked(2)[row]
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEachIndexed { col, cat ->
                    CategoryStamp(vm, cat, 2 + row * 2 + col, wide = false, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
            Spacer(Modifier.height(2.dp))
            Mono(stringResource(R.string.ui_offline_no_ads_no_tracking), color = MaterialTheme.colorScheme.onBackground, alpha = 0.6f, caps = true)
        }
    }
}

/** A category as a postage stamp: number, line icon, name, blurb. */
@Composable
private fun CategoryStamp(vm: AppViewModel, cat: Category, number: Int, wide: Boolean, modifier: Modifier) {
    val ink = onCategoryColor(cat)
    val modes = QuizMode.values().count { it.category == cat }
    val count = if (modes == 1) "1 quiz" else "$modes quizzes"
    val no = "№ %02d".format(number)
    Stamp(
        color = categoryColor(cat),
        ink = ink,
        modifier = modifier,
        onClick = { vm.navigate(Screen.CategoryHub(cat)) },
    ) {
        if (wide) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Mono("$no · $count", color = ink, caps = true)
                    Spacer(Modifier.height(4.dp))
                    Text(cat.label.uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ink)
                    Spacer(Modifier.height(2.dp))
                    Text(cat.blurb, style = MaterialTheme.typography.bodySmall, color = ink)
                }
                Spacer(Modifier.width(10.dp))
                CategoryIcon(cat, ink, Modifier.size(54.dp))
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Mono(no, color = ink, caps = true, modifier = Modifier.weight(1f))
                CategoryIcon(cat, ink, Modifier.size(34.dp))
            }
            Spacer(Modifier.weight(1f).heightIn(min = 10.dp))
            Text(cat.label.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ink)
            Spacer(Modifier.height(2.dp))
            Text(cat.blurb, style = MaterialTheme.typography.bodySmall, color = ink)
            Spacer(Modifier.height(4.dp))
            Mono(count, color = ink, alpha = 0.8f, caps = true)
        }
    }
}

/** A slowly turning globe, coloured by how well the player knows each country. */
@Composable
private fun HomeGlobe(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val shapes = vm.map.shapes
    val all = remember { shapes.map { it.alpha2 }.toSet() }
    var highlighted by remember { mutableStateOf<String?>(null) }
    val state = rememberGlobeState(20f, 18f, 1f)
    val stats = vm.countryStats
    val weakest = remember(stats) { CountryStatsMath.weakest(stats.values, limit = 6) }

    // Turns slowly, and every so often drifts to a country you keep missing and
    // pauses there. A quiet nudge rather than a notification.
    LaunchedEffect(weakest) {
        var sinceVisit = 0
        while (true) {
            state.centreLon = GlobeCamera.wrapLon(state.centreLon + 0.11f)
            kotlinx.coroutines.delay(32)
            sinceVisit += 32
            if (sinceVisit > 14_000 && weakest.isNotEmpty()) {
                sinceVisit = 0
                val pick = weakest.random()
                val shape = vm.map[pick.alpha2]
                if (shape != null) {
                    val fromLon = state.centreLon
                    val fromLat = state.centreLat
                    val dLon = GlobeMath.shortestLonDelta(fromLon, shape.centerLon)
                    val dLat = shape.centerLat.coerceIn(-55f, 55f) - fromLat
                    val steps = 34
                    for (i in 1..steps) {
                        val t = i / steps.toFloat()
                        val e = 1f - (1f - t) * (1f - t)
                        state.centreLon = GlobeCamera.wrapLon(fromLon + dLon * e)
                        state.centreLat = fromLat + dLat * e
                        kotlinx.coroutines.delay(16)
                    }
                    highlighted = pick.alpha2
                    kotlinx.coroutines.delay(2600)
                    highlighted = null
                }
            }
        }
    }

    // A satellite on a tilted orbit: one lap every 24 seconds.
    val orbit = rememberInfiniteTransition(label = "orbit")
    val angle by orbit.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "satellite",
    )

    val density = LocalDensity.current
    val dotMinPx = with(density) { DOT_THRESHOLD_DP.dp.toPx() }
    val dotRadiusPx = with(density) { 6.dp.toPx() }
    val solid = cs.primary
    val learning = butterColor()
    val shaky = cs.error
    val unseen = globeUnseenLand()
    val dark = LocalIsDarkTheme.current
    val orbitColor = if (dark) Kosmos.Coral else Color(0xFFC9604E)
    val satColor = cs.onSurface
    val shape = MaterialTheme.shapes.large

    Box(
        Modifier
            .fillMaxWidth()
            .height(236.dp)
            .clip(shape)
            .background(cs.surface)
            .border(1.5.dp, panelBorder(), shape),
    ) {
        // Back half of the orbit first, so the globe hides whatever passes behind it.
        Canvas(Modifier.fillMaxSize()) { drawOrbit(state.zoom, angle, orbitColor, satColor, front = false) }
        GlobeView(
            state = state,
            shapes = shapes,
            pool = all,
            colors = kosmosGlobeColors(land = unseen, muted = unseen),
            modifier = Modifier.fillMaxSize(),
            fillFor = { s ->
                when (CountryStatsMath.mastery(stats[s.alpha2])) {
                    Mastery.SOLID -> solid
                    Mastery.LEARNING -> learning
                    Mastery.SHAKY -> shaky
                    Mastery.UNSEEN -> unseen
                }
            },
            onTap = { _, _, _ -> vm.navigate(Screen.KnowledgeGlobe) },
            overlay = { cam ->
                highlighted?.let { code ->
                    vm.map[code]?.let { drawHighlight(cam, it, cs.tertiary, dotMinPx, dotRadiusPx) }
                }
            },
        )
        Canvas(Modifier.fillMaxSize()) { drawOrbit(state.zoom, angle, orbitColor, satColor, front = true) }
        Mono(
            stringResource(R.string.ui_orbit_live),
            color = cs.onSurface,
            alpha = 0.75f,
            caps = true,
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
        )
        GlobeReadout(state, Modifier.align(Alignment.TopEnd).padding(14.dp))
        highlighted?.let { code ->
            val c = runCatching { vm.country(code) }.getOrNull()
            if (c != null) {
                Card(
                    Modifier.align(Alignment.BottomStart).padding(12.dp),
                    color = cs.tertiaryContainer,
                ) {
                    Mono(stringResource(R.string.ui_worth_revisiting), color = cs.onTertiaryContainer, alpha = 0.85f, caps = true)
                    Text(
                        c.name.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

/** Live centre of the globe, e.g. "18°N 28°E". Its own scope, so only it recomposes. */
@Composable
private fun GlobeReadout(state: GlobeState, modifier: Modifier) {
    val lat = state.centreLat.roundToInt()
    val lon = state.centreLon.roundToInt()
    val text = "${kotlin.math.abs(lat)}°${if (lat >= 0) "N" else "S"} ${kotlin.math.abs(lon)}°${if (lon >= 0) "E" else "W"}"
    Mono(text, color = MaterialTheme.colorScheme.onSurface, alpha = 0.75f, modifier = modifier)
}

/** One half of the tilted orbit ellipse, plus the satellite when it is on that half. */
private fun DrawScope.drawOrbit(zoom: Float, angleDeg: Float, line: Color, sat: Color, front: Boolean) {
    val r = globeRadiusPx(size.width, size.height, zoom)
    val c = Offset(size.width / 2f, size.height / 2f)
    val rx = r * 1.32f
    val ry = r * 0.36f
    rotate(-18f, pivot = c) {
        val topLeft = Offset(c.x - rx, c.y - ry)
        val oval = Size(rx * 2, ry * 2)
        if (front) {
            drawArc(line, 0f, 180f, false, topLeft, oval, style = Stroke(width = 1.6.dp.toPx()))
        } else {
            drawArc(
                line.copy(alpha = 0.5f), 180f, 180f, false, topLeft, oval,
                style = Stroke(
                    width = 1.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 5.dp.toPx())),
                ),
            )
        }
        val t = Math.toRadians(angleDeg.toDouble())
        val onFront = kotlin.math.sin(t) >= 0
        if (onFront == front) {
            val p = Offset(c.x + rx * kotlin.math.cos(t).toFloat(), c.y + ry * kotlin.math.sin(t).toFloat())
            val a = 3.5.dp.toPx()
            drawLine(sat, p, p + Offset(-a * 3f, a * 1.6f), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(sat, p, p + Offset(-a * 1.2f, a * 3.2f), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(sat, p, p + Offset(a * 1.8f, a * 2.8f), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(sat, a, p)
        }
    }
}

@Composable
fun CategoryHubScreen(vm: AppViewModel, category: Category) {
    val modes = QuizMode.values().filter { it.category == category }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        ScreenHeader(category.label, onBack = { vm.back() }) {
            CategoryIcon(category, categoryColor(category), Modifier.size(36.dp))
        }
        Spacer(Modifier.height(10.dp))
        SectionLabel(stringResource(R.string.ui_region))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Region.values().forEach { r ->
                Pill(selected = vm.region == r, label = r.short, onClick = { vm.region = r })
            }
        }
        Spacer(Modifier.height(22.dp))
        SectionLabel(stringResource(R.string.ui_quizzes))
        modes.forEach { mode ->
            val n = vm.poolSize(mode)
            val ok = vm.playable(mode)
            Card(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                onClick = { if (ok) vm.navigate(Screen.ModeSetup(mode)) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(mode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            mode.blurb,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        )
                    }
                    Box(
                        Modifier.size(44.dp),
                        contentAlignment = Alignment.Center,
                    ) { Mono(if (ok) "$n" else "-", alpha = 0.8f) }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ModeSetupScreen(vm: AppViewModel, mode: QuizMode) {
    val n = vm.poolSize(mode)
    // What the session will really run: no country twice, so a small pool is spread
    // over the rounds rather than repeated.
    val sizes = vm.roundSizes(mode)
    val perRound = when {
        sizes.isEmpty() -> "0"
        sizes.min() == sizes.max() -> "${sizes.first()}"
        else -> "${sizes.min()}–${sizes.max()}"
    }
    val requested = (if (vm.questionsPerRound == 0) n else vm.questionsPerRound) * vm.rounds
    val split = sizes.sum() < requested
    val lengths = listOf(5, 10, 20, 0)
    val rounds = listOf(1, 3, 5)
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader(mode.title, onBack = { vm.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Card(Modifier.fillMaxWidth(), color = categoryColor(mode.category)) {
                val ink = onCategoryColor(mode.category)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(mode.blurb, style = MaterialTheme.typography.bodyLarge, color = ink)
                        Spacer(Modifier.height(6.dp))
                        Mono("${vm.region.label}  -  $n countries", color = ink, alpha = 0.75f)
                    }
                    CategoryIcon(mode.category, ink, Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.ui_difficulty))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.values().forEach { d ->
                    val selected = vm.difficulty == d
                    Card(
                        Modifier.fillMaxWidth(),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        onClick = { vm.chooseDifficulty(d) },
                    ) {
                        val ink = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        Text(d.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ink)
                        Spacer(Modifier.height(2.dp))
                        Text(d.blurb, style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.8f))
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.ui_questions_per_round))
            PillRow(
                options = lengths.map { if (it == 0) "All" else "$it" },
                selectedIndex = lengths.indexOf(vm.questionsPerRound),
                onSelect = { vm.chooseQuestionsPerRound(lengths[it]) },
            )
            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.ui_rounds))
            PillRow(
                options = rounds.map { "$it" },
                selectedIndex = rounds.indexOf(vm.rounds),
                onSelect = { vm.chooseRounds(rounds[it]) },
            )
            if (split) {
                Spacer(Modifier.height(12.dp))
                Mono(
                    stringResource(R.string.ui_split_note, n),
                    color = MaterialTheme.colorScheme.onBackground,
                    alpha = 0.8f,
                )
            }
            Spacer(Modifier.height(16.dp))
            Mono(stringResource(R.string.ui_saved_as_your_defaults), color = MaterialTheme.colorScheme.onBackground, alpha = 0.6f)
            Spacer(Modifier.height(24.dp))
        }
        PanelButton(
            text = "Start  ·  ${sizes.size} × $perRound",
            onClick = { vm.startSession(mode) },
            modifier = Modifier.fillMaxWidth(),
            enabled = vm.playable(mode),
            trailing = "→",
        )
        Spacer(Modifier.height(20.dp))
    }
}
