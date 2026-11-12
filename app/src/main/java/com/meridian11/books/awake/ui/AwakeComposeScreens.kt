package com.meridian11.books.awake.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.meridian11.books.awake.R
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    val switchThumbHeroOn: Color,
    val switchThumbOff: Color,
    val cupBadgeEmptyBg: Color,
    val cupBadgeFullBg: Color,
    val cupIconTintInactive: Color,
    val cupIconTintActive: Color
)

/**
 * Single source of truth for Awake's Light & Dark Material 3 Expressive palette,
 * resolved directly from `res/values/colors.xml` and `res/values-night/colors.xml`
 * so Compose and AppWidget XML surfaces always match 1:1.
 */
@Composable
fun rememberAwakeThemeColors(): AwakeThemeColors {
    return AwakeThemeColors(
        windowBg = colorResource(R.color.window_bg),
        cardBg = colorResource(R.color.surface_card_bg),
        cardStroke = colorResource(R.color.surface_card_stroke),
        tonalTrackBg = colorResource(R.color.surface_tonal_bg),
        heroInactiveBg = colorResource(R.color.hero_card_inactive_bg),
        heroActiveBg = colorResource(R.color.hero_card_active_bg),
        textPrimary = colorResource(R.color.dialog_text_primary),
        textSecondary = colorResource(R.color.dialog_text_secondary),
        chipSelectedBg = colorResource(R.color.chip_selected_bg),
        chipSelectedText = colorResource(R.color.chip_selected_text),
        chipUnselectedText = colorResource(R.color.chip_unselected_text),
        switchTrackOn = colorResource(R.color.switch_track_on),
        switchTrackHeroOn = colorResource(R.color.switch_track_hero_on),
        switchTrackOff = colorResource(R.color.switch_track_off),
        switchTrackOffStroke = colorResource(R.color.switch_track_off_stroke),
        switchThumbOn = colorResource(R.color.switch_thumb_on),
        switchThumbHeroOn = colorResource(R.color.switch_thumb_hero_on),
        switchThumbOff = colorResource(R.color.switch_thumb_off),
        cupBadgeEmptyBg = colorResource(R.color.cup_badge_empty_bg),
        cupBadgeFullBg = colorResource(R.color.cup_badge_full_bg),
        cupIconTintInactive = colorResource(R.color.cup_stroke_empty),
        cupIconTintActive = colorResource(R.color.cup_stroke_full)
    )
}

