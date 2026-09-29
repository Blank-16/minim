package com.minim.launcher.util

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import kotlin.system.exitProcess

/**
 * A launcher crashing is uniquely bad: the user loses their home screen, not
 * just one app. This installs a default uncaught-exception handler that logs
 * the failure, then relaunches MainActivity in a fresh task rather than
 * letting Android fall back to the *next* installed launcher (which is a
 * confusing, hard-to-reverse experience for most people).
 */
object CrashGuard {

    private const val LOOP_WINDOW_MILLIS = 3_000L
    private const val LOOP_THRESHOLD = 3

    fun install(app: Application, mainActivityClass: Class<*>) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // If MainActivity itself crashes on startup (a bad DB migration, a
            // corrupted preference, etc.), naively restarting it here would
            // crash again immediately, forever — the one thing worse than a
            // launcher crashing once is a launcher stuck flickering in an
            // infinite crash loop that the user can't back out of. Once 3
            // crashes land within a 3s window, stop auto-restarting and fall
            // through to the platform's own handling instead.
            val isLooping = runCatching { recordCrash(app, throwable) }.getOrDefault(true)
            if (!isLooping) {
                runCatching { restartHomeActivity(app, mainActivityClass) }
            }
            // Still let the platform know the process died abnormally, but only
            // after we've queued the restart intent — otherwise some OEM skins
            // race us back to their own launcher first.
            previousHandler?.uncaughtException(thread, throwable) ?: run {
                Process.killProcess(Process.myPid())
                exitProcess(1)
            }
        }
    }

    /** Returns true if this crash is part of a tight loop and auto-restart should be skipped this time. */
    private fun recordCrash(context: Context, throwable: Throwable): Boolean {
        val prefs = context.getSharedPreferences("minim_crash_log", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCrashMillis = prefs.getLong("last_crash_millis", 0L)
        val loopCount = if (now - lastCrashMillis < LOOP_WINDOW_MILLIS) {
            prefs.getInt("loop_count", 0) + 1
        } else {
            1
        }

        prefs.edit()
            .putInt("crash_count", prefs.getInt("crash_count", 0) + 1)
            .putInt("loop_count", loopCount)
            .putLong("last_crash_millis", now)
            .putString("last_crash_message", throwable.message ?: throwable.javaClass.simpleName)
            .apply()

        return loopCount >= LOOP_THRESHOLD
    }

    private fun restartHomeActivity(context: Context, mainActivityClass: Class<*>) {
        val intent = Intent(context, mainActivityClass).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        context.startActivity(intent)
    }
}
