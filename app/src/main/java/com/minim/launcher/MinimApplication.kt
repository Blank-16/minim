package com.minim.launcher

import android.app.Application
import com.minim.launcher.data.repository.AppRepository
import com.minim.launcher.data.repository.GestureRepository
import com.minim.launcher.data.repository.IconLoader
import com.minim.launcher.data.repository.SettingsRepository
import com.minim.launcher.data.repository.SpacesRepository
import com.minim.launcher.data.repository.SuggestionRepository
import com.minim.launcher.util.CrashGuard

/**
 * No dependency-injection framework on purpose — Hilt/Dagger add real APK size
 * and generated-code surface for what is, here, five cheap singletons. A
 * launcher benefits more from a small, auditable footprint than DI ceremony.
 */
class MinimApplication : Application() {

    lateinit var appRepository: AppRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var suggestionRepository: SuggestionRepository
        private set
    lateinit var spacesRepository: SpacesRepository
        private set
    lateinit var gestureRepository: GestureRepository
        private set
    lateinit var iconLoader: IconLoader
        private set

    override fun onCreate() {
        super.onCreate()
        // Installed first, before anything else can throw during init — a
        // launcher crashing means the user loses their home screen entirely,
        // so recovery matters more here than in a typical app.
        CrashGuard.install(this, MainActivity::class.java)

        appRepository = AppRepository(this)
        settingsRepository = SettingsRepository(this)
        suggestionRepository = SuggestionRepository(this)
        spacesRepository = SpacesRepository(this)
        gestureRepository = GestureRepository(this)
        iconLoader = IconLoader(this)
    }
}
