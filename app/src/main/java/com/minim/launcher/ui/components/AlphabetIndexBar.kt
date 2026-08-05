package com.minim.launcher.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LETTERS = listOf('#') + ('A'..'Z')

/**
 * Fast-scroll index strip. Dragging vertically anywhere on the strip jumps the
 * list to that letter — this, plus the text-only list, is what makes a 300-app
 * list feel instant without any icon-grid pagination.
 */
@Composable
fun AlphabetIndexBar(
    availableLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .pointerInput(availableLetters) {
                detectVerticalDragGestures { change, _ ->
                    val index = (change.position.y / size.height * LETTERS.size)
                        .toInt()
                        .coerceIn(0, LETTERS.size - 1)
                    onLetterSelected(LETTERS[index])
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LETTERS.forEach { letter ->
            val enabled = letter in availableLetters
            Text(
                text = letter.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                },
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}
