package com.minim.launcher.util

import com.minim.launcher.data.db.SpaceEntity
import com.minim.launcher.ui.theme.DesignLanguage

data class Appearance(
    val designLanguage: DesignLanguage,
    val accentName: String,
    val themeModeRaw: String
)

/**
 * Merges a Contextual Profile's style overrides (if one is active) with the
 * global Settings values — a Space's own design/accent/theme always wins
 * when set. Pure function, no Context or Compose dependency, so it's
 * trivially unit-testable — this logic used to live inline in
 * MainActivity's setContent, where it couldn't be tested at all.
 */
object AppearanceResolver {
    fun resolve(
        activeProfile: SpaceEntity?,
        globalDesignLanguageRaw: String,
        globalAccentName: String,
        globalThemeModeRaw: String
    ): Appearance {
        val designLanguageRaw = activeProfile?.designLanguage ?: globalDesignLanguageRaw
        return Appearance(
            designLanguage = DesignLanguage.fromRaw(designLanguageRaw),
            accentName = activeProfile?.accentName ?: globalAccentName,
            themeModeRaw = activeProfile?.themeMode ?: globalThemeModeRaw
        )
    }
}
