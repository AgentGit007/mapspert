@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package app.mapspert.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mapspert.Kosmos
import app.mapspert.LocalIsDarkTheme
import app.mapspert.PtMono
import app.mapspert.R
import app.mapspert.Unbounded
import app.mapspert.cardPaper
import app.mapspert.panelBorder
import app.mapspert.quiz.Category
import app.mapspert.quiz.Country
import app.mapspert.quiz.Facts
import app.mapspert.quiz.MapShape
import kotlin.math.max
import kotlin.math.roundToInt

// ---- surfaces ---------------------------------------------------------------------------

/** Rounded panel. Plain panels get a hairline border, like a control-desk inset. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    outlined: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val interaction = remember { MutableInteractionSource() }
    var m = modifier.clip(shape).background(color)
    if (outlined || color == MaterialTheme.colorScheme.surface) m = m.border(1.5.dp, panelBorder(), shape)
    if (onClick != null) {
        m = m.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
    }
    Column(m.padding(18.dp), content = content)
}

/** Typewriter "telemetry" text. [caps] for labels; values keep their own case. */
@Composable
fun Mono(
    text: String,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    color: Color? = null,
    caps: Boolean = false,
) {
    Text(
        if (caps) text.uppercase() else text,
        modifier = modifier,
        fontFamily = PtMono,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = if (caps) 1.6.sp else 0.6.sp,
        color = (color ?: MaterialTheme.colorScheme.onSurface).copy(alpha = alpha),
    )
}

/** "MISSIONS ————" : a mono label running into a rule. */
@Composable
fun SectionLabel(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(),
            fontFamily = PtMono,
            style = MaterialTheme.typography.labelMedium,
            letterSpacing = 2.4.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(panelBorder()))
    }
    Spacer(Modifier.height(10.dp))
}

/** Flat selector; the whole set reads as one control. */
@Composable
fun Pill(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val bg by animateColorAsState(if (selected) cs.primary else cs.surface, tween(160), label = "pillBg")
    val fg by animateColorAsState(
        if (selected) cs.onPrimary else cs.onSurface.copy(alpha = 0.85f),
        tween(160),
        label = "pillFg",
    )
    Box(
        modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(bg)
            .border(1.5.dp, if (selected) bg else panelBorder(), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PillRow(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            Pill(selected = i == selectedIndex, label = label, onClick = { onSelect(i) })
        }
    }
}

/** The five category colours as slanted bars, constructivist-poster style. */
@Composable
fun RetroRule(modifier: Modifier = Modifier, width: Dp = 150.dp, thickness: Dp = 7.dp) {
    val dark = LocalIsDarkTheme.current
    val colors = if (dark) {
        listOf(Kosmos.Blue, Kosmos.Mint, Kosmos.Coral, Kosmos.Ochre, Kosmos.Lilac)
    } else {
        // A step deeper so the bars still read on paper.
        listOf(Color(0xFF6F95C2), Color(0xFF79B9A2), Color(0xFFE07462), Color(0xFFDDB04C), Color(0xFFB98BC6))
    }
    Canvas(modifier.width(width).height(thickness)) {
        val gap = 5.dp.toPx()
        val skew = size.height * 0.55f
        val bar = (size.width - skew - gap * (colors.size - 1)) / colors.size
        colors.forEachIndexed { i, c ->
            val x = i * (bar + gap)
            val p = Path().apply {
                moveTo(x + skew, 0f)
                lineTo(x + skew + bar, 0f)
                lineTo(x + bar, size.height)
                lineTo(x, size.height)
                close()
            }
            drawPath(p, c)
        }
    }
}

/**
 * A postage stamp: perforated edge, dashed inner frame. Used for the home categories
 * and to frame the flag being asked about.
 */
@Composable
fun Stamp(
    color: Color,
    modifier: Modifier = Modifier,
    ink: Color = Kosmos.Ink,
    tilt: Float = 0f,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    var m = modifier
        .rotate(tilt)
        // Offscreen so the perforation holes can be punched clean through.
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawBehind {
            drawRect(color)
            val r = 3.5.dp.toPx()
            val step = 12.dp.toPx()
            fun edge(length: Float, at: (Float) -> Offset) {
                val n = max(1, (length / step).roundToInt())
                val s = length / n
                for (i in 0..n) drawCircle(Color.Black, r, at(i * s), blendMode = BlendMode.Clear)
            }
            edge(size.width) { Offset(it, 0f) }
            edge(size.width) { Offset(it, size.height) }
            edge(size.height) { Offset(0f, it) }
            edge(size.height) { Offset(size.width, it) }
            val inset = 10.dp.toPx()
            drawRect(
                ink.copy(alpha = 0.45f),
                topLeft = Offset(inset, inset),
                size = Size(size.width - 2 * inset, size.height - 2 * inset),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                ),
            )
        }
    if (onClick != null) m = m.clickable(role = Role.Button, onClick = onClick)
    Column(m.padding(contentPadding + 6.dp), content = content)
}

