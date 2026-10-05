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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
        TopBar("Browse facts") { vm.back() }
        OutlinedTextField(
            value = vm.search,
            onValueChange = { vm.search = it },
            singleLine = true,
            label = { Text("Name, capital, code, domain or continent") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(list, key = { it.alpha2 }) { c ->
                Tile(Modifier.fillMaxWidth(), onClick = { vm.navigate(Screen.Detail(c.alpha2)) }) {
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TopBar(c.name) { vm.back() }
        Spacer(Modifier.height(8.dp))
        FlagImage(c.flag, Modifier.fillMaxWidth().height(200.dp))
        Spacer(Modifier.height(16.dp))
        Tile(Modifier.fillMaxWidth()) { FactBlock(c) }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun AboutScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TopBar("About") { vm.back() }
        Spacer(Modifier.height(8.dp))
        Text(
            "Mapspert is free software (GPL-3.0-or-later). It works fully offline, has no ads, " +
                "no tracking and no accounts. Scores stay on this device.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Country list and codes follow ISO 3166-1. Capitals and domains come from Wikidata. " +
                "Map outlines come from Natural Earth and are simplified for speed; borders in disputed " +
                "areas follow Natural Earth's own rendering. Status labels repeat what ISO 3166-1 lists " +
                "and are not a political statement. Some capitals, domains and continent assignments were " +
                "chosen by hand.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Text("Third-party material", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LicenseBlock("Flags: flag-icons (MIT)", "MIT-flag-icons.txt")
        Spacer(Modifier.height(8.dp))
        LicenseBlock("Font: Space Grotesk (SIL OFL 1.1)", "OFL-SpaceGrotesk.txt")
        Spacer(Modifier.height(8.dp))
        Tile(Modifier.fillMaxWidth()) {
            Text("Map: Natural Earth (public domain)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Made with Natural Earth. Free vector and raster map data at naturalearthdata.com.",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(Modifier.height(24.dp))
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
    Tile(Modifier.fillMaxWidth(), onClick = { open = !open }) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (open) {
            Spacer(Modifier.height(8.dp))
            Text(text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
    }
}
