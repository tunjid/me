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
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs
import kotlin.math.min

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
    crossinline itemOffset: LazyState.(List<LazyStateItem>) -> Float,
    crossinline itemPercentVisible: LazyState.(LazyStateItem) -> Float,
    crossinline itemIndex: (LazyStateItem) -> Int,
    crossinline reverseLayout: LazyState.() -> Boolean,
): ScrollbarState {
    var state by remember { mutableStateOf(ScrollbarState.FULL) }
    var heuristics by remember(itemsAvailable) { mutableStateOf(LazyScrollHeuristics()) }

    LaunchedEffect(
        key1 = this,
        key2 = itemsAvailable,
    ) {
        snapshotFlow {
            if (itemsAvailable == 0) return@snapshotFlow null

            val visibleItemsInfo = visibleItems(this@scrollbarState)
            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val firstVisibleItem = visibleItemsInfo.first()

            val firstVisibleIndex = itemIndex(firstVisibleItem)
            if (firstVisibleIndex < 0) return@snapshotFlow null

            val itemsVisible = heuristics.accumulate(
                visibleItemsInfo.sumOf { itemPercentVisible(it).toDouble() }.toFloat(),
            ).visible

            // Add the item offset for interpolation between scroll indices
            val interpolatedFirstIndex = firstVisibleIndex.toFloat() + itemOffset(visibleItemsInfo)

            val initialThumbTravelPercent = min(
                a = interpolatedFirstIndex / itemsAvailable,
                b = 1f,
            )
            // This estimate is good for the scrollbar at the top.
            val thumbTravelBias = initialThumbTravelPercent * itemsVisible

            val thumbTravelIndex = min(
                a = interpolatedFirstIndex + thumbTravelBias,
                b = itemsAvailable.toFloat(),
            )
            val thumbTravelPercent = min(
                a = thumbTravelIndex / itemsAvailable,
                b = 1f,
            )
            val thumbSizePercent = min(
                a = itemsVisible / itemsAvailable,
                b = 1f,
            )

            println("vi: $itemsVisible; ifi: $interpolatedFirstIndex; ttb: $thumbTravelBias; tti: $thumbTravelIndex; ia: $itemsAvailable; ttp: $thumbTravelPercent")

            ScrollbarState(
                thumbSizePercent = thumbSizePercent,
                thumbTravelPercent = when {
                    reverseLayout() -> 1f - thumbTravelPercent
                    else -> thumbTravelPercent
                },
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect {
                val itemsVisible = it.thumbSizePercent * itemsAvailable
                heuristics = heuristics.accumulate(itemsVisible)
                state = it
            }
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

internal inline fun itemVisibilityPercentage(
    itemSize: Int,
    itemStart: Int,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
): Float {
    if (itemSize == 0) return 0f
    val itemEnd = itemStart + itemSize
    val itemStartOffset = when {
        itemStart > viewportStartOffset -> 0
        else -> abs(abs(viewportStartOffset) - abs(itemStart))
    }
    val itemEndOffset = when {
        itemEnd < viewportEndOffset -> 0
        else -> abs(abs(itemEnd) - abs(viewportEndOffset))
    }
    val size = itemSize.toFloat()
    return (size - itemStartOffset - itemEndOffset) / size
}

/**
 * Normalizes the progession
 */
@JvmInline
internal value class LazyScrollHeuristics(
    val packedValue: Long = 0L,
) {
    internal constructor(
        count: Float,
        visible: Float,
    ) : this(packFloats(count, visible))
}

private val LazyScrollHeuristics.count get() = unpackFloat1(packedValue)

private val LazyScrollHeuristics.visible get() = unpackFloat2(packedValue)

private fun LazyScrollHeuristics.accumulate(newVisible: Float) = when (count) {
    0f -> LazyScrollHeuristics(
        count = 1f,
        visible = newVisible,
    )

    else -> LazyScrollHeuristics(
        count = count + 1,
        visible = ((visible * count) + newVisible) / (count + 1),
    )
}
