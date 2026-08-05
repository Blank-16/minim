package com.minim.launcher.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * One field searches apps, contacts, and settings together (results are merged
 * by the caller) rather than three separate search surfaces — keeps the UI
 * to a single, predictable entry point.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search apps, contacts, settings"
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.padding(horizontal = 16.dp),
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}
