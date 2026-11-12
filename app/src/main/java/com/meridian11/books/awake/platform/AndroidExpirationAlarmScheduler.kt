package com.meridian11.books.awake.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.meridian11.books.awake.domain.ExpirationAlarmPort
import com.meridian11.books.awake.receiver.SessionExpirationReceiver

/**
 * Schedules OS-level wakeup alarms in the `ELAPSED_REALTIME_WAKEUP` clock domain
 * using [AlarmManager.setWindow] (requiring zero exact-alarm permissions on Android 12+)
 * so timed Keep-Awake sessions expire reliably even if the app process was cached or reclaimed.
 */
class AndroidExpirationAlarmScheduler(
    private val context: Context
) : ExpirationAlarmPort {

    override fun scheduleExpirationAlarm(triggerAtElapsedMillis: Long) {
        if (triggerAtElapsedMillis <= 0L) {
            cancelExpirationAlarm()
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EXPIRATION_ALARM,
            createIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            alarmManager.setWindow(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtElapsedMillis,
                EXPIRATION_WINDOW_MS,
                pendingIntent
            )
        }
    }

    override fun cancelExpirationAlarm() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val existing = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EXPIRATION_ALARM,
            createIntent(context),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            runCatching { alarmManager?.cancel(existing) }
            existing.cancel()
        }
    }

    companion object {
        private const val REQUEST_CODE_EXPIRATION_ALARM = 501
        private const val EXPIRATION_WINDOW_MS = 5_000L

        fun createIntent(context: Context): Intent =
            Intent(context, SessionExpirationReceiver::class.java).apply {
                action = SessionExpirationReceiver.ACTION_SESSION_EXPIRED
            }
    }
}
