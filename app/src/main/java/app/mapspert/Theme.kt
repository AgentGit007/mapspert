package app.mapspert

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mapspert.quiz.Category

// ---- "Kosmos pastel" -------------------------------------------------------------------
// Soviet space-age posters crossed with the faded pastels of post-Soviet panel blocks.
// Both fonts are bundled under the SIL OFL 1.1 and never fetched from the network.

/** Wide geometric display face for titles, names and buttons. */
val Unbounded = FontFamily(
    Font(R.font.unbounded_regular, FontWeight.Normal),
    Font(R.font.unbounded_medium, FontWeight.Medium),
    Font(R.font.unbounded_bold, FontWeight.Bold),
    Font(R.font.unbounded_extrabold, FontWeight.ExtraBold),
)

/** Typewriter-style mono for labels, codes and readouts ("telemetry"). */
val PtMono = FontFamily(
    Font(R.font.ptmono_regular, FontWeight.Normal),
)

// Fixed palette. The pastels are the same in light and dark mode: like printed stamps,
// they sit on either ground and always carry dark ink.
object Kosmos {
    val Night = Color(0xFF1D2230)
    val Panel = Color(0xFF272D40)
    val PanelBorder = Color(0xFF3A4260)
    val Paper = Color(0xFFEEE5D3)
    val PaperPanel = Color(0xFFE4D9C2)
    val PaperBorder = Color(0xFFCFC3A8)
    val CardPaper = Color(0xFFF8F3E8)
    val Ink = Color(0xFF1D2230)
    val InkSoft = Color(0xFF4A4F63)

    val Blue = Color(0xFF8DB3DC)
    val Mint = Color(0xFF9FD3BF)
    val Coral = Color(0xFFEC8A78)
    val Ochre = Color(0xFFE8C26A)
    val Lilac = Color(0xFFCDA8D8)

    /** Darker "lip" under each pastel, for the pressable panel buttons. */
    val MintLip = Color(0xFF5E9C86)
    val CoralLip = Color(0xFFA45648)

    /** "Right answer" on the globe: a signal green that stands apart from mint land. */
    val Signal = Color(0xFF2FA36F)

    /** Rubber-stamp inks, dark enough to pass contrast on paper. */
    val StampGreen = Color(0xFF256B57)
    val StampRed = Color(0xFFA8435A)
}

