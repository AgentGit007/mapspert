package app.mapspert

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.mapspert.quiz.Country
import app.mapspert.quiz.Question
import app.mapspert.quiz.QuizEngine
import app.mapspert.quiz.QuizMode
import app.mapspert.quiz.Scope

sealed interface Screen {
    data object Home : Screen
    data object Quiz : Screen
    data object Results : Screen
    data object Browse : Screen
    data class Detail(val alpha2: String) : Screen
    data object About : Screen
}

class Session(
    val mode: QuizMode,
    val scope: Scope,
    val lengthKey: Int,
    val questions: List<Question>,
) {
    var index by mutableIntStateOf(0)
    var selected by mutableStateOf<Int?>(null)
    var score by mutableIntStateOf(0)
    var previousBest by mutableIntStateOf(-1)
    val missed = mutableStateListOf<Country>()
    val current: Question get() = questions[index]
    val percent: Int get() = if (questions.isEmpty()) 0 else score * 100 / questions.size
}

class AppViewModel(val countries: List<Country>, private val stats: Stats) : ViewModel() {
    var screen by mutableStateOf<Screen>(Screen.Home)
        private set
    var scope by mutableStateOf(Scope.UN_AND_OBSERVERS)
    /** 0 means "all eligible countries". */
    var length by mutableIntStateOf(10)
    var session by mutableStateOf<Session?>(null)
        private set
    var search by mutableStateOf("")

    private val stack = ArrayList<Screen>()

    fun eligibleCount(mode: QuizMode): Int =
        QuizEngine.eligible(mode, QuizEngine.pool(countries, scope)).size

    fun bestFor(mode: QuizMode): Int = stats.best(key(mode, scope, length))

    fun country(alpha2: String): Country = countries.first { it.alpha2 == alpha2 }

    fun navigate(target: Screen) {
        stack.add(screen)
        screen = target
    }

    fun back() {
        screen = if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) else Screen.Home
    }

    fun startQuiz(mode: QuizMode) {
        val s = newSession(mode) ?: return
        session = s
        navigate(Screen.Quiz)
    }

    fun playAgain() {
        val old = session ?: return
        val s = newSession(old.mode) ?: return
        session = s
        stack.clear()
        stack.add(Screen.Home)
        screen = Screen.Quiz
    }

    private fun newSession(mode: QuizMode): Session? {
        val qs = QuizEngine.newSession(mode, scope, countries, if (length == 0) null else length)
        return if (qs.isEmpty()) null else Session(mode, scope, length, qs)
    }

    fun choose(option: Int) {
        val s = session ?: return
        if (s.selected != null) return
        s.selected = option
        if (option == s.current.correctIndex) s.score++ else s.missed.add(s.current.subject)
    }

    fun next() {
        val s = session ?: return
        if (s.selected == null) return
        if (s.index + 1 < s.questions.size) {
            s.index++
            s.selected = null
        } else {
            val k = key(s.mode, s.scope, s.lengthKey)
            s.previousBest = stats.best(k)
            stats.record(k, s.percent)
            stack.clear()
            stack.add(Screen.Home)
            screen = Screen.Results
        }
    }

    private fun key(mode: QuizMode, scope: Scope, length: Int) = "${mode.name}_${scope.name}_$length"
}

class AppViewModelFactory(
    private val countries: List<Country>,
    private val stats: Stats,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(countries, stats) as T
}
