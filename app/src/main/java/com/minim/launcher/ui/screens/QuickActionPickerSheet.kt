package com.minim.launcher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.minim.launcher.data.AppInfo

/**
 * Lets the user assign what swiping this app's row does. Stored as a plain
 * string in AppCacheEntity.quickAction ("call:+15555550123",
 * "sms:+15555550123", or "open" for deep-link-to-app), parsed back out in
 * MainActivity.runQuickAction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionPickerSheet(
    app: AppInfo,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var mode by remember(app.packageName) {
        mutableStateOf(
            when {
                app.quickAction == null -> "none"
                app.quickAction == "open" -> "open"
                app.quickAction.startsWith("call:") -> "call"
                app.quickAction.startsWith("sms:") -> "sms"
                else -> "none"
            }
        )
    }
    var phoneNumber by remember(app.packageName) {
        mutableStateOf(
            app.quickAction?.takeIf { it.contains(":") }?.substringAfter(":") ?: ""
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Swipe quick action for ${app.label}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            listOf("none" to "No quick action", "call" to "Call a number", "sms" to "Message a number", "open" to "Just open the app")
                .forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == value, onClick = { mode = value })
                        Text(label)
                    }
                }

            if (mode == "call" || mode == "sms") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    val action = when (mode) {
                        "call" -> if (phoneNumber.isNotBlank()) "call:$phoneNumber" else null
                        "sms" -> if (phoneNumber.isNotBlank()) "sms:$phoneNumber" else null
                        "open" -> "open"
                        else -> null
                    }
                    onSave(action)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
