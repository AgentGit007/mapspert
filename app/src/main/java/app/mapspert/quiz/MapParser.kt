package app.mapspert.quiz

/**
 * Parses the delta-encoded map.json produced by tools/build_map.py.
 * Kept free of Android imports so it can be exercised on the JVM.
 */
object MapParser {
    fun parse(
        quant: Int,
        entries: List<RawShape>,
    ): MapData = MapData(entries.map { e ->
        val rings = e.rings.map { deltas ->
            val out = FloatArray(deltas.size)
            var x = 0
            var y = 0
            var i = 0
            while (i < deltas.size) {
                x += deltas[i]
                y += deltas[i + 1]
                out[i] = x.toFloat() / quant
                out[i + 1] = y.toFloat() / quant
                i += 2
            }
            out
        }
        MapShape(
            alpha2 = e.alpha2,
            rings = rings,
            minLon = e.bbox[0], minLat = e.bbox[1], maxLon = e.bbox[2], maxLat = e.bbox[3],
            centerLon = e.center[0], centerLat = e.center[1],
            area = e.area,
        )
    })

    data class RawShape(
        val alpha2: String,
        val bbox: FloatArray,
        val center: FloatArray,
        val area: Float,
        val rings: List<IntArray>,
    )
}
