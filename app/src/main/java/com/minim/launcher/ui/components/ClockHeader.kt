package com.minim.launcher.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Always-on clock/date. Ticks once a minute via a suspend loop, not a
 * Handler/BroadcastReceiver on ACTION_TIME_TICK, so it costs nothing while
 * backgrounded. Type treatment follows the active design language: monospace
 * digits for Nothing, bold Material for Android 16, light SF-style for Glass.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockHeader(
    modifier: Modifier = Modifier,
    activeProfileName: String? = null,
    infoLine: String? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val language = LocalDesignLanguage.current
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            val millisToNextMinute = 60_000 - (System.currentTimeMillis() % 60_000)
            delay(millisToNextMinute)
        }
    }

    val timeFormat = remember { SimpleDateFormat("h:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val (family, weight) = when (language) {
        DesignLanguage.NOTHING -> FontFamily.Monospace to FontWeight.Normal
        DesignLanguage.ANDROID_16 -> FontFamily.Default to FontWeight.Bold
        DesignLanguage.GLASS -> FontFamily.Default to FontWeight.Light
    }
    val dateText = if (language == DesignLanguage.NOTHING) {
        dateFormat.format(now).uppercase()
    } else {
        dateFormat.format(now)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = timeFormat.format(now),
            fontSize = 72.sp,
            fontWeight = weight,
            fontFamily = family,
            letterSpacing = (-1).sp,
            lineHeight = 72.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        if (!infoLine.isNullOrBlank()) {
            Text(
                text = infoLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (activeProfileName != null) {
            Text(
                text = (if (language == DesignLanguage.NOTHING) "PROFILE: " else "Profile: ") +
                    (if (language == DesignLanguage.NOTHING) activeProfileName.uppercase() else activeProfileName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