/** A paper ticket with notched sides and an optional tear-off stub. */
@Composable
fun Ticket(
    modifier: Modifier = Modifier,
    stubLabel: String? = null,
    stubValue: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val paper = cardPaper()
    Row(
        modifier
            .height(IntrinsicSize.Min)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                val cr = 14.dp.toPx()
                drawRoundRect(paper, cornerRadius = CornerRadius(cr, cr))
                val r = 9.dp.toPx()
                drawCircle(Color.Black, r, Offset(0f, size.height / 2), blendMode = BlendMode.Clear)
                drawCircle(Color.Black, r, Offset(size.width, size.height / 2), blendMode = BlendMode.Clear)
            },
    ) {
        Column(Modifier.weight(1f).padding(start = 22.dp, end = 14.dp, top = 14.dp, bottom = 14.dp), content = content)
        if (stubValue != null) {
            Column(
                Modifier
                    .width(70.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        drawLine(
                            Kosmos.Ink.copy(alpha = 0.35f),
                            Offset(0f, 8.dp.toPx()),
                            Offset(0f, size.height - 8.dp.toPx()),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                stubLabel?.let { Mono(it, color = Kosmos.InkSoft, caps = true) }
                Text(stubValue, fontFamily = PtMono, fontSize = 20.sp, color = Kosmos.Ink)
            }
        }
    }
}

/** A ruled library index card with a punched hole; holds country facts. */
@Composable
fun ArchiveCard(
    header: String,
    modifier: Modifier = Modifier,
    stamp: Verdict? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val paper = cardPaper()
    val rule = Kosmos.Blue.copy(alpha = 0.55f)
    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                val cr = 10.dp.toPx()
                drawRoundRect(paper, cornerRadius = CornerRadius(cr, cr))
                val step = 26.dp.toPx()
                var y = 44.dp.toPx()
                while (y < size.height - 14.dp.toPx()) {
                    drawLine(rule, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    y += step
                }
                drawCircle(
                    Color.Black,
                    radius = 5.dp.toPx(),
                    center = Offset(size.width / 2, size.height - 9.dp.toPx()),
                    blendMode = BlendMode.Clear,
                )
            }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
    ) {
        Column {
            Mono(header, color = Kosmos.InkSoft, caps = true)
            Spacer(Modifier.height(6.dp))
            content()
        }
        stamp?.let { RubberStamp(it, Modifier.align(Alignment.TopEnd).padding(top = 18.dp)) }
    }
}

enum class Verdict { CORRECT, MISSED, SKIPPED }

