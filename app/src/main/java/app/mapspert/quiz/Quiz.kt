package app.mapspert.quiz

import kotlin.random.Random

/** Pure Kotlin (no Android imports) so it can be unit-tested on the JVM. */
data class Country(
    val alpha2: String,
    val alpha3: String,
    val numeric: String,
    val name: String,
    val capitals: List<String>,
    val isoIndependent: Boolean?,
    val unMember: Boolean,
    /** un_member | un_observer | territory | not_in_iso */
    val status: String,
    val tld: String?,
    val tldInUse: Boolean,
    val tldOther: List<String>,
    val flag: String,
    val isoNote: String?,
    val continent: String,
    val euMember: Boolean,
    /** Has an outline in map.json, so it can be used in the map quiz. */
    val mapAvailable: Boolean,
) {
    val capital: String? get() = capitals.firstOrNull()
    val acceptedTlds: List<String> get() = listOfNotNull(tld) + tldOther
}

/** Which countries a round draws from. */
enum class Region(val label: String, val short: String) {
    WORLD("World", "World"),
    EUROPE("Europe", "Europe"),
    EU("European Union", "EU"),
    AFRICA("Africa", "Africa"),
    ASIA("Asia", "Asia"),
    NORTH_AMERICA("North America", "N. America"),
    SOUTH_AMERICA("South America", "S. America"),
    OCEANIA("Oceania", "Oceania");

    fun includes(c: Country): Boolean = when (this) {
        WORLD -> true
        EU -> c.euMember
        EUROPE -> c.continent == "Europe"
        AFRICA -> c.continent == "Africa"
        ASIA -> c.continent == "Asia"
        NORTH_AMERICA -> c.continent == "North America"
        SOUTH_AMERICA -> c.continent == "South America"
        OCEANIA -> c.continent == "Oceania"
    }
}

/**
 * How wide the pool is. Easy/Medium also trim the option set, so they are a genuine
 * difficulty ladder rather than only a size filter.
 */
enum class Difficulty(val label: String, val blurb: String, val options: Int) {
    EASY("Easy", "UN members, 3 options", 3),
    MEDIUM("Medium", "UN members and observers, 4 options", 4),
    HARD("Hard", "Everything incl. territories, 4 options", 4);

    fun includes(c: Country): Boolean = when (this) {
        EASY -> c.status == "un_member"
        MEDIUM -> c.status == "un_member" || c.status == "un_observer"
        HARD -> true
    }
}

enum class Category(val label: String, val blurb: String) {
    FLAGS("Flags", "Recognise flags"),
    PLACES("Countries & capitals", "Capitals and map locations"),
    CODES("ISO codes", "Alpha-2 and alpha-3"),
    OTHER("Other", "Internet domains"),
}

/** OPTIONS questions are answered by picking a tile; MAP questions by tapping the map. */
enum class Answering { OPTIONS, MAP }

enum class QuizMode(
    val category: Category,
    val title: String,
    val blurb: String,
    val answering: Answering = Answering.OPTIONS,
) {
    MAP_CLICK(Category.PLACES, "Find on the map", "Tap the named country", Answering.MAP),
    FLAG_TO_COUNTRY(Category.FLAGS, "Name the flag", "Which country has this flag?"),
    COUNTRY_TO_FLAG(Category.FLAGS, "Find the flag", "Pick the flag for a country"),
    COUNTRY_TO_CAPITAL(Category.PLACES, "Capitals", "Which city is the capital?"),
    CAPITAL_TO_COUNTRY(Category.PLACES, "Capital to country", "Which country has this capital?"),
    ALPHA2(Category.CODES, "Alpha-2 codes", "Two-letter codes such as DE or JP"),
    ALPHA3(Category.CODES, "Alpha-3 codes", "Three-letter codes such as DEU or JPN"),
    TLD(Category.OTHER, "Internet domains", "Country domains such as .de or .jp"),
}

sealed interface Prompt {
    val caption: String
    data class Text(override val caption: String, val text: String) : Prompt
    data class Flag(override val caption: String, val flag: String) : Prompt
}

/** Exactly one of [label] / [flag] is set. */
data class Option(val label: String? = null, val flag: String? = null)

