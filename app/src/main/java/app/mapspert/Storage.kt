package app.mapspert

import android.content.Context
import app.mapspert.quiz.Country
import app.mapspert.quiz.Difficulty
import app.mapspert.quiz.MapData
import app.mapspert.quiz.MapParser
import app.mapspert.quiz.RoundRecord
import org.json.JSONArray
import org.json.JSONObject

object CountryRepository {
    fun load(context: Context): List<Country> {
        val text = context.assets.open("countries.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        return List(arr.length()) { parse(arr.getJSONObject(it)) }
    }

    private fun JSONObject.str(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.strings(key: String): List<String> {
        val a = optJSONArray(key) ?: return emptyList()
        return List(a.length()) { a.getString(it) }
    }

    private fun parse(o: JSONObject) = Country(
        alpha2 = o.getString("alpha2"),
        alpha3 = o.getString("alpha3"),
        numeric = o.getString("numeric"),
        name = o.getString("name"),
        capitals = o.strings("capitals"),
        isoIndependent = if (o.isNull("iso_independent")) null else o.getBoolean("iso_independent"),
        unMember = o.getBoolean("un_member"),
        status = o.getString("status"),
        tld = o.str("tld"),
        tldInUse = o.getBoolean("tld_in_use"),
        tldOther = o.strings("tld_other"),
        flag = o.getString("flag"),
        isoNote = o.str("iso_note"),
        continent = o.getString("continent"),
        euMember = o.getBoolean("eu_member"),
        mapAvailable = o.getBoolean("map_available"),
    )
}

object MapRepository {
    fun load(context: Context): MapData {
        val text = context.assets.open("map.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val quant = root.getInt("quant")
        val arr = root.getJSONArray("countries")
        val shapes = ArrayList<MapParser.RawShape>(arr.length())
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            val bbox = e.getJSONArray("bbox")
            val c = e.getJSONArray("c")
            val ringsArr = e.getJSONArray("rings")
            val rings = ArrayList<IntArray>(ringsArr.length())
            for (r in 0 until ringsArr.length()) {
                val ra = ringsArr.getJSONArray(r)
                val ints = IntArray(ra.length()) { ra.getInt(it) }
                rings.add(ints)
            }
            shapes.add(
                MapParser.RawShape(
                    alpha2 = e.getString("a2"),
                    bbox = FloatArray(4) { bbox.getDouble(it).toFloat() },
                    center = FloatArray(2) { c.getDouble(it).toFloat() },
                    area = e.getDouble("area").toFloat(),
                    rings = rings,
                ),
            )
        }
        return MapParser.parse(quant, shapes)
    }
}

enum class ThemeMode(val label: String) { SYSTEM("Follow system"), LIGHT("Light"), DARK("Dark") }

/** All settings and stats live in SharedPreferences. Nothing is sent anywhere. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("mapspert", Context.MODE_PRIVATE)

    var difficulty: Difficulty
        get() = Difficulty.values().firstOrNull { it.name == prefs.getString("difficulty", null) } ?: Difficulty.MEDIUM
        set(v) = prefs.edit().putString("difficulty", v.name).apply()

    /** 0 means "every country in the pool". */
    var questionsPerRound: Int
        get() = prefs.getInt("qpr", 10)
        set(v) = prefs.edit().putInt("qpr", v).apply()

    var rounds: Int
        get() = prefs.getInt("rounds", 1)
        set(v) = prefs.edit().putInt("rounds", v).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.values().firstOrNull { it.name == prefs.getString("theme", null) } ?: ThemeMode.SYSTEM
        set(v) = prefs.edit().putString("theme", v.name).apply()

    var player: String
        get() = prefs.getString("player", "Me") ?: "Me"
        set(v) = prefs.edit().putString("player", v.ifBlank { "Me" }).apply()

    fun knownPlayers(): List<String> =
        (prefs.getStringSet("players", null) ?: emptySet()).toMutableSet().apply { add(player) }.sorted()

    private fun rememberPlayer(name: String) {
        val set = (prefs.getStringSet("players", null) ?: emptySet()).toMutableSet()
        set.add(name)
        prefs.edit().putStringSet("players", set).apply()
    }

    fun records(): List<RoundRecord> {
        val raw = prefs.getString("records", null) ?: return emptyList()
        val arr = JSONArray(raw)
        return List(arr.length()) {
            val o = arr.getJSONObject(it)
            RoundRecord(
                player = o.getString("player"),
                epochMillis = o.getLong("t"),
                mode = o.getString("mode"),
                region = o.getString("region"),
                difficulty = o.getString("difficulty"),
                questions = o.getInt("q"),
                correct = o.getInt("c"),
                millis = o.getLong("ms"),
            )
        }
    }

    fun addRecord(r: RoundRecord) {
        // Keep the newest 500 rounds; plenty for the charts, bounded on disk.
        val kept = (records() + r).takeLast(500)
        val arr = JSONArray()
        kept.forEach {
            arr.put(
                JSONObject()
                    .put("player", it.player).put("t", it.epochMillis).put("mode", it.mode)
                    .put("region", it.region).put("difficulty", it.difficulty)
                    .put("q", it.questions).put("c", it.correct).put("ms", it.millis),
            )
        }
        prefs.edit().putString("records", arr.toString()).apply()
        rememberPlayer(r.player)
    }

    fun clearRecords(player: String?) {
        val kept = if (player == null) emptyList() else records().filter { it.player != player }
        val arr = JSONArray()
        kept.forEach {
            arr.put(
                JSONObject()
                    .put("player", it.player).put("t", it.epochMillis).put("mode", it.mode)
                    .put("region", it.region).put("difficulty", it.difficulty)
                    .put("q", it.questions).put("c", it.correct).put("ms", it.millis),
            )
        }
        prefs.edit().putString("records", arr.toString()).apply()
    }
}
