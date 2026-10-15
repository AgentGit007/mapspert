package app.mapspert.quiz

import java.io.DataInputStream
import java.io.InputStream

/**
 * Reads the binary asset produced by tools/build_map.py.
 *
 * It is binary rather than JSON because the outlines carry ~90k points and
 * parsing that as JSON on the main thread is noticeably slow on a phone.
 */
object MapParser {
    private const val MAGIC = 0x4D535054 // "MSPT"
    private const val ESCAPE = (-32768).toShort()

    fun parse(input: InputStream): MapData {
        val d = DataInputStream(input.buffered(1 shl 16))
        require(d.readInt() == MAGIC) { "not a Mapspert map asset" }
        val version = d.readInt()
        require(version == 1) { "unsupported map asset version $version" }
        val quant = d.readInt().toFloat()
        val count = d.readInt()

        val shapes = ArrayList<MapShape>(count)
        val code = ByteArray(2)
        repeat(count) {
            d.readFully(code)
            val minLon = d.readShort() / quant
            val minLat = d.readShort() / quant
            val maxLon = d.readShort() / quant
            val maxLat = d.readShort() / quant
            val centerLon = d.readShort() / quant
            val centerLat = d.readShort() / quant
            val area = d.readFloat()
            d.readByte() // dot hint, recomputed at runtime from the current zoom
            val hit = readRings(d, quant)
            val draw = readRings(d, quant)
            shapes.add(
                MapShape(
                    alpha2 = String(code, Charsets.US_ASCII),
                    rings = hit,
                    drawRings = draw,
                    minLon = minLon, minLat = minLat, maxLon = maxLon, maxLat = maxLat,
                    centerLon = centerLon, centerLat = centerLat,
                    area = area,
                ),
            )
        }
        return MapData(shapes)
    }

    private fun readRings(d: DataInputStream, quant: Float): List<FloatArray> {
        val n = d.readUnsignedByte()
        val rings = ArrayList<FloatArray>(n)
        repeat(n) {
            val points = d.readInt()
            val out = FloatArray(points * 2)
            var x = 0
            var y = 0
            var i = 0
            while (i < out.size) {
                x += readDelta(d)
                y += readDelta(d)
                out[i] = x / quant
                out[i + 1] = y / quant
                i += 2
            }
            rings.add(out)
        }
        return rings
    }

    private fun readDelta(d: DataInputStream): Int {
        val s = d.readShort()
        return if (s == ESCAPE) d.readInt() else s.toInt()
    }
}
