package app.mapspert.quiz

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Orthographic globe: the camera looks at the point (centreLon, centreLat) and the
 * sphere is drawn as a disc of radius [radiusPx] around (cx, cy).
 *
 * Pure Kotlin so the projection and the hit-testing can be unit-tested on the JVM.
 */
class GlobeCamera(
    var centreLon: Float,
    var centreLat: Float,
    var radiusPx: Float,
    var cx: Float,
    var cy: Float,
) {
    private var sinYaw = 0f
    private var cosYaw = 1f
    private var sinPitch = 0f
    private var cosPitch = 1f

    init { refresh() }

    /** Recompute the rotation after centreLon / centreLat change. */
    fun refresh() {
        val yaw = Math.toRadians(centreLon.toDouble())
        val pitch = Math.toRadians(centreLat.toDouble())
        sinYaw = sin(yaw).toFloat(); cosYaw = cos(yaw).toFloat()
        sinPitch = sin(pitch).toFloat(); cosPitch = cos(pitch).toFloat()
    }

    /**
     * Projects lon/lat. Writes screen x into out[0], y into out[1] and the depth into
     * out[2]; depth > 0 means the point is on the near side of the globe.
     */
    fun project(lon: Float, lat: Float, out: FloatArray) {
        val la = Math.toRadians(lat.toDouble())
        val lo = Math.toRadians(lon.toDouble())
        val cosLa = cos(la).toFloat()
        val x = cosLa * sin(lo).toFloat()
        val y = sin(la).toFloat()
        val z = cosLa * cos(lo).toFloat()

        val x2 = x * cosYaw - z * sinYaw
        val z2 = x * sinYaw + z * cosYaw
        val y3 = y * cosPitch - z2 * sinPitch
        val z3 = y * sinPitch + z2 * cosPitch

        out[0] = cx + radiusPx * x2
        out[1] = cy - radiusPx * y3
        out[2] = z3
    }

    /** True if the point faces the camera. Cheaper than a full projection. */
    fun isVisible(lon: Float, lat: Float): Boolean {
        val la = Math.toRadians(lat.toDouble())
        val lo = Math.toRadians(lon.toDouble())
        val cosLa = cos(la).toFloat()
        val x = cosLa * sin(lo).toFloat()
        val y = sin(la).toFloat()
        val z = cosLa * cos(lo).toFloat()
        val z2 = x * sinYaw + z * cosYaw
        return y * sinPitch + z2 * cosPitch > 0f
    }

    /**
     * Screen point back to lon/lat, or null when the tap missed the disc.
     * Returns a pair of (lon, lat) in degrees.
     */
    fun unproject(px: Float, py: Float): Pair<Float, Float>? {
        val dx = (px - cx) / radiusPx
        val dy = (cy - py) / radiusPx
        val d2 = dx * dx + dy * dy
        if (d2 > 1f) return null
        val dz = sqrt(1f - d2)

        // undo pitch, then yaw
        val y = dy * cosPitch + dz * sinPitch
        val z2 = -dy * sinPitch + dz * cosPitch
        val x = dx * cosYaw + z2 * sinYaw
        val z = -dx * sinYaw + z2 * cosYaw

        val lat = Math.toDegrees(asin(y.coerceIn(-1f, 1f).toDouble())).toFloat()
        val lon = Math.toDegrees(atan2(x.toDouble(), z.toDouble())).toFloat()
        return lon to lat
    }

    /** Degrees of arc per pixel at the centre of the disc. */
    fun degreesPerPixel(): Float = (180.0 / Math.PI / radiusPx).toFloat()

    fun rotateBy(dxPx: Float, dyPx: Float) {
        val perPx = degreesPerPixel()
        centreLon = wrapLon(centreLon - dxPx * perPx)
        centreLat = (centreLat + dyPx * perPx).coerceIn(-85f, 85f)
        refresh()
    }

    companion object {
        fun wrapLon(lon: Float): Float {
            var l = lon
            while (l > 180f) l -= 360f
            while (l < -180f) l += 360f
            return l
        }
    }
}

object GlobeMath {
    /** Great-circle distance in degrees. */
    fun angularDistance(lon1: Float, lat1: Float, lon2: Float, lat2: Float): Float {
        val p1 = Math.toRadians(lat1.toDouble())
        val p2 = Math.toRadians(lat2.toDouble())
        val dl = Math.toRadians((lon2 - lon1).toDouble())
        val c = sin(p1) * sin(p2) + cos(p1) * cos(p2) * cos(dl)
        return Math.toDegrees(kotlin.math.acos(c.coerceIn(-1.0, 1.0))).toFloat()
    }

    /**
     * Which country was tapped at [lon]/[lat].
     *
     * Outlines win, and among overlapping outlines the smallest one wins, so an
     * enclave beats the country around it. A shape too small to draw at the current
     * zoom is marked with a dot, and such a dot only wins when the tap lands very
     * close to it - otherwise tapping the middle of Germany could select Luxembourg.
     */
    fun hitTest(
        shapes: List<MapShape>,
        lon: Float,
        lat: Float,
        codes: Set<String>,
        dotShapes: Set<String>,
        dotRadiusDeg: Float,
    ): String? {
        var dot: String? = null
        var dotDist = Float.MAX_VALUE
        var outline: String? = null
        var outlineArea = Float.MAX_VALUE

        for (s in shapes) {
            if (s.alpha2 !in codes) continue
            if (s.alpha2 in dotShapes) {
                val d = angularDistance(lon, lat, s.centerLon, s.centerLat)
                if (d <= dotRadiusDeg && d < dotDist) {
                    dotDist = d
                    dot = s.alpha2
                }
            } else if (s.area < outlineArea && s.contains(lon, lat)) {
                outlineArea = s.area
                outline = s.alpha2
            }
        }
        // A dot sitting inside a bigger country (Luxembourg in Germany, Monaco in France)
        // only wins when the tap lands on the drawn dot itself, not merely nearby.
        if (dot != null && (outline == null || dotDist <= dotRadiusDeg * 0.45f)) return dot
        return outline
    }

