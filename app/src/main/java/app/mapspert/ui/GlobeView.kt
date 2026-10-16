@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package app.mapspert.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextMeasurer
import app.mapspert.PtMono
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import app.mapspert.quiz.SunPosition
import app.mapspert.quiz.GlobeCamera
import app.mapspert.quiz.GlobeMath
import app.mapspert.quiz.MapShape
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A country narrower than this on screen is drawn as a ringed dot instead of an
 * outline, so micro-states stay visible and tappable. Tuned by eye: at 26dp even
 * Belgium and Switzerland turned into circles.
 */
const val DOT_THRESHOLD_DP = 18

/**
 * Below this zoom no dots are drawn at all. At world view they crowd the map, and
 * anything that small is not a fair question until the player zooms in anyway.
 */
const val DOT_MIN_ZOOM = 1.6f

/**
 * Which countries are drawn (and tapped) as dots at this zoom. The threshold is
 * generous enough that a micro-state never flips back to an unclickable sliver at
 * maximum zoom: Andorra used to vanish exactly when you had zoomed in to find it.
 */
fun dotCodes(
    shapes: List<MapShape>,
    pool: Set<String>,
    radiusPx: Float,
    dotThresholdPx: Float,
    zoom: Float,
): Set<String> {
    if (zoom < DOT_MIN_ZOOM) return emptySet()
    return GlobeMath.dotShapes(shapes, pool, radiusPx, dotThresholdPx)
}

/** Camera the caller owns, so the quiz and the knowledge globe can both drive it. */
class GlobeState(lon: Float, lat: Float, zoom: Float) {
    var centreLon by mutableFloatStateOf(lon)
    var centreLat by mutableFloatStateOf(lat)
    var zoom by mutableFloatStateOf(zoom)
    /** Degrees per second, for the flick-to-spin decay. */
    var spinLon by mutableFloatStateOf(0f)
    var spinLat by mutableFloatStateOf(0f)
    var spinTick by mutableIntStateOf(0)

    fun moveTo(lon: Float, lat: Float, z: Float) {
        centreLon = lon; centreLat = lat; zoom = z
        spinLon = 0f; spinLat = 0f
    }

    fun rotate(dLonDeg: Float, dLatDeg: Float) {
        centreLon = GlobeCamera.wrapLon(centreLon + dLonDeg)
        centreLat = (centreLat + dLatDeg).coerceIn(-85f, 85f)
    }
}

@Composable
fun rememberGlobeState(lon: Float, lat: Float, zoom: Float, key: Any? = Unit): GlobeState =
    remember(key) { GlobeState(lon, lat, zoom) }

/** Turns the globe slowly on its own, for decorative use. */
@Composable
fun IdleSpin(state: GlobeState, degreesPerSecond: Float = 3.5f, enabled: Boolean = true) {
    LaunchedEffect(enabled, degreesPerSecond) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            state.centreLon = GlobeCamera.wrapLon(state.centreLon + degreesPerSecond * 0.032f)
            kotlinx.coroutines.delay(32)
        }
    }
}

/** Colours for one pass of the globe. */
data class GlobeColors(
    val ocean: Color,
    val land: Color,
    val muted: Color,
    val line: Color,
    val atmosphere: Color,
    /**
     * Country borders and coastlines. Unspecified falls back to a faint [line], which
     * vanished between two same-coloured countries; the Kosmos palette sets dark ink.
     */
    val border: Color = Color.Unspecified,
)