/** A tilted rubber stamp, as if the archive clerk had graded the answer. */
@Composable
fun RubberStamp(verdict: Verdict, modifier: Modifier = Modifier) {
    val (label, color) = when (verdict) {
        Verdict.CORRECT -> stringResource(R.string.ui_stamp_correct) to Kosmos.StampGreen
        Verdict.MISSED -> stringResource(R.string.ui_stamp_missed) to Kosmos.StampRed
        Verdict.SKIPPED -> stringResource(R.string.ui_stamp_skipped) to Kosmos.InkSoft
    }
    Text(
        label.uppercase(),
        modifier = modifier
            .rotate(-9f)
            .border(3.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        fontFamily = Unbounded,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        letterSpacing = 1.8.sp,
        color = color.copy(alpha = 0.92f),
    )
}

/** Country facts on an archive card, optionally stamped with the verdict. */
@Composable
fun FactCard(c: Country, modifier: Modifier = Modifier, compact: Boolean = false, verdict: Verdict? = null) {
    ArchiveCard(
        header = stringResource(R.string.ui_archive_card, c.alpha2),
        modifier = modifier,
        stamp = verdict,
    ) {
        FactBlock(c, compact = compact, ink = Kosmos.Ink)
    }
}

enum class PanelKind { PRIMARY, CORAL, OUTLINE }

/**
 * A chunky push-button with a darker lip underneath, like a Bakelite control panel.
 * The face sinks onto the lip while pressed.
 */
@Composable
fun PanelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: PanelKind = PanelKind.PRIMARY,
    enabled: Boolean = true,
    trailing: String? = null,
) {
    val cs = MaterialTheme.colorScheme
    val dark = LocalIsDarkTheme.current
    val face: Color
    val ink: Color
    val lip: Color
    when (kind) {
        PanelKind.PRIMARY -> if (dark) {
            face = Kosmos.Mint; ink = Kosmos.Ink; lip = Kosmos.MintLip
        } else {
            face = cs.primary; ink = cs.onPrimary; lip = Color(0xFF173F35)
        }
        PanelKind.CORAL -> { face = Kosmos.Coral; ink = Kosmos.Ink; lip = Kosmos.CoralLip }
        PanelKind.OUTLINE -> { face = cs.background; ink = cs.onBackground; lip = panelBorder() }
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateDpAsState(if (pressed && enabled) 3.dp else 0.dp, tween(70), label = "sink")
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .height(58.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .drawBehind {
                val cr = 14.dp.toPx()
                val top = 4.dp.toPx()
                drawRoundRect(lip, topLeft = Offset(0f, top), size = Size(size.width, size.height - top), cornerRadius = CornerRadius(cr, cr))
            },
    ) {
        var face2 = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .offset(y = sink)
            .clip(shape)
            .background(face)
        if (kind == PanelKind.OUTLINE) face2 = face2.border(2.dp, ink, shape)
        Row(face2, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = ink,
                maxLines = 1,
            )
            trailing?.let {
                Spacer(Modifier.width(8.dp))
                Text(it, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ink)
            }
        }
    }
}

/** A small dark chip of telemetry text, laid over the globe. */
@Composable
fun TelemetryChip(text: String, modifier: Modifier = Modifier, swatch: Color? = null) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Kosmos.Night.copy(alpha = 0.82f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        swatch?.let {
            Box(Modifier.size(9.dp).background(it))
            Spacer(Modifier.width(7.dp))
        }
        Mono(text, color = Kosmos.Paper, caps = true)
    }
}

// ---- icons (hand drawn, so no icon dependency is needed) ---------------------------------

enum class AppIcon { SETTINGS, STATS, BROWSE, BACK, CLOSE }

