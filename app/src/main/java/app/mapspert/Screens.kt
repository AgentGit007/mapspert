package app.mapspert

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mapspert.quiz.Country
import app.mapspert.quiz.Facts
import app.mapspert.quiz.Option
import app.mapspert.quiz.Prompt
import app.mapspert.quiz.QuizMode
import app.mapspert.quiz.Scope

@Composable
fun MapspertApp(vm: AppViewModel) {
    BackHandler(enabled = vm.screen != Screen.Home) { vm.back() }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (val s = vm.screen) {
                Screen.Home -> HomeScreen(vm)
                Screen.Quiz -> vm.session?.let { QuizScreen(vm, it) }
                Screen.Results -> vm.session?.let { ResultsScreen(vm, it) }
                Screen.Browse -> BrowseScreen(vm)
                is Screen.Detail -> DetailScreen(vm, vm.country(s.alpha2))
                Screen.About -> AboutScreen(vm)
            }
        }
    }
}

// ---- shared building blocks ---------------------------------------------------------------

@Composable
private fun RetroStripes() {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(cs.primaryContainer, cs.secondaryContainer, cs.tertiaryContainer, cs.surfaceVariant).forEach {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(it))
        }
    }
}

@Composable
private fun Tile(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    var m = modifier
        .clip(shape)
        .background(color)
        .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(16.dp), content = content)
}

@Composable
private fun Mono(text: String, modifier: Modifier = Modifier, alpha: Float = 1f) {
    Text(
        text,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
    )
}