@Composable
fun GlobeView(
    state: GlobeState,
    shapes: List<MapShape>,
    pool: Set<String>,
    colors: GlobeColors,
    modifier: Modifier = Modifier,
    maxZoom: Float = 14f,
    fillFor: (MapShape) -> Color? = { null },
    onTap: ((String?, Float, Float) -> Unit)? = null,
    /** Day/night shading from the real subsolar point. */
    showTerminator: Boolean = true,
    showLatitudes: Boolean = true,
    overlay: DrawScope.(GlobeCamera) -> Unit = {},
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val dotThresholdPx = with(density) { DOT_THRESHOLD_DP.dp.toPx() }
    val dotRadiusPx = with(density) { 6.dp.toPx() }
    val tapRadiusPx = with(density) { 22.dp.toPx() }
    val slopPx = with(density) { 8.dp.toPx() }

    // Flick to spin: decay the velocity once the finger lifts.
    LaunchedEffect(state.spinTick) {
        if (state.spinLon == 0f && state.spinLat == 0f) return@LaunchedEffect
        var vLon = state.spinLon
        var vLat = state.spinLat
        while (abs(vLon) > 1.5f || abs(vLat) > 1.5f) {
            state.rotate(vLon * 0.016f, vLat * 0.016f)
            vLon *= 0.93f
            vLat *= 0.93f
            kotlinx.coroutines.delay(16)
        }
        state.spinLon = 0f
        state.spinLat = 0f
    }

    Canvas(
        modifier.pointerInput(pool, onTap) {
            awaitEachGesture {
                val tracker = VelocityTracker()
                val down = awaitFirstDown(requireUnconsumed = false)
                state.spinLon = 0f
                state.spinLat = 0f
                var travelled = 0f
                var zoomed = false
                var pointerCount = 1
                var multiTouched = false
                tracker.resetTracking()
                tracker.addPosition(down.uptimeMillis, down.position)

                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) break

                    // When a finger lands or lifts, the centroid jumps. Treating that
                    // jump as a drag threw the globe across the world on pinch release,
                    // so this frame only resyncs and contributes nothing.
                    if (pressed.size != pointerCount) {
                        pointerCount = pressed.size
                        if (pointerCount > 1) multiTouched = true
                        tracker.resetTracking()
                        tracker.addPosition(pressed.first().uptimeMillis, pressed.first().position)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                        continue
                    }

                    val pan = event.calculatePan()
                    val zoomChange = event.calculateZoom()
                    if (pressed.size > 1 && abs(zoomChange - 1f) > 0.002f) zoomed = true

                    val r = globeRadiusPx(this.size.width.toFloat(), this.size.height.toFloat(), state.zoom)
                    val perPx = (180.0 / Math.PI / r).toFloat()
                    if (pan != Offset.Zero) {
                        state.rotate(-pan.x * perPx, pan.y * perPx)
                        travelled += hypot(pan.x, pan.y)
                    }
                    if (zoomChange != 1f) {
                        state.zoom = (state.zoom * zoomChange).coerceIn(1f, maxZoom)
                    }
                    // Velocity is only meaningful for a one-finger drag.
                    if (pressed.size == 1) {
                        tracker.addPosition(pressed.first().uptimeMillis, pressed.first().position)
                    }
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }

                if (travelled <= slopPx && !zoomed && !multiTouched) {
                    if (onTap != null) {
                        val w = this.size.width.toFloat()
                        val h = this.size.height.toFloat()
                        val r = globeRadiusPx(w, h, state.zoom)
                        val cam = GlobeCamera(state.centreLon, state.centreLat, r, w / 2f, h / 2f)
                        val ll = cam.unproject(down.position.x, down.position.y)
                        if (ll != null) {
                            val dots = dotCodes(shapes, pool, r, dotThresholdPx, state.zoom)
                            val radiusDeg = cam.degreesPerPixel() * tapRadiusPx
                            val hit = GlobeMath.hitTest(shapes, ll.first, ll.second, pool, dots, radiusDeg)
                            onTap(hit, ll.first, ll.second)
                        } else {
                            onTap(null, 0f, 0f)
                        }
                    }
                } else if (!multiTouched) {
                    // No fling after a pinch: the tracker has seen centroid jumps.
                    val v = tracker.calculateVelocity()
                    val r = globeRadiusPx(this.size.width.toFloat(), this.size.height.toFloat(), state.zoom)
                    val perPx = (180.0 / Math.PI / r).toFloat()
                    state.spinLon = (-v.x * perPx).coerceIn(-320f, 320f)
                    state.spinLat = (v.y * perPx).coerceIn(-320f, 320f)
                    state.spinTick++
                }
            }
        },
    ) {
        val r = globeRadiusPx(size.width, size.height, state.zoom)
        val cam = GlobeCamera(state.centreLon, state.centreLat, r, size.width / 2f, size.height / 2f)
        val dotSet = dotCodes(shapes, pool, r, dotThresholdPx, state.zoom)
        drawGlobe(cam, shapes, pool, colors, fillFor, dotSet, dotRadiusPx)
        if (showTerminator) drawTerminator(cam, System.currentTimeMillis())
        if (showLatitudes && state.zoom >= 2f) drawLatitudeStrip(cam, measurer, colors.line)
        overlay(cam)
    }
}

