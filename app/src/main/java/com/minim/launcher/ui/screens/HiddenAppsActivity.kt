package com.minim.launcher.ui.screens

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.minim.launcher.MinimApplication
import com.minim.launcher.ui.theme.AccentOptions
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.MinimTheme
import com.minim.launcher.ui.theme.MinimThemeMode
import com.minim.launcher.util.BiometricGate
import com.minim.launcher.util.WindowChromeController
import kotlinx.coroutines.launch

class HiddenAppsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MinimApplication
        val settings = app.settingsRepository
        val scope = lifecycleScope

        setContent {
            val locked by settings.hiddenAppsLocked.collectAsState(initial = false)
            val hiddenApps by app.appRepository.observeHiddenApps().collectAsState(initial = emptyList())
            val designLanguageStr by settings.designLanguage.collectAsState(initial = "nothing")
            val themeModeStr by settings.themeMode.collectAsState(initial = "system")
            val dynamicColor by settings.dynamicColor.collectAsState(initial = false)
            val accentName by settings.accentName.collectAsState(initial = "Red")

            var authState by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(locked) {
                authState = when {
                    !locked -> true
                    !BiometricGate.isAvailable(this@HiddenAppsActivity) -> true
                    else -> BiometricGate.authenticate(this@HiddenAppsActivity)
                }
                if (authState == false) finish()
            }

            val designLanguage = DesignLanguage.fromRaw(designLanguageStr)
            val themeMode = MinimThemeMode.fromRaw(themeModeStr)
            val isDark = when (themeMode) {
                MinimThemeMode.SYSTEM -> isSystemInDarkTheme()
                MinimThemeMode.LIGHT -> false
                MinimThemeMode.DARK -> true
            }

            LaunchedEffect(designLanguage, isDark) {
                WindowChromeController.apply(this@HiddenAppsActivity, designLanguage, isDark)
            }

            MinimTheme(
                designLanguage = designLanguage,
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                accentColor = AccentOptions[accentName] ?: AccentOptions.getValue("Red")
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (designLanguage == DesignLanguage.GLASS) Color.Transparent else MaterialTheme.colorScheme.background
                ) {
                    val isGlass = designLanguage == DesignLanguage.GLASS
                    val textColor = if (isGlass) Color.Black else MaterialTheme.colorScheme.onSurface
                    val textWeight = if (isGlass) FontWeight.Bold else null

                    when (authState) {
                        true -> Column {
                            @OptIn(ExperimentalMaterial3Api::class)
                            TopAppBar(
                                title = { Text("Hidden apps", color = textColor, fontWeight = textWeight) },
                                navigationIcon = {
                                    IconButton(onClick = { finish() }) {
                                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                            )
                            if (hiddenApps.isEmpty()) {
                                Text(
                                    "No hidden apps. Hide an app from its long-press menu on the home screen.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isGlass) Color.Black.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                            LazyColumn {
                                items(hiddenApps, key = { it.packageName }) { hiddenApp ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(hiddenApp.label, style = MaterialTheme.typography.bodyLarge, color = textColor)
                                        TextButton(onClick = {
                                            scope.launch { app.appRepository.setHidden(hiddenApp.packageName, false) }
                                        }) { Text("Unhide") }
                                    }
                                }
                            }
                        }
                        else -> Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
