package com.tunjid.me.core.ui.scrollbar

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue


/**
 * Remembers a function to react to [Scrollbar] thumb position movements for a [ScrollbarState]
 */
@Composable
fun ScrollState.thumbInteractions(): (Float) -> Unit {
    var percentage by remember { mutableStateOf(Float.NaN) }

    LaunchedEffect(percentage) {
        if (percentage.isNaN()) return@LaunchedEffect
        scrollTo((percentage * maxValue).toInt())
    }
    return remember {
        { percentage = it }
    }
}

/**
 * Remembers a function to react to [Scrollbar] thumb position movements for a [LazyListState]
 * @param itemsAvailable the amount of items in the list.
 */
@Composable
fun LazyListState.thumbInteractions(
    itemsAvailable: Int,
): (Float) -> Unit = thumbInteractions(
    itemsAvailable = itemsAvailable,
    scroll = ::scrollToItem
)

/**
 * Remembers a function to react to [Scrollbar] thumb position movements for a [LazyGridState]
 * @param itemsAvailable the amount of items in the grid.
 */
@Composable
fun LazyGridState.thumbInteractions(
    itemsAvailable: Int,
): (Float) -> Unit = thumbInteractions(
    itemsAvailable = itemsAvailable,
    scroll = ::scrollToItem
)

/**
 * Remembers a function to react to [Scrollbar] thumb position movements for a [LazyStaggeredGridState]
 * @param itemsAvailable the amount of items in the grid.
 */
@Composable
fun LazyStaggeredGridState.thumbInteractions(
    itemsAvailable: Int,
): (Float) -> Unit = thumbInteractions(
    itemsAvailable = itemsAvailable,
    scroll = ::scrollToItem
)

/**
 * Generic function to react to [Scrollbar] thumb interactions in a lazy layout.
 * @param itemsAvailable the total amount of items available to scroll in the layout.
 * @param scroll a function to be invoked when an index has been identified to scroll to.
 */
@Composable
private inline fun thumbInteractions(
    itemsAvailable: Int,
    crossinline scroll: suspend (index: Int) -> Unit
): (Float) -> Unit {
    var percentage by remember { mutableStateOf(Float.NaN) }
    val itemCount by rememberUpdatedState(itemsAvailable)

    LaunchedEffect(percentage) {
        if (percentage.isNaN()) return@LaunchedEffect
        val indexToFind = (itemCount * percentage).toInt()
        scroll(indexToFind)
    }
    return remember {
        { percentage = it }
    }
}