fun globeRadiusPx(w: Float, h: Float, zoom: Float): Float = min(w, h) * 0.46f * zoom

private fun DrawScope.drawGlobe(
    cam: GlobeCamera,
    shapes: List<MapShape>,
    pool: Set<String>,
    colors: GlobeColors,
    fillFor: (MapShape) -> Color?,
    dotSet: Set<String>,
    dotRadiusPx: Float,
) {
    val centre = Offset(cam.cx, cam.cy)
    val p = FloatArray(3)
    val sealPx = 2.5f

    // Atmosphere: a soft halo just outside the rim. Vintage space-programme look,
    // and it separates the globe from the card behind it.
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.86f to Color.Transparent,
                0.905f to colors.atmosphere.copy(alpha = 0.34f),
                0.945f to colors.atmosphere.copy(alpha = 0.16f),
                1.0f to Color.Transparent,
            ),
            center = centre,
            radius = cam.radiusPx * 1.16f,
        ),
        radius = cam.radiusPx * 1.16f,
        center = centre,
    )

    // Ocean, lit from the upper left so the disc reads as a ball rather than a circle.
    drawCircle(colors.ocean, radius = cam.radiusPx, center = centre)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(cam.cx - cam.radiusPx * 0.35f, cam.cy - cam.radiusPx * 0.35f),
            radius = cam.radiusPx * 1.1f,
        ),
        radius = cam.radiusPx,
        center = centre,
    )

    val lineColor = colors.line.copy(alpha = 0.16f)
    var lon = -180
    while (lon < 180) {
        drawArc3d(cam, p, lineColor) { emit ->
            var lat = -90
            while (lat <= 90) { emit(lon.toFloat(), lat.toFloat()); lat += 6 }
        }
        lon += 30
    }
    var lat = -60
    while (lat <= 60) {
        drawArc3d(cam, p, lineColor) { emit ->
            var l = -180
            while (l <= 180) { emit(l.toFloat(), lat.toFloat()); l += 6 }
        }
        lat += 30
    }

    val pxPerDeg = cam.radiusPx * (Math.PI / 180.0).toFloat()
    val dotted = ArrayList<MapShape>()
    // Zoomed in, draw the detailed outlines (the hit-test level of detail) so borders
    // are not angular, and skip countries that cannot be in view so the extra points
    // cost little. Each outline is built once and reused for its border below.
    val fine = cam.radiusPx / (size.minDimension * 0.46f) >= 3f
    val halfDiag = hypot(size.width, size.height) / 2f
    val viewDeg = if (halfDiag >= cam.radiusPx) 180f
        else Math.toDegrees(kotlin.math.asin((halfDiag / cam.radiusPx).toDouble())).toFloat()
    val built = ArrayList<Pair<MapShape, List<Path>>>(shapes.size)
    for (s in shapes) {
        val inPool = s.alpha2 in pool
        val fill = fillFor(s) ?: if (inPool) colors.land else colors.muted
        if (s.alpha2 in dotSet) {
            dotted.add(s)
            continue
        }
        if (viewDeg < 90f &&
            GlobeMath.angularDistance(cam.centreLon, cam.centreLat, s.centerLon, s.centerLat) >
            viewDeg + max(s.widthDeg, s.heightDeg)
        ) continue
        val paths = ArrayList<Path>()
        for (ring in if (fine) s.rings else s.drawRings) {
            if (ring.size < 8) continue
            val path = buildRingPath(cam, ring, p) ?: continue
            drawPath(path, fill)
            // Natural Earth's neighbours do not share every border vertex, so a hairline
            // of ocean can show between two countries. Stroking each fill in its own
            // colour closes it; neighbours usually share a colour, so it is invisible.
            drawPath(path, fill, style = Stroke(width = sealPx))
            paths.add(path)
        }
        built.add(s to paths)
    }

    // Borders: dark ink, in dp so they hold up on dense screens. Thinner at world view,
    // where small countries would otherwise clog up, and lighter outside the pool.
    val inked = colors.border.isSpecified
    val zoomT = ((cam.radiusPx / (size.minDimension * 0.46f) - 1f) / 1.5f).coerceIn(0f, 1f)
    val poolStroke = Stroke(width = (0.9f + 0.5f * zoomT).dp.toPx(), join = StrokeJoin.Round)
    val otherStroke = Stroke(width = (0.7f + 0.3f * zoomT).dp.toPx(), join = StrokeJoin.Round)
    for ((s, paths) in built) {
        val inPool = s.alpha2 in pool
        val color = if (inked) {
            colors.border.copy(alpha = colors.border.alpha * if (inPool) 0.9f else 0.6f)
        } else {
            colors.line.copy(alpha = if (inPool) 0.30f else 0.12f)
        }
        val stroke = if (!inked) Stroke(width = 1f) else if (inPool) poolStroke else otherStroke
        for (path in paths) drawPath(path, color, style = stroke)
    }

    // Dots last so they are never covered by a neighbour's fill.
    for (s in dotted) {
        if (!cam.isVisible(s.centerLon, s.centerLat)) continue
        val inPool = s.alpha2 in pool
        cam.project(s.centerLon, s.centerLat, p)
        val c = Offset(p[0], p[1])
        val fill = fillFor(s) ?: if (inPool) colors.land else colors.muted
        if (colors.border.isSpecified) {
            // A bullseye that reads on any background: paper halo, thick ink ring, fill.
            // A thin ring vanished when the dot sat on a same-coloured neighbour
            // (Luxembourg inside mint Germany, France and Belgium).
            if (inPool) drawCircle(colors.line.copy(alpha = 0.9f), radius = dotRadiusPx + 4.dp.toPx(), center = c)
            drawCircle(
                colors.border.copy(alpha = if (inPool) 1f else 0.6f),
                radius = dotRadiusPx + 2.5.dp.toPx(),
                center = c,
            )
        } else {
            drawCircle(colors.line.copy(alpha = if (inPool) 0.85f else 0.25f), radius = dotRadiusPx + 1.5f, center = c)
        }
        drawCircle(fill, radius = dotRadiusPx, center = c)
    }

    // Limb shading: a soft dark rim sells the curvature.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.30f)),
            center = Offset(cam.cx - cam.radiusPx * 0.18f, cam.cy - cam.radiusPx * 0.18f),
            radius = cam.radiusPx * 1.25f,
        ),
        radius = cam.radiusPx,
        center = centre,
    )
    drawCircle(colors.line.copy(alpha = 0.45f), radius = cam.radiusPx, center = centre, style = Stroke(width = 2f))
}