@Composable
fun AwakeMaterialTheme(
    colors: AwakeThemeColors,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.chipSelectedBg,
            onPrimary = colors.chipSelectedText,
            secondary = colors.switchTrackHeroOn,
            onSecondary = colors.switchThumbHeroOn,
            background = colors.windowBg,
            onBackground = colors.textPrimary,
            surface = colors.cardBg,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.tonalTrackBg,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.cardStroke
        )
    } else {
        lightColorScheme(
            primary = colors.chipSelectedBg,
            onPrimary = colors.chipSelectedText,
            secondary = colors.switchTrackHeroOn,
            onSecondary = colors.switchThumbHeroOn,
            background = colors.windowBg,
            onBackground = colors.textPrimary,
            surface = colors.cardBg,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.tonalTrackBg,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.cardStroke
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
            content()
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

    val vesselPath = remember { Path() }
    val liquidWavePath = remember { Path() }
    val cupHandlePath = remember { Path() }

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

        vesselPath.reset()
        vesselPath.addRoundRect(
            RoundRect(
                rect = Rect(cupLeft, cupTop, cupRight, cupBottom),
                topLeft = CornerRadius.Zero,
                topRight = CornerRadius.Zero,
                bottomRight = CornerRadius(cupCorner, cupCorner),
                bottomLeft = CornerRadius(cupCorner, cupCorner)
            )
        )

        // 3. Clipped Single-Tone Material Themed Liquid Fill (0.5f = Half-Empty, 1.0f = Filled)
        clipPath(vesselPath) {
            val cupHeight = cupBottom - cupTop
            val liquidTop = cupBottom - cupHeight * fillLevel.coerceIn(0f, 1f)
            val waveAmp = if (fillLevel in 0.52f..0.98f) 2.2f * s else 0f
            val midX = (cupLeft + cupRight) / 2f
            val waveOffset = sin(fillLevel * Math.PI * 2.0).toFloat() * waveAmp

            liquidWavePath.reset()
            liquidWavePath.moveTo(cupLeft, cupBottom)
            liquidWavePath.lineTo(cupLeft, liquidTop)
            liquidWavePath.cubicTo(
                cupLeft + (midX - cupLeft) * 0.5f,
                liquidTop - waveOffset,
                midX + (cupRight - midX) * 0.5f,
                liquidTop + waveOffset,
                cupRight,
                liquidTop
            )
            liquidWavePath.lineTo(cupRight, cupBottom)
            liquidWavePath.close()
            drawPath(path = liquidWavePath, color = iconTint)

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
            path = vesselPath,
            color = iconTint,
            style = Stroke(width = 4.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 5. Cup Handle
        cupHandlePath.reset()
        cupHandlePath.moveTo(66f * s, 42f * s)
        cupHandlePath.lineTo(72f * s, 42f * s)
        cupHandlePath.arcTo(
            rect = Rect(65f * s, 42f * s, 79f * s, 60f * s),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        cupHandlePath.lineTo(66f * s, 60f * s)
        drawPath(
            path = cupHandlePath,
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
 * Custom Material 3 Expressive 52dp x 32dp Pill Switch in Jetpack Compose
 * with true [Modifier.toggleable] `ToggleableState` semantics, keyboard/hover indication,
 * and M3 Expressive spring thumb morph (`18dp` -> `22dp`).
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
    val activeThumbColor = if (useHeroOrangeWhenChecked) colors.switchThumbHeroOn else colors.switchThumbOn
    val trackColor by animateColorAsState(
        targetValue = if (checked) activeTrackColor else colors.switchTrackOff,
        animationSpec = tween(durationMillis = 200),
        label = "switchTrackColor"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) activeThumbColor else colors.switchThumbOff,
        animationSpec = tween(durationMillis = 200),
        label = "switchThumbColor"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (checked) 22.dp else 18.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "switchThumbSize"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 25.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "switchThumbOffset"
    )

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(CircleShape)
            .background(trackColor)
            .then(
                if (!checked) {
                    Modifier.border(2.dp, colors.switchTrackOffStroke, CircleShape)
                } else {
                    Modifier
                }
            )
            .toggleable(
                value = checked,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

/**
 * Single-line Segmented Pill Button Track used for Duration (`Infinite | 30m | 1h | 2h`).
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
            .selectableGroup()
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
                    .heightIn(min = 42.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(value) }
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp)
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

enum class LegalSubpageTab {
    ABOUT,
    PRIVACY,
    TERMS
}

/**
 * Full Googlebook App Window (`SettingsDialogActivity`) built in Jetpack Compose.
 * - Preserves the opaque, non-blurred Material 3 Expressive Pastel Blue / Pastel Orange
 *   design with a seamless window header (`captionBar`) and an outline-free
 *   blue/orange Hero Toggle Container.
 */
@Composable
fun SettingsWindowScreen(
    state: AwakeState,
    onToggleAwake: () -> Unit,
    onSelectDuration: (Int) -> Unit,
    onToggleDirectTap: (Boolean) -> Unit,
    onAddQsTile: () -> Unit,
    onPinWidget: () -> Unit,
    onUpdateGuardrails: (
        lidClose: Boolean,
        lowBattery: Boolean,
        dimming: Boolean
    ) -> Unit,
    onCloseWindow: () -> Unit
) {
    val colors = rememberAwakeThemeColors()
    val windowInsetsPadding = WindowInsets.systemBars
        .union(WindowInsets.captionBar)
        .asPaddingValues()

    var activeLegalTab by rememberSaveable { mutableStateOf<LegalSubpageTab?>(null) }

    BackHandler(enabled = activeLegalTab != null) {
        activeLegalTab = null
    }

    AwakeMaterialTheme(colors = colors) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.windowBg)
                .padding(windowInsetsPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val isTwoColumn = maxWidth >= 800.dp

            val currentLegalTab = activeLegalTab
            if (currentLegalTab != null) {
                LegalAndAboutSubpageScreen(
                    selectedTab = currentLegalTab,
                    colors = colors,
                    onSelectTab = { activeLegalTab = it },
                    onBack = { activeLegalTab = null }
                )
            } else if (isTwoColumn) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .widthIn(max = 1040.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HeroToggleCard(state = state, colors = colors, onToggleAwake = onToggleAwake)
                            DurationPresetsCard(state = state, colors = colors, onSelectDuration = onSelectDuration)
                            QsAndWidgetCard(colors = colors, onAddQsTile = onAddQsTile, onPinWidget = onPinWidget)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DirectToggleCard(state = state, colors = colors, onToggleDirectTap = onToggleDirectTap)
                            GuardrailsCard(
                                state = state,
                                colors = colors,
                                onUpdateGuardrails = onUpdateGuardrails,
                                onCloseWindow = onCloseWindow
                            )
                        }
                    }
                    LegalFooterRow(
                        colors = colors,
                        onOpenTab = { activeLegalTab = it }
                    )
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroToggleCard(state = state, colors = colors, onToggleAwake = onToggleAwake)
                    DurationPresetsCard(state = state, colors = colors, onSelectDuration = onSelectDuration)
                    DirectToggleCard(state = state, colors = colors, onToggleDirectTap = onToggleDirectTap)
                    QsAndWidgetCard(colors = colors, onAddQsTile = onAddQsTile, onPinWidget = onPinWidget)
                    GuardrailsCard(
                        state = state,
                        colors = colors,
                        onUpdateGuardrails = onUpdateGuardrails,
                        onCloseWindow = onCloseWindow
                    )
                    LegalFooterRow(
                        colors = colors,
                        onOpenTab = { activeLegalTab = it }
                    )
                }
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var nowElapsedMillis by remember(state.isAwake, state.sessionExpiresAtEpochMillis) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }
    if (state.isAwake && state.sessionExpiresAtEpochMillis > 0L) {
        LaunchedEffect(lifecycleOwner, state.sessionExpiresAtEpochMillis) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    nowElapsedMillis = SystemClock.elapsedRealtime()
                    delay(1_000L)
                }
            }
        }
    }

    val durationLabel = AndroidShortcutPublisher.formatDurationLong(state.durationMinutes)
    val activeCountdownText = if (
        state.isAwake &&
        state.sessionExpiresAtEpochMillis > 0L &&
        nowElapsedMillis > 0L &&
        state.sessionExpiresAtEpochMillis > nowElapsedMillis
    ) {
        formatLiveRemaining(state.sessionExpiresAtEpochMillis - nowElapsedMillis)
    } else {
        durationLabel
    }

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
            .clickable(role = Role.Button) { onToggleAwake() }
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("tv_dialog_title")
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (state.isAwake) {
                    "Filled Cup ($activeCountdownText) • Preventing Googlebook sleep"
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

private fun formatLiveRemaining(remainingMillis: Long): String {
    val totalSeconds = ((remainingMillis.coerceAtLeast(0L) + 999L) / 1_000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> "${hours}h ${minutes}m left"
        minutes > 0L -> "${minutes}m ${seconds.toString().padStart(2, '0')}s left"
        else -> "${seconds}s left"
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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
            .clickable { onToggleDirectTap(!state.directToggleOnTap) }
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Quick Settings Tile: Tap system tray tile to toggle Awake, or long-press for Settings. Widget: Starts 1×1 by default — expand larger for duration controls.",
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
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .background(colors.chipSelectedBg)
                    .clickable(role = Role.Button) { onAddQsTile() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .background(colors.tonalTrackBg)
                    .clickable(role = Role.Button) { onPinWidget() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Smart Googlebook Guardrails",
            fontFamily = AwakeSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .background(colors.chipSelectedBg)
                    .clickable(role = Role.Button) { onCloseWindow() }
                    .padding(horizontal = 26.dp, vertical = 10.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
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

/**
 * Subtle, centered wrapping footer row at the bottom of the main Settings window providing
 * one-click access to About, Privacy Policy, and Terms of Service.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LegalFooterRow(
    colors: AwakeThemeColors,
    onOpenTab: (LegalSubpageTab) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        SubtleFooterLink(
            label = "About",
            testTag = "link_footer_about",
            colors = colors,
            onClick = { onOpenTab(LegalSubpageTab.ABOUT) }
        )
        FooterDot(colors)
        SubtleFooterLink(
            label = "Privacy Policy",
            testTag = "link_footer_privacy",
            colors = colors,
            onClick = { onOpenTab(LegalSubpageTab.PRIVACY) }
        )
        FooterDot(colors)
        SubtleFooterLink(
            label = "Terms of Service",
            testTag = "link_footer_terms",
            colors = colors,
            onClick = { onOpenTab(LegalSubpageTab.TERMS) }
        )
        FooterDot(colors)
        Box(
            modifier = Modifier.heightIn(min = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "© Meridian11 LLC",
                fontFamily = AwakeSansFontFamily,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun SubtleFooterLink(
    label: String,
    testTag: String,
    colors: AwakeThemeColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = AwakeSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun FooterDot(colors: AwakeThemeColors) {
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .padding(horizontal = 2.dp)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "•",
            fontFamily = AwakeSansFontFamily,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
    }
}

/**
 * In-App Subpage for About, Privacy Policy, and Terms of Service owned by Meridian11 LLC.
 */
@Composable
private fun LegalAndAboutSubpageScreen(
    selectedTab: LegalSubpageTab,
    colors: AwakeThemeColors,
    onSelectTab: (LegalSubpageTab) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(selectedTab) {
        scrollState.scrollTo(0)
    }

    Column(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("screen_legal_subpage"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Navigation Bar (Pinned at top)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 42.dp)
                    .clip(CircleShape)
                    .background(colors.tonalTrackBg)
                    .clickable(role = Role.Button) { onBack() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("btn_legal_back"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Back to Awake",
                    fontFamily = AwakeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colors.chipUnselectedText
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.heroInactiveBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Meridian11 LLC",
                    fontFamily = AwakeSansFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = colors.textPrimary
                )
            }
        }

        // Segmented Subpage Tab Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .clip(CircleShape)
                .background(colors.tonalTrackBg)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                Triple(LegalSubpageTab.ABOUT, "About", "tab_legal_about"),
                Triple(LegalSubpageTab.PRIVACY, "Privacy Policy", "tab_legal_privacy"),
                Triple(LegalSubpageTab.TERMS, "Terms of Service", "tab_legal_terms")
            )
            tabs.forEach { (tab, title, tag) ->
                val isSelected = selectedTab == tab
                val bg = if (isSelected) colors.chipSelectedBg else Color.Transparent
                val fg = if (isSelected) colors.chipSelectedText else colors.chipUnselectedText
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onSelectTab(tab) }
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .testTag(tag),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontFamily = AwakeSansFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Scrollable Content Card (Fixed outer clip & border, inner verticalScroll + padding)
        val cardShape = RoundedCornerShape(26.dp)
        SelectionContainer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .clip(cardShape)
                .background(colors.cardBg)
                .border(1.dp, colors.cardStroke, cardShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    LegalSubpageTab.ABOUT -> {
                        Text(
                            text = "Awake for Googlebooks",
                            fontFamily = AwakeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.textPrimary
                        )
                        val context = LocalContext.current
                        val versionName = remember(context) {
                            runCatching {
                                context.packageManager.getPackageInfo(context.packageName, 0).versionName
                            }.getOrNull() ?: "?"
                        }
                        Text(
                            text = "Version $versionName • Package: ${context.packageName}",
                            fontFamily = AwakeSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "Owned, designed, and built by Meridian11 LLC. Copyright © 2026 Meridian11 LLC. All rights reserved.",
                            fontFamily = AwakeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.testTag("tv_meridian11_ownership")
                        )
                        Text(
                            text = "Awake keeps your Googlebook awake while you need it. Turn it on from the taskbar icon, the Quick Settings tile, or the widget, and it holds a wake lock until you turn it off, the timer ends, or a guardrail (lid closed, low battery) turns it off for you. It needs no internet connection.",
                            fontFamily = AwakeSansFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "Support: meridian11labs@gmail.com • github.com/Meridian11Labs/awake",
                            fontFamily = AwakeSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = colors.textPrimary
                        )
                    }

                    LegalSubpageTab.PRIVACY -> {
                        Text(
                            text = "Privacy Policy — Meridian11 LLC",
                            fontFamily = AwakeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Effective Date: November 11, 2026 • Contact: meridian11labs@gmail.com",
                            fontFamily = AwakeSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "No internet access\n" +
                                "Awake does not declare the internet permission. It cannot transmit anything off your Googlebook.\n\n" +
                                "No data collected or shared\n" +
                                "Awake collects no personal data and shares nothing with Meridian11 LLC or any third party. There are no ads, analytics, or crash reporting SDKs.\n\n" +
                                "Settings stay strictly on your device\n" +
                                "Your preferences (on or off, timer preset, guardrails) are stored locally on this Googlebook only (cloud backup and device transfer are disabled) and are permanently deleted when you uninstall Awake. While Awake is active, it checks battery level, charger state, and display state locally in memory to apply your guardrails; none of this is stored or sent anywhere.\n\n" +
                                "Permissions used locally\n" +
                                "• WAKE_LOCK & FOREGROUND_SERVICE_SPECIAL_USE: Holds CPU and display wake locks while toggled ON.\n" +
                                "• POST_NOTIFICATIONS: Shows the ongoing status notification with Turn Off and +30m controls.\n" +
                                "• RECEIVE_BOOT_COMPLETED: Resets Awake to normal sleep after a device reboot.\n\n" +
                                "This policy is also published at https://meridian11.com/privacy/awake. Questions: meridian11labs@gmail.com.",
                            fontFamily = AwakeSansFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.testTag("tv_legal_privacy_body")
                        )
                    }

                    LegalSubpageTab.TERMS -> {
                        Text(
                            text = "Terms of Service — Meridian11 LLC",
                            fontFamily = AwakeSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Effective Date: November 11, 2026 • Owner: Meridian11 LLC",
                            fontFamily = AwakeSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "1. License\n" +
                                "Awake is owned by Meridian11 LLC. You may use it on your compatible Googlebook devices under a personal, non-exclusive, non-transferable license.\n\n" +
                                "2. Battery and heat\n" +
                                "Keeping a screen and processor awake uses battery and generates heat. We recommend keeping 'Backpack Safety' and 'Battery Guard' turned on. You are responsible for your device's battery, temperature, and screen.\n\n" +
                                "3. No warranty\n" +
                                "THE APPLICATION IS PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND. TO THE MAXIMUM EXTENT PERMITTED BY LAW, MERIDIAN11 LLC SHALL NOT BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR HARDWARE DAMAGES ARISING OUT OF YOUR USE OF THE APPLICATION.\n\n" +
                                "Questions: meridian11labs@gmail.com",
                            fontFamily = AwakeSansFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.testTag("tv_legal_terms_body")
                        )
                    }
                }
            }
        }
    }
}
