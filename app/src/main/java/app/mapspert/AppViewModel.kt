package app.mapspert

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.mapspert.quiz.Answering
import app.mapspert.quiz.Category
import app.mapspert.quiz.Country
import app.mapspert.quiz.Difficulty
import app.mapspert.quiz.MapData
import app.mapspert.quiz.Question
import app.mapspert.quiz.QuizEngine
import app.mapspert.quiz.QuizMode
import app.mapspert.quiz.Region
import app.mapspert.quiz.RoundRecord
import app.mapspert.quiz.Setup

sealed interface Screen {
    data object Home : Screen
    data class CategoryHub(val category: Category) : Screen
    data class ModeSetup(val mode: QuizMode) : Screen
    data object Quiz : Screen
    data object RoundSummary : Screen
    data object SessionSummary : Screen
    data object Stats : Screen
    data object Settings : Screen
    data object Browse : Screen
    data class Detail(val alpha2: String) : Screen
    data object About : Screen
}

class RoundState(val setup: Setup, val questions: List<Question>) {
    var index by mutableIntStateOf(0)
    /** Index of the chosen option, or for map rounds the tapped country code. */
    var selected by mutableStateOf<Int?>(null)
    var tapped by mutableStateOf<String?>(null)
    var score by mutableIntStateOf(0)
    var millis by mutableIntStateOf(0)
    val missed = mutableStateListOf<Country>()
    val current: Question get() = questions[index]
    val answered: Boolean get() = selected != null || tapped != null
    val percent: Int get() = if (questions.isEmpty()) 0 else score * 100 / questions.size
}

class Session(val setup: Setup, val player: String) {
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

    var screen by mutableStateOf<Screen>(Screen.Home)
        private set
    private val stack = ArrayList<Screen>()

    // settings, mirrored into state so the UI recomposes
    var themeMode by mutableStateOf(store.themeMode)
        private set
    var difficulty by mutableStateOf(store.difficulty)
        private set
    var questionsPerRound by mutableIntStateOf(store.questionsPerRound)
        private set
    var rounds by mutableIntStateOf(store.rounds)
        private set
    var player by mutableStateOf(store.player)
        private set

    // per-run choices
    var region by mutableStateOf(Region.WORLD)
    var search by mutableStateOf("")
    var session by mutableStateOf<Session?>(null)
        private set
    var records by mutableStateOf(store.records())
        private set
    private var questionStartedAt = 0L

    // ---- settings ----

    fun setTheme(m: ThemeMode) { themeMode = m; store.themeMode = m }
    fun chooseDifficulty(d: Difficulty) { difficulty = d; store.difficulty = d }
    fun chooseQuestionsPerRound(n: Int) { questionsPerRound = n; store.questionsPerRound = n }
    fun chooseRounds(n: Int) { rounds = n; store.rounds = n }
    fun choosePlayer(name: String) { player = name.ifBlank { "Me" }; store.player = player }
    fun knownPlayers(): List<String> = store.knownPlayers()
    fun clearRecords(onlyPlayer: String?) { store.clearRecords(onlyPlayer); records = store.records() }

    // ---- navigation ----

    fun navigate(target: Screen) { stack.add(screen); screen = target }

    fun back() { screen = if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) else Screen.Home }

    private fun resetTo(target: Screen) { stack.clear(); stack.add(Screen.Home); screen = target }

    fun home() { stack.clear(); screen = Screen.Home }

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

    fun startSession(mode: QuizMode) {
        val setup = setupFor(mode)
        if (!QuizEngine.playable(setup, countries)) return
        val s = Session(setup, player)
        s.round = RoundState(setup, QuizEngine.newRound(setup, countries))
        session = s
        questionStartedAt = System.currentTimeMillis()
        resetTo(Screen.Quiz)
    }

    fun choose(option: Int) {
        val r = session?.round ?: return
        if (r.answered) return
        r.selected = option
        r.millis += elapsed()
        if (option == r.current.correctIndex) r.score++ else r.missed.add(r.current.subject)
    }

    fun tapCountry(alpha2: String) {
        val r = session?.round ?: return
        if (r.answered) return
        r.tapped = alpha2
        r.millis += elapsed()
        if (alpha2 == r.current.subject.alpha2) r.score++ else r.missed.add(r.current.subject)
    }

    /** Codes the map may accept in the current round. */
    fun mapCodes(): Set<String> {
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

    fun next() {
        val s = session ?: return
        val r = s.round ?: return
        if (!r.answered) return
        if (r.index + 1 < r.questions.size) {
            r.index++
            r.selected = null
            r.tapped = null
            questionStartedAt = System.currentTimeMillis()
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
            resetTo(Screen.RoundSummary)
        }
    }

    fun nextRound() {
        val s = session ?: return
        if (s.roundNumber >= s.setup.rounds) { resetTo(Screen.SessionSummary); return }
        s.roundNumber++
        s.round = RoundState(s.setup, QuizEngine.newRound(s.setup, countries))
        questionStartedAt = System.currentTimeMillis()
        resetTo(Screen.Quiz)
    }

    fun isLastRound(): Boolean = (session?.let { it.roundNumber >= it.setup.rounds }) ?: true

    fun replaySession() { session?.let { startSession(it.setup.mode) } }

    fun quitSession() { session = null; home() }

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
