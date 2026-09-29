package com.minim.launcher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * One field searches apps, contacts, and settings together (results are merged
 * by the caller) rather than three separate search surfaces — keeps the UI
 * to a single, predictable entry point.
 *
 * Deliberately NOT a Material `TextField`: the stock component brings a
 * 56dp-tall container, a focus indicator line, and its own internal padding —
 * all things that read as "generic Android app" the moment they sit next to
 * a bare text list. A `BasicTextField` costs nothing visually until content
 * is typed, matches the row typography exactly, and takes up only the space
 * its own text needs.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search"
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onBackground
    )
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp)) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
