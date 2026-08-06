package com.minim.launcher.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.minim.launcher.MinimApplication
import com.minim.launcher.data.db.SpaceEntity
import com.minim.launcher.ui.theme.AccentOptions
import com.minim.launcher.ui.theme.MinimTheme
import kotlinx.coroutines.launch

class SpacesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MinimApplication
        val spacesRepo = app.spacesRepository
        val scope = lifecycleScope

        setContent {
            val spaces by spacesRepo.observeSpaces().collectAsState(initial = emptyList())
            var editingSpaceId by remember { mutableStateOf<String?>(null) }
            var showCreateDialog by remember { mutableStateOf(false) }
            var newSpaceName by remember { mutableStateOf("") }

            val designLanguageStr by app.settingsRepository.designLanguage.collectAsState(initial = "nothing")

            MinimTheme(
                designLanguage = com.minim.launcher.ui.theme.DesignLanguage.fromRaw(designLanguageStr)
            ) {
                Surface {
                    val editing = spaces.firstOrNull { it.id == editingSpaceId }
                    if (editing != null) {
                        SpaceEditor(
                            space = editing,
                            memberPackagesFlow = { spacesRepo.observeMembers(editing.id) },
                            onBack = { editingSpaceId = null },
                            onUpdate = { updated -> scope.launch { spacesRepo.updateSpace(updated) } },
                            onRemoveMember = { pkg -> scope.launch { spacesRepo.removeAppFromSpace(editing.id, pkg) } },
                            onDelete = {
                                scope.launch { spacesRepo.deleteSpace(editing.id) }
                                editingSpaceId = null
                            }
                        )
                    } else {
                        Column {
                            @OptIn(ExperimentalMaterial3Api::class)
                            TopAppBar(
                                title = { Text("Spaces") },
                                actions = {
                                    IconButton(onClick = { showCreateDialog = true }) {
                                        Icon(Icons.Filled.Add, contentDescription = "New space")
                                    }
                                }
                            )
                            if (spaces.isEmpty()) {
                                Text(
                                    "Spaces group apps together and can carry their own look — " +
                                        "tap + to create one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                            LazyColumn {
                                items(spaces, key = { it.id }) { space ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { editingSpaceId = space.id }
                                            .padding(horizontal = 20.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(space.name, style = MaterialTheme.typography.titleMedium)
                                            val subtitle = buildString {
                                                if (space.designLanguage != null) append("Custom look")
                                                if (space.autoActivateStartHour != null) {
                                                    if (isNotEmpty()) append(" · ")
                                                    append("Auto ${space.autoActivateStartHour}:00–${space.autoActivateEndHour}:00")
                                                }
                                                if (isEmpty()) append("Tap to configure")
                                            }
                                            Text(
                                                subtitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showCreateDialog) {
                        AlertDialog(
                            onDismissRequest = { showCreateDialog = false },
                            title = { Text("New space") },
                            text = {
                                OutlinedTextField(
                                    value = newSpaceName,
                                    onValueChange = { newSpaceName = it },
                                    label = { Text("Name") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (newSpaceName.isNotBlank()) {
                                        scope.launch {
                                            val id = spacesRepo.createSpace(newSpaceName.trim(), spaces.size)
                                            editingSpaceId = id
                                        }
                                    }
                                    newSpaceName = ""
                                    showCreateDialog = false
                                }) { Text("Create") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceEditor(
    space: SpaceEntity,
    memberPackagesFlow: () -> kotlinx.coroutines.flow.Flow<List<String>>,
    onBack: () -> Unit,
    onUpdate: (SpaceEntity) -> Unit,
    onRemoveMember: (String) -> Unit,
    onDelete: () -> Unit
) {
    val members by memberPackagesFlow().collectAsState(initial = emptyList())
    var name by remember(space.id) { mutableStateOf(space.name) }

    Column {
        @OptIn(ExperimentalMaterial3Api::class)
        TopAppBar(
            title = { Text(space.name) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete space", tint = MaterialTheme.colorScheme.error)
                }
            }
        )
        LazyColumn(modifier = Modifier.padding(horizontal = 4.dp)) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onUpdate(space.copy(name = it))
                    },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }

            item { SectionHeader("Look") }
            item {
                DesignOverrideRow(
                    current = space.designLanguage,
                    onSelect = { onUpdate(space.copy(designLanguage = it)) }
                )
            }
            if (space.designLanguage != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AccentOptions.forEach { (accentLabel, color) ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (accentLabel == space.accentName) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        shape = CircleShape
                                    )
                                    .clickable { onUpdate(space.copy(accentName = accentLabel)) }
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Auto-activate") }
            item {
                AutoActivateRow(
                    space = space,
                    onUpdate = onUpdate
                )
            }

            item { SectionHeader("Apps (${members.size})") }
            items(members, key = { it }) { pkg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(pkg, style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { onRemoveMember(pkg) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove from space")
                    }
                }
            }
            if (members.isEmpty()) {
                item {
                    Text(
                        "Add apps to this space from their long-press menu on the home screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun DesignOverrideRow(current: String?, onSelect: (String?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(null to "Inherit", "nothing" to "Nothing", "android16" to "Android 16", "glass" to "Glass")
            .forEach { (value, label) ->
                FilterChip(selected = current == value, onClick = { onSelect(value) }, label = { Text(label) })
            }
    }
}

@Composable
private fun AutoActivateRow(space: SpaceEntity, onUpdate: (SpaceEntity) -> Unit) {
    val enabled = space.autoActivateStartHour != null
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Activate automatically", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    onUpdate(
                        if (checked) space.copy(autoActivateStartHour = 9, autoActivateEndHour = 17)
                        else space.copy(autoActivateStartHour = null, autoActivateEndHour = null)
                    )
                }
            )
        }
        if (enabled) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HourStepper(
                    label = "From",
                    hour = space.autoActivateStartHour ?: 9,
                    onChange = { onUpdate(space.copy(autoActivateStartHour = it)) }
                )
                HourStepper(
                    label = "To",
                    hour = space.autoActivateEndHour ?: 17,
                    onChange = { onUpdate(space.copy(autoActivateEndHour = it)) }
                )
            }
            Text(
                "Checked when you open the launcher — not a background timer",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(((hour - 1) + 24) % 24) }) { Text("−") }
            Text("%02d:00".format(hour), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = { onChange((hour + 1) % 24) }) { Text("+") }
        }
    }
}
