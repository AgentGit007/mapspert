package app.mapspert

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Space Grotesk, bundled (SIL OFL 1.1) -- never fetched from the network.
val SpaceGrotesk = FontFamily(
    Font(R.font.spacegrotesk_regular, FontWeight.Normal),
    Font(R.font.spacegrotesk_medium, FontWeight.Medium),
    Font(R.font.spacegrotesk_bold, FontWeight.Bold),
)

private fun Typography.withFont(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F7F6B), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9ED8C6), onPrimaryContainer = Color(0xFF12332B),
    secondary = Color(0xFF6A58B0), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB9A8E6), onSecondaryContainer = Color(0xFF241A4A),
    tertiary = Color(0xFFB2563C), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF7B7A3), onTertiaryContainer = Color(0xFF4A1C0E),
    background = Color(0xFFF4EFE6), onBackground = Color(0xFF2B2A33),
    surface = Color(0xFFFFFBF2), onSurface = Color(0xFF2B2A33),
    surfaceVariant = Color(0xFFA8D0F0), onSurfaceVariant = Color(0xFF12293D),
    outline = Color(0xFF2B2A33),
    error = Color(0xFFB3261E), errorContainer = Color(0xFFF7B7A3), onErrorContainer = Color(0xFF4A1C0E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FC4AE), onPrimary = Color(0xFF0B2A22),
    primaryContainer = Color(0xFF2D5E50), onPrimaryContainer = Color(0xFFCFF2E6),
    secondary = Color(0xFFA08FD6), onSecondary = Color(0xFF1D1440),
    secondaryContainer = Color(0xFF473B7A), onSecondaryContainer = Color(0xFFE3DBFF),
    tertiary = Color(0xFFE69F8B), onTertiary = Color(0xFF3F170B),
    tertiaryContainer = Color(0xFF7A3A28), onTertiaryContainer = Color(0xFFFFDCD2),
    background = Color(0xFF1B1A24), onBackground = Color(0xFFECE8F5),
    surface = Color(0xFF262537), onSurface = Color(0xFFECE8F5),
    surfaceVariant = Color(0xFF2F5A7E), onSurfaceVariant = Color(0xFFD6EAFB),
    outline = Color(0xFFECE8F5),
    error = Color(0xFFF2B8B5), errorContainer = Color(0xFF7A3A28), onErrorContainer = Color(0xFFFFDCD2),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun MapspertTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography().withFont(SpaceGrotesk),
        shapes = AppShapes,
        content = content,
    )
}
