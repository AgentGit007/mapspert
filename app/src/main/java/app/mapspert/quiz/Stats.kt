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
