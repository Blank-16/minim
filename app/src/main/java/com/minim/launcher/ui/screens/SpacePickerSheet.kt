package com.minim.launcher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minim.launcher.data.AppInfo
import com.minim.launcher.data.db.SpaceEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpacePickerSheet(
    app: AppInfo,
    spaces: List<SpaceEntity>,
    onAddToSpace: (spaceId: String) -> Unit,
    onCreateAndAdd: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "Add ${app.label} to a space",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            spaces.forEach { space ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddToSpace(space.id); onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(space.name, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (!showCreate) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreate = true }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("New space")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Space name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (newName.isNotBlank()) {
                            onCreateAndAdd(newName.trim())
                            onDismiss()
                        }
                    }) { Text("Add") }
                }
            }
        }
    }
}
