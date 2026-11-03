package com.meridian11.books.awake.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.domain.AwakeState
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Compact Frosted-Glass Popover Panel opened when clicking the Taskbar / Launcher Coffee Cup icon
 * (when `directToggleOnTap == false`).
 *
 * Uses `Theme.Awake.GlassPanel` (`windowIsFloating = true`, `windowIsTranslucent = true`,
 * `windowBackgroundBlurRadius = 64dp`) so the background blur is applied strictly behind
 * this compact glass panel while the full app (`SettingsDialogActivity`) remains opaque.
 */
class QuickGlassPanelActivity : AppCompatActivity() {

    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var isGlassBlurConfigured: Boolean = false
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureFrostedGlassBlur()

        val app = applicationContext as? AwakeApp
        setContent {
            val state by if (app != null) {
                app.repository.stateFlow.collectAsState(initial = AwakeState())
            } else {
                androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(AwakeState()) }
            }

            QuickGlassPanelScreen(
                state = state,
                onToggleAwake = {
                    if (app != null) {
                        uiScope.launch {
                            app.controller.toggle()
                        }
                    }
                },
                onSelectMode = { minutes ->
                    if (app != null) {
                        uiScope.launch {
                            app.controller.enableWithDuration(minutes)
                        }
                    }
                },
                onOpenFullSettings = {
                    openFullSettingsAndClose()
                },
                onDismiss = {
                    finish()
                }
            )
        }
        configureFrostedGlassBlur()
    }

    private fun configureFrostedGlassBlur() {
        isGlassBlurConfigured = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                window.setBackgroundDrawableResource(com.meridian11.books.awake.R.drawable.bg_glass_panel)
                val blurPx = (80 * resources.displayMetrics.density).toInt().coerceAtLeast(100)
                window.setBackgroundBlurRadius(blurPx)
                window.decorView.post {
                    try {
                        window.setBackgroundBlurRadius(blurPx)
                    } catch (_: Throwable) {
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun openFullSettingsAndClose() {
        val settingsIntent = Intent(this, SettingsDialogActivity::class.java).apply {
            action = AndroidShortcutPublisher.ACTION_OPEN_SETTINGS
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        startActivity(settingsIntent)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        uiScope.cancel()
        super.onDestroy()
    }
}
