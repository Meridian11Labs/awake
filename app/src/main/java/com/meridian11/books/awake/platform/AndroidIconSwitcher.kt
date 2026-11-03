package com.meridian11.books.awake.platform

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.meridian11.books.awake.domain.IconSwitcherPort

/**
 * Switches the Googlebook Taskbar launcher icon between the Empty Coffee Cup
 * (`EmptyCupAlias`) and Full Coffee Cup (`FullCupAlias`).
 *
 * Enforces two critical Launcher3 / Desktop Taskbar invariants:
 * 1. Always enables the incoming `<activity-alias>` BEFORE disabling the outgoing one
 *    (and uses atomic `setComponentEnabledSettings` on API 34+).
 * 2. Self-heals if both aliases are ever disabled or both enabled.
 */
class AndroidIconSwitcher(
    private val context: Context,
    private val onComponentStateChange: ((ComponentName, Int) -> Unit)? = null
) : IconSwitcherPort {

    private val packageManager: PackageManager = context.packageManager

    override fun setCoffeeCupIcon(isFullCup: Boolean) {
        val emptyAlias = emptyCupComponent(context)
        val fullAlias = fullCupComponent(context)

        val targetToEnable = if (isFullCup) fullAlias else emptyAlias
        val targetToDisable = if (isFullCup) emptyAlias else fullAlias

        onComponentStateChange?.invoke(
            targetToEnable,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        )
        onComponentStateChange?.invoke(
            targetToDisable,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val settings = listOf(
                    PackageManager.ComponentEnabledSetting(
                        targetToEnable,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    ),
                    PackageManager.ComponentEnabledSetting(
                        targetToDisable,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                )
                packageManager.setComponentEnabledSettings(settings)
            } catch (_: Throwable) {
                // Fallback to sequential enable-before-disable below
            }
        }

        // Ensure state is applied (including on API < 34 or Robolectric ShadowPackageManager)
        if (packageManager.getComponentEnabledSetting(targetToEnable) !=
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            packageManager.getComponentEnabledSetting(targetToDisable) !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        ) {
            packageManager.setComponentEnabledSetting(
                targetToEnable,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            packageManager.setComponentEnabledSetting(
                targetToDisable,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    override fun isFullCupAliasEnabled(): Boolean {
        val fullState = packageManager.getComponentEnabledSetting(fullCupComponent(context))
        return fullState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    /**
     * Returns the currently active launcher `<activity-alias>` ComponentName so
     * right-click dynamic shortcuts can bind to the active Taskbar icon.
     */
    fun getActiveAliasComponent(): ComponentName {
        return if (isFullCupAliasEnabled()) {
            fullCupComponent(context)
        } else {
            emptyCupComponent(context)
        }
    }

    companion object {
        const val EMPTY_CUP_ALIAS_NAME = "com.meridian11.books.awake.EmptyCupAlias"
        const val FULL_CUP_ALIAS_NAME = "com.meridian11.books.awake.FullCupAlias"

        fun emptyCupComponent(context: Context): ComponentName =
            ComponentName(context.packageName, EMPTY_CUP_ALIAS_NAME)

        fun fullCupComponent(context: Context): ComponentName =
            ComponentName(context.packageName, FULL_CUP_ALIAS_NAME)
    }
}
