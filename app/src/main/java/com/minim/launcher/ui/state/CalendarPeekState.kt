package com.minim.launcher.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.minim.launcher.util.CalendarPeek
import kotlinx.coroutines.delay

/**
 * Foreground-only refresh loop, not a background job — it runs as long as
 * this composition is alive (i.e. the launcher is visible) and simply stops
 * when it isn't. Extracted from MainActivity so the polling mechanics don't
 * clutter setContent.
 */
@Composable
fun rememberCalendarPeekText(showCalendarPeek: Boolean): String? {
    val context = LocalContext.current
    var text by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showCalendarPeek) {
        if (!showCalendarPeek) {
            text = null
            return@LaunchedEffect
        }
        while (true) {
            text = CalendarPeek.nextEventLine(context)
            delay(15 * 60 * 1000L)
        }
    }

    return text
}