data class Question(
    val subject: Country,
    val prompt: Prompt,
    val options: List<Option>,
    val correctIndex: Int,
)

data class Setup(
    val mode: QuizMode,
    val region: Region,
    val difficulty: Difficulty,
    /** null = every eligible country */
    val questionsPerRound: Int?,
    val rounds: Int,
) {
    val statsKey: String get() = "${mode.name}|${region.name}|${difficulty.name}"
}

object QuizEngine {

    fun pool(all: List<Country>, region: Region, difficulty: Difficulty): List<Country> =
        all.filter { region.includes(it) && difficulty.includes(it) }

    /** Countries that can be asked about in [mode] within [pool]. */
    fun eligible(mode: QuizMode, pool: List<Country>): List<Country> = when (mode) {
        QuizMode.FLAG_TO_COUNTRY, QuizMode.COUNTRY_TO_FLAG -> pool
        QuizMode.COUNTRY_TO_CAPITAL -> pool.filter { it.capital != null }
        QuizMode.CAPITAL_TO_COUNTRY -> pool.filter { c ->
            c.capital != null && pool.count { o -> c.capital in o.capitals } == 1
        }
        // Kosovo's XK is only user-assigned, so it is not an ISO code question.
        QuizMode.ALPHA2, QuizMode.ALPHA3 -> pool.filter { it.status != "not_in_iso" }
        QuizMode.MAP_CLICK -> pool.filter { it.mapAvailable }
        QuizMode.TLD -> pool.filter { it.tld != null && it.tldInUse }
    }

    /** A pool with fewer than 4 countries cannot fill an option set. */
    fun playable(setup: Setup, all: List<Country>): Boolean {
        val n = eligible(setup.mode, pool(all, setup.region, setup.difficulty)).size
        return if (setup.mode.answering == Answering.MAP) n >= 1 else n >= setup.difficulty.options
    }

    fun newRound(setup: Setup, all: List<Country>, rng: Random = Random.Default): List<Question> {
        val pool = pool(all, setup.region, setup.difficulty)
        val shuffled = eligible(setup.mode, pool).shuffled(rng)
        val subjects = setup.questionsPerRound?.let { shuffled.take(it) } ?: shuffled
        return subjects.map { buildQuestion(setup.mode, it, pool, setup.difficulty.options, rng) }
    }

    fun buildQuestion(
        mode: QuizMode,
        s: Country,
        pool: List<Country>,
        optionCount: Int,
        rng: Random,
    ): Question = when (mode) {
        // The map screen builds its own answer surface, so there are no option tiles.
        QuizMode.MAP_CLICK -> Question(s, Prompt.Text("Find on the map", s.name), emptyList(), -1)
        QuizMode.FLAG_TO_COUNTRY -> names(
            Prompt.Flag("Which country has this flag?", s.flag), s, pool, optionCount, rng,
        )
        QuizMode.COUNTRY_TO_FLAG -> {
            val wrong = pool.filter { it.alpha2 != s.alpha2 }.shuffled(rng).take(optionCount - 1)
            finish(s, Prompt.Text("Which flag belongs to", s.name), Option(flag = s.flag), wrong.map { Option(flag = it.flag) }, rng)
        }
        QuizMode.COUNTRY_TO_CAPITAL -> labels(
            Prompt.Text("What is the capital of", s.name), s, pool, optionCount, rng,
            correct = s.capital!!, forbidden = s.capitals.toSet(),
        ) { it.capital }
        QuizMode.CAPITAL_TO_COUNTRY -> names(
            Prompt.Text("Which country has the capital", s.capital!!), s, pool, optionCount, rng,
            exclude = { it.capitals.contains(s.capital) },
        )
        QuizMode.ALPHA2 -> codeQuestion("alpha-2", s, pool, optionCount, rng, reverseAllowed = true, code = { it.alpha2 }, accepted = { listOf(it.alpha2) })
        QuizMode.ALPHA3 -> codeQuestion("alpha-3", s, pool, optionCount, rng, reverseAllowed = true, code = { it.alpha3 }, accepted = { listOf(it.alpha3) })
        QuizMode.TLD -> {
            val unique = pool.count { s.tld in it.acceptedTlds } == 1
            codeQuestion("domain", s, pool, optionCount, rng, reverseAllowed = unique, code = { it.tld }, accepted = { it.acceptedTlds })
        }
    }