/**
 * Day/night shading.
 *
 * The terminator is the great circle 90 degrees from the subsolar point, which under
 * an orthographic projection is an ellipse. Earlier this was faked with a radial
 * gradient centred on the sun, which drifted out of place as the globe turned; this
 * builds the actual night region: the visible part of the terminator, closed along
 * the limb on whichever side is dark.
 */
private fun DrawScope.drawTerminator(cam: GlobeCamera, nowMillis: Long) {
    val (sunLon, sunLat) = SunPosition.subsolar(nowMillis)
    val s = FloatArray(3)
    cam.cameraSpace(sunLon, sunLat, s)

    val night = Color.Black.copy(alpha = 0.34f)
    val disc = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                cam.cx - cam.radiusPx, cam.cy - cam.radiusPx,
                cam.cx + cam.radiusPx, cam.cy + cam.radiusPx,
            ),
        )
    }

    // An orthonormal pair spanning the plane perpendicular to the sun.
    val helper = if (abs(s[0]) > 0.9f) floatArrayOf(0f, 0f, 1f) else floatArrayOf(1f, 0f, 0f)
    val u = cross(s, helper)
    normalize(u)
    val v2 = cross(s, u)

    val steps = 96
    val vis = BooleanArray(steps)
    val px = FloatArray(steps)
    val py = FloatArray(steps)
    val vx = FloatArray(steps)
    val vy = FloatArray(steps)
    for (i in 0 until steps) {
        val t = (2.0 * Math.PI * i / steps)
        val c = cos(t).toFloat()
        val sn = sin(t).toFloat()
        val x = c * u[0] + sn * v2[0]
        val y = c * u[1] + sn * v2[1]
        val z = c * u[2] + sn * v2[2]
        vis[i] = z > 0f
        vx[i] = x; vy[i] = y
        px[i] = cam.cx + cam.radiusPx * x
        py[i] = cam.cy - cam.radiusPx * y
    }

    clipPath(disc) {
        if (vis.none { it }) {
            // No terminator in view: the whole visible face is day or night.
            if (s[2] < 0f) drawCircle(night, radius = cam.radiusPx, center = Offset(cam.cx, cam.cy))
            return@clipPath
        }
        var start = 0
        for (i in 0 until steps) {
            if (vis[i] && !vis[(i - 1 + steps) % steps]) { start = i; break }
        }
        val path = Path()
        var i = start
        var first = true
        var lastX = 0f
        var lastY = 0f
        while (vis[i]) {
            if (first) { path.moveTo(px[i], py[i]); first = false } else path.lineTo(px[i], py[i])
            lastX = vx[i]; lastY = vy[i]
            i = (i + 1) % steps
            if (i == start) break
        }
        // Close along the limb, taking the way round that stays on the night side.
        val phiA = atan2(vy[start], vx[start])
        val phiB = atan2(lastY, lastX)
        var d = ((phiA - phiB) % (2.0 * Math.PI) + 2.0 * Math.PI) % (2.0 * Math.PI)
        val mid = phiB + d / 2.0
        if (cos(mid).toFloat() * s[0] + sin(mid).toFloat() * s[1] >= 0f) d -= 2.0 * Math.PI
        val k = 48
        for (j in 1..k) {
            val phi = phiB + d * j / k
            path.lineTo(
                cam.cx + cam.radiusPx * cos(phi).toFloat(),
                cam.cy - cam.radiusPx * sin(phi).toFloat(),
            )
        }
        path.close()
        drawPath(path, night)
    }
}

