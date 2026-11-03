package com.meridian11.books.awake.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import kotlin.math.sin

val AwakeSansFontFamily = FontFamily(
    Font(R.font.awake_sans, FontWeight.Normal),
    Font(R.font.awake_sans, FontWeight.Medium),
    Font(R.font.awake_sans, FontWeight.SemiBold),
    Font(R.font.awake_sans, FontWeight.Bold)
)

data class AwakeThemeColors(
    val windowBg: Color,
    val cardBg: Color,
    val cardStroke: Color,
    val tonalTrackBg: Color,
    val heroInactiveBg: Color,
    val heroInactiveStroke: Color,
    val heroActiveBg: Color,
    val heroActiveStroke: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val chipSelectedBg: Color,
    val chipSelectedText: Color,
    val chipUnselectedText: Color,
    val switchTrackOn: Color,
    val switchTrackHeroOn: Color,
    val switchTrackOff: Color,
    val switchTrackOffStroke: Color,
    val switchThumbOn: Color,
    val switchThumbOff: Color,
    val cupBadgeEmptyBg: Color,
    val cupBadgeFullBg: Color,
    val cupLiquidEmpty: Color,
    val cupLiquidFull: Color,
    val cupStrokeEmpty: Color,
    val cupStrokeFull: Color,
    val cupSteam: Color,
    val cupCrema: Color,
    val glassSurfaceBg: Color,
    val glassSurfaceStroke: Color,
    val glassCardBg: Color,
    val glassCardStroke: Color
)

@Composable
fun rememberAwakeThemeColors(darkTheme: Boolean = isSystemInDarkTheme()): AwakeThemeColors {
    return remember(darkTheme) {
        if (darkTheme) {
            AwakeThemeColors(
                windowBg = Color(0xFF0F172A),
                cardBg = Color(0xFF1E293B),
                cardStroke = Color(0xFF334155),
                tonalTrackBg = Color(0xFF0F172A),
                heroInactiveBg = Color(0xFF172554),
                heroInactiveStroke = Color(0xFF1E40AF),
                heroActiveBg = Color(0xFF431407),
                heroActiveStroke = Color(0xFF9A3412),
                textPrimary = Color(0xFFF8FAFC),
                textSecondary = Color(0xFF94A3B8),
                chipSelectedBg = Color(0xFF3B82F6),
                chipSelectedText = Color(0xFFFFFFFF),
                chipUnselectedText = Color(0xFFCBD5E1),
                switchTrackOn = Color(0xFF3B82F6),
                switchTrackHeroOn = Color(0xFFFF7A28),
                switchTrackOff = Color(0xFF334155),
                switchTrackOffStroke = Color(0xFF64748B),
                switchThumbOn = Color(0xFFFFFFFF),
                switchThumbOff = Color(0xFF94A3B8),
                cupBadgeEmptyBg = Color(0xFF1E3A8A),
                cupBadgeFullBg = Color(0xFF7C2D12),
                cupLiquidEmpty = Color(0xFF60A5FA),
                cupLiquidFull = Color(0xFFFF7A28),
                cupStrokeEmpty = Color(0xFF93C5FD),
                cupStrokeFull = Color(0xFFFDBA74),
                cupSteam = Color(0xFFFF9E5E),
                cupCrema = Color(0xFFFFEDD5),
                glassSurfaceBg = Color(0xB80F172A),
                glassSurfaceStroke = Color(0x4DFFFFFF),
                glassCardBg = Color(0x661E293B),
                glassCardStroke = Color(0x33FFFFFF)
            )
        } else {
            AwakeThemeColors(
                windowBg = Color(0xFFF0F4F9),
                cardBg = Color(0xFFFFFFFF),
                cardStroke = Color(0xFFE2E8F0),
                tonalTrackBg = Color(0xFFE8EFF8),
                heroInactiveBg = Color(0xFFDCE8FA),
                heroInactiveStroke = Color(0xFFB6CFF5),
                heroActiveBg = Color(0xFFFFE6D5),
                heroActiveStroke = Color(0xFFFFC59E),
                textPrimary = Color(0xFF0F172A),
                textSecondary = Color(0xFF475569),
                chipSelectedBg = Color(0xFF2563EB),
                chipSelectedText = Color(0xFFFFFFFF),
                chipUnselectedText = Color(0xFF1E3A5F),
                switchTrackOn = Color(0xFF2563EB),
                switchTrackHeroOn = Color(0xFFFF7A28),
                switchTrackOff = Color(0xFFD5E0F0),
                switchTrackOffStroke = Color(0xFF94A3B8),
                switchThumbOn = Color(0xFFFFFFFF),
                switchThumbOff = Color(0xFF475569),
                cupBadgeEmptyBg = Color(0xFFC7DCFA),
                cupBadgeFullBg = Color(0xFFFFD2B3),
                cupLiquidEmpty = Color(0xFF3B82F6),
                cupLiquidFull = Color(0xFFFF7A28),
                cupStrokeEmpty = Color(0xFF1D4ED8),
                cupStrokeFull = Color(0xFFC2410C),
                cupSteam = Color(0xFFFF5500),
                cupCrema = Color(0xFFFFF7ED),
                glassSurfaceBg = Color(0xB8F8FAFD),
                glassSurfaceStroke = Color(0xE6FFFFFF),
                glassCardBg = Color(0x8CFFFFFF),
                glassCardStroke = Color(0xB3FFFFFF)
            )
        }
    }
}

