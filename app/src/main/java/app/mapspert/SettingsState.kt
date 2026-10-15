package app.mapspert

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.mapspert.quiz.Difficulty

/**
 * The stored preferences, mirrored into Compose state.
 *
 * Methods are named choose*: a Kotlin property already generates setDifficulty,
 * setRounds and so on for the JVM, and a function of the same name clashes with it.
 */
class SettingsState(private val store: Store) {
    var theme by mutableStateOf(store.themeMode)
        private set
    var difficulty by mutableStateOf(store.difficulty)
        private set
    var questionsPerRound by mutableIntStateOf(store.questionsPerRound)
        private set
    var rounds by mutableIntStateOf(store.rounds)
        private set
    var player by mutableStateOf(store.player)
        private set
    var practiceWeighting by mutableStateOf(store.practiceWeighting)
        private set
    var keepScreenOn by mutableStateOf(store.keepScreenOn)
        private set

    fun chooseTheme(v: ThemeMode) { theme = v; store.themeMode = v }
    fun chooseDifficulty(v: Difficulty) { difficulty = v; store.difficulty = v }
    fun chooseQuestionsPerRound(v: Int) { questionsPerRound = v; store.questionsPerRound = v }
    fun chooseRounds(v: Int) { rounds = v; store.rounds = v }
    fun choosePracticeWeighting(v: Boolean) { practiceWeighting = v; store.practiceWeighting = v }
    fun chooseKeepScreenOn(v: Boolean) { keepScreenOn = v; store.keepScreenOn = v }
    fun choosePlayer(name: String) { player = name.ifBlank { "Me" }; store.player = player }

    /** Re-reads everything, after an import replaced the stored values. */
    fun reload() {
        theme = store.themeMode
        difficulty = store.difficulty
        questionsPerRound = store.questionsPerRound
        rounds = store.rounds
        player = store.player
        practiceWeighting = store.practiceWeighting
        keepScreenOn = store.keepScreenOn
    }
}
