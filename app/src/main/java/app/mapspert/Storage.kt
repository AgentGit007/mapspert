package app.mapspert

import android.content.Context
import app.mapspert.quiz.Country
import app.mapspert.quiz.CountryStat
import app.mapspert.quiz.Difficulty
import app.mapspert.quiz.MapData
import app.mapspert.quiz.MapParser
import app.mapspert.quiz.QuizMode
import app.mapspert.quiz.Region
import app.mapspert.quiz.RoundRecord
import app.mapspert.quiz.Setup
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
        mapArea = o.optDouble("map_area", 0.0).toFloat(),
    )
}

object MapRepository {
    /** Reads the binary outline asset. ~110k points, so JSON would be slow here. */
    fun load(context: Context): MapData =
        context.assets.open("map.bin").use { MapParser.parse(it) }
}

enum class ThemeMode(val label: String) { SYSTEM("Follow system"), LIGHT("Light"), DARK("Dark") }

/** A round interrupted by the system, kept so it can be resumed. */
data class SavedRound(
    val player: String,
    val mode: String,
    val region: String,
    val difficulty: String,
    val questionsPerRound: Int,
    val rounds: Int,
    val roundNumber: Int,
    val subjects: List<String>,
    val index: Int,
    val score: Int,
    val millis: Long,
    val missed: List<String>,
    val outcomes: List<Boolean>,
    val finished: List<RoundRecord>,
    /** Subjects of every round in the session; empty in saves from older versions. */
    val plan: List<List<String>> = emptyList(),
) {
    fun setup(): Setup? {
        val m = QuizMode.values().firstOrNull { it.name == mode } ?: return null
        val r = Region.values().firstOrNull { it.name == region } ?: return null
        val d = Difficulty.values().firstOrNull { it.name == difficulty } ?: return null
        return Setup(m, r, d, questionsPerRound.takeIf { it > 0 }, rounds)
    }
}

