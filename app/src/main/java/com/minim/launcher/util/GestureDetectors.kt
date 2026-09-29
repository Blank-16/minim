package com.minim.launcher.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.hypot

/**
 * One unified pointer-input state machine for every gesture the launcher
 * recognizes, instead of five independent `pointerInput` modifiers racing
 * each other on the same Box. A single gesture cycle (first finger down to
 * last finger up) is classified exactly once at its end, so a fast
 * two-finger drag can't simultaneously register as a failed pinch AND a
 * failed two-finger tap the way separate detectors could.
 *
 * Deliberately simple classification, not a general-purpose recognizer:
 * - 1 pointer, barely moved, short duration -> tap (checked against the
 *   previous tap's time for double-tap)
 * - 1 pointer, moved mostly vertically past the threshold -> swipe up/down
 * - 1 pointer, started within the edge zone and moved inward -> edge swipe
 * - 2 pointers, distance between them shrank past the threshold -> pinch in
 * - 2 pointers, barely moved, short duration -> two-finger tap
 * Anything else (two-finger drag, three+ finger gestures, slow ambiguous
 * moves) is intentionally ignored rather than guessed at.
 */
suspend fun PointerInputScope.detectAllGestures(
    onDoubleTap: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onPinchIn: () -> Unit = {},
    onTwoFingerTap: () -> Unit = {},
    onEdgeSwipeFromLeftEdge: () -> Unit = {},
    onEdgeSwipeFromRightEdge: () -> Unit = {}
) {
    val edgeWidthPx = 24.dp.toPx()
    val verticalSwipeThresholdPx = 120.dp.toPx()
    val edgeSwipeThresholdPx = 48.dp.toPx()
    val tapSlopPx = 18.dp.toPx()
    val doubleTapTimeoutMillis = 300L
    val tapMaxDurationMillis = 400L
    val multiTouchTapMaxDurationMillis = 300L
    val pinchRatioThreshold = 0.8f

    // Persists across gesture cycles (not reset each awaitEachGesture pass)
    // so consecutive taps can be compared for double-tap.
    var lastTapUpTimeMillis = 0L

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startTimeMillis = System.currentTimeMillis()
        val startX = down.position.x
        val startedAtLeftEdge = startX <= edgeWidthPx
        val startedAtRightEdge = startX >= size.width - edgeWidthPx

        val lastKnownPosition = mutableMapOf(down.id to down.position)
        var maxPointerCount = 1
        var pinchPointerIds: Pair<PointerId, PointerId>? = null
        var pinchBaselineDistance: Float? = null
        var primaryEnd = down.position
        var primaryMaxMovement = 0f
        // True once the primary pointer's change has been consumed by
        // anything — including a descendant like AppRow's tap/long-press or
        // swipe-reveal handler. Without this, two quick taps on two
        // different apps (each already handled by that AppRow) would *also*
        // satisfy the tap-timing check below and misfire the assigned
        // double-tap gesture action on top of actually opening the app.
        var primaryConsumedByDescendant = false

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val pressed = event.changes.filter { it.pressed }
            for (change in event.changes) lastKnownPosition[change.id] = change.position
            maxPointerCount = maxOf(maxPointerCount, pressed.size)

            if (pressed.size >= 2 && pinchPointerIds == null) {
                val ids = pressed.take(2).map { it.id }
                pinchPointerIds = ids[0] to ids[1]
                pinchBaselineDistance = distanceBetween(
                    lastKnownPosition.getValue(ids[0]),
                    lastKnownPosition.getValue(ids[1])
                )
            }

            val primary = event.changes.firstOrNull { it.id == down.id }
            if (primary != null) {
                // Checked on the Main pass, which visits children before
                // their parents — so by the time we see this event here (on
                // the root), a descendant's own tap/click/drag handling has
                // already had the chance to consume it.
                if (primary.isConsumed) primaryConsumedByDescendant = true
                if (primary.pressed) {
                    primaryEnd = primary.position
                    val movement = hypot(primaryEnd.x - startX, primaryEnd.y - down.position.y)
                    primaryMaxMovement = maxOf(primaryMaxMovement, movement)
                    // Once a single-finger gesture has clearly become a drag,
                    // consume so the row/list beneath doesn't also treat it as a
                    // scroll or a click.
                    if (movement > tapSlopPx) primary.consume()
                }
            }

            if (pressed.isEmpty()) break
        }

        val endTimeMillis = System.currentTimeMillis()
        val durationMillis = endTimeMillis - startTimeMillis
        val dx = primaryEnd.x - startX
        val dy = primaryEnd.y - down.position.y

        when {
            maxPointerCount >= 2 && pinchPointerIds != null && pinchBaselineDistance != null && pinchBaselineDistance > 0f -> {
                val (idA, idB) = pinchPointerIds
                val posA = lastKnownPosition[idA]
                val posB = lastKnownPosition[idB]
                val finalDistance = if (posA != null && posB != null) distanceBetween(posA, posB) else pinchBaselineDistance
                val ratio = finalDistance / pinchBaselineDistance
                when {
                    ratio < pinchRatioThreshold -> onPinchIn()
                    durationMillis < multiTouchTapMaxDurationMillis -> onTwoFingerTap()
                    else -> Unit // two-finger drag with no clear pinch — intentionally unassigned
                }
            }

            abs(dy) > verticalSwipeThresholdPx && abs(dy) > abs(dx) -> {
                if (dy > 0) onSwipeDown() else onSwipeUp()
            }

            startedAtLeftEdge && dx > edgeSwipeThresholdPx -> onEdgeSwipeFromLeftEdge()
            startedAtRightEdge && dx < -edgeSwipeThresholdPx -> onEdgeSwipeFromRightEdge()

            primaryMaxMovement < tapSlopPx && durationMillis < tapMaxDurationMillis && !primaryConsumedByDescendant -> {
                if (endTimeMillis - lastTapUpTimeMillis < doubleTapTimeoutMillis) {
                    onDoubleTap()
                    lastTapUpTimeMillis = 0L // consume the pair so a third tap doesn't chain
                } else {
                    lastTapUpTimeMillis = endTimeMillis
                }
            }
        }
    }
}

private fun distanceBetween(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)