@Composable
fun IconBadge(icon: AppIcon, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color? = null) {
    val cs = MaterialTheme.colorScheme
    val color = tint ?: cs.onSurface
    val label = stringResource(
        when (icon) {
            AppIcon.SETTINGS -> R.string.a11y_settings
            AppIcon.STATS -> R.string.a11y_statistics
            AppIcon.BROWSE -> R.string.a11y_browse
            AppIcon.BACK -> R.string.a11y_back
            AppIcon.CLOSE -> R.string.a11y_close
        },
    )
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .size(48.dp)   // 48dp is the minimum comfortable touch target
            .semantics { contentDescription = label; role = Role.Button }
            .clip(shape)
            .background(cs.surface)
            .border(1.5.dp, panelBorder(), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val w = size.width
            val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round)
            when (icon) {
                AppIcon.SETTINGS -> {
                    drawCircle(color, radius = w * 0.26f, style = stroke)
                    for (k in 0 until 8) {
                        val a = Math.toRadians(k * 45.0)
                        val sx = (w / 2 + Math.cos(a) * w * 0.36f).toFloat()
                        val sy = (w / 2 + Math.sin(a) * w * 0.36f).toFloat()
                        val ex = (w / 2 + Math.cos(a) * w * 0.47f).toFloat()
                        val ey = (w / 2 + Math.sin(a) * w * 0.47f).toFloat()
                        drawLine(color, Offset(sx, sy), Offset(ex, ey), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
                    }
                }
                AppIcon.STATS -> {
                    val bw = w * 0.16f
                    listOf(0.45f, 0.72f, 0.95f).forEachIndexed { i, h ->
                        val x = w * (0.22f + i * 0.28f)
                        drawLine(color, Offset(x, w * 0.88f), Offset(x, w * (0.88f - h * 0.72f)), strokeWidth = bw, cap = StrokeCap.Round)
                    }
                }
                AppIcon.BROWSE -> {
                    drawCircle(color, radius = w * 0.32f, center = Offset(w * 0.43f, w * 0.43f), style = stroke)
                    drawLine(color, Offset(w * 0.66f, w * 0.66f), Offset(w * 0.9f, w * 0.9f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
                }
                AppIcon.BACK -> {
                    drawLine(color, Offset(w * 0.6f, w * 0.22f), Offset(w * 0.32f, w * 0.5f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.32f, w * 0.5f), Offset(w * 0.6f, w * 0.78f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
                }
                AppIcon.CLOSE -> {
                    drawLine(color, Offset(w * 0.25f, w * 0.25f), Offset(w * 0.75f, w * 0.75f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
                    drawLine(color, Offset(w * 0.75f, w * 0.25f), Offset(w * 0.25f, w * 0.75f), strokeWidth = w * 0.12f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

/**
 * Line icons for the five categories, drawn on a 48-unit grid: a globe with an orbit,
 * a flag, a columned building, a punch card and a lattice radio tower.
 */
@Composable
fun CategoryIcon(category: Category, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 48f
        fun o(x: Float, y: Float) = Offset(x * s, y * s)
        val stroke = Stroke(width = 2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, o(x1, y1), o(x2, y2), strokeWidth = 2.2f * s, cap = StrokeCap.Round)
        when (category) {
            Category.GLOBE -> {
                drawCircle(color, 13f * s, o(24f, 24f), style = stroke)
                drawOval(color, o(18f, 11f), Size(12f * s, 26f * s), style = stroke)
                line(11f, 24f, 37f, 24f)
                rotate(-20f, pivot = o(24f, 24f)) {
                    drawOval(color, o(3f, 17f), Size(42f * s, 14f * s), style = stroke)
                }
                drawCircle(color, 2.4f * s, o(42f, 17f))
            }
            Category.FLAGS -> {
                line(12f, 44f, 12f, 5f)
                val p = Path().apply {
                    moveTo(12f * s, 7f * s)
                    relativeCubicTo(8f * s, -4f * s, 13f * s, 4f * s, 22f * s, 0f)
                    relativeLineTo(0f, 18f * s)
                    relativeCubicTo(-9f * s, 4f * s, -14f * s, -4f * s, -22f * s, 0f)
                }
                drawPath(p, color, style = stroke)
            }
            Category.PLACES -> {
                val roof = Path().apply {
                    moveTo(6f * s, 17f * s); lineTo(24f * s, 6f * s); lineTo(42f * s, 17f * s); close()
                }
                drawPath(roof, color, style = stroke)
                listOf(11f, 19f, 29f, 37f).forEach { x -> line(x, 21f, x, 37f) }
                line(5f, 42f, 43f, 42f)
            }
            Category.CODES -> {
                val card = Path().apply {
                    moveTo(5f * s, 10f * s); lineTo(37f * s, 10f * s); lineTo(43f * s, 16f * s)
                    lineTo(43f * s, 38f * s); lineTo(5f * s, 38f * s); close()
                }
                drawPath(card, color, style = stroke)
                listOf(11f to 18f, 18f to 18f, 25f to 18f, 11f to 25f, 25f to 25f, 32f to 25f, 11f to 32f, 18f to 32f, 32f to 32f)
                    .forEach { (x, y) -> line(x, y, x + 3f, y) }
            }
            Category.OTHER -> {
                line(17f, 44f, 21f, 14f); line(21f, 14f, 27f, 14f); line(27f, 14f, 31f, 44f)
                line(19f, 29f, 29f, 29f); line(18f, 37f, 30f, 37f)
                line(21f, 14f, 29f, 23f); line(27f, 14f, 19f, 23f); line(24f, 14f, 24f, 9f)
                drawArc(color, -138.6f, 97.2f, false, o(12f, 4.94f), Size(24f * s, 24f * s), style = stroke)
                drawArc(color, -139.9f, 99.8f, false, o(7f, -1.05f), Size(34f * s, 34f * s), style = stroke)
            }
        }
    }
}

// ---- headers ----------------------------------------------------------------------------

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconBadge(AppIcon.BACK, onBack)
            Spacer(Modifier.width(14.dp))
        }
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.weight(1f),
        )
        actions?.invoke()
    }
}

// ---- content ----------------------------------------------------------------------------

/**
 * [countryName] becomes the image's description for screen readers. Pass null only
 * where the flag is decorative and the name is already announced next to it.
 */
@Composable
fun FlagImage(key: String, modifier: Modifier = Modifier, countryName: String? = null) {
    val ctx = LocalContext.current
    val id = remember(key) { ctx.resources.getIdentifier("flag_$key", "drawable", ctx.packageName) }
    if (id != 0) {
        val shape = RoundedCornerShape(3.dp)
        Image(
            painter = painterResource(id),
            contentDescription = countryName?.let { stringResource(R.string.a11y_flag_of, it) },
            contentScale = ContentScale.Fit,
            modifier = modifier.clip(shape).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), shape),
        )
    }
}

/** [ink] lets the block sit on paper (archive card) as well as on a panel. */
@Composable
fun FactBlock(c: Country, compact: Boolean = false, ink: Color? = null) {
    val fg = ink ?: MaterialTheme.colorScheme.onSurface
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            c.name.uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
        )
        Text(Facts.statusLabel(c), style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.78f))
        if (!compact) {
            Text(Facts.isoLine(c), style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.78f))
            c.isoNote?.let {
                Text("ISO note: $it", style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.78f))
            }
        }
        Spacer(Modifier.height(2.dp))
        Facts.rows(c).forEach { (k, v) ->
            Row(verticalAlignment = Alignment.Top) {
                Mono(k, modifier = Modifier.width(108.dp), color = fg, alpha = 0.72f, caps = true)
                Mono(v, color = fg)
            }
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(260), label = "progress")
    Box(modifier.height(6.dp).clip(CircleShape).background(cs.onBackground.copy(alpha = 0.12f))) {
        Box(Modifier.fillMaxWidth(f).height(6.dp).clip(CircleShape).background(cs.primary))
    }
}