private fun scaled(base: TextStyle, family: FontFamily, size: Float, line: Float, spacing: Float = 0f) =
    base.copy(fontFamily = family, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

/**
 * Unbounded is much wider than Material's default, so every size steps down a notch.
 * Small print and labels use PT Mono.
 */
private fun kosmosTypography(): Typography {
    val t = Typography()
    return t.copy(
        displayLarge = scaled(t.displayLarge, Unbounded, 52f, 60f),
        displayMedium = scaled(t.displayMedium, Unbounded, 40f, 48f),
        displaySmall = scaled(t.displaySmall, Unbounded, 32f, 40f),
        headlineLarge = scaled(t.headlineLarge, Unbounded, 28f, 34f, 0.4f),
        headlineMedium = scaled(t.headlineMedium, Unbounded, 23f, 30f),
        headlineSmall = scaled(t.headlineSmall, Unbounded, 20f, 27f),
        titleLarge = scaled(t.titleLarge, Unbounded, 18f, 25f),
        titleMedium = scaled(t.titleMedium, Unbounded, 15f, 22f),
        titleSmall = scaled(t.titleSmall, Unbounded, 13.5f, 20f),
        bodyLarge = scaled(t.bodyLarge, Unbounded, 14.5f, 22f),
        bodyMedium = scaled(t.bodyMedium, Unbounded, 12.5f, 19f),
        bodySmall = scaled(t.bodySmall, PtMono, 12.5f, 18f),
        labelLarge = scaled(t.labelLarge, Unbounded, 13.5f, 18f, 0.6f),
        labelMedium = scaled(t.labelMedium, PtMono, 12.5f, 17f, 0.8f),
        labelSmall = scaled(t.labelSmall, PtMono, 11f, 15f, 1f),
    )
}

private val DarkColors = darkColorScheme(
    primary = Kosmos.Mint, onPrimary = Kosmos.Ink,
    primaryContainer = Kosmos.Mint, onPrimaryContainer = Kosmos.Ink,
    secondary = Kosmos.Blue, onSecondary = Kosmos.Ink,
    secondaryContainer = Kosmos.Paper, onSecondaryContainer = Kosmos.Ink,
    tertiary = Kosmos.Coral, onTertiary = Kosmos.Ink,
    tertiaryContainer = Kosmos.Coral, onTertiaryContainer = Kosmos.Ink,
    background = Kosmos.Night, onBackground = Kosmos.Paper,
    surface = Kosmos.Panel, onSurface = Kosmos.Paper,
    surfaceVariant = Color(0xFF30374D), onSurfaceVariant = Color(0xFFA9AEC4),
    outline = Kosmos.Paper, outlineVariant = Kosmos.PanelBorder,
    error = Color(0xFFE36B7D), onError = Kosmos.Ink,
    errorContainer = Kosmos.StampRed, onErrorContainer = Color(0xFFFBEDEA),
)

private val LightColors = lightColorScheme(
    // Mint is too pale for lines and filled buttons on paper, so light mode uses the
    // deeper instrument green and keeps mint for containers.
    primary = Color(0xFF276E5C), onPrimary = Kosmos.CardPaper,
    primaryContainer = Kosmos.Mint, onPrimaryContainer = Kosmos.Ink,
    secondary = Color(0xFF3F6A9C), onSecondary = Kosmos.CardPaper,
    secondaryContainer = Kosmos.CardPaper, onSecondaryContainer = Kosmos.Ink,
    tertiary = Color(0xFFB4503E), onTertiary = Kosmos.CardPaper,
    tertiaryContainer = Kosmos.Coral, onTertiaryContainer = Kosmos.Ink,
    background = Kosmos.Paper, onBackground = Kosmos.Ink,
    surface = Kosmos.PaperPanel, onSurface = Kosmos.Ink,
    surfaceVariant = Color(0xFFD9CDB3), onSurfaceVariant = Kosmos.InkSoft,
    outline = Kosmos.Ink, outlineVariant = Kosmos.PaperBorder,
    error = Color(0xFFB23A50), onError = Kosmos.CardPaper,
    errorContainer = Kosmos.StampRed, onErrorContainer = Color(0xFFFBEDEA),
)

/** The theme's own dark flag, so colours outside the Material scheme can follow it. */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/** "Learning" on the mastery globe and in statistics: ochre. Name kept from the old palette. */
@Composable
fun butterColor(): Color = if (LocalIsDarkTheme.current) Kosmos.Ochre else Color(0xFFD9A93C)

@Composable
fun onButterColor(): Color = Kosmos.Ink

/** Hairline border for panels and icon buttons. */
@Composable
fun panelBorder(): Color = if (LocalIsDarkTheme.current) Kosmos.PanelBorder else Kosmos.PaperBorder

/** Paper for tickets and archive cards: warm cream on night, a lighter sheet on paper. */
@Composable
fun cardPaper(): Color = if (LocalIsDarkTheme.current) Kosmos.Paper else Kosmos.CardPaper

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

/** One pastel per category, the same in both modes. */
@Composable
fun categoryColor(c: Category): Color = when (c) {
    Category.GLOBE -> Kosmos.Blue
    Category.FLAGS -> Kosmos.Mint
    Category.PLACES -> Kosmos.Coral
    Category.CODES -> Kosmos.Ochre
    Category.OTHER -> Kosmos.Lilac
}

@Composable
fun onCategoryColor(@Suppress("UNUSED_PARAMETER") c: Category): Color = Kosmos.Ink

@Composable
fun MapspertTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalIsDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = kosmosTypography(),
            shapes = AppShapes,
            content = content,
        )
    }
}
