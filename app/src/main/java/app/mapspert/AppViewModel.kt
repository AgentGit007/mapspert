package app.mapspert

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.mapspert.quiz.Answering
import app.mapspert.quiz.Country
import app.mapspert.quiz.CountryStat
import app.mapspert.quiz.CountryStatsMath
import app.mapspert.quiz.Difficulty
import app.mapspert.quiz.MapData
import app.mapspert.quiz.Question
import app.mapspert.quiz.QuizEngine
import app.mapspert.quiz.QuizMode
import app.mapspert.quiz.Region
import app.mapspert.quiz.RoundRecord
import app.mapspert.quiz.Setup

class RoundState(val setup: Setup, val questions: List<Question>) {
    var index by mutableIntStateOf(0)
    var selected by mutableStateOf<Int?>(null)
    /** The country tapped on the globe. */
    var tapped by mutableStateOf<String?>(null)
    /** True when the player pressed "I don't know". */
    var passed by mutableStateOf(false)
    var score by mutableIntStateOf(0)
    var millis by mutableIntStateOf(0)
    val missed = mutableStateListOf<Country>()
    /** One entry per answered question, for the segmented progress bar. */
    val outcomes = mutableStateListOf<Boolean>()
    val current: Question get() = questions[index]
    val answered: Boolean get() = selected != null || tapped != null || passed
    val percent: Int get() = if (questions.isEmpty()) 0 else score * 100 / questions.size
}

class Session(val setup: Setup, val player: String) {
    /**
     * Subjects of every round, drawn once when the session starts so no country is asked
     * twice in it. Can hold fewer rounds than [Setup.rounds] when the pool is small.
     */
    var plan: List<List<Country>> = emptyList()
    val totalRounds: Int get() = if (plan.isEmpty()) setup.rounds else plan.size
    var roundNumber by mutableIntStateOf(1)
    var round by mutableStateOf<RoundState?>(null)
    val finished = mutableStateListOf<RoundRecord>()
    val totalQuestions: Int get() = finished.sumOf { it.questions }
    val totalCorrect: Int get() = finished.sumOf { it.correct }
    val totalMillis: Long get() = finished.sumOf { it.millis }
    val percent: Int get() = if (totalQuestions == 0) 0 else totalCorrect * 100 / totalQuestions
}

