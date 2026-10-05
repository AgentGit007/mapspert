package app.mapspert

import android.content.Context
import app.mapspert.quiz.Country
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
    )
}

/** Local-only progress: best score per mode/scope/length. No network, no accounts. */
class Stats(context: Context) {
    private val prefs = context.getSharedPreferences("stats", Context.MODE_PRIVATE)
    fun best(key: String): Int = prefs.getInt("best_$key", -1)
    fun record(key: String, percent: Int) {
        if (percent > best(key)) prefs.edit().putInt("best_$key", percent).apply()
    }
}
