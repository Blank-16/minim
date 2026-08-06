package com.minim.launcher.ui.screens

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.minim.launcher.MinimApplication
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.MinimTheme
import com.minim.launcher.util.BiometricGate
import kotlinx.coroutines.launch

/**
 * FragmentActivity (not the usual ComponentActivity) because BiometricPrompt
 * requires it. Gated by the device's existing lock method — see
 * BiometricGate — rather than a launcher-specific PIN.
 */
class HiddenAppsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MinimApplication
        val scope = lifecycleScope

        setContent {
            val locked by app.settingsRepository.hiddenAppsLocked.collectAsState(initial = false)
            val hiddenApps by app.appRepository.observeHiddenApps().collectAsState(initial = emptyList())
            val designLanguageStr by app.settingsRepository.designLanguage.collectAsState(initial = "nothing")

            var authState by remember { mutableStateOf<Boolean?>(null) } // null = pending, true/false = resolved

            LaunchedEffect(locked) {
                authState = when {
                    !locked -> true
                    !BiometricGate.isAvailable(this@HiddenAppsActivity) -> true // no lock configured on device — nothing to gate with
                    else -> BiometricGate.authenticate(this@HiddenAppsActivity)
                }
                if (authState == false) finish()
            }

            MinimTheme(
                designLanguage = DesignLanguage.fromRaw(designLanguageStr)
            ) {
                Surface {
                    when (authState) {
                        true -> Column {
                            @OptIn(ExperimentalMaterial3Api::class)
                            TopAppBar(title = { Text("Hidden apps") })
                            if (hiddenApps.isEmpty()) {
                                Text(
                                    "No hidden apps. Hide an app from its long-press menu on the home screen.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
                                        Text(hiddenApp.label, style = MaterialTheme.typography.bodyLarge)
                                        TextButton(onClick = {
                                            scope.launch { app.appRepository.setHidden(hiddenApp.packageName, false) }
                                        }) { Text("Unhide") }
                                    }
                                }
                            }
                        }
                        else -> Box(modifier = Modifier.fillMaxSize()) // waiting on the prompt or finishing
                    }
                }
            }
        }
    }
}
