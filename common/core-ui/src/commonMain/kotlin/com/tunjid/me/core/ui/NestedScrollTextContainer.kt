package com.tunjid.me.core.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun NestedScrollTextContainer(
    modifier: Modifier = Modifier,
    canConsumeScrollEvents: Boolean,
    onScrolled: (Float) -> Float,
    onPointerInput: suspend PointerInputScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        content()
        // Overlay a box over the TextField to intercept scroll events
        Spacer(
            modifier = Modifier
                .fillMaxSize(if (canConsumeScrollEvents) 0f else 1f)
                .scrollable(
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                    enabled = !canConsumeScrollEvents,
                    // Pass the scroll events up the tree to the parent to consume
                    state = rememberScrollableState(onScrolled)
                )
                .pointerInput(
                    key1 = Unit,
                    block = onPointerInput
                )
        )
    }
}

@Composable
fun NestedScrollTextContainer2(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    key: Any,
    content: @Composable () -> Unit
) {
    val textIsAtTopOfViewport by produceState(
        initialValue = false,
        key1 = listState,
        key2 = key,
    ) {
        snapshotFlow {
            val textFieldInfo = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == key }
                ?: return@snapshotFlow false
            textFieldInfo.offset <= listState.layoutInfo.viewportStartOffset
        }
            .distinctUntilChanged()
            .collectLatest(::value::set)
    }

    Box(
        modifier = modifier
            .nestedScroll(
                connection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            return if (textIsAtTopOfViewport) Offset.Zero
                            else {
                                Offset(
                                    x = 0f,
                                    y = listState.dispatchRawDelta(-available.y)
                                )
                                available
                            }
                        }
                    }
                }
            )
    ) {
        content()

    }
}

@Composable
fun LazyListState.isInViewport(key: Any): Boolean {
    val isScrollingForward = isScrollingForward()
    return produceState(
        initialValue = false,
        key1 = key,
        key2 = isScrollingForward,
        key3 = this,
    ) {
        snapshotFlow {
            when {
                isScrollingForward -> layoutInfo.visibleItemsInfo.firstOrNull()
                else -> layoutInfo.visibleItemsInfo.lastOrNull()
            }?.key == key
        }
            .distinctUntilChanged()
            .collect { value = it }
    }.value
}


@Composable
private fun LazyListState.isScrollingForward(): Boolean {
    var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            when {
                !canScrollBackward -> true
                !canScrollForward -> false
                previousIndex != firstVisibleItemIndex -> {
                    previousIndex < firstVisibleItemIndex
                }

                else -> {
                    previousScrollOffset <= firstVisibleItemScrollOffset
                }
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}