private fun cross(a: FloatArray, b: FloatArray) = floatArrayOf(
    a[1] * b[2] - a[2] * b[1],
    a[2] * b[0] - a[0] * b[2],
    a[0] * b[1] - a[1] * b[0],
)

private fun normalize(v: FloatArray) {
    val n = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    if (n > 0f) { v[0] /= n; v[1] /= n; v[2] /= n }
}

/**
 * Camera-space unit vector for a lon/lat: x right, y up, z toward the viewer. Built from
 * project() and isVisible() so it works with every version of GlobeCamera.
 */
private fun GlobeCamera.cameraSpace(lon: Float, lat: Float, out: FloatArray) {
    project(lon, lat, out)
    val x = (out[0] - cx) / radiusPx
    val y = (cy - out[1]) / radiusPx
    val z = sqrt((1f - x * x - y * y).coerceAtLeast(0f))
    out[0] = x
    out[1] = y
    out[2] = if (isVisible(lon, lat)) z else -z
}

/** Latitude labels down the left of the visible disc, once zoomed in enough to read them. */
private fun DrawScope.drawLatitudeStrip(cam: GlobeCamera, measurer: TextMeasurer, line: Color) {
    val p = FloatArray(3)
    val style = TextStyle(fontSize = 10.sp, color = line.copy(alpha = 0.55f), fontFamily = PtMono)
    var lat = -60
    while (lat <= 60) {
        if (cam.isVisible(cam.centreLon, lat.toFloat())) {
            cam.project(cam.centreLon, lat.toFloat(), p)
            val dy = p[1] - cam.cy
            val half = cam.radiusPx * cam.radiusPx - dy * dy
            if (half > 0f) {
                val x = cam.cx - sqrt(half) + 10f
                if (x > 0f && p[1] > 0f && p[1] < size.height) {
                    val label = when {
                        lat > 0 -> "$lat N"
                        lat < 0 -> "${-lat} S"
                        else -> "0"
                    }
                    val m = measurer.measure(label, style)
                    drawText(m, topLeft = Offset(x, p[1] - m.size.height / 2f))
                }
            }
        }
        lat += 30
    }
}