/**
 * Jetpack Compose Material 3 Expressive Coffee Cup Pictogram:
 * - Animates smoothly between 0.5f (Half-Empty Pastel Blue) and 1.0f (Filled Pastel Orange with steam).
 */
@Composable
fun CoffeeCupPictogramComposable(
    isAwake: Boolean,
    colors: AwakeThemeColors,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp
) {
    val fillLevel by animateFloatAsState(
        targetValue = if (isAwake) 1.0f else 0.5f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "cupFillLevel"
    )
    val steamAlpha by animateFloatAsState(
        targetValue = if (isAwake) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "cupSteamAlpha"
    )
    val badgeBg by animateColorAsState(
        targetValue = if (isAwake) colors.cupBadgeFullBg else colors.cupBadgeEmptyBg,
        animationSpec = tween(durationMillis = 320),
        label = "cupBadgeBg"
    )
    val liquidColor by animateColorAsState(
        targetValue = if (isAwake) colors.cupLiquidFull else colors.cupLiquidEmpty,
        animationSpec = tween(durationMillis = 320),
        label = "cupLiquidColor"
    )
    val strokeColor by animateColorAsState(
        targetValue = if (isAwake) colors.cupStrokeFull else colors.cupStrokeEmpty,
        animationSpec = tween(durationMillis = 320),
        label = "cupStrokeColor"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = minOf(w, h) / 108f

        // 1. Rounded Squircle Badge Background
        val badgeInset = 4f * s
        val badgeRadius = 28f * s
        drawRoundRect(
            color = badgeBg,
            topLeft = Offset(badgeInset, badgeInset),
            size = Size(w - 2 * badgeInset, h - 2 * badgeInset),
            cornerRadius = CornerRadius(badgeRadius, badgeRadius)
        )

        // 2. Cup Bowl Geometry
        val cupLeft = 30f * s
        val cupTop = 36f * s
        val cupRight = 66f * s
        val cupBottom = 68f * s
        val cupCorner = 9f * s

        val cupPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(cupLeft, cupTop, cupRight, cupBottom),
                    topLeft = CornerRadius.Zero,
                    topRight = CornerRadius.Zero,
                    bottomRight = CornerRadius(cupCorner, cupCorner),
                    bottomLeft = CornerRadius(cupCorner, cupCorner)
                )
            )
        }

        // 3. Clipped Liquid Fill (0.5f = Half-Empty, 1.0f = Completely Filled)
        clipPath(cupPath) {
            val cupHeight = cupBottom - cupTop
            val liquidTop = cupBottom - cupHeight * fillLevel.coerceIn(0f, 1f)
            val wavePath = Path().apply {
                val waveAmp = if (fillLevel in 0.52f..0.98f) 2.2f * s else 0f
                moveTo(cupLeft, cupBottom)
                lineTo(cupLeft, liquidTop)
                val midX = (cupLeft + cupRight) / 2f
                val waveOffset = sin(fillLevel * Math.PI * 2.0).toFloat() * waveAmp
                cubicTo(
                    cupLeft + (midX - cupLeft) * 0.5f,
                    liquidTop - waveOffset,
                    midX + (cupRight - midX) * 0.5f,
                    liquidTop + waveOffset,
                    cupRight,
                    liquidTop
                )
                lineTo(cupRight, cupBottom)
                close()
            }
            drawPath(path = wavePath, color = liquidColor)

            if (steamAlpha > 0.05f) {
                drawRoundRect(
                    color = colors.cupCrema.copy(alpha = steamAlpha),
                    topLeft = Offset(cupLeft + 2f * s, liquidTop + 1.5f * s),
                    size = Size((cupRight - cupLeft) - 4f * s, 3f * s),
                    cornerRadius = CornerRadius(1.5f * s, 1.5f * s)
                )
            }
        }

        // 4. Cup Bowl Outline
        drawPath(
            path = cupPath,
            color = strokeColor,
            style = Stroke(width = 4.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 5. Cup Handle
        val handlePath = Path().apply {
            moveTo(66f * s, 42f * s)
            lineTo(72f * s, 42f * s)
            arcTo(
                rect = Rect(65f * s, 42f * s, 79f * s, 60f * s),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(66f * s, 60f * s)
        }
        drawPath(
            path = handlePath,
            color = strokeColor,
            style = Stroke(width = 4.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 6. Saucer Base Pill
        drawLine(
            color = strokeColor,
            start = Offset(26f * s, 76f * s),
            end = Offset(74f * s, 76f * s),
            strokeWidth = 4.5f * s,
            cap = StrokeCap.Round
        )

        // 7. Vertical Steam Pills (Active Filled Cup Only)
        if (steamAlpha > 0.02f) {
            val lift = (1f - steamAlpha) * 4f * s
            val steamColor = colors.cupSteam.copy(alpha = steamAlpha)
            drawLine(
                color = steamColor,
                start = Offset(42f * s, 28f * s + lift),
                end = Offset(42f * s, 20f * s + lift),
                strokeWidth = 4.2f * s,
                cap = StrokeCap.Round
            )
            drawLine(
                color = steamColor,
                start = Offset(54f * s, 28f * s + lift),
                end = Offset(54f * s, 20f * s + lift),
                strokeWidth = 4.2f * s,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Custom Material 3 Expressive 52dp x 30dp Pill Switch in Jetpack Compose.
 */
@Composable
fun AwakeM3Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: AwakeThemeColors,
    modifier: Modifier = Modifier,
    useHeroOrangeWhenChecked: Boolean = false
) {
    val activeTrackColor = if (useHeroOrangeWhenChecked) colors.switchTrackHeroOn else colors.switchTrackOn
    val trackColor by animateColorAsState(
        targetValue = if (checked) activeTrackColor else colors.switchTrackOff,
        animationSpec = tween(durationMillis = 200),
        label = "switchTrackColor"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) colors.switchThumbOn else colors.switchThumbOff,
        animationSpec = tween(durationMillis = 200),
        label = "switchThumbColor"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 25.dp else 5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switchThumbOffset"
    )

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(CircleShape)
            .background(trackColor)
            .then(
                if (!checked) {
                    Modifier.border(2.dp, colors.switchTrackOffStroke, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(
                role = Role.Switch,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

/**
 * Single-line Segmented Pill Button Track used for Duration (`Infinite | 30m | 1h | 2h`)
 * and Tap Interval (`15s | 30s | 60s | 120s`).
 */
@Composable
fun <T> SegmentedPillRow(
    items: List<Pair<T, String>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    colors: AwakeThemeColors,
    tagPrefix: String,
    modifier: Modifier = Modifier,
    trackColor: Color = colors.tonalTrackBg
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(trackColor)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (value, label) ->
            val isSelected = value == selectedValue
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) colors.chipSelectedBg else Color.Transparent,
                animationSpec = tween(durationMillis = 180),
                label = "segmentBg"
            )
            val textColor = if (isSelected) colors.chipSelectedText else colors.chipUnselectedText

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 6.dp)
                    .testTag("${tagPrefix}_$value"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontFamily = AwakeSansFontFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Compact Frosted-Glass Popover Panel (`QuickGlassPanelActivity`) shown when clicking
 * the Taskbar Coffee Cup icon.
 * - Features a whiteish frosted-glass background with cross-window blur ONLY behind the panel.
 * - Exposes the main Awake toggle, main duration modes (`Infinite | 30m | 1h | 2h`), and a
 *   `⚙️ Settings…` item that opens the full non-blurred Awake window (`SettingsDialogActivity`).
 */
@Composable
fun QuickGlassPanelScreen(
    state: AwakeState,
    onToggleAwake: () -> Unit,
    onSelectMode: (Int) -> Unit,
    onOpenFullSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = rememberAwakeThemeColors()
    val durationLabel = AndroidShortcutPublisher.formatDurationLong(state.durationMinutes)

    val heroBg by animateColorAsState(
        targetValue = if (state.isAwake) colors.heroActiveBg.copy(alpha = 0.82f)
        else colors.heroInactiveBg.copy(alpha = 0.82f),
        animationSpec = tween(durationMillis = 250),
        label = "glassHeroBg"
    )
    val heroStroke by animateColorAsState(
        targetValue = if (state.isAwake) colors.heroActiveStroke else colors.heroInactiveStroke,
        animationSpec = tween(durationMillis = 250),
        label = "glassHeroStroke"
    )

    Column(
        modifier = Modifier
            .width(380.dp)
            .clip(RoundedCornerShape(28.dp))
            .padding(18.dp)
            .testTag("quick_glass_panel_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header Bar inside Glass Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoffeeCupPictogramComposable(
                    isAwake = state.isAwake,
                    colors = colors,
                    size = 24.dp
                )
                Text(
                    text = "Awake",
                    fontFamily = AwakeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.glassCardBg)
                    .clickable { onDismiss() }
                    .testTag("glass_close_btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    fontFamily = AwakeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }

        // 1. Hero Awake Toggle Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(heroBg)
                .border(1.25.dp, heroStroke, RoundedCornerShape(22.dp))
                .clickable { onToggleAwake() }
                .padding(14.dp)
                .testTag("glass_hero_toggle"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoffeeCupPictogramComposable(
                isAwake = state.isAwake,
                colors = colors,
                size = 60.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.isAwake) "Keeping Awake" else "Normal Sleep",
                    fontFamily = AwakeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (state.isAwake) {
                        "Filled Cup ($durationLabel) • Active"
                    } else {
                        "Half-Empty Cup ($durationLabel)"
                    },
                    fontFamily = AwakeSansFontFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AwakeM3Switch(
                checked = state.isAwake,
                onCheckedChange = { onToggleAwake() },
                colors = colors,
                useHeroOrangeWhenChecked = true
            )
        }

        // 2. Main Modes (Keep-Awake Duration) on Frosted Glass Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.glassCardBg)
                .border(1.dp, colors.glassCardStroke, RoundedCornerShape(20.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Keep-Awake Mode",
                fontFamily = AwakeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedPillRow(
                items = listOf(
                    0 to "Infinite",
                    30 to "30m",
                    60 to "1h",
                    120 to "2h"
                ),
                selectedValue = state.durationMinutes,
                onSelect = onSelectMode,
                colors = colors,
                tagPrefix = "glass_mode",
                trackColor = colors.tonalTrackBg.copy(alpha = 0.65f)
            )
        }

        // 3. Settings Item -> Opens Full Opaque App Window
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.glassCardBg)
                .border(1.dp, colors.glassCardStroke, RoundedCornerShape(18.dp))
                .clickable { onOpenFullSettings() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("glass_open_settings"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "⚙️ Settings & Guardrails…",
                fontFamily = AwakeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = colors.textPrimary
            )
            Text(
                text = "Open App →",
                fontFamily = AwakeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = colors.chipSelectedBg
            )
        }
    }
}

/**
 * Full Desktop / Googlebook App Window (`SettingsDialogActivity`) built in Jetpack Compose.
 * - Preserves the exact opaque, non-blurred Material 3 Expressive Pastel Blue / Pastel Orange
 *   2-column design on the device with a seamless window header (`captionBar`).
 */
@Composable
fun SettingsWindowScreen(
    state: AwakeState,
    isAccessibilityEnabled: Boolean,
    onToggleAwake: () -> Unit,
    onSelectDuration: (Int) -> Unit,
    onToggleDirectTap: (Boolean) -> Unit,
    onAddQsTile: () -> Unit,
    onPinWidget: () -> Unit,
    onToggleTapSimulation: (Boolean) -> Unit,
    onSelectTapInterval: (Int) -> Unit,
    onOpenAccessibilitySetup: () -> Unit,
    onUpdateGuardrails: (
        lidClose: Boolean,
        lowBattery: Boolean,
        charger: Boolean,
        dimming: Boolean
    ) -> Unit,
    onCloseWindow: () -> Unit
) {
    val colors = rememberAwakeThemeColors()
    val windowInsetsPadding = WindowInsets.systemBars
        .union(WindowInsets.captionBar)
        .asPaddingValues()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.windowBg)
            .padding(windowInsetsPadding)
    ) {
        val isTwoColumn = maxWidth >= 640.dp
        val scrollState = rememberScrollState()

        if (isTwoColumn) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroToggleCard(state = state, colors = colors, onToggleAwake = onToggleAwake)
                    DurationPresetsCard(state = state, colors = colors, onSelectDuration = onSelectDuration)
                    DirectToggleCard(state = state, colors = colors, onToggleDirectTap = onToggleDirectTap)
                    QsAndWidgetCard(colors = colors, onAddQsTile = onAddQsTile, onPinWidget = onPinWidget)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TapSimulationCard(
                        state = state,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        colors = colors,
                        onToggleTapSimulation = onToggleTapSimulation,
                        onSelectTapInterval = onSelectTapInterval,
                        onOpenAccessibilitySetup = onOpenAccessibilitySetup
                    )
                    GuardrailsCard(
                        state = state,
                        colors = colors,
                        onUpdateGuardrails = onUpdateGuardrails,
                        onCloseWindow = onCloseWindow
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroToggleCard(state = state, colors = colors, onToggleAwake = onToggleAwake)
                DurationPresetsCard(state = state, colors = colors, onSelectDuration = onSelectDuration)
                DirectToggleCard(state = state, colors = colors, onToggleDirectTap = onToggleDirectTap)
                QsAndWidgetCard(colors = colors, onAddQsTile = onAddQsTile, onPinWidget = onPinWidget)
                TapSimulationCard(
                    state = state,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    colors = colors,
                    onToggleTapSimulation = onToggleTapSimulation,
                    onSelectTapInterval = onSelectTapInterval,
                    onOpenAccessibilitySetup = onOpenAccessibilitySetup
                )
                GuardrailsCard(
                    state = state,
                    colors = colors,
                    onUpdateGuardrails = onUpdateGuardrails,
                    onCloseWindow = onCloseWindow
                )
            }
        }
    }
}

@Composable
private fun HeroToggleCard(
    state: AwakeState,
    colors: AwakeThemeColors,
    onToggleAwake: () -> Unit
) {
    val durationLabel = AndroidShortcutPublisher.formatDurationLong(state.durationMinutes)
    val bg by animateColorAsState(
        targetValue = if (state.isAwake) colors.heroActiveBg else colors.heroInactiveBg,
        animationSpec = tween(durationMillis = 250),
        label = "heroCardBg"
    )
    val stroke by animateColorAsState(
        targetValue = if (state.isAwake) colors.heroActiveStroke else colors.heroInactiveStroke,
        animationSpec = tween(durationMillis = 250),
        label = "heroCardStroke"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .border(1.5.dp, stroke, RoundedCornerShape(28.dp))
            .clickable { onToggleAwake() }
            .padding(18.dp)
            .testTag("card_hero_toggle"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoffeeCupPictogramComposable(
            isAwake = state.isAwake,
            colors = colors,
            size = 76.dp,
            modifier = Modifier.testTag("coffee_cup_hero")
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (state.isAwake) "Keeping Awake" else "Normal Sleep",
                fontFamily = AwakeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.textPrimary,
                maxLines = 1,
                modifier = Modifier.testTag("tv_dialog_title")
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (state.isAwake) {
                    "Filled Cup ($durationLabel) • Preventing Googlebook sleep"
                } else {
                    "Half-Empty Cup ($durationLabel) • Normal Googlebook sleep"
                },
                fontFamily = AwakeSansFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = colors.textSecondary,
                modifier = Modifier.testTag("tv_status_subtitle")
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        AwakeM3Switch(
            checked = state.isAwake,
            onCheckedChange = { onToggleAwake() },
            colors = colors,
            useHeroOrangeWhenChecked = true,
            modifier = Modifier.testTag("switch_master_awake")
        )
    }
}

@Composable
private fun DurationPresetsCard(
    state: AwakeState,
    colors: AwakeThemeColors,
    onSelectDuration: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.cardStroke, RoundedCornerShape(26.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Keep-Awake Duration",
            fontFamily = AwakeSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.textPrimary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(10.dp))
        SegmentedPillRow(
            items = listOf(
                0 to "Infinite",
                30 to "30m",
                60 to "1h",
                120 to "2h"
            ),
            selectedValue = state.durationMinutes,
            onSelect = onSelectDuration,
            colors = colors,
            tagPrefix = "btn_duration"
        )
    }
}

@Composable
private fun DirectToggleCard(
    state: AwakeState,
    colors: AwakeThemeColors,
    onToggleDirectTap: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.cardStroke, RoundedCornerShape(26.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hide settings, tap icon to toggle",
                fontFamily = AwakeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.textPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (state.directToggleOnTap) {
                    "Active: Tapping Taskbar icon toggles directly (right-click icon for Settings)."
                } else {
                    "Off: Tapping Taskbar icon opens quick glass panel (turn ON for 1-click toggle)."
                },
                fontFamily = AwakeSansFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = colors.textSecondary,
                modifier = Modifier.testTag("tv_direct_toggle_subtitle")
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        AwakeM3Switch(
            checked = state.directToggleOnTap,
            onCheckedChange = onToggleDirectTap,
            colors = colors,
            modifier = Modifier.testTag("switch_direct_toggle_on_tap")
        )
    }
}

@Composable
private fun QsAndWidgetCard(
    colors: AwakeThemeColors,
    onAddQsTile: () -> Unit,
    onPinWidget: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.cardStroke, RoundedCornerShape(26.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Quick Settings Tile & Squircle Widget",
            fontFamily = AwakeSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.textPrimary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Quick Settings Tile: Tap system tray tile to toggle Awake, or long-press for Settings.",
            fontFamily = AwakeSansFontFamily,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = colors.textSecondary,
            modifier = Modifier.testTag("tv_qs_tile_info")
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(colors.chipSelectedBg)
                    .clickable { onAddQsTile() }
                    .padding(horizontal = 8.dp)
                    .testTag("btn_add_qs_tile"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "＋ Quick Settings Tile",
                    fontFamily = AwakeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colors.chipSelectedText,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(colors.tonalTrackBg)
                    .clickable { onPinWidget() }
                    .padding(horizontal = 8.dp)
                    .testTag("btn_pin_widget"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "＋ Squircle Widget",
                    fontFamily = AwakeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colors.chipUnselectedText,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TapSimulationCard(
    state: AwakeState,
    isAccessibilityEnabled: Boolean,
    colors: AwakeThemeColors,
    onToggleTapSimulation: (Boolean) -> Unit,
    onSelectTapInterval: (Int) -> Unit,
    onOpenAccessibilitySetup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.cardStroke, RoundedCornerShape(26.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Simulate Tap / Click Fallback",
                    fontFamily = AwakeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (state.tapSimulationEnabled) {
                        "Active: Simulating corner micro-tap every ${state.tapIntervalSeconds}s"
                    } else {
                        "Off: Micro-tap every ${state.tapIntervalSeconds}s when WakeLock can't be used"
                    },
                    fontFamily = AwakeSansFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.testTag("tv_tap_interval_summary")
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            AwakeM3Switch(
                checked = state.tapSimulationEnabled,
                onCheckedChange = onToggleTapSimulation,
                colors = colors,
                modifier = Modifier.testTag("switch_tap_simulation")
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        SegmentedPillRow(
            items = listOf(
                15 to "15s",
                30 to "30s",
                60 to "60s",
                120 to "120s"
            ),
            selectedValue = state.tapIntervalSeconds,
            onSelect = onSelectTapInterval,
            colors = colors,
            tagPrefix = "btn_tap"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(CircleShape)
                .background(colors.tonalTrackBg)
                .clickable { onOpenAccessibilitySetup() }
                .padding(horizontal = 12.dp)
                .testTag("btn_accessibility_setup"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isAccessibilityEnabled) {
                    "✓ Accessibility Keep-Alive Service Enabled"
                } else {
                    "Enable Accessibility Service for Tap Simulation →"
                },
                fontFamily = AwakeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = colors.chipUnselectedText,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GuardrailsCard(
    state: AwakeState,
    colors: AwakeThemeColors,
    onUpdateGuardrails: (
        lidClose: Boolean,
        lowBattery: Boolean,
        charger: Boolean,
        dimming: Boolean
    ) -> Unit,
    onCloseWindow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(colors.cardBg)
            .border(1.dp, colors.cardStroke, RoundedCornerShape(26.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Smart Googlebook Guardrails",
            fontFamily = AwakeSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.textPrimary,
            maxLines = 1
        )

        GuardrailSwitchRow(
            label = "Backpack Safety: Auto-Off When Lid Closes",
            checked = state.lidCloseAutoShutoffEnabled,
            colors = colors,
            testTag = "switch_lid_close_shutoff",
            onCheckedChange = {
                onUpdateGuardrails(
                    it,
                    state.lowBatteryCutoffEnabled,
                    state.autoAwakeOnChargerEnabled,
                    state.allowScreenDimming
                )
            }
        )

        GuardrailSwitchRow(
            label = "Battery Guard: Auto-Off Below 20% Unplugged",
            checked = state.lowBatteryCutoffEnabled,
            colors = colors,
            testTag = "switch_low_battery_cutoff",
            onCheckedChange = {
                onUpdateGuardrails(
                    state.lidCloseAutoShutoffEnabled,
                    it,
                    state.autoAwakeOnChargerEnabled,
                    state.allowScreenDimming
                )
            }
        )

        GuardrailSwitchRow(
            label = "Plug-in Brew: Auto-Awake on AC Charger",
            checked = state.autoAwakeOnChargerEnabled,
            colors = colors,
            testTag = "switch_auto_awake_charger",
            onCheckedChange = {
                onUpdateGuardrails(
                    state.lidCloseAutoShutoffEnabled,
                    state.lowBatteryCutoffEnabled,
                    it,
                    state.allowScreenDimming
                )
            }
        )

        GuardrailSwitchRow(
            label = "Allow Screen Dimming While Keeping Awake",
            checked = state.allowScreenDimming,
            colors = colors,
            testTag = "switch_allow_dimming",
            onCheckedChange = {
                onUpdateGuardrails(
                    state.lidCloseAutoShutoffEnabled,
                    state.lowBatteryCutoffEnabled,
                    state.autoAwakeOnChargerEnabled,
                    it
                )
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(colors.chipSelectedBg)
                    .clickable { onCloseWindow() }
                    .padding(horizontal = 24.dp)
                    .testTag("btn_close_settings"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    fontFamily = AwakeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = colors.chipSelectedText,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun GuardrailSwitchRow(
    label: String,
    checked: Boolean,
    colors: AwakeThemeColors,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = AwakeSansFontFamily,
            fontSize = 13.sp,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        AwakeM3Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = colors,
            modifier = Modifier.testTag(testTag)
        )
    }
}
