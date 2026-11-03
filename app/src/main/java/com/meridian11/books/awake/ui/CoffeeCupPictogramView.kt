package com.meridian11.books.awake.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import com.meridian11.books.awake.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * Custom Material 3 Expressive animated pictogram view rendering the Coffee Cup
 * in the Pastel Blue / Pastel Orange palette:
 * - Deactive (`isAwake = false`): 50% Half-Empty cup (`fillLevel = 0.5f`) in Pastel Sky Blue, no steam.
 * - Active (`isAwake = true`): 100% Completely Filled cup (`fillLevel = 1.0f`) in Warm Pastel Orange + 2 vertical pill steam bars.
 */
class CoffeeCupPictogramView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var currentFillLevel: Float = 0.5f
        private set
    var targetFillLevel: Float = 0.5f
        private set

    private var waveAmplitudeFraction: Float = 0f
    private var wavePhaseRadians: Float = 0f
    private var fillAnimator: ValueAnimator? = null
    private val argbEvaluator = ArgbEvaluator()

    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val liquidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val cremaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val steamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val badgeRect = RectF()
    private val cupClipPath = Path()
    private val liquidWavePath = Path()
    private val handlePath = Path()

    private val fillInterpolator = PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f)
    private val drainInterpolator = PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)

    fun setAwakeState(isAwake: Boolean, animate: Boolean = true) {
        val desiredLevel = if (isAwake) 1.0f else 0.5f
        if (targetFillLevel == desiredLevel && fillAnimator?.isRunning != true) {
            currentFillLevel = desiredLevel
            invalidate()
            return
        }
        targetFillLevel = desiredLevel
        fillAnimator?.cancel()

        if (!animate) {
            currentFillLevel = desiredLevel
            waveAmplitudeFraction = 0f
            invalidate()
            return
        }

        val startLevel = currentFillLevel
        fillAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (isAwake) 450L else 350L
            interpolator = if (isAwake) fillInterpolator else drainInterpolator
            addUpdateListener { anim ->
                val f = anim.animatedFraction
                currentFillLevel = startLevel + (desiredLevel - startLevel) * (anim.animatedValue as Float)
                waveAmplitudeFraction = sin(f * PI).toFloat() * 0.06f
                wavePhaseRadians = (f * 2.0 * PI).toFloat()
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Progress t in [0..1] between Half-Empty (0.5f) and Full (1.0f)
        val t = ((currentFillLevel - 0.5f) / 0.5f).coerceIn(0f, 1f)

        val badgeEmptyColor = context.getColor(R.color.cup_badge_empty_bg)
        val badgeFullColor = context.getColor(R.color.cup_badge_full_bg)
        val liquidEmptyColor = context.getColor(R.color.cup_liquid_empty)
        val liquidFullColor = context.getColor(R.color.cup_liquid_full)
        val strokeEmptyColor = context.getColor(R.color.cup_stroke_empty)
        val strokeFullColor = context.getColor(R.color.cup_stroke_full)
        val steamColor = context.getColor(R.color.cup_steam_color)
        val cremaColor = context.getColor(R.color.cup_crema_color)

        badgePaint.color = argbEvaluator.evaluate(t, badgeEmptyColor, badgeFullColor) as Int
        liquidPaint.color = argbEvaluator.evaluate(t, liquidEmptyColor, liquidFullColor) as Int
        strokePaint.color = argbEvaluator.evaluate(t, strokeEmptyColor, strokeFullColor) as Int
        steamPaint.color = steamColor
        cremaPaint.color = cremaColor

        // 1. Squircle Badge Background
        badgeRect.set(0f, 0f, w, h)
        val badgeRadius = w * 0.30f
        canvas.drawRoundRect(badgeRect, badgeRadius, badgeRadius, badgePaint)

        // Scale 108x108 pictogram coordinates to view canvas
        val scale = w / 108f
        strokePaint.strokeWidth = 5.0f * scale
        cremaPaint.strokeWidth = 3.5f * scale
        steamPaint.strokeWidth = 5.0f * scale

        val cupLeft = 32f * scale
        val cupTop = 44f * scale
        val cupRight = 68f * scale
        val cupBottom = 74f * scale
        val cupWidth = cupRight - cupLeft
        val cupHeight = cupBottom - cupTop
        val bottomRadius = 14f * scale

        // 2. Build Cup Vessel Path
        cupClipPath.reset()
        val radii = floatArrayOf(
            2f * scale, 2f * scale,
            2f * scale, 2f * scale,
            bottomRadius, bottomRadius,
            bottomRadius, bottomRadius
        )
        cupClipPath.addRoundRect(RectF(cupLeft, cupTop, cupRight, cupBottom), radii, Path.Direction.CW)

        // 3. Draw Liquid Fill (0.5f = Half-Empty, 1.0f = Completely Filled) clipped to cup interior
        val saveCount = canvas.save()
        canvas.clipPath(cupClipPath)

        val liquidTopY = cupBottom - (cupHeight * currentFillLevel)
        val waveDy = cupHeight * waveAmplitudeFraction * sin(wavePhaseRadians)

        liquidWavePath.reset()
        liquidWavePath.moveTo(cupLeft, cupBottom)
        liquidWavePath.lineTo(cupLeft, liquidTopY)
        liquidWavePath.cubicTo(
            cupLeft + cupWidth * 0.33f, liquidTopY - waveDy,
            cupLeft + cupWidth * 0.66f, liquidTopY + waveDy,
            cupRight, liquidTopY
        )
        liquidWavePath.lineTo(cupRight, cupBottom)
        liquidWavePath.close()
        canvas.drawPath(liquidWavePath, liquidPaint)

        if (currentFillLevel < 0.65f) {
            val meniscusAlpha = ((0.65f - currentFillLevel) / 0.15f).coerceIn(0f, 1f)
            val prevAlpha = strokePaint.alpha
            strokePaint.alpha = (meniscusAlpha * 255).toInt()
            canvas.drawLine(cupLeft + 1f * scale, liquidTopY, cupRight - 1f * scale, liquidTopY, strokePaint)
            strokePaint.alpha = prevAlpha
        } else if (currentFillLevel > 0.85f) {
            val cremaAlpha = ((currentFillLevel - 0.85f) / 0.15f).coerceIn(0f, 1f)
            cremaPaint.alpha = (cremaAlpha * 255).toInt()
            canvas.drawLine(38f * scale, 49.5f * scale, 62f * scale, 49.5f * scale, cremaPaint)
        }

        canvas.restoreToCount(saveCount)

        // 4. Draw Geometric D-Handle, Cup Vessel Outline, and Saucer Pill Base
        handlePath.reset()
        handlePath.moveTo(68f * scale, 49f * scale)
        handlePath.lineTo(73f * scale, 49f * scale)
        handlePath.arcTo(RectF(66f * scale, 49f * scale, 81f * scale, 63f * scale), -90f, 180f, false)
        handlePath.lineTo(68f * scale, 63f * scale)
        canvas.drawPath(handlePath, strokePaint)

        canvas.drawPath(cupClipPath, strokePaint)
        canvas.drawLine(29f * scale, 81f * scale, 71f * scale, 81f * scale, strokePaint)

        // 5. Draw 2 Vertical Pill Steam Bars when active / filling
        val steamProgress = ((currentFillLevel - 0.65f) / 0.35f).coerceIn(0f, 1f)
        if (steamProgress > 0.01f) {
            steamPaint.alpha = (steamProgress * 255).toInt()
            val slideOffset = (1f - steamProgress) * 4f * scale
            canvas.drawLine(
                44f * scale, 36f * scale + slideOffset,
                44f * scale, 24f * scale + slideOffset,
                steamPaint
            )
            canvas.drawLine(
                56f * scale, 36f * scale + slideOffset,
                56f * scale, 24f * scale + slideOffset,
                steamPaint
            )
        }
    }

    override fun onDetachedFromWindow() {
        fillAnimator?.cancel()
        super.onDetachedFromWindow()
    }
}