/** All settings and statistics. SharedPreferences; nothing is sent anywhere. */
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

    /** Ask more often about countries you miss or have not seen. */
    var practiceWeighting: Boolean
        get() = prefs.getBoolean("weighted", true)
        set(v) = prefs.edit().putBoolean("weighted", v).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("awake", true)
        set(v) = prefs.edit().putBoolean("awake", v).apply()

    fun knownPlayers(): List<String> =
        (prefs.getStringSet("players", null) ?: emptySet()).toMutableSet().apply { add(player) }.sorted()

    private fun rememberPlayer(name: String) {
        val set = (prefs.getStringSet("players", null) ?: emptySet()).toMutableSet()
        set.add(name)
        prefs.edit().putStringSet("players", set).apply()
    }

    // ---- round records ----

    fun records(): List<RoundRecord> = parseRecords(prefs.getString("records", null))

    private fun parseRecords(raw: String?): List<RoundRecord> {
        if (raw.isNullOrBlank()) return emptyList()
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

    private fun recordsJson(list: List<RoundRecord>): JSONArray {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("player", it.player).put("t", it.epochMillis).put("mode", it.mode)
                    .put("region", it.region).put("difficulty", it.difficulty)
                    .put("q", it.questions).put("c", it.correct).put("ms", it.millis),
            )
        }
        return arr
    }

    fun addRecord(r: RoundRecord) {
        val kept = (records() + r).takeLast(500)
        prefs.edit().putString("records", recordsJson(kept).toString()).apply()
        rememberPlayer(r.player)
    }

    // ---- per-country statistics ----

    private fun statsKey(player: String) = "countrystats_$player"

    fun countryStats(player: String): Map<String, CountryStat> {
        val raw = prefs.getString(statsKey(player), null) ?: return emptyMap()
        return parseCountryStats(raw)
    }

    private fun parseCountryStats(raw: String): Map<String, CountryStat> {
        val o = JSONObject(raw)
        val out = HashMap<String, CountryStat>()
        for (code in o.keys()) {
            val a = o.getJSONArray(code)
            out[code] = CountryStat(code, a.getInt(0), a.getInt(1), a.getLong(2))
        }
        return out
    }

    private fun countryStatsJson(map: Map<String, CountryStat>): JSONObject {
        val o = JSONObject()
        map.values.forEach { s ->
            o.put(s.alpha2, JSONArray().put(s.seen).put(s.correct).put(s.millis))
        }
        return o
    }

    fun recordAnswer(player: String, alpha2: String, correct: Boolean, millis: Long) {
        val map = countryStats(player).toMutableMap()
        val cur = map[alpha2] ?: CountryStat(alpha2, 0, 0, 0)
        map[alpha2] = cur.plus(correct, millis)
        prefs.edit().putString(statsKey(player), countryStatsJson(map).toString()).apply()
    }

    fun clearRecords(player: String?) {
        val e = prefs.edit()
        if (player == null) {
            e.putString("records", "[]")
            knownPlayers().forEach { e.remove(statsKey(it)) }
        } else {
            e.putString("records", recordsJson(records().filter { it.player != player }).toString())
            e.remove(statsKey(player))
        }
        e.apply()
    }

    // ---- an interrupted round ----

    fun saveRound(r: SavedRound?) {
        if (r == null) {
            prefs.edit().remove("inflight").apply()
            return
        }
        val o = JSONObject()
            .put("player", r.player).put("mode", r.mode).put("region", r.region)
            .put("difficulty", r.difficulty).put("qpr", r.questionsPerRound).put("rounds", r.rounds)
            .put("roundNumber", r.roundNumber).put("index", r.index).put("score", r.score)
            .put("millis", r.millis)
            .put("subjects", JSONArray(r.subjects))
            .put("missed", JSONArray(r.missed))
            .put("outcomes", JSONArray(r.outcomes.map { if (it) 1 else 0 }))
            .put("finished", recordsJson(r.finished))
            .put("plan", JSONArray(r.plan.map { JSONArray(it) }))
        prefs.edit().putString("inflight", o.toString()).apply()
    }

    fun savedRound(): SavedRound? {
        val raw = prefs.getString("inflight", null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            fun strings(key: String): List<String> {
                val a = o.optJSONArray(key) ?: return emptyList()
                return List(a.length()) { a.getString(it) }
            }
            SavedRound(
                player = o.getString("player"),
                mode = o.getString("mode"),
                region = o.getString("region"),
                difficulty = o.getString("difficulty"),
                questionsPerRound = o.getInt("qpr"),
                rounds = o.getInt("rounds"),
                roundNumber = o.getInt("roundNumber"),
                subjects = strings("subjects"),
                index = o.getInt("index"),
                score = o.getInt("score"),
                millis = o.getLong("millis"),
                missed = strings("missed"),
                outcomes = o.optJSONArray("outcomes")?.let { a -> List(a.length()) { a.getInt(it) == 1 } } ?: emptyList(),
                finished = parseRecords(o.optJSONArray("finished")?.toString()),
                plan = o.optJSONArray("plan")?.let { a ->
                    List(a.length()) { i ->
                        val r = a.getJSONArray(i)
                        List(r.length()) { r.getString(it) }
                    }
                } ?: emptyList(),
            )
        }.getOrNull()
    }

    // ---- export / import ----

    fun exportJson(): String {
        val players = JSONObject()
        knownPlayers().forEach { p -> players.put(p, countryStatsJson(countryStats(p))) }
        return JSONObject()
            .put("app", "mapspert")
            .put("version", 1)
            .put("exported", System.currentTimeMillis())
            .put("settings", JSONObject()
                .put("player", player)
                .put("difficulty", difficulty.name)
                .put("questionsPerRound", questionsPerRound)
                .put("rounds", rounds)
                .put("theme", themeMode.name)
                .put("practiceWeighting", practiceWeighting)
                .put("keepScreenOn", keepScreenOn))
            .put("records", recordsJson(records()))
            .put("countryStats", players)
            .toString(2)
    }

    /** Merge-by-default: an import adds to what is here rather than wiping it. */
    fun importJson(text: String, replace: Boolean): ImportResult {
        val o = runCatching { JSONObject(text) }.getOrNull()
            ?: return ImportResult(false, "That file is not readable JSON.")
        if (o.optString("app") != "mapspert") {
            return ImportResult(false, "That file was not exported by Mapspert.")
        }
        val incoming = parseRecords(o.optJSONArray("records")?.toString())
        val mergedRecords = if (replace) incoming else {
            // A round is identified by its player and timestamp, so re-importing is safe.
            val have = records()
            val seen = have.map { it.player to it.epochMillis }.toHashSet()
            have + incoming.filterNot { (it.player to it.epochMillis) in seen }
        }
        val e = prefs.edit()
        e.putString("records", recordsJson(mergedRecords.takeLast(500)).toString())

        val stats = o.optJSONObject("countryStats")
        var players = 0
        if (stats != null) {
            for (p in stats.keys()) {
                players++
                val incomingStats = parseCountryStats(stats.getJSONObject(p).toString())
                val merged = if (replace) incomingStats else {
                    val cur = countryStats(p).toMutableMap()
                    incomingStats.forEach { (code, s) ->
                        val old = cur[code]
                        cur[code] = if (old == null) s else CountryStat(
                            code, old.seen + s.seen, old.correct + s.correct, old.millis + s.millis,
                        )
                    }
                    cur
                }
                e.putString(statsKey(p), countryStatsJson(merged).toString())
            }
            val names = (prefs.getStringSet("players", null) ?: emptySet()).toMutableSet()
            names.addAll(stats.keys().asSequence())
            e.putStringSet("players", names)
        }
        o.optJSONObject("settings")?.let { s ->
            if (replace) {
                s.optString("player").takeIf { it.isNotBlank() }?.let { e.putString("player", it) }
                e.putString("difficulty", s.optString("difficulty", difficulty.name))
                e.putInt("qpr", s.optInt("questionsPerRound", questionsPerRound))
                e.putInt("rounds", s.optInt("rounds", rounds))
                e.putString("theme", s.optString("theme", themeMode.name))
                e.putBoolean("weighted", s.optBoolean("practiceWeighting", practiceWeighting))
                e.putBoolean("awake", s.optBoolean("keepScreenOn", keepScreenOn))
            }
        }
        e.apply()
        return ImportResult(
            true,
            "Imported ${incoming.size} rounds and statistics for $players " +
                if (players == 1) "player." else "players.",
        )
    }
}

data class ImportResult(val ok: Boolean, val message: String)
