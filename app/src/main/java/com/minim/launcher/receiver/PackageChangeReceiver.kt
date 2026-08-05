package com.minim.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.PackageInfoCompat
import com.minim.launcher.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The entire reason this launcher never needs to poll PackageManager: install/
 * uninstall/update events arrive here and we patch exactly one row in the Room
 * cache. This is what keeps steady-state CPU at ~0 between user interactions.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val removed = intent.action == Intent.ACTION_PACKAGE_REMOVED &&
            !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        val repo = AppRepository(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                repo.syncSinglePackage(packageName, removed)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
