package com.meridian11.books.awake.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.domain.AwakeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Optional, Google Play-compliant AccessibilityService (`canRetrieveWindowContent="false"`,
 * `accessibilityEventTypes=""`, `isAccessibilityTool="false"`) that provides two hardware-backed
 * Keep-Awake mechanisms ONLY after the user accepts the in-app Prominent Disclosure:
 * 1. Attaches a `1x1` transparent `TYPE_ACCESSIBILITY_OVERLAY` view with `FLAG_KEEP_SCREEN_ON`.
 * 2. When `tapSimulationEnabled` is active, dispatches a 1ms micro-tap every X seconds via `dispatchGesture`.
 */
class KeepAwakeAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlayView: View? = null
    private var tapEngine: TapSimulatorEngine? = null

    val isOverlayAttached: Boolean
        get() = overlayView != null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        tapEngine = TapSimulatorEngine(
            scope = serviceScope,
            displayBoundsProvider = {
                val wm = getSystemService(WindowManager::class.java)
                val bounds = wm?.currentWindowMetrics?.bounds
                if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                    bounds.width() to bounds.height()
                } else {
                    val dm = resources.displayMetrics
                    dm.widthPixels to dm.heightPixels
                }
            },
            gestureDispatcher = { x, y, durationMs ->
                dispatchStationaryTap(x, y, durationMs)
            }
        )

        val app = applicationContext as? AwakeApp
        if (app != null) {
            serviceScope.launch {
                app.repository.stateFlow.collectLatest { state ->
                    applyState(state)
                }
            }
        }
    }

    fun applyState(state: AwakeState) {
        if (!state.accessibilityDisclosureAccepted) {
            // Strict Google Play Accessibility API Policy compliance:
            // Never attach TYPE_ACCESSIBILITY_OVERLAY or dispatch gestures unless
            // the user has explicitly accepted the in-app Prominent Disclosure.
            detachKeepScreenOnOverlay()
            tapEngine?.stop()
            return
        }

        if (state.isAwake) {
            if (!state.allowScreenDimming) {
                attachKeepScreenOnOverlay()
            } else {
                detachKeepScreenOnOverlay()
            }

            if (state.tapSimulationEnabled) {
                tapEngine?.start(state.tapIntervalSeconds)
            } else {
                tapEngine?.stop()
            }
        } else {
            detachKeepScreenOnOverlay()
            tapEngine?.stop()
        }
    }

    private fun attachKeepScreenOnOverlay() {
        if (overlayView != null) return
        val view = View(this)
        overlayView = view
        val wm = getSystemService(WindowManager::class.java) ?: return
        try {
            val params = WindowManager.LayoutParams(
                1,
                1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            }
            wm.addView(view, params)
        } catch (_: Throwable) {
        }
    }

    private fun detachKeepScreenOnOverlay() {
        val view = overlayView ?: return
        overlayView = null
        val wm = getSystemService(WindowManager::class.java) ?: return
        try {
            wm.removeView(view)
        } catch (_: Throwable) {
        }
    }

    private fun dispatchStationaryTap(x: Float, y: Float, durationMs: Long) {
        try {
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        } catch (_: Throwable) {
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Never reads accessibility events (canRetrieveWindowContent="false", accessibilityEventTypes="")
    }

    override fun onInterrupt() {
        tapEngine?.stop()
        detachKeepScreenOnOverlay()
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        tapEngine?.stop()
        detachKeepScreenOnOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: KeepAwakeAccessibilityService? = null
            private set

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            if (instance != null) return true
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expectedComponent =
                "${context.packageName}/${KeepAwakeAccessibilityService::class.java.name}"
            return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
        }
    }
}
