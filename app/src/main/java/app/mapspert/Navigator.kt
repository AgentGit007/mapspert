package app.mapspert

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.mapspert.quiz.Category
import app.mapspert.quiz.QuizMode

sealed interface Screen {
    data object Home : Screen
    data class CategoryHub(val category: Category) : Screen
    data class ModeSetup(val mode: QuizMode) : Screen
    data object Quiz : Screen
    data object RoundSummary : Screen
    data object SessionSummary : Screen
    data object Stats : Screen
    data object KnowledgeGlobe : Screen
    data object Settings : Screen
    data object Browse : Screen
    data class Detail(val alpha2: String) : Screen
    data object About : Screen
}

/** A plain back stack. Split out so the ViewModel is not also a router. */
class Navigator {
    var screen by mutableStateOf<Screen>(Screen.Home)
        private set

    private val stack = ArrayList<Screen>()

    fun to(target: Screen) {
        stack.add(screen)
        screen = target
    }

    fun back() {
        screen = if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) else Screen.Home
    }

    /** Clears the stack and lands on [target], so Back from there goes Home. */
    fun resetTo(target: Screen) {
        stack.clear()
        stack.add(Screen.Home)
        screen = target
    }

    fun home() {
        stack.clear()
        screen = Screen.Home
    }
}
