package com.dayflow.app.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex

/**
 * Long-press-and-drag reordering for short lists (targets, habits). Non-lazy on purpose: it lets
 * the list live inside a parent scroll container. Apply [handle] to the drag grip of each row.
 * [onReorder] fires once, on drop, with the new order.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    idOf: (T) -> Long,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, handle: Modifier, dragging: Boolean) -> Unit,
) {
    val working = remember { mutableStateListOf<T>() }
    val heights = remember { mutableMapOf<Long, Int>() }
    var draggedKey by remember { mutableStateOf<Long?>(null) }
    var offset by remember { mutableStateOf(0f) }
    var pending by remember { mutableStateOf<List<T>?>(null) }
    val haptic = LocalHapticFeedback.current

    // Fresh data from the database replaces the optimistic order.
    LaunchedEffect(items) { pending = null }

    val shown: List<T> = when {
        draggedKey != null -> working.toList()
        pending != null -> pending!!
        else -> items
    }

    Column(modifier) {
        shown.forEach { item ->
            val k = idOf(item)
            key(k) {
                val dragging = draggedKey == k
                val handle = Modifier.pointerInput(k) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            working.clear(); working.addAll(items)
                            draggedKey = k; offset = 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            offset += amount.y
                            val idx = working.indexOfFirst { idOf(it) == k }
                            if (idx < 0) return@detectDragGesturesAfterLongPress
                            if (offset > 0 && idx < working.lastIndex) {
                                val h = heights[idOf(working[idx + 1])] ?: 0
                                if (offset > h / 2f) {
                                    working.add(idx + 1, working.removeAt(idx)); offset -= h
                                }
                            } else if (offset < 0 && idx > 0) {
                                val h = heights[idOf(working[idx - 1])] ?: 0
                                if (-offset > h / 2f) {
                                    working.add(idx - 1, working.removeAt(idx)); offset += h
                                }
                            }
                        },
                        onDragEnd = {
                            val result = working.toList()
                            pending = result
                            draggedKey = null; offset = 0f
                            onReorder(result)
                        },
                        onDragCancel = { draggedKey = null; offset = 0f },
                    )
                }
                Box(
                    Modifier
                        .onGloballyPositioned { heights[k] = it.size.height }
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) offset else 0f }
                        .alpha(if (dragging) 0.92f else 1f)
                ) { itemContent(item, handle, dragging) }
            }
        }
    }
}
