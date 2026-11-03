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
    val heroActiveBg: Color,
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
    val cupIconTintInactive: Color,
    val cupIconTintActive: Color
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
                heroActiveBg = Color(0xFF431407),
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
                cupIconTintInactive = Color(0xFF93C5FD),
                cupIconTintActive = Color(0xFFFFB88C)
            )
        } else {
            AwakeThemeColors(
                windowBg = Color(0xFFF0F4F9),
                cardBg = Color(0xFFFFFFFF),
                cardStroke = Color(0xFFE2E8F0),
                tonalTrackBg = Color(0xFFE8EFF8),
                heroInactiveBg = Color(0xFFDCE8FA),
                heroActiveBg = Color(0xFFFFE6D5),
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
                cupIconTintInactive = Color(0xFF1D4ED8),
                cupIconTintActive = Color(0xFFEA580C)
            )
        }
    }
}

/**
 * Single-tone Material Design Themed Coffee Cup Icon in Jetpack Compose:
 * - Uses a single monochrome Material theme icon tint (`cupIconTintInactive` on the blue badge
 *   when Half-Empty, and `cupIconTintActive` on the orange badge when Filled) with negative-space
 *   rim detail and rising steam pills.
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
    val iconTint by animateColorAsState(
        targetValue = if (isAwake) colors.cupIconTintActive else colors.cupIconTintInactive,
        animationSpec = tween(durationMillis = 320),
        label = "cupIconTint"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val s = minOf(w, h) / 108f

        // 1. Rounded Squircle Badge Background (No outline)
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

        // 3. Clipped Single-Tone Material Themed Liquid Fill (0.5f = Half-Empty, 1.0f = Filled)
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
            drawPath(path = wavePath, color = iconTint)

            // Negative-space rim cut (in badge background color) so the icon remains strictly 1-color Material themed
            if (steamAlpha > 0.05f) {
                drawRoundRect(
                    color = badgeBg.copy(alpha = steamAlpha),
                    topLeft = Offset(cupLeft + 4f * s, liquidTop + 4f * s),
                    size = Size((cupRight - cupLeft) - 8f * s, 3f * s),
                    cornerRadius = CornerRadius(1.5f * s, 1.5f * s)
                )
            }
        }

        // 4. Cup Bowl Outline (Same Material Theme Icon Tint)
        drawPath(
            path = cupPath,
            color = iconTint,
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
            color = iconTint,
            style = Stroke(width = 4.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 6. Saucer Base Pill
        drawLine(
            color = iconTint,
            start = Offset(26f * s, 76f * s),
            end = Offset(74f * s, 76f * s),
            strokeWidth = 4.5f * s,
            cap = StrokeCap.Round
        )

        // 7. Vertical Steam Pills (Same Material Theme Icon Tint)
        if (steamAlpha > 0.02f) {
            val lift = (1f - steamAlpha) * 4f * s
            val steamColor = iconTint.copy(alpha = steamAlpha)
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
 * Material Design Themed Plus (+) Vector Icon.
 */
@Composable
private fun MaterialAddIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.5f),
            end = Offset(w * 0.8f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.2f),
            end = Offset(w * 0.5f, h * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Material Design Themed Checkmark or Chevron Vector Icon.
 */
@Composable
private fun MaterialStatusIcon(isEnabled: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        val path = Path().apply {
            if (isEnabled) {
                moveTo(w * 0.18f, h * 0.52f)
                lineTo(w * 0.42f, h * 0.76f)
                lineTo(w * 0.84f, h * 0.26f)
            } else {
                moveTo(w * 0.35f, h * 0.22f)
                lineTo(w * 0.68f, h * 0.5f)
                lineTo(w * 0.35f, h * 0.78f)
            }
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
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
 * Full Desktop / Googlebook App Window (`SettingsDialogActivity`) built in Jetpack Compose.
 * - Preserves the opaque, non-blurred Material 3 Expressive Pastel Blue / Pastel Orange
 *   2-column design with a seamless window header (`captionBar`) and an outline-free
 *   blue/orange Hero Toggle Container.
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

    // Toggle container has NO outline—pure solid blue (inactive) or orange (active)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
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
                    "Off: Tapping Taskbar icon opens window (turn ON for 1-click toggle)."
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
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(colors.chipSelectedBg)
                    .clickable { onAddQsTile() }
                    .padding(horizontal = 10.dp)
                    .testTag("btn_add_qs_tile"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                MaterialAddIcon(tint = colors.chipSelectedText)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Quick Settings Tile",
                    fontFamily = AwakeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colors.chipSelectedText,
                    maxLines = 1
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(colors.tonalTrackBg)
                    .clickable { onPinWidget() }
                    .padding(horizontal = 10.dp)
                    .testTag("btn_pin_widget"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                MaterialAddIcon(tint = colors.chipUnselectedText)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Squircle Widget",
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(CircleShape)
                .background(colors.tonalTrackBg)
                .clickable { onOpenAccessibilitySetup() }
                .padding(horizontal = 12.dp)
                .testTag("btn_accessibility_setup"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isAccessibilityEnabled) {
                MaterialStatusIcon(isEnabled = true, tint = colors.chipUnselectedText)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = if (isAccessibilityEnabled) {
                    "Accessibility Keep-Alive Service Enabled"
                } else {
                    "Enable Accessibility Service for Tap Simulation"
                },
                fontFamily = AwakeSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = colors.chipUnselectedText,
                maxLines = 1
            )
            if (!isAccessibilityEnabled) {
                Spacer(modifier = Modifier.width(6.dp))
                MaterialStatusIcon(isEnabled = false, tint = colors.chipUnselectedText)
            }
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
