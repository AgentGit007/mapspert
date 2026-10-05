package app.mapspert.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mapspert.AppViewModel
import app.mapspert.RoundState
import app.mapspert.Session
import app.mapspert.quiz.MapShape
import kotlin.math.max
import kotlin.math.min

/** Camera for the plate-carree map: a lon/lat window mapped onto the canvas. */
private class Camera(var minLon: Float, var minLat: Float, var maxLon: Float, var maxLat: Float) {
    fun toScreen(lon: Float, lat: Float, size: Size): Offset {
        val x = (lon - minLon) / (maxLon - minLon) * size.width
        val y = (maxLat - lat) / (maxLat - minLat) * size.height
        return Offset(x, y)
    }

    fun toLonLat(p: Offset, size: Size): Pair<Float, Float> {
        val lon = minLon + p.x / size.width * (maxLon - minLon)
        val lat = maxLat - p.y / size.height * (maxLat - minLat)
        return lon to lat
    }

    /** Degrees per pixel on the longitude axis. */
    fun degPerPx(size: Size): Float = (maxLon - minLon) / max(1f, size.width)
}

@Composable
fun MapQuizScreen(vm: AppViewModel, session: Session, round: RoundState) {
    val cs = MaterialTheme.colorScheme
    val codes = remember(session) { vm.mapCodes() }
    val shapes = remember(codes) { vm.map.shapes.filter { it.alpha2 in codes } }
    val home = remember(codes) { vm.map.boundsOf(codes) }

    // Camera state, reset whenever the region changes.
    var cLon by remember(codes) { mutableFloatStateOf((home[0] + home[2]) / 2f) }
    var cLat by remember(codes) { mutableFloatStateOf((home[1] + home[3]) / 2f) }
    var spanLon by remember(codes) { mutableFloatStateOf(home[2] - home[0]) }

    val target = round.current.subject
    val tapped = round.tapped
    val correct = tapped != null && tapped == target.alpha2

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        QuizHeader(vm, session, round)
        Spacer(Modifier.height(10.dp))
        Tile(Modifier.fillMaxWidth()) {
            Text("Find on the map", style = MaterialTheme.typography.bodyMedium)
            Text(target.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(cs.surface)
                .border(1.5.dp, cs.outline.copy(alpha = 0.55f), MaterialTheme.shapes.medium),
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(codes) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val size = Size(this.size.width.toFloat(), this.size.height.toFloat())
                            val aspect = size.height / max(1f, size.width)
                            spanLon = (spanLon / zoom).coerceIn(2f, (home[2] - home[0]) * 1.2f)
                            val spanLat = spanLon * aspect
                            cLon -= pan.x / size.width * spanLon
                            cLat += pan.y / size.height * spanLat
                            cLon = cLon.coerceIn(home[0], home[2])
                            cLat = cLat.coerceIn(home[1], home[3])
                        }
                    }
                    .pointerInput(codes, round.index, round.answered) {
                        detectTapGestures { p ->
                            if (round.answered) return@detectTapGestures
                            val size = Size(this.size.width.toFloat(), this.size.height.toFloat())
                            val cam = cameraFor(cLon, cLat, spanLon, size)
                            val (lon, lat) = cam.toLonLat(p, size)
                            // ~22dp finger radius, converted to degrees at the current zoom
                            val radius = cam.degPerPx(size) * 22f * density
                            val hit = vm.map.hitTest(lon, lat, codes, radius)
                            if (hit != null) vm.tapCountry(hit)
                        }
                    },
            ) {
                val cam = cameraFor(cLon, cLat, spanLon, size)
                val dotPx = 7.dp.toPx()
                val minShapePx = 18.dp.toPx()
                val answered = round.answered
                for (s in shapes) {
                    val fill = when {
                        !answered -> cs.surfaceVariant.copy(alpha = 0.55f)
                        s.alpha2 == target.alpha2 -> cs.primaryContainer
                        s.alpha2 == tapped -> cs.errorContainer
                        else -> cs.surfaceVariant.copy(alpha = 0.55f)
                    }
                    drawShape(s, cam, size, fill, cs.outline.copy(alpha = 0.6f), dotPx, minShapePx)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (round.answered) {
            Tile(Modifier.fillMaxWidth(), color = if (correct) cs.primaryContainer else cs.errorContainer) {
                val ink = if (correct) cs.onPrimaryContainer else cs.onErrorContainer
                Text(
                    if (correct) "Correct" else "That was " + (tapped?.let { vm.country(it).name } ?: "elsewhere"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                )
                if (!correct) {
                    Text("${target.name} is highlighted in green.", style = MaterialTheme.typography.bodyMedium, color = ink)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { vm.next() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (round.index + 1 < round.questions.size) "Next" else "Finish round")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    cLon = (home[0] + home[2]) / 2f
                    cLat = (home[1] + home[3]) / 2f
                    spanLon = home[2] - home[0]
                }) { Text("Reset view") }
                Spacer(Modifier.weight(1f))
                Mono("pinch to zoom", alpha = 0.6f)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

private fun cameraFor(cLon: Float, cLat: Float, spanLon: Float, size: Size): Camera {
    val aspect = size.height / max(1f, size.width)
    val spanLat = spanLon * aspect
    return Camera(cLon - spanLon / 2, cLat - spanLat / 2, cLon + spanLon / 2, cLat + spanLat / 2)
}

private fun DrawScope.drawShape(
    s: MapShape,
    cam: Camera,
    size: Size,
    fill: Color,
    outline: Color,
    dotPx: Float,
    minShapePx: Float,
) {
    // Skip anything completely outside the window.
    if (s.maxLon < cam.minLon || s.minLon > cam.maxLon || s.maxLat < cam.minLat || s.minLat > cam.maxLat) return

    val wPx = s.widthDeg / max(0.0001f, cam.maxLon - cam.minLon) * size.width
    val hPx = s.heightDeg / max(0.0001f, cam.maxLat - cam.minLat) * size.height
    if (wPx < minShapePx && hPx < minShapePx) {
        // Too small to see or tap: draw a dot instead, as agreed for island states.
        val c = cam.toScreen(s.centerLon, s.centerLat, size)
        drawCircle(fill, radius = dotPx, center = c)
        drawCircle(outline, radius = dotPx, center = c, style = Stroke(width = 1.5f))
        return
    }
    for (ring in s.rings) {
        if (ring.size < 6) continue
        val path = Path()
        var i = 0
        while (i < ring.size) {
            val p = cam.toScreen(ring[i], ring[i + 1], size)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            i += 2
        }
        path.close()
        drawPath(path, fill)
        drawPath(path, outline, style = Stroke(width = min(1.5f, max(0.6f, wPx / 200f))))
    }
}
