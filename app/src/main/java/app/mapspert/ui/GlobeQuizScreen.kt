package app.mapspert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import app.mapspert.Kosmos
import app.mapspert.LocalIsDarkTheme
import app.mapspert.panelBorder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.mapspert.R
import app.mapspert.AppViewModel
import app.mapspert.RoundState
import app.mapspert.Session
import app.mapspert.quiz.GlobeCamera
import app.mapspert.quiz.GlobeMath

@Composable
fun GlobeQuizScreen(vm: AppViewModel, session: Session, round: RoundState) {
    val cs = MaterialTheme.colorScheme
    val codes = remember(session) { vm.globeCodes() }
    val shapes = vm.map.shapes
    val start = remember(codes) { GlobeMath.frame(shapes, codes) }
    // Rounds pick up where the last one left the globe: same zoom, same view.
    // "Reset view" still goes back to the framing of the whole pool.
    val carried = session.camera
    val state = rememberGlobeState(
        carried?.get(0) ?: start[0],
        carried?.get(1) ?: start[1],
        carried?.get(2) ?: start[2],
        key = codes,
    )
    DisposableEffect(state, session) {
        onDispose { session.camera = floatArrayOf(state.centreLon, state.centreLat, state.zoom) }
    }
    var showDetails by remember(round.index) { mutableStateOf(false) }
    var tapLon by remember(round.index) { mutableStateOf(0f) }
    var tapLat by remember(round.index) { mutableStateOf(0f) }

    val target = round.current.subject
    val tapped = round.tapped
    val correct = tapped != null && tapped == target.alpha2
    val targetShape = remember(target.alpha2) { vm.map[target.alpha2] }

    val globeDescription = stringResource(R.string.a11y_globe, target.name)
    val poolLand = globePoolLand()
    // The answer lights up green, a wrong tap red. Green rather than mint so it stands
    // apart from the countries in play.
    val targetColor = if (LocalIsDarkTheme.current) Kosmos.Signal else cs.primary
    val density = LocalDensity.current
    val dotMinPx = with(density) { DOT_THRESHOLD_DP.dp.toPx() }
    val dotRadiusPx = with(density) { 6.dp.toPx() }

    val missKm = remember(round.index, tapped, round.passed) {
        if (round.answered && !correct && targetShape != null && (tapped != null)) {
            GlobeMath.distanceKm(tapLon, tapLat, targetShape.centerLon, targetShape.centerLat)
        } else null
    }

    // After an answer, turn the globe to the target so the player always sees where it was.
    LaunchedEffect(round.index, round.answered) {
        if (!round.answered) return@LaunchedEffect
        val shape = targetShape ?: return@LaunchedEffect
        val fromLon = state.centreLon
        val fromLat = state.centreLat
        val dLon = GlobeMath.shortestLonDelta(fromLon, shape.centerLon)
        val dLat = shape.centerLat.coerceIn(-70f, 70f) - fromLat
        val steps = 20
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            val e = 1f - (1f - t) * (1f - t)
            state.centreLon = GlobeCamera.wrapLon(fromLon + dLon * e)
            state.centreLat = fromLat + dLat * e
            kotlinx.coroutines.delay(16)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        QuizHeader(vm, session, round)
        Spacer(Modifier.height(12.dp))
        // The objective, printed on a ticket with a tear-off stub.
        Ticket(
            Modifier.fillMaxWidth(),
            stubLabel = stringResource(R.string.ui_task),
            stubValue = "%02d".format(round.index + 1),
        ) {
            Mono(stringResource(R.string.ui_find_on_the_globe), color = Kosmos.InkSoft, caps = true)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlagImage(target.flag, Modifier.width(52.dp), countryName = target.name)
                Spacer(Modifier.width(12.dp))
                Text(
                    target.name.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Kosmos.Ink,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(MaterialTheme.shapes.large)
                .background(cs.surface)
                .border(2.dp, panelBorder(), MaterialTheme.shapes.large),
        ) {
            GlobeView(
                modifier = Modifier.fillMaxSize().semantics {
                    contentDescription = globeDescription
                },
                state = state,
                shapes = shapes,
                pool = codes,
                colors = kosmosGlobeColors(land = poolLand, muted = globeMutedLand()),
                onTap = { hit, lon, lat ->
                    if (!round.answered) {
                        tapLon = lon
                        tapLat = lat
                        if (hit != null) vm.tapCountry(hit)
                    }
                },
                overlay = { cam ->
                    if (round.answered) {
                        val picked = tapped?.let { code -> vm.map[code] }
                        if (picked != null && !correct) {
                            drawHighlight(cam, picked, cs.error, dotMinPx, dotRadiusPx)
                        }
                        targetShape?.let { drawHighlight(cam, it, targetColor, dotMinPx, dotRadiusPx) }
                        // How far the tap landed from the target.
                        if (!correct && tapped != null && targetShape != null) {
                            drawGreatCircle(
                                cam, tapLon, tapLat, targetShape.centerLon, targetShape.centerLat,
                                cs.error.copy(alpha = 0.8f),
                            )
                        }
                    }
                },
            )
            TelemetryChip(
                stringResource(R.string.ui_in_play, codes.size),
                Modifier.align(Alignment.TopStart).padding(12.dp),
                swatch = poolLand,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (!round.answered) {
            // Two equal buttons: the old row squeezed the labels until they wrapped.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PanelButton(
                    text = stringResource(R.string.ui_reset_view),
                    onClick = { state.moveTo(start[0], start[1], start[2]) },
                    modifier = Modifier.weight(1f),
                    kind = PanelKind.OUTLINE,
                )
                PanelButton(
                    text = stringResource(R.string.ui_skip),
                    onClick = { vm.pass() },
                    modifier = Modifier.weight(1f),
                    kind = PanelKind.CORAL,
                    trailing = "»",
                )
            }
            Spacer(Modifier.height(16.dp))
        } else {
            val picked = tapped?.let { vm.country(it) }
            Card(Modifier.fillMaxWidth(), color = if (correct) cs.primaryContainer else cs.errorContainer) {
                val ink = if (correct) cs.onPrimaryContainer else cs.onErrorContainer
                if (correct) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlagImage(target.flag, Modifier.width(38.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "\u2713  Correct - ${target.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (picked != null) {
                            FlagImage(picked.flag, Modifier.width(38.dp))
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            when {
                                round.passed -> "\u2192  Skipped"
                                picked != null -> "\u2717  You picked ${picked.name}"
                                else -> "\u2717  That was open water"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                        )
                    }
                    missKm?.let {
                        Spacer(Modifier.height(4.dp))
                        Mono(formatDistance(it) + " from the mark", color = ink, alpha = 0.9f)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlagImage(target.flag, Modifier.width(38.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${target.name} is highlighted",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ink.copy(alpha = 0.9f),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Mono(
                    if (showDetails) "hide details" else "show details",
                    alpha = 0.8f,
                    color = ink,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { showDetails = !showDetails }
                        .padding(vertical = 4.dp),
                )
            }
            if (showDetails) {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                    FactCard(
                        target,
                        Modifier.fillMaxWidth(),
                        verdict = when {
                            correct -> Verdict.CORRECT
                            round.passed -> Verdict.SKIPPED
                            else -> Verdict.MISSED
                        },
                    )
                    if (picked != null && !correct) {
                        Spacer(Modifier.height(10.dp))
                        FactCard(picked, Modifier.fillMaxWidth(), compact = true)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            PanelButton(
                text = if (round.index + 1 < round.questions.size) "Next task" else "Finish round",
                onClick = { vm.next() },
                modifier = Modifier.fillMaxWidth(),
                trailing = "→",
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

fun formatDistance(km: Double): String = when {
    km < 10 -> "${km.toInt() + 1} km"
    km < 1000 -> "${km.toInt()} km"
    else -> "${"%,d".format(km.toInt())} km"
}
