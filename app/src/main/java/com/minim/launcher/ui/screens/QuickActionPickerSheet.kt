package com.minim.launcher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.minim.launcher.data.AppInfo
import com.minim.launcher.ui.components.AdaptiveSurface
import com.minim.launcher.ui.theme.DesignLanguage
import com.minim.launcher.ui.theme.LocalDesignLanguage

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

    val language = LocalDesignLanguage.current
    val isGlass = language == DesignLanguage.GLASS
    val textColor = if (isGlass) Color.Black else MaterialTheme.colorScheme.onSurface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surface
    ) {
        val content = @Composable {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Swipe quick action for ${app.label}", style = MaterialTheme.typography.titleMedium, color = textColor)
                Spacer(modifier = Modifier.height(12.dp))

                listOf("none" to "No quick action", "call" to "Call a number", "sms" to "Message a number", "open" to "Just open the app")
                    .forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { mode = value }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = mode == value,
                                onClick = { mode = value },
                                colors = if (isGlass) RadioButtonDefaults.colors(
                                    selectedColor = Color.Black,
                                    unselectedColor = Color.Black.copy(alpha = 0.6f)
                                ) else RadioButtonDefaults.colors()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = textColor)
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

        if (isGlass) {
            AdaptiveSurface { content() }
        } else {
            content()
        }
    }
}
