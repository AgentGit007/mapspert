package app.mapspert.quiz

/** One completed round. Stored locally as JSON; never leaves the device. */
data class RoundRecord(
    val player: String,
    val epochMillis: Long,
    val mode: String,
    val region: String,
    val difficulty: String,
    val questions: Int,
    val correct: Int,
    /** Total answering time for the round. */
    val millis: Long,
) {
    val percent: Int get() = if (questions == 0) 0 else correct * 100 / questions
    val secondsPerQuestion: Double get() = if (questions == 0) 0.0 else millis / 1000.0 / questions
    val modeEnum: QuizMode? get() = QuizMode.values().firstOrNull { it.name == mode }
    val category: Category? get() = modeEnum?.category
}

data class Summary(
    val rounds: Int,
    val questions: Int,
    val correct: Int,
    val millis: Long,
    val bestPercent: Int,
) {
    val percent: Int get() = if (questions == 0) 0 else correct * 100 / questions
    val secondsPerQuestion: Double get() = if (questions == 0) 0.0 else millis / 1000.0 / questions
}

object StatsMath {
    fun summarize(rs: List<RoundRecord>) = Summary(
        rounds = rs.size,
        questions = rs.sumOf { it.questions },
        correct = rs.sumOf { it.correct },
        millis = rs.sumOf { it.millis },
        bestPercent = rs.maxOfOrNull { it.percent } ?: 0,
    )

    /** Oldest first, ready to plot. */
    fun trend(rs: List<RoundRecord>): List<RoundRecord> = rs.sortedBy { it.epochMillis }

    /** Mean of the last [n] rounds, used for the "improving?" line. */
    fun movingAverage(values: List<Double>, n: Int = 5): List<Double> =
        values.indices.map { i ->
            val from = maxOf(0, i - n + 1)
            values.subList(from, i + 1).average()
        }

    fun byCategory(rs: List<RoundRecord>): Map<Category, Summary> =
        rs.groupBy { it.category }.mapNotNull { (k, v) -> k?.let { it to summarize(v) } }.toMap()

    fun byMode(rs: List<RoundRecord>): Map<QuizMode, Summary> =
        rs.groupBy { it.modeEnum }.mapNotNull { (k, v) -> k?.let { it to summarize(v) } }.toMap()

    fun players(rs: List<RoundRecord>): List<String> =
        rs.map { it.player }.distinct().sorted()
}

/** What the player knows about one country. Local only, like everything else. */
data class CountryStat(
    val alpha2: String,
    val seen: Int,
    val correct: Int,
    val millis: Long,
) {
    val accuracy: Double get() = if (seen == 0) 0.0 else correct.toDouble() / seen
    val secondsPerQuestion: Double get() = if (seen == 0) 0.0 else millis / 1000.0 / seen

    fun plus(wasCorrect: Boolean, ms: Long) = CountryStat(
        alpha2 = alpha2,
        seen = seen + 1,
        correct = correct + if (wasCorrect) 1 else 0,
        millis = millis + ms,
    )
}

/** How well the player knows a country, for the knowledge globe and for weighting. */
enum class Mastery(val label: String) {
    UNSEEN("Not seen yet"),
    SHAKY("Shaky"),
    LEARNING("Getting there"),
    SOLID("Solid"),
}

object CountryStatsMath {
    /** Needs a few attempts before a verdict means anything. */
    fun mastery(s: CountryStat?): Mastery = when {
        s == null || s.seen == 0 -> Mastery.UNSEEN
        s.seen < 3 -> if (s.accuracy >= 0.99) Mastery.LEARNING else Mastery.SHAKY
        s.accuracy >= 0.85 -> Mastery.SOLID
        s.accuracy >= 0.55 -> Mastery.LEARNING
        else -> Mastery.SHAKY
    }

    /**
     * Relative likelihood of being asked when practice weighting is on.
     *
     * Unseen countries are introduced promptly, and a country you keep missing can
     * outrank them: seven misses out of eight is a clearer signal than no data. The floor keeps
     * well-known countries in rotation instead of retiring them, so a round still
     * feels like a quiz rather than a drill of your worst ten.
     */
    fun weight(s: CountryStat?): Double {
        if (s == null || s.seen == 0) return 4.0
        val missRate = 1.0 - s.accuracy
        val confidence = (s.seen.coerceAtMost(6)) / 6.0
        return 1.0 + 5.0 * missRate * confidence + 1.5 * (1.0 - confidence)
    }

    fun summarize(stats: Collection<CountryStat>): Map<Mastery, Int> {
        val out = Mastery.values().associateWith { 0 }.toMutableMap()
        for (s in stats) out[mastery(s)] = (out[mastery(s)] ?: 0) + 1
        return out
    }

    /**
     * One attempt says almost nothing, so both lists ignore anything seen fewer than
     * [minSeen] times. Without that, a country answered right once appeared in the
     * "weakest" list purely because the list had room.
     */
    const val MIN_SEEN_FOR_RANKING = 2

    /** Countries you actually get wrong, worst first. A perfect record is never "weak". */
    fun weakest(
        stats: Collection<CountryStat>,
        limit: Int = 5,
        minSeen: Int = MIN_SEEN_FOR_RANKING,
    ): List<CountryStat> =
        stats.filter { it.seen >= minSeen && it.accuracy < 1.0 }
            .sortedWith(compareBy({ it.accuracy }, { -it.seen }))
            .take(limit)

    /** Countries you reliably know, best first; ties broken by how often you have proved it. */
    fun strongest(
        stats: Collection<CountryStat>,
        limit: Int = 5,
        minSeen: Int = MIN_SEEN_FOR_RANKING,
    ): List<CountryStat> =
        stats.filter { it.seen >= minSeen }
            .sortedWith(compareByDescending<CountryStat> { it.accuracy }.thenByDescending { it.seen })
            .take(limit)

    fun percent(s: CountryStat): Int = Math.round(s.accuracy * 100).toInt()
}
