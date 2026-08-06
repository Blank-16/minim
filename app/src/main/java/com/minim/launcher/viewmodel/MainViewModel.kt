package com.minim.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minim.launcher.data.AppInfo
import com.minim.launcher.data.db.SpaceEntity
import com.minim.launcher.data.repository.AppRepository
import com.minim.launcher.data.repository.SettingsRepository
import com.minim.launcher.data.repository.SpacesRepository
import com.minim.launcher.data.repository.SuggestionRepository
import com.minim.launcher.util.AppSearchIndex
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class HomeUiState(
    val allApps: List<AppInfo> = emptyList(),
    val favorites: List<AppInfo> = emptyList(),
    val suggestions: List<AppInfo> = emptyList(),
    val recentlyInstalled: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val filteredApps: List<AppInfo> = emptyList(),
    val showIcons: Boolean = false,
    val showSuggestions: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val iconShape: String = "circle",
    val isLoading: Boolean = true
)

class MainViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val suggestionRepository: SuggestionRepository,
    private val spacesRepository: SpacesRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val suggestionPackages = MutableStateFlow<List<String>>(emptyList())
    private val searchIndex = MutableStateFlow(AppSearchIndex.build(emptyList()))

    // Bumped on every onResume() — auto-activation windows are time-of-day
    // based, so re-evaluating only on resume (never on a timer/alarm) is
    // what keeps this from becoming a background job.
    private val resumeSignal = MutableStateFlow(0)

    fun onResume() {
        resumeSignal.value += 1
    }

    /**
     * The Space currently "in charge" of the launcher's look, if any. Manual
     * activation (picked from the profile switcher) always wins over a
     * time-window auto-match; null means "no profile active, use global
     * Settings as-is."
     */
    val activeProfile: StateFlow<SpaceEntity?> = combine(
        spacesRepository.observeSpaces(),
        settingsRepository.activeSpaceId,
        settingsRepository.autoActivateProfiles,
        resumeSignal
    ) { spaces, activeId, autoEnabled, _ ->
        spaces.firstOrNull { it.id == activeId }
            ?: if (autoEnabled) spacesRepository.resolveAutoActivated(spaces, Calendar.getInstance()) else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun activateProfile(spaceId: String?) {
        viewModelScope.launch { settingsRepository.setActiveSpaceId(spaceId ?: "") }
    }

    private val recentWindowMillis = TimeUnit.DAYS.toMillis(3)

    // Memoization guard: combine() re-runs its lambda on every emission from
    // ANY source flow, including searchQuery on every keystroke. We only want
    // to rebuild the index when the app list itself changed, so we track the
    // list instance we last indexed and skip rebuilding otherwise.
    private var lastIndexedApps: List<AppInfo>? = null

    val uiState: StateFlow<HomeUiState> = combine(
        appRepository.observeApps(),
        appRepository.observeFavorites(),
        appRepository.observeRecentlyInstalled(System.currentTimeMillis() - recentWindowMillis),
        searchQuery,
        suggestionPackages,
        settingsRepository.showIcons,
        settingsRepository.showSuggestions,
        settingsRepository.hapticFeedback,
        settingsRepository.iconShape
    ) { flows ->
        @Suppress("UNCHECKED_CAST") val allApps = flows[0] as List<AppInfo>
        @Suppress("UNCHECKED_CAST") val favorites = flows[1] as List<AppInfo>
        @Suppress("UNCHECKED_CAST") val recent = flows[2] as List<AppInfo>
        val query = flows[3] as String
        @Suppress("UNCHECKED_CAST") val suggPkgs = flows[4] as List<String>
        val showIcons = flows[5] as Boolean
        val showSuggestions = flows[6] as Boolean
        val hapticsEnabled = flows[7] as Boolean
        val iconShape = flows[8] as String

        // Rebuild the index only when the underlying app list actually changed
        // (rare — install/uninstall), never on keystrokes.
        if (allApps !== lastIndexedApps) {
            searchIndex.value = AppSearchIndex.build(allApps)
            lastIndexedApps = allApps
        }
        val index = searchIndex.value

        val appsByPackage = allApps.associateBy { it.packageName }
        val suggestions = suggPkgs.mapNotNull { appsByPackage[it] }
        val filtered = index.query(query)

        HomeUiState(
            allApps = allApps,
            favorites = favorites,
            suggestions = suggestions,
            recentlyInstalled = recent,
            searchQuery = query,
            filteredApps = filtered,
            showIcons = showIcons,
            showSuggestions = showSuggestions,
            hapticsEnabled = hapticsEnabled,
            iconShape = iconShape,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            if (appRepository.isCacheEmpty()) {
                appRepository.buildInitialCache()
            }
            refreshSuggestions()
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onAppLaunched(packageName: String) {
        viewModelScope.launch {
            suggestionRepository.recordLaunch(packageName)
            refreshSuggestions()
        }
    }

    fun onToggleFavorite(packageName: String, favorite: Boolean) {
        viewModelScope.launch { appRepository.setFavorite(packageName, favorite) }
    }

    fun onHideApp(packageName: String, hidden: Boolean) {
        viewModelScope.launch { appRepository.setHidden(packageName, hidden) }
    }

    fun onSetQuickAction(packageName: String, action: String?) {
        viewModelScope.launch { appRepository.setQuickAction(packageName, action) }
    }

    private suspend fun refreshSuggestions() {
        suggestionPackages.value = suggestionRepository.topSuggestions()
    }
}