/**
 * One indicator lamp per question on a control-panel strip: mint for right, red for
 * wrong, ochre for the current one, dark for still to come.
 */
@Composable
fun SegmentedProgress(
    total: Int,
    outcomes: List<Boolean>,
    current: Int,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val dark = LocalIsDarkTheme.current
    // The lamps are colour-only, so they need a spoken equivalent.
    val spoken = stringResource(
        R.string.a11y_progress,
        (current + 1).coerceAtMost(total),
        total,
        outcomes.count { it },
    )
    @Suppress("NAME_SHADOWING")
    val modifier = modifier.semantics { contentDescription = spoken }
    // Beyond this a lamp would be a speck, so fall back to a plain bar.
    if (total > 30) {
        ProgressBar(fraction = outcomes.size.toFloat() / total, modifier = modifier)
        return
    }
    val right = Kosmos.Mint
    val wrong = cs.error
    val now = if (dark) Kosmos.Ochre else Color(0xFFD9A93C)
    val offFill = cs.background
    val offRing = if (dark) Color(0xFF4A5374) else Kosmos.PaperBorder
    val litRing = Kosmos.Ink.copy(alpha = if (dark) 0f else 0.35f)
    val connector = panelBorder()
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .clip(shape)
            .background(cs.surface)
            .border(1.5.dp, panelBorder(), shape)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().height(16.dp)) {
            val n = total.coerceAtLeast(1)
            val lampR = (if (n <= 12) 7.dp.toPx() else 5.dp.toPx()).coerceAtMost(size.width / (n * 2.6f))
            val gap = if (n == 1) 0f else (size.width - 2 * lampR) / (n - 1)
            val cy = size.height / 2
            val pad = 4.dp.toPx()
            for (i in 0 until n) {
                val cx = if (n == 1) size.width / 2 else lampR + i * gap
                if (n <= 12 && i < n - 1) {
                    drawLine(connector, Offset(cx + lampR + pad, cy), Offset(cx + gap - lampR - pad, cy), strokeWidth = 1.dp.toPx())
                }
                val lit = when {
                    i < outcomes.size -> if (outcomes[i]) right else wrong
                    i == current -> now
                    else -> null
                }
                val c = Offset(cx, cy)
                if (lit != null) {
                    if (dark) {
                        drawCircle(
                            Brush.radialGradient(listOf(lit.copy(alpha = 0.55f), Color.Transparent), center = c, radius = lampR * 2.2f),
                            radius = lampR * 2.2f,
                            center = c,
                        )
                    }
                    drawCircle(lit, lampR, c)
                    drawCircle(litRing, lampR - 0.5.dp.toPx(), c, style = Stroke(1.dp.toPx()))
                } else {
                    drawCircle(offFill, lampR, c)
                    drawCircle(offRing, lampR - 0.75.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
                }
            }
        }
    }
}