class AppViewModel(
    val countries: List<Country>,
    val map: MapData,
    private val store: Store,
) : ViewModel() {

    private val nav = Navigator()
    private val settings = SettingsState(store)

    val screen: Screen get() = nav.screen

    val themeMode: ThemeMode get() = settings.theme
    val difficulty: Difficulty get() = settings.difficulty
    val questionsPerRound: Int get() = settings.questionsPerRound
    val rounds: Int get() = settings.rounds
    val player: String get() = settings.player
    val practiceWeighting: Boolean get() = settings.practiceWeighting
    val keepScreenOn: Boolean get() = settings.keepScreenOn

    var region by mutableStateOf(Region.WORLD)
    var search by mutableStateOf("")
    var session by mutableStateOf<Session?>(null)
        private set
    var records by mutableStateOf(store.records())
        private set
    var countryStats by mutableStateOf(store.countryStats(store.player))
        private set
    /** Set when a round was interrupted and can be resumed. */
    var resumable by mutableStateOf(store.savedRound())
        private set

    private var questionStartedAt = 0L

    // ---- settings ----

    fun chooseTheme(m: ThemeMode) = settings.chooseTheme(m)
    fun chooseDifficulty(d: Difficulty) = settings.chooseDifficulty(d)
    fun chooseQuestionsPerRound(n: Int) = settings.chooseQuestionsPerRound(n)
    fun chooseRounds(n: Int) = settings.chooseRounds(n)
    fun choosePracticeWeighting(on: Boolean) = settings.choosePracticeWeighting(on)
    fun chooseKeepScreenOn(on: Boolean) = settings.chooseKeepScreenOn(on)

    fun choosePlayer(name: String) {
        settings.choosePlayer(name)
        countryStats = store.countryStats(player)
    }

    fun knownPlayers(): List<String> = store.knownPlayers()

    fun clearRecords(onlyPlayer: String?) {
        store.clearRecords(onlyPlayer)
        records = store.records()
        countryStats = store.countryStats(player)
    }

    fun statsFor(playerName: String): Map<String, CountryStat> = store.countryStats(playerName)

    fun exportJson(): String = store.exportJson()

    fun importJson(text: String, replace: Boolean): ImportResult {
        val r = store.importJson(text, replace)
        if (r.ok) {
            records = store.records()
            settings.reload()
            countryStats = store.countryStats(player)
        }
        return r
    }

    // ---- navigation ----

    fun navigate(target: Screen) = nav.to(target)
    fun back() = nav.back()
    fun home() = nav.home()
    private fun resetTo(target: Screen) = nav.resetTo(target)

    // ---- quiz ----

    fun setupFor(mode: QuizMode) = Setup(
        mode = mode,
        region = region,
        difficulty = difficulty,
        questionsPerRound = questionsPerRound.takeIf { it > 0 },
        rounds = rounds,
    )

    fun poolSize(mode: QuizMode): Int =
        QuizEngine.eligible(mode, QuizEngine.pool(countries, region, difficulty)).size

    fun playable(mode: QuizMode): Boolean = QuizEngine.playable(setupFor(mode), countries)

    private fun weight(): (Country) -> Double =
        if (practiceWeighting) {
            { c -> CountryStatsMath.weight(countryStats[c.alpha2]) }
        } else {
            { 1.0 }
        }

    fun startSession(mode: QuizMode) {
        val setup = setupFor(mode)
        if (!QuizEngine.playable(setup, countries)) return
        val plan = QuizEngine.planSession(setup, countries, weight = weight())
        if (plan.isEmpty()) return
        val s = Session(setup, player)
        s.plan = plan
        s.round = RoundState(setup, QuizEngine.rebuild(setup, countries, plan[0]))
        session = s
        resumable = null
        questionStartedAt = System.currentTimeMillis()
        persistRound()
        resetTo(Screen.Quiz)
    }

    /** Picks up a round the system interrupted. Option tiles are reshuffled. */
    fun resumeSaved() {
        val saved = resumable ?: return
        val setup = saved.setup() ?: run { discardSaved(); return }
        val subjects = QuizEngine.subjectsFor(setup, countries, saved.subjects)
        if (subjects.size != saved.subjects.size || subjects.isEmpty()) { discardSaved(); return }
        val s = Session(setup, saved.player)
        s.roundNumber = saved.roundNumber
        val restoredPlan = saved.plan.map { codes -> QuizEngine.subjectsFor(setup, countries, codes) }
        s.plan = if (restoredPlan.size >= saved.roundNumber) {
            restoredPlan
        } else {
            // Saved by an older version, which kept no plan: rebuild the remaining rounds
            // around the current one so at least those do not repeat it.
            val remaining = setup.rounds - saved.roundNumber
            val later = if (remaining > 0) {
                QuizEngine.planSession(
                    setup.copy(rounds = remaining), countries, weight = weight(),
                    exclude = saved.subjects.toSet(),
                )
            } else emptyList()
            List(saved.roundNumber - 1) { emptyList<Country>() } + listOf(subjects) + later
        }
        s.finished.addAll(saved.finished)
        val r = RoundState(setup, QuizEngine.rebuild(setup, countries, subjects))
        r.index = saved.index.coerceIn(0, subjects.size - 1)
        r.score = saved.score
        r.millis = saved.millis.toInt()
        val byCode = countries.associateBy { it.alpha2 }
        saved.missed.mapNotNull { byCode[it] }.forEach { r.missed.add(it) }
        r.outcomes.addAll(saved.outcomes)
        s.round = r
        session = s
        resumable = null
        questionStartedAt = System.currentTimeMillis()
        resetTo(Screen.Quiz)
    }

    fun discardSaved() {
        resumable = null
        store.saveRound(null)
    }

    fun choose(option: Int) {
        val r = session?.round ?: return
        if (r.answered) return
        r.selected = option
        finishQuestion(r, option == r.current.correctIndex)
    }

    fun tapCountry(alpha2: String) {
        val r = session?.round ?: return
        if (r.answered) return
        r.tapped = alpha2
        finishQuestion(r, alpha2 == r.current.subject.alpha2)
    }

    /** "I don't know": counts as wrong, but is not a guess. */
    fun pass() {
        val r = session?.round ?: return
        if (r.answered) return
        r.passed = true
        finishQuestion(r, false)
    }

    private fun finishQuestion(r: RoundState, correct: Boolean) {
        val ms = elapsed()
        r.millis += ms
        r.outcomes.add(correct)
        if (correct) r.score++ else r.missed.add(r.current.subject)
        val p = session?.player ?: player
        store.recordAnswer(p, r.current.subject.alpha2, correct, ms.toLong())
        if (p == player) countryStats = store.countryStats(player)
        persistRound()
    }

    /** Codes the globe may accept in the current round. */
    fun globeCodes(): Set<String> {
        val setup = session?.setup ?: return emptySet()
        return QuizEngine.pool(countries, setup.region, setup.difficulty)
            .filter { it.mapAvailable }.map { it.alpha2 }.toSet()
    }

    private fun elapsed(): Int {
        val now = System.currentTimeMillis()
        val d = (now - questionStartedAt).coerceIn(0, 120_000)
        questionStartedAt = now
        return d.toInt()
    }

    private fun persistRound() {
        val s = session
        val r = s?.round
        if (s == null || r == null) { store.saveRound(null); return }
        store.saveRound(
            SavedRound(
                player = s.player,
                mode = s.setup.mode.name,
                region = s.setup.region.name,
                difficulty = s.setup.difficulty.name,
                questionsPerRound = s.setup.questionsPerRound ?: 0,
                rounds = s.setup.rounds,
                plan = s.plan.map { round -> round.map { it.alpha2 } },
                roundNumber = s.roundNumber,
                subjects = r.questions.map { it.subject.alpha2 },
                index = r.index,
                score = r.score,
                millis = r.millis.toLong(),
                missed = r.missed.map { it.alpha2 },
                outcomes = r.outcomes.toList(),
                finished = s.finished.toList(),
            ),
        )
    }

    fun next() {
        val s = session ?: return
        val r = s.round ?: return
        if (!r.answered) return
        if (r.index + 1 < r.questions.size) {
            r.index++
            r.selected = null
            r.tapped = null
            r.passed = false
            questionStartedAt = System.currentTimeMillis()
            persistRound()
        } else {
            val record = RoundRecord(
                player = s.player,
                epochMillis = System.currentTimeMillis(),
                mode = s.setup.mode.name,
                region = s.setup.region.name,
                difficulty = s.setup.difficulty.name,
                questions = r.questions.size,
                correct = r.score,
                millis = r.millis.toLong(),
            )
            store.addRecord(record)
            records = store.records()
            s.finished.add(record)
            store.saveRound(null)
            resetTo(Screen.RoundSummary)
        }
    }

    fun nextRound() {
        val s = session ?: return
        if (s.roundNumber >= s.totalRounds) { resetTo(Screen.SessionSummary); return }
        s.roundNumber++
        val subjects = s.plan.getOrNull(s.roundNumber - 1)
        if (subjects.isNullOrEmpty()) { resetTo(Screen.SessionSummary); return }
        s.round = RoundState(s.setup, QuizEngine.rebuild(s.setup, countries, subjects))
        questionStartedAt = System.currentTimeMillis()
        persistRound()
        resetTo(Screen.Quiz)
    }

    fun isLastRound(): Boolean = (session?.let { it.roundNumber >= it.totalRounds }) ?: true

    /** Questions per round as the session will actually run them, for the Start button. */
    fun roundSizes(mode: QuizMode): List<Int> =
        QuizEngine.roundSizes(poolSize(mode), questionsPerRound.takeIf { it > 0 }, rounds)

    fun replaySession() { session?.let { startSession(it.setup.mode) } }

    fun quitSession() {
        session = null
        store.saveRound(null)
        home()
    }

    fun country(alpha2: String): Country = countries.first { it.alpha2 == alpha2 }

    fun isMapRound(): Boolean = session?.setup?.mode?.answering == Answering.MAP
}

class AppViewModelFactory(
    private val countries: List<Country>,
    private val map: MapData,
    private val store: Store,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(countries, map, store) as T
}
