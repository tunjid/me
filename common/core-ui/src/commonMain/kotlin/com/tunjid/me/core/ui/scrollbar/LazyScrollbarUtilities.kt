package com.tunjid.me.core.ui.scrollbar

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs


/**
 * A generic function for remembering [ScrollbarState] for lazy layouts.
 * @param itemsAvailable the total amount of items available to scroll in the layout.
 * @param visibleItems a list of items currently visible in the layout.
 * @param viewportArea a lookup function for the size of the view port in the [Orientation] axis
 * of the layout.
 * @param maxItemArea a lookup function for the visible item with the maximum size in the
 * [Orientation] axis of the layout.
 * @param itemIndex a lookup function for index of an item in the layout relative to [itemsAvailable].
 */
@Composable
internal inline fun <LazyState : ScrollableState, LazyStateItem> LazyState.scrollbarState(
    itemsAvailable: Int,
    crossinline visibleItems: LazyState.() -> List<LazyStateItem>,
    crossinline viewportArea: LazyState.() -> Int,
    crossinline maxItemArea: LazyState.(LazyStateItem) -> Int,
    crossinline itemOffset: LazyState.(List<LazyStateItem>) -> Float,
    crossinline itemIndex: (LazyStateItem) -> Int,
): ScrollbarState {
    var state by remember { mutableStateOf(ScrollbarState.FULL) }
    LaunchedEffect(
        key1 = this,
        key2 = itemsAvailable
    ) {
        snapshotFlow {
            if (itemsAvailable == 0) return@snapshotFlow null

            val visibleItemsInfo = visibleItems(this@scrollbarState)
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val firstVisibleItem = visibleItemsInfo.first()

            val firstVisibleIndex = itemIndex(firstVisibleItem)
            if (firstVisibleIndex < 0) return@snapshotFlow null

            val maxSize = visibleItemsInfo.maxOf { item ->
                maxItemArea(this@scrollbarState, item)
            }

            val itemsVisible = viewportArea(this@scrollbarState).toFloat() / maxSize
            val scrollIndex = firstVisibleIndex.toFloat() + itemOffset(visibleItemsInfo)

            ScrollbarState(
                thumbSizePercent = itemsVisible / itemsAvailable,
                thumbTravelPercent = scrollIndex / itemsAvailable
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { state = it }
    }
    return state
}

internal inline fun <LazyState : ScrollableState, LazyStateItem> LazyState.offsetCalculator(
    visibleItems: List<LazyStateItem>,
    crossinline maxItemSize: LazyState.(LazyStateItem) -> Int,
    crossinline offset: LazyState.(LazyStateItem) -> Int,
    crossinline next: LazyState.(LazyStateItem) -> LazyStateItem?,
    crossinline itemIndex: (LazyStateItem) -> Int,
): Float {
    if (visibleItems.isEmpty()) return 0f

    val firstItem = visibleItems.first()
    val itemOffset = offset(firstItem).toFloat()
    val offsetPercentage = abs(itemOffset) / maxItemSize(firstItem)

    val nextItem = next(firstItem) ?: return offsetPercentage

    val firstItemIndex = itemIndex(firstItem)
    val nextItemIndex = itemIndex(nextItem)


    return (nextItemIndex - firstItemIndex) * offsetPercentage
}