/** Highlight one country on top of the globe, sealed wide enough to cover any hairline. */
fun DrawScope.drawHighlight(
    cam: GlobeCamera,
    shape: MapShape,
    color: Color,
    dotMinPx: Float,
    dotRadiusPx: Float,
    asDot: Boolean = run {
        val pxPerDeg = cam.radiusPx * (Math.PI / 180.0).toFloat()
        shape.widthDeg * pxPerDeg < dotMinPx && shape.heightDeg * pxPerDeg < dotMinPx
    },
) {
    val p = FloatArray(3)
    if (asDot) {
        if (!cam.isVisible(shape.centerLon, shape.centerLat)) return
        cam.project(shape.centerLon, shape.centerLat, p)
        val c = Offset(p[0], p[1])
        drawCircle(Color.White.copy(alpha = 0.9f), radius = dotRadiusPx * 1.6f + 2f, center = c)
        drawCircle(color, radius = dotRadiusPx * 1.6f, center = c)
        return
    }
    for (ring in shape.drawRings) {
        if (ring.size < 8) continue
        val path = buildRingPath(cam, ring, p) ?: continue
        drawPath(path, color)
        drawPath(path, color, style = Stroke(width = 4f))
    }
}

/** A great-circle line, for showing how far a wrong tap landed from the target. */
fun DrawScope.drawGreatCircle(
    cam: GlobeCamera,
    lon1: Float, lat1: Float,
    lon2: Float, lat2: Float,
    color: Color,
) {
    val p = FloatArray(3)
    val steps = 48
    var lastX = 0f
    var lastY = 0f
    var started = false
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val (lon, lat) = GlobeMath.interpolate(lon1, lat1, lon2, lat2, t)
        cam.project(lon, lat, p)
        if (p[2] > 0f) {
            if (started) {
                drawLine(color, Offset(lastX, lastY), Offset(p[0], p[1]), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            lastX = p[0]; lastY = p[1]; started = true
        } else {
            started = false
        }
    }
}

/**
 * Projects a ring. Points on the far side are pushed out to the limb, which keeps
 * part-visible countries looking right without a full spherical clip.
 */
private fun buildRingPath(cam: GlobeCamera, ring: FloatArray, p: FloatArray): Path? {
    var anyVisible = false
    val path = Path()
    var i = 0
    while (i < ring.size) {
        cam.project(ring[i], ring[i + 1], p)
        var x = p[0]
        var y = p[1]
        if (p[2] > 0f) {
            anyVisible = true
        } else {
            val dx = x - cam.cx
            val dy = y - cam.cy
            val len = max(0.0001f, sqrt(dx * dx + dy * dy))
            x = cam.cx + dx / len * cam.radiusPx
            y = cam.cy + dy / len * cam.radiusPx
        }
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        i += 2
    }
    if (!anyVisible) return null
    path.close()
    return path
}

private inline fun DrawScope.drawArc3d(
    cam: GlobeCamera,
    p: FloatArray,
    color: Color,
    points: ((Float, Float) -> Unit) -> Unit,
) {
    var started = false
    var lastX = 0f
    var lastY = 0f
    points { lon, lat ->
        cam.project(lon, lat, p)
        if (p[2] > 0f) {
            if (started) {
                drawLine(color, Offset(lastX, lastY), Offset(p[0], p[1]), strokeWidth = 1f, cap = StrokeCap.Round)
            }
            lastX = p[0]; lastY = p[1]; started = true
        } else {
            started = false
        }
    }
}