    /** A view that frames the given countries: centre plus a globe radius multiplier. */
    fun frame(shapes: List<MapShape>, codes: Set<String>): FloatArray {
        if (codes.isEmpty()) return floatArrayOf(0f, 20f, 1f)
        var minLon = 180f; var maxLon = -180f; var minLat = 90f; var maxLat = -90f
        var any = false
        for (s in shapes) {
            if (s.alpha2 !in codes) continue
            any = true
            minLon = min(minLon, s.centerLon); maxLon = max(maxLon, s.centerLon)
            minLat = min(minLat, s.centerLat); maxLat = max(maxLat, s.centerLat)
        }
        if (!any) return floatArrayOf(0f, 20f, 1f)
        // Regions that straddle the antimeridian (Oceania) need the centre picked the long way.
        val spanDirect = maxLon - minLon
        val centreLon = if (spanDirect > 180f) {
            GlobeCamera.wrapLon((minLon + maxLon) / 2f + 180f)
        } else {
            (minLon + maxLon) / 2f
        }
        val span = max(if (spanDirect > 180f) 360f - spanDirect else spanDirect, maxLat - minLat)
        // 1x shows the whole globe; zoom in for a small region, never past 3.2x.
        val zoom = when {
            span > 120f -> 1f
            span > 70f -> 1.4f
            span > 40f -> 1.9f
            span > 20f -> 2.5f
            else -> 3.2f
        }
        return floatArrayOf(centreLon, ((minLat + maxLat) / 2f).coerceIn(-70f, 70f), zoom)
    }

    /** Shapes whose on-screen size is below [minPx] and therefore need a dot. */
    fun dotShapes(shapes: List<MapShape>, codes: Set<String>, radiusPx: Float, minPx: Float): Set<String> {
        val pxPerDeg = radiusPx * (Math.PI / 180.0).toFloat()
        val out = HashSet<String>()
        for (s in shapes) {
            if (s.alpha2 !in codes) continue
            if (s.widthDeg * pxPerDeg < minPx && s.heightDeg * pxPerDeg < minPx) out.add(s.alpha2)
        }
        return out
    }

    /** Point t of the way along the great circle from 1 to 2. */
    fun interpolate(lon1: Float, lat1: Float, lon2: Float, lat2: Float, t: Float): Pair<Float, Float> {
        val p1 = Math.toRadians(lat1.toDouble()); val l1 = Math.toRadians(lon1.toDouble())
        val p2 = Math.toRadians(lat2.toDouble()); val l2 = Math.toRadians(lon2.toDouble())
        val x1 = cos(p1) * cos(l1); val y1 = cos(p1) * sin(l1); val z1 = sin(p1)
        val x2 = cos(p2) * cos(l2); val y2 = cos(p2) * sin(l2); val z2 = sin(p2)
        val dot = (x1 * x2 + y1 * y2 + z1 * z2).coerceIn(-1.0, 1.0)
        val omega = kotlin.math.acos(dot)
        if (omega < 1e-6) return lon1 to lat1
        val s1 = sin((1 - t) * omega) / sin(omega)
        val s2 = sin(t * omega) / sin(omega)
        val x = s1 * x1 + s2 * x2
        val y = s1 * y1 + s2 * y2
        val z = s1 * z1 + s2 * z2
        val lat = Math.toDegrees(asin(z.coerceIn(-1.0, 1.0)))
        val lon = Math.toDegrees(atan2(y, x))
        return lon.toFloat() to lat.toFloat()
    }

    /** Great-circle distance in kilometres, using the mean Earth radius. */
    fun distanceKm(lon1: Float, lat1: Float, lon2: Float, lat2: Float): Double =
        angularDistance(lon1, lat1, lon2, lat2) * 111.195

    fun shortestLonDelta(from: Float, to: Float): Float {
        var d = to - from
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return d
    }

    fun approxEqual(a: Float, b: Float, eps: Float = 0.001f) = abs(a - b) < eps
}

/**
 * Where the sun is overhead right now, as lon/lat.
 *
 * Deliberately simple: a cosine for the declination and 15 degrees per hour for the
 * longitude. It ignores the equation of time, so it can be off by up to about four
 * degrees of longitude (roughly fifteen minutes). That is invisible at globe scale and
 * keeps the app free of any date library.
 */
object SunPosition {
    fun subsolar(epochMillis: Long): Pair<Float, Float> {
        val daysSinceEpoch = epochMillis / 86_400_000.0
        val secondsOfDay = ((epochMillis % 86_400_000L) + 86_400_000L) % 86_400_000L / 1000.0
        // 1970-01-01 was day 1 of the year; +10 shifts the cosine to the solstice.
        val dayOfYear = (daysSinceEpoch % 365.2422) + 1
        val decl = -23.44 * kotlin.math.cos(Math.toRadians(360.0 / 365.2422 * (dayOfYear + 10)))
        val lon = 180.0 - secondsOfDay / 240.0   // 86400 s over 360 deg = 240 s per degree
        return GlobeCamera.wrapLon(lon.toFloat()) to decl.toFloat()
    }

    /** 1 at the subsolar point, 0 at the terminator, negative on the night side. */
    fun illumination(lon: Float, lat: Float, sunLon: Float, sunLat: Float): Float =
        kotlin.math.cos(Math.toRadians(GlobeMath.angularDistance(lon, lat, sunLon, sunLat).toDouble())).toFloat()
}
