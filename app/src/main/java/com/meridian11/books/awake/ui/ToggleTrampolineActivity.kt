package com.meridian11.books.awake.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.meridian11.books.awake.AwakeApp
import com.meridian11.books.awake.platform.AndroidShortcutPublisher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Trampoline Activity invoked when the user clicks the Awake Coffee Cup icon in
 * the Googlebook Taskbar or Launcher:
 * - By default (`directToggleOnTap == false`), opens the adaptive Googlebook
 *   Settings / Control window (`SettingsDialogActivity`).
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
                        val settingsIntent = Intent(
                            this@ToggleTrampolineActivity,
                            SettingsDialogActivity::class.java
                        ).apply {
                            action = AndroidShortcutPublisher.ACTION_OPEN_SETTINGS
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                        }
                        startActivity(settingsIntent)
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