    // ---- helpers -----------------------------------------------------------------------

    private fun codeQuestion(
        kind: String,
        s: Country,
        pool: List<Country>,
        optionCount: Int,
        rng: Random,
        reverseAllowed: Boolean,
        code: (Country) -> String?,
        accepted: (Country) -> List<String>,
    ): Question {
        val mine = code(s)!!
        return if (reverseAllowed && rng.nextBoolean()) {
            names(
                Prompt.Text("Which country has the $kind code", mine), s, pool, optionCount, rng,
                exclude = { mine in accepted(it) },
            )
        } else {
            labels(
                Prompt.Text("What is the $kind code of", s.name), s, pool, optionCount, rng,
                correct = mine, forbidden = accepted(s).toSet(),
            ) { code(it) }
        }
    }

    /** Options are country names. */
    private fun names(
        prompt: Prompt,
        s: Country,
        pool: List<Country>,
        optionCount: Int,
        rng: Random,
        exclude: (Country) -> Boolean = { false },
    ): Question {
        val wrong = pool.asSequence()
            .filter { it.alpha2 != s.alpha2 && !exclude(it) && it.name != s.name }
            .toList().shuffled(rng).take(optionCount - 1)
        return finish(s, prompt, Option(label = s.name), wrong.map { Option(label = it.name) }, rng)
    }

    /** Options are strings derived from other countries (capital, code, ...). */
    private fun labels(
        prompt: Prompt,
        s: Country,
        pool: List<Country>,
        optionCount: Int,
        rng: Random,
        correct: String,
        forbidden: Set<String>,
        pick: (Country) -> String?,
    ): Question {
        val seen = HashSet<String>()
        val wrong = ArrayList<String>()
        for (c in pool.filter { it.alpha2 != s.alpha2 }.shuffled(rng)) {
            val v = pick(c) ?: continue
            if (v in forbidden || v == correct || !seen.add(v)) continue
            wrong.add(v)
            if (wrong.size == optionCount - 1) break
        }
        return finish(s, prompt, Option(label = correct), wrong.map { Option(label = it) }, rng)
    }

    private fun finish(s: Country, prompt: Prompt, correct: Option, wrong: List<Option>, rng: Random): Question {
        val all = (wrong + correct).shuffled(rng)
        return Question(s, prompt, all, all.indexOf(correct))
    }
}

/** Human-readable facts, shared by the answer card and the detail screen. */
object Facts {
    fun statusLabel(c: Country): String = when (c.status) {
        "un_member" -> "UN member state"
        "un_observer" -> "UN observer state"
        "not_in_iso" -> "Not in ISO 3166-1 (code XK is user-assigned)"
        else -> "Territory or special area"
    }

    fun isoLine(c: Country): String = when (c.isoIndependent) {
        true -> "ISO 3166-1: listed as independent"
        false -> "ISO 3166-1: listed as not independent"
        null -> "ISO 3166-1: no official entry"
    }

    fun rows(c: Country): List<Pair<String, String>> = buildList {
        add((if (c.capitals.size > 1) "Capitals" else "Capital") to
            (if (c.capitals.isEmpty()) "none" else c.capitals.joinToString(", ")))
        add("Continent" to c.continent + if (c.euMember) " (EU)" else "")
        add("Alpha-2" to if (c.status == "not_in_iso") "${c.alpha2} (user-assigned)" else c.alpha2)
        if (c.status != "not_in_iso") {
            add("Alpha-3" to c.alpha3)
            if (c.numeric.isNotEmpty()) add("Numeric" to c.numeric)
        }
        add("Domain" to when {
            c.tld == null -> "none"
            !c.tldInUse -> "${c.tld} (reserved, not in use)"
            else -> c.tld
        })
        if (c.tldOther.isNotEmpty()) add("Other domains" to c.tldOther.joinToString(" "))
    }
}
