package app.mapspert.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import app.mapspert.quiz.Country
import app.mapspert.quiz.Facts

@Composable
fun RetroStripes(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(cs.primaryContainer, cs.secondaryContainer, cs.tertiaryContainer, cs.surfaceVariant).forEach {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(it))
        }
    }
}

@Composable
fun Tile(
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
fun Mono(text: String, modifier: Modifier = Modifier, alpha: Float = 1f, color: Color? = null) {
    Text(
        text,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.labelLarge,
        color = (color ?: MaterialTheme.colorScheme.onSurface).copy(alpha = alpha),
    )
}

@Composable
fun FlagImage(key: String, modifier: Modifier = Modifier) {
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
fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FactBlock(c: Country) {
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

fun formatSeconds(seconds: Double): String =
    if (seconds >= 60) "${(seconds / 60).toInt()}m ${(seconds % 60).toInt()}s"
    else String.format("%.1fs", seconds)
