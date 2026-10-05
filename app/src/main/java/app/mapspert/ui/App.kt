package app.mapspert.ui

import androidx.activity.compose.BackHandler
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
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (val s = vm.screen) {
                Screen.Home -> HomeScreen(vm)
                is Screen.CategoryHub -> CategoryHubScreen(vm, s.category)
                is Screen.ModeSetup -> ModeSetupScreen(vm, s.mode)
                Screen.Quiz -> vm.session?.let { ses -> ses.round?.let { QuizScreen(vm, ses, it) } }
                Screen.RoundSummary -> vm.session?.let { ses -> ses.round?.let { RoundSummaryScreen(vm, ses, it) } }
                Screen.SessionSummary -> vm.session?.let { SessionSummaryScreen(vm, it) }
                Screen.Stats -> StatsScreen(vm)
                Screen.Settings -> SettingsScreen(vm)
                Screen.Browse -> BrowseScreen(vm)
                is Screen.Detail -> DetailScreen(vm, vm.country(s.alpha2))
                Screen.About -> AboutScreen(vm)
            }
        }
    }
}
