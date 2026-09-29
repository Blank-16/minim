package com.minim.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.minim.launcher.data.AppInfo
import com.minim.launcher.data.db.AppCacheEntity
import com.minim.launcher.data.db.MinimDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Single source of truth for "what apps exist". The UI NEVER calls PackageManager
 * directly — it observes Room via [observeApps]. PackageManager is only touched:
 *   1. On first-run full sync (buildInitialCache)
 *   2. When PackageChangeReceiver fires for a single package (syncSinglePackage)
 * This is what keeps the launcher's steady-state CPU/battery footprint near zero.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val db = MinimDatabase.get(appContext)
    private val dao = db.appCacheDao()

    fun observeApps(): Flow<List<AppInfo>> =
        dao.observeVisibleApps().map { list -> list.map { it.toAppInfo() } }

    fun observeHiddenApps(): Flow<List<AppInfo>> =
        dao.observeHiddenApps().map { list -> list.map { it.toAppInfo() } }

    fun observeFavorites(): Flow<List<AppInfo>> =
        dao.observeFavorites().map { list -> list.map { it.toAppInfo() } }

    fun observeRecentlyInstalled(sinceMillis: Long): Flow<List<AppInfo>> =
        dao.observeRecentlyInstalled(sinceMillis).map { list -> list.map { it.toAppInfo() } }

    /** Full rebuild — call only on first launch or if the cache is empty/corrupt. */
    suspend fun buildInitialCache() = withContext(Dispatchers.Default) {
        val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(launchIntent, 0)
        val entities = resolved.map { ri ->
            val ai = ri.activityInfo.applicationInfo
            AppCacheEntity(
                packageName = ri.activityInfo.packageName,
                activityClassName = ri.activityInfo.name,
                label = ri.loadLabel(pm).toString(),
                isSystemApp = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                installTimeMillis = runCatching {
                    pm.getPackageInfo(ri.activityInfo.packageName, 0).firstInstallTime
                }.getOrDefault(0L)
            )
        }
        dao.upsertAll(entities)
    }

    /**
     * Called from PackageChangeReceiver for exactly the package that changed.
     *
     * Preserves the existing row's favorite/hidden/quickAction flags when the
     * package is merely updated (not freshly installed) — `@Upsert` replaces
     * every column of a conflicting row, so building a brand-new
     * [AppCacheEntity] with its constructor defaults here would otherwise
     * silently un-favorite, un-hide, and clear the quick action of any app
     * the moment it auto-updates from the Play Store.
     */
    suspend fun syncSinglePackage(packageName: String, removed: Boolean) = withContext(Dispatchers.Default) {
        if (removed) {
            dao.deleteByPackage(packageName)
            return@withContext
        }
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return@withContext
        val resolveInfo = pm.resolveActivity(launchIntent, 0) ?: return@withContext
        val ai = resolveInfo.activityInfo.applicationInfo
        val existing = dao.getByPackage(packageName)
        dao.upsert(
            AppCacheEntity(
                packageName = packageName,
                activityClassName = resolveInfo.activityInfo.name,
                label = resolveInfo.loadLabel(pm).toString(),
                isSystemApp = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                installTimeMillis = existing?.installTimeMillis ?: runCatching {
                    pm.getPackageInfo(packageName, 0).firstInstallTime
                }.getOrDefault(0L),
                favorite = existing?.favorite ?: false,
                hidden = existing?.hidden ?: false,
                quickAction = existing?.quickAction
            )
        )
    }

    suspend fun setFavorite(packageName: String, favorite: Boolean) =
        dao.setFavorite(packageName, favorite)

    suspend fun setHidden(packageName: String, hidden: Boolean) =
        dao.setHidden(packageName, hidden)

    suspend fun setQuickAction(packageName: String, action: String?) =
        dao.setQuickAction(packageName, action)

    suspend fun isCacheEmpty(): Boolean = dao.getAllOnce().isEmpty()

    private fun AppCacheEntity.toAppInfo() = AppInfo(
        packageName = packageName,
        activityClassName = activityClassName,
        label = label,
        isSystemApp = isSystemApp,
        installTimeMillis = installTimeMillis,
        favorite = favorite,
        hidden = hidden,
        quickAction = quickAction
    )
}