@Composable
private fun FlagImage(key: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val id = remember(key) { ctx.resources.getIdentifier("flag_$key", "drawable", ctx.packageName) }
    if (id != 0) {
        val shape = RoundedCornerShape(8.dp)
        Image(
            painter = painterResource(id),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape),
        )
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FactBlock(c: Country) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(c.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(Facts.statusLabel(c), style = MaterialTheme.typography.bodyMedium)
        Text(Facts.isoLine(c), style = MaterialTheme.typography.bodyMedium)
        c.isoNote?.let { Text("ISO note: $it", style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(4.dp))
        Facts.rows(c).forEach { (k, v) ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    k,
                    modifier = Modifier.width(112.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Mono(v)
            }
        }
    }
}

// ---- home ---------------------------------------------------------------------------------

@Composable
private fun HomeScreen(vm: AppViewModel) {
    val cs = MaterialTheme.colorScheme
    val accents = listOf(cs.primaryContainer, cs.secondaryContainer, cs.tertiaryContainer, cs.surfaceVariant)
    val onAccents = listOf(cs.onPrimaryContainer, cs.onSecondaryContainer, cs.onTertiaryContainer, cs.onSurfaceVariant)
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RetroStripes()
            Spacer(Modifier.height(16.dp))
            Text(
                "MAPSPERT",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            )
            Mono("flags / capitals / codes", alpha = 0.7f)
        }
        item {
            Text("Countries", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Scope.values().forEach { sc ->
                    FilterChip(
                        selected = vm.scope == sc,
                        onClick = { vm.scope = sc },
                        label = { Text(sc.label) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            Text("Questions per round", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10 to "10", 20 to "20", 0 to "All").forEach { (n, label) ->
                    FilterChip(
                        selected = vm.length == n,
                        onClick = { vm.length = n },
                        label = { Text(label) },
                    )
                }
            }
        }
        itemsIndexed(QuizMode.values().toList()) { i, mode ->
            val best = vm.bestFor(mode)
            Tile(
                modifier = Modifier.fillMaxWidth(),
                color = accents[i % accents.size],
                onClick = { vm.startQuiz(mode) },
            ) {
                val ink = onAccents[i % onAccents.size]
                Text(mode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ink)
                Text(mode.blurb, style = MaterialTheme.typography.bodyMedium, color = ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${vm.eligibleCount(mode)} entries" + if (best >= 0) "  |  best $best%" else "",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.navigate(Screen.Browse) }) { Text("Browse facts") }
                TextButton(onClick = { vm.navigate(Screen.About) }) { Text("About and licenses") }
            }
        }
    }
}

// ---- quiz ---------------------------------------------------------------------------------

@Composable
private fun QuizScreen(vm: AppViewModel, s: Session) {
    val cs = MaterialTheme.colorScheme
    val q = s.current
    val answered = s.selected != null
    val correct = answered && s.selected == q.correctIndex
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.back() }) { Text("Quit") }
            Spacer(Modifier.weight(1f))
            Mono("${s.index + 1} / ${s.questions.size}   score ${s.score}")
        }
        LinearProgressIndicator(
            progress = { (s.index + if (answered) 1 else 0).toFloat() / s.questions.size },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = cs.primary,
            trackColor = cs.surface,
        )
        Tile(Modifier.fillMaxWidth()) {
            Text(q.prompt.caption, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            when (val p = q.prompt) {
                is Prompt.Flag -> FlagImage(p.flag, Modifier.fillMaxWidth().height(180.dp))
                is Prompt.Text -> if (p.text.length <= 4 || p.text.startsWith(".")) {
                    Text(p.text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                } else {
                    Text(p.text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        val flagOptions = q.options.all { it.flag != null }
        if (flagOptions) {
            q.options.chunked(2).forEachIndexed { row, pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEachIndexed { col, opt ->
                        val i = row * 2 + col
                        OptionTile(Modifier.weight(1f), opt, optionColor(i, q.correctIndex, s.selected)) { vm.choose(i) }
                    }
                }
            }
        } else {
            q.options.forEachIndexed { i, opt ->
                OptionTile(Modifier.fillMaxWidth(), opt, optionColor(i, q.correctIndex, s.selected)) { vm.choose(i) }
            }
        }
        if (answered) {
            Tile(Modifier.fillMaxWidth(), color = if (correct) cs.primaryContainer else cs.errorContainer) {
                Text(
                    if (correct) "Correct" else "Not quite",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (correct) cs.onPrimaryContainer else cs.onErrorContainer,
                )
            }
            Tile(Modifier.fillMaxWidth()) { FactBlock(q.subject) }
            Button(onClick = { vm.next() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (s.index + 1 < s.questions.size) "Next" else "Finish")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun optionColor(i: Int, correctIndex: Int, selected: Int?): Color {
    val cs = MaterialTheme.colorScheme
    return when {
        selected == null -> cs.surface
        i == correctIndex -> cs.primaryContainer
        i == selected -> cs.errorContainer
        else -> cs.surface
    }
}

@Composable
private fun OptionTile(modifier: Modifier, opt: Option, color: Color, onClick: () -> Unit) {
    Tile(modifier, color = color, onClick = onClick) {
        if (opt.flag != null) {
            FlagImage(opt.flag, Modifier.fillMaxWidth().height(84.dp))
        } else {
            val label = opt.label.orEmpty()
            val mono = label.length <= 3 || label.startsWith(".")
            Text(
                label,
                fontFamily = if (mono) FontFamily.Monospace else null,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

// ---- results ------------------------------------------------------------------------------

@Composable
private fun ResultsScreen(vm: AppViewModel, s: Session) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            RetroStripes()
            Spacer(Modifier.height(16.dp))
            Text("${s.percent}%", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            Text("${s.score} of ${s.questions.size} correct  |  ${s.mode.title}", style = MaterialTheme.typography.bodyLarge)
            if (s.previousBest < 0 || s.percent > s.previousBest) {
                Mono("new best")
            } else {
                Mono("best ${s.previousBest}%", alpha = 0.7f)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.playAgain() }) { Text("Play again") }
                OutlinedButton(onClick = { vm.back() }) { Text("Home") }
            }
        }
        if (s.missed.isNotEmpty()) {
            item { Text("To review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(s.missed.toList()) { c ->
                Tile(Modifier.fillMaxWidth(), onClick = { vm.navigate(Screen.Detail(c.alpha2)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlagImage(c.flag, Modifier.width(56.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(c.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// ---- browse and detail --------------------------------------------------------------------

@Composable
private fun BrowseScreen(vm: AppViewModel) {
    val q = vm.search.trim()
    val list = remember(q) {
        vm.countries.filter {
            q.isEmpty() ||
                it.name.contains(q, ignoreCase = true) ||
                it.alpha2.equals(q, ignoreCase = true) ||
                it.alpha3.equals(q, ignoreCase = true) ||
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
            label = { Text("Name, capital, code or domain") },
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
                            Mono(listOfNotNull(c.alpha2, c.alpha3.takeIf { it.isNotEmpty() }, c.tld).joinToString("  "), alpha = 0.7f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(vm: AppViewModel, c: Country) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TopBar(c.name) { vm.back() }
        Spacer(Modifier.height(8.dp))
        FlagImage(c.flag, Modifier.fillMaxWidth().height(200.dp))
        Spacer(Modifier.height(16.dp))
        Tile(Modifier.fillMaxWidth()) { FactBlock(c) }
        Spacer(Modifier.height(20.dp))
    }
}

// ---- about --------------------------------------------------------------------------------

@Composable
private fun AboutScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TopBar("About") { vm.back() }
        Spacer(Modifier.height(8.dp))
        Text(
            "Mapspert is free software (GPL-3.0-or-later). It works fully offline, has no ads, " +
                "no tracking and no accounts. Progress is stored only on this device.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Country list and codes follow ISO 3166-1. Capitals and domains come from Wikidata. " +
                "Status labels (independent or not, disputes) repeat what ISO 3166-1 lists and are " +
                "not a political statement. Some capitals and domains were chosen or corrected by hand.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Text("Third-party material", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LicenseBlock("Flags: flag-icons (MIT)", "MIT-flag-icons.txt")
        Spacer(Modifier.height(8.dp))
        LicenseBlock("Font: Space Grotesk (SIL OFL 1.1)", "OFL-SpaceGrotesk.txt")
        Spacer(Modifier.height(20.dp))
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
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (open) {
            Spacer(Modifier.height(8.dp))
            Text(text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
    }
}