/** Counts up to [value], like a mechanical counter. */
@Composable
fun RollingNumber(
    value: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
    style: TextStyle = MaterialTheme.typography.displayLarge,
) {
    var start by remember { mutableStateOf(false) }
    LaunchedEffect(value) { start = true }
    val shown by animateIntAsState(
        targetValue = if (start) value else 0,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "rolling",
    )
    Text(
        "$shown$suffix",
        modifier = modifier,
        style = style,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = Unbounded,
    )
}

/** A faint halftone of dots behind everything, like a printed poster. */
@Composable
fun DotGrid(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.075f)
    Canvas(modifier) {
        val step = 16.dp.toPx()
        val r = 1.dp.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(color, radius = r, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

// ---- globe palette ----------------------------------------------------------------------

/** Countries in play on the quiz globe. */
@Composable
fun globePoolLand(): Color = if (LocalIsDarkTheme.current) Kosmos.Mint else Color(0xFF79B9A2)

/** Countries out of play: present but quiet. */
@Composable
fun globeMutedLand(): Color = if (LocalIsDarkTheme.current) Color(0xFF3B4560) else Color(0xFFF6F0E2)

/** Not yet played, on the mastery globes. */
@Composable
fun globeUnseenLand(): Color = if (LocalIsDarkTheme.current) Kosmos.Blue.copy(alpha = 0.45f) else Color(0xFFF6F0E2)

/** Night ocean, paper graticule and ink borders in dark mode; pale sea and ink in light mode. */
@Composable
fun kosmosGlobeColors(land: Color, muted: Color): GlobeColors =
    if (LocalIsDarkTheme.current) {
        GlobeColors(
            ocean = Color(0xFF2A3352), land = land, muted = muted, line = Kosmos.Paper,
            atmosphere = Kosmos.Blue, border = Kosmos.Night,
        )
    } else {
        GlobeColors(
            ocean = Color(0xFFA9C3E0), land = land, muted = muted, line = Kosmos.Ink,
            atmosphere = Color(0xFF6F95C2), border = Kosmos.Ink.copy(alpha = 0.75f),
        )
    }

fun formatSeconds(seconds: Double): String =
    if (seconds >= 60) "${(seconds / 60).toInt()}m ${(seconds % 60).toInt()}s"
    else String.format("%.1fs", seconds)

/** Draws a country's silhouette alone, scaled to fit, for the outline quiz. */
@Composable
fun OutlineShape(shape: MapShape?, color: Color, modifier: Modifier = Modifier) {
    if (shape == null) {
        Box(modifier)
        return
    }
    Canvas(modifier) {
        // Equirectangular, with longitude squeezed by cos(latitude) so the shape does
        // not look stretched near the poles.
        val midLat = (shape.minLat + shape.maxLat) / 2.0
        val kx = kotlin.math.cos(Math.toRadians(midLat)).toFloat().coerceAtLeast(0.15f)
        val w = shape.widthDeg * kx
        val h = shape.heightDeg
        if (w <= 0f || h <= 0f) return@Canvas
        val scale = kotlin.math.min(size.width * 0.86f / w, size.height * 0.86f / h)
        val offX = (size.width - w * scale) / 2f
        val offY = (size.height - h * scale) / 2f
        for (ring in shape.drawRings) {
            if (ring.size < 6) continue
            val path = Path()
            var i = 0
            while (i < ring.size) {
                val x = offX + (ring[i] - shape.minLon) * kx * scale
                val y = offY + (shape.maxLat - ring[i + 1]) * scale
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                i += 2
            }
            path.close()
            drawPath(path, color)
        }
    }
}

/** Keeps the screen on while a round is in progress. */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled, view) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
