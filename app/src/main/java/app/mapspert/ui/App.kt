package app.mapspert.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.mapspert.AppViewModel
import app.mapspert.Screen

@Composable
fun MapspertApp(vm: AppViewModel) {
    BackHandler(enabled = vm.screen != Screen.Home) { vm.back() }
    KeepScreenOn(vm.keepScreenOn && vm.screen == Screen.Quiz)
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Box(Modifier.fillMaxSize()) {
            DotGrid(Modifier.fillMaxSize())
            Crossfade(
                targetState = vm.screen,
                animationSpec = tween(220),
                label = "screen",
                modifier = Modifier.fillMaxSize().padding(inner),
            ) { screen ->
            when (val s = screen) {
                Screen.Home -> HomeScreen(vm)
                is Screen.CategoryHub -> CategoryHubScreen(vm, s.category)
                is Screen.ModeSetup -> ModeSetupScreen(vm, s.mode)
                Screen.Quiz -> vm.session?.let { ses -> ses.round?.let { QuizScreen(vm, ses, it) } }
                Screen.RoundSummary -> vm.session?.let { ses -> ses.round?.let { RoundSummaryScreen(vm, ses, it) } }
                Screen.SessionSummary -> vm.session?.let { SessionSummaryScreen(vm, it) }
                Screen.Stats -> StatsScreen(vm)
                Screen.KnowledgeGlobe -> KnowledgeGlobeScreen(vm, vm.player)
                Screen.Settings -> SettingsScreen(vm)
                Screen.Browse -> BrowseScreen(vm)
                is Screen.Detail -> DetailScreen(vm, vm.country(s.alpha2))
                Screen.About -> AboutScreen(vm)
            }
            }
        }
    }
}
