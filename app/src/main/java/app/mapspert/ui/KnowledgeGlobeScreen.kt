package app.mapspert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.mapspert.R
import app.mapspert.AppViewModel
import app.mapspert.Screen
import app.mapspert.butterColor
import app.mapspert.quiz.CountryStat
import app.mapspert.quiz.CountryStatsMath
import app.mapspert.quiz.GlobeMath
import app.mapspert.quiz.Mastery
import app.mapspert.quiz.Region

/** The whole world, coloured by how well this player knows each country. */
@Composable
fun KnowledgeGlobeScreen(vm: AppViewModel, playerName: String) {
    val cs = MaterialTheme.colorScheme
    val stats = remember(playerName, vm.countryStats) { vm.statsFor(playerName) }
    val shapes = vm.map.shapes
    val all = remember { shapes.map { it.alpha2 }.toSet() }
    val start = remember { GlobeMath.frame(shapes, all) }
    val state = rememberGlobeState(start[0], start[1], 1f)
    var selected by remember { mutableStateOf<String?>(null) }

    val solid = cs.primary
    val learning = butterColor()
    val shaky = cs.error
    val unseen = globeUnseenLand()

    fun colorOf(code: String): Color = when (CountryStatsMath.mastery(stats[code])) {
        Mastery.SOLID -> solid
        Mastery.LEARNING -> learning
        Mastery.SHAKY -> shaky
        Mastery.UNSEEN -> unseen
    }

    val counts = remember(stats) {
        val m = CountryStatsMath.summarize(
            shapes.map { s -> stats[s.alpha2] ?: CountryStat(s.alpha2, 0, 0, 0) },
        )
        m
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader(stringResource(R.string.ui_knowledge_globe), onBack = { vm.back() })
        Mono("how well $playerName knows each country", alpha = 0.55f)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().weight(1f).clip(MaterialTheme.shapes.large).background(cs.surface)) {
            GlobeView(
                state = state,
                shapes = shapes,
                pool = all,
                colors = kosmosGlobeColors(land = unseen, muted = unseen),
                modifier = Modifier.fillMaxSize(),
                fillFor = { colorOf(it.alpha2) },
                onTap = { hit, _, _ -> selected = hit },
            )
        }
        Spacer(Modifier.height(12.dp))
        val code = selected
        if (code != null) {
            val c = runCatching { vm.country(code) }.getOrNull()
            val s = stats[code]
            Card(Modifier.fillMaxWidth(), onClick = { c?.let { vm.navigate(Screen.Detail(it.alpha2)) } }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (c != null) {
                        FlagImage(c.flag, Modifier.width(44.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            c?.name ?: code,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Mono(
                            if (s == null || s.seen == 0) "not asked yet"
                            else "${s.correct}/${s.seen} right  -  " +
                                formatSeconds(s.secondsPerQuestion) + "/q",
                            alpha = 0.7f,
                        )
                    }
                    Dot(colorOf(code))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Legend(solid, "Solid", counts[Mastery.SOLID] ?: 0)
            Legend(learning, "Learning", counts[Mastery.LEARNING] ?: 0)
            Legend(shaky, "Shaky", counts[Mastery.SHAKY] ?: 0)
            Legend(unseen, "Unseen", counts[Mastery.UNSEEN] ?: 0)
        }
        Spacer(Modifier.height(8.dp))
        Mono(stringResource(R.string.ui_tap_a_country_for_its_record), alpha = 0.45f)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Legend(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color)
        Spacer(Modifier.width(6.dp))
        Column {
            Text("$count", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Mono(label.lowercase(), alpha = 0.55f)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(14.dp).clip(CircleShape).background(color))
}
