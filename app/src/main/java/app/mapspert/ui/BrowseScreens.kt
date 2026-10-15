package app.mapspert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import app.mapspert.R
import app.mapspert.PtMono
import app.mapspert.AppViewModel
import app.mapspert.Screen
import app.mapspert.quiz.Country

@Composable
fun BrowseScreen(vm: AppViewModel) {
    val q = vm.search.trim()
    val list = remember(q) {
        vm.countries.filter {
            q.isEmpty() ||
                it.name.contains(q, ignoreCase = true) ||
                it.alpha2.equals(q, ignoreCase = true) ||
                it.alpha3.equals(q, ignoreCase = true) ||
                it.continent.contains(q, ignoreCase = true) ||
                it.capitals.any { c -> c.contains(q, ignoreCase = true) } ||
                it.acceptedTlds.any { t -> t.equals(q, ignoreCase = true) || t.equals(".$q", ignoreCase = true) }
        }.sortedBy { it.name }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader(stringResource(R.string.ui_browse), onBack = { vm.back() })
        OutlinedTextField(
            value = vm.search,
            onValueChange = { vm.search = it },
            singleLine = true,
            label = { Text(stringResource(R.string.ui_name_capital_code_domain_or_continent)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(list, key = { it.alpha2 }) { c ->
                Card(Modifier.fillMaxWidth(), onClick = { vm.navigate(Screen.Detail(c.alpha2)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlagImage(c.flag, Modifier.width(56.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleMedium)
                            Mono(
                                listOfNotNull(c.alpha2, c.alpha3.takeIf { it.isNotEmpty() }, c.tld).joinToString("  "),
                                alpha = 0.7f,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(vm: AppViewModel, c: Country) {
    val cs = MaterialTheme.colorScheme
    val shape = remember(c.alpha2) { vm.map[c.alpha2] }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        ScreenHeader(c.name, onBack = { vm.back() })
        Spacer(Modifier.height(8.dp))
        FlagImage(c.flag, Modifier.fillMaxWidth().height(180.dp), countryName = c.name)
        if (shape != null) {
            Spacer(Modifier.height(14.dp))
            CountryGlobe(vm, c.alpha2, shape.centerLon, shape.centerLat)
        }
        Spacer(Modifier.height(16.dp))
        FactCard(c, Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
    }
}

/** Where in the world this country sits, highlighted and ready to be spun. */
@Composable
private fun CountryGlobe(vm: AppViewModel, alpha2: String, lon: Float, lat: Float) {
    val cs = MaterialTheme.colorScheme
    val shapes = vm.map.shapes
    val all = remember { shapes.map { it.alpha2 }.toSet() }
    val state = rememberGlobeState(lon, lat.coerceIn(-70f, 70f), 1.6f, key = alpha2)
    val highlight = cs.primary
    val density = LocalDensity.current
    val dotMinPx = with(density) { DOT_THRESHOLD_DP.dp.toPx() }
    val dotRadiusPx = with(density) { 6.dp.toPx() }
    val shape = remember(alpha2) { vm.map[alpha2] }

    Box(
        Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(MaterialTheme.shapes.large)
            .background(cs.surface),
    ) {
        GlobeView(
            state = state,
            shapes = shapes,
            pool = all,
            colors = kosmosGlobeColors(land = globeMutedLand(), muted = globeMutedLand()),
            modifier = Modifier.fillMaxSize(),
            overlay = { cam ->
                shape?.let { drawHighlight(cam, it, highlight, dotMinPx, dotRadiusPx) }
            },
        )
    }
}

@Composable
fun AboutScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        ScreenHeader(stringResource(R.string.ui_about), onBack = { vm.back() })
        Spacer(Modifier.height(4.dp))
        RetroRule()
        Spacer(Modifier.height(18.dp))

        Text(stringResource(R.string.ui_mapspert),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.ui_an_offline_geography_quiz_spin_a_globe_and_tap) +
                "for, or practise flags, capitals, ISO 3166-1 codes and country domains.",
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.ui_privacy))
        Card(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ui_mapspert_never_connects_to_the_internet_it_doe) +
                    "permission at all, so that is a property of the app rather than a setting " +
                    "you have to trust. There are no ads, no analytics and no accounts, and your " +
                    "scores stay on this device.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.ui_how_a_round_works))
        Card(Modifier.fillMaxWidth()) {
            AboutRow("Region", "World, Europe, the EU, or a single continent.")
            AboutRow("Difficulty", "Easy is the 193 UN members with three options. Medium adds the two observer states. Hard covers all 250 entries including territories, which is where the small islands live.")
            AboutRow("Length", "Pick how many questions a round holds and how many rounds a session runs.")
            AboutRow("Statistics", "Each finished round records its score and your answering time, so you can see whether you are getting faster as well as more accurate.")
            AboutRow("Players", "Set a name in Settings before handing the phone over, and a friend's round stays out of your own numbers.")
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.ui_where_the_data_comes_from))
        Card(Modifier.fillMaxWidth()) {
            AboutRow("Countries", "The list follows ISO 3166-1: 249 officially assigned two-letter codes, plus Kosovo under the user-assigned code XK.")
            AboutRow("Facts", "Names, capitals, codes and domains come from Wikidata. Some capitals and a few domains were chosen by hand where the source was ambiguous.")
            AboutRow("Outlines", "Country outlines come from Natural Earth, simplified for speed. Borders in disputed areas follow Natural Earth's own rendering.")
            AboutRow(
                "Labels",
                "Status labels such as \"independent\" or \"not independent\", and the notes on " +
                    "disputed entries, repeat what ISO 3166-1 lists. They are not a political " +
                    "statement by this app.",
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.ui_corrections))
        Card(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ui_found_a_wrong_capital_a_stale_domain_or_a_coun) +
                    "Corrections are welcome on the project page. The data files and the scripts " +
                    "that generate them are part of the source, and the decisions made by hand " +
                    "are listed there.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.ui_licence_and_credits))
        Card(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ui_mapspert_is_free_software_under_the_gnu_gpl_ve) +
                    "study, share and modify it; versions you pass on must stay free too.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(10.dp))
        LicenseBlock("Flags: flag-icons (MIT)", "MIT-flag-icons.txt")
        Spacer(Modifier.height(8.dp))
        LicenseBlock("Font: Unbounded (SIL OFL 1.1)", "OFL-Unbounded.txt")
        Spacer(Modifier.height(8.dp))
        LicenseBlock("Font: PT Mono (SIL OFL 1.1)", "OFL-PTMono.txt")
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ui_map_natural_earth_public_domain), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Mono(stringResource(R.string.ui_made_with_natural_earth_free_vector_and_raster), alpha = 0.75f)
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ui_app_icon), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Mono(stringResource(R.string.ui_drawn_for_this_project_same_licence_as_the_app), alpha = 0.75f)
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun AboutRow(label: String, text: String) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun LicenseBlock(title: String, asset: String) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val text = remember(asset) {
        runCatching { ctx.assets.open("licenses/$asset").bufferedReader().use { it.readText() } }
            .getOrDefault("License text missing from this build.")
    }
    Card(Modifier.fillMaxWidth(), onClick = { open = !open }) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (open) {
            Spacer(Modifier.height(8.dp))
            Text(text, fontFamily = PtMono, style = MaterialTheme.typography.bodySmall)
        }
    }
}
