package com.meridian11.books.awake.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.meridian11.books.awake.AwakeApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Trampoline Activity invoked when the user clicks the Awake Coffee Cup icon in
 * the Googlebook Taskbar or Launcher:
 * - By default (`directToggleOnTap == false`), opens the compact Frosted-Glass
 *   Popover Panel (`QuickGlassPanelActivity`) with the main modes and a Settings item
 *   that opens the full non-blurred app (`SettingsDialogActivity`).
 * - When "Hide settings, tap icon to toggle" (`directToggleOnTap == true`) is enabled,
 *   immediately toggles the Keep-Awake state (Half-Empty Cup <-> Filled Cup) with
 *   zero window rendered.
 */
class ToggleTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        suppressWindowTransitions()
        super.onCreate(savedInstanceState)

        val app = applicationContext as? AwakeApp
        if (app != null) {
            app.appScope.launch {
                try {
                    val state = app.repository.stateFlow.first()
                    if (state.directToggleOnTap) {
                        app.controller.toggle()
                    } else {
                        val glassPanelIntent = Intent(
                            this@ToggleTrampolineActivity,
                            QuickGlassPanelActivity::class.java
                        ).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                        }
                        startActivity(glassPanelIntent)
                    }
                } finally {
                    finish()
                    suppressWindowTransitions()
                }
            }
        } else {
            finish()
            suppressWindowTransitions()
        }
    }

    private fun suppressWindowTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
