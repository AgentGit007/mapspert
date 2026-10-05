package app.mapspert.quiz

/**
 * Country outlines in plate carree (lon/lat) degrees.
 * Pure Kotlin so the hit-testing can be unit-tested on the JVM.
 */
class MapShape(
    val alpha2: String,
    /** Each ring is x0,y0,x1,y1,... in degrees. */
    val rings: List<FloatArray>,
    val minLon: Float,
    val minLat: Float,
    val maxLon: Float,
    val maxLat: Float,
    val centerLon: Float,
    val centerLat: Float,
    /** Square degrees; used to decide whether the shape needs a dot marker. */
    val area: Float,
) {
    val widthDeg: Float get() = maxLon - minLon
    val heightDeg: Float get() = maxLat - minLat

    fun contains(lon: Float, lat: Float): Boolean {
        if (lon < minLon || lon > maxLon || lat < minLat || lat > maxLat) return false
        return rings.any { pointInRing(it, lon, lat) }
    }

    private fun pointInRing(r: FloatArray, x: Float, y: Float): Boolean {
        var inside = false
        var j = r.size - 2
        var i = 0
        while (i < r.size) {
            val xi = r[i]; val yi = r[i + 1]
            val xj = r[j]; val yj = r[j + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
            j = i
            i += 2
        }
        return inside
    }

    /** Degrees from the shape's centre; used for dot-sized countries. */
    fun distanceTo(lon: Float, lat: Float): Float {
        val dx = lon - centerLon
        val dy = lat - centerLat
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

class MapData(val shapes: List<MapShape>) {
    private val byCode = shapes.associateBy { it.alpha2 }

    operator fun get(alpha2: String): MapShape? = byCode[alpha2]

    /**
     * Which country was tapped. [dotRadiusDeg] lets small shapes be hit from a little
     * way off, so island states stay playable; larger shapes are only hit inside.
     */
    fun hitTest(lon: Float, lat: Float, codes: Set<String>, dotRadiusDeg: Float): String? {
        // 1. Dot-sized shapes win: a tap near Monaco should not hit France.
        var bestDot: String? = null
        var bestDist = Float.MAX_VALUE
        // 2. Among overlapping outlines, the smallest wins (enclaves such as Lesotho or
        //    San Marino sit inside a larger neighbour).
        var bestShape: String? = null
        var bestArea = Float.MAX_VALUE
        for (s in shapes) {
            if (s.alpha2 !in codes) continue
            val needsDot = s.widthDeg < dotRadiusDeg * 2 && s.heightDeg < dotRadiusDeg * 2
            if (needsDot) {
                val d = s.distanceTo(lon, lat)
                if (d <= dotRadiusDeg && d < bestDist) {
                    bestDist = d
                    bestDot = s.alpha2
                }
            } else if (s.area < bestArea && s.contains(lon, lat)) {
                bestArea = s.area
                bestShape = s.alpha2
            }
        }
        return bestDot ?: bestShape
    }

    /** Bounding box of a set of countries, padded, for the initial camera. */
    fun boundsOf(codes: Set<String>): FloatArray {
        var minLon = 180f; var minLat = 90f; var maxLon = -180f; var maxLat = -90f
        for (s in shapes) {
            if (s.alpha2 !in codes) continue
            if (s.minLon < minLon) minLon = s.minLon
            if (s.minLat < minLat) minLat = s.minLat
            if (s.maxLon > maxLon) maxLon = s.maxLon
            if (s.maxLat > maxLat) maxLat = s.maxLat
        }
        if (minLon > maxLon) return floatArrayOf(-180f, -60f, 180f, 85f)
        val padX = ((maxLon - minLon) * 0.06f).coerceAtLeast(1f)
        val padY = ((maxLat - minLat) * 0.06f).coerceAtLeast(1f)
        return floatArrayOf(minLon - padX, minLat - padY, maxLon + padX, maxLat + padY)
    }
}
