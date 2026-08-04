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

    fun install(app: Application, mainActivityClass: Class<*>) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                recordCrash(app, throwable)
                restartHomeActivity(app, mainActivityClass)
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

    private fun recordCrash(context: Context, throwable: Throwable) {
        val prefs = context.getSharedPreferences("minim_crash_log", Context.MODE_PRIVATE)
        val count = prefs.getInt("crash_count", 0) + 1
        prefs.edit()
            .putInt("crash_count", count)
            .putLong("last_crash_millis", System.currentTimeMillis())
            .putString("last_crash_message", throwable.message ?: throwable.javaClass.simpleName)
            .apply()
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
