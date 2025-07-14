package com.woocommerce.android.wear.compose.component

import androidx.compose.runtime.State
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType.Companion.ItemCenter
import androidx.wear.compose.foundation.lazy.ScalingLazyListItemInfo
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.PositionIndicatorState
import androidx.wear.compose.material.PositionIndicatorVisibility

/**
 * Google currently doesn't offer a full solution to the ScalingLazyColumn to show the scroll bar,
 * but still rejects Wear apps without it. This adapter is a workaround to always show the scroll
 * and it was taken from https://stackoverflow.com/a/77356995.
 */
class ScrollingLazyColumnAdapter(
    private val state: ScalingLazyListState,
    private val viewportHeightPx: State<Int?>,
    private val anchorType: ScalingLazyListAnchorType = ItemCenter,
) : PositionIndicatorState {
    override val positionFraction: Float
        get() {
            return if (state.layoutInfo.visibleItemsInfo.isEmpty()) {
                0.0f
            } else {
                val decimalFirstItemIndex = decimalFirstItemIndex()
                val decimalLastItemIndex = decimalLastItemIndex()
                val decimalLastItemIndexDistanceFromEnd = state.layoutInfo.totalItemsCount -
                    decimalLastItemIndex

                if (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd == 0.0f) {
                    0.0f
                } else {
                    decimalFirstItemIndex /
                        (decimalFirstItemIndex + decimalLastItemIndexDistanceFromEnd)
                }
            }
        }

    override fun sizeFraction(scrollableContainerSizePx: Float) =
        if (state.layoutInfo.totalItemsCount == 0) {
            1.0f
        } else {
            val decimalFirstItemIndex = decimalFirstItemIndex()
            val decimalLastItemIndex = decimalLastItemIndex()

            (decimalLastItemIndex - decimalFirstItemIndex) /
                state.layoutInfo.totalItemsCount.toFloat()
        }

    override fun visibility(scrollableContainerSizePx: Float): PositionIndicatorVisibility {
        val canScroll = state.layoutInfo.visibleItemsInfo.isNotEmpty() &&
            (
                decimalFirstItemIndex() > 0 ||
                    decimalLastItemIndex() < state.layoutInfo.totalItemsCount
                )

        return if (canScroll) PositionIndicatorVisibility.Show else PositionIndicatorVisibility.Hide
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return (other as? ScrollingLazyColumnAdapter)?.state == state
    }

    private fun decimalLastItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val lastItem = state.layoutInfo.visibleItemsInfo.last()
        val lastItemEndOffset = lastItem.startOffset(anchorType) + lastItem.size
        val viewportEndOffset = viewportHeightPx.value!! / 2f
        val lastItemVisibleFraction =
            (1f - ((lastItemEndOffset - viewportEndOffset) / lastItem.size)).coerceAtMost(1f)

        return lastItem.index.toFloat() + lastItemVisibleFraction
    }

    private fun decimalFirstItemIndex(): Float {
        if (state.layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val firstItem = state.layoutInfo.visibleItemsInfo.first()
        val firstItemStartOffset = firstItem.startOffset(anchorType)
        val viewportStartOffset = -(viewportHeightPx.value!! / 2f)
        val firstItemInvisibleFraction =
            ((viewportStartOffset - firstItemStartOffset) / firstItem.size).coerceAtLeast(0f)

        return firstItem.index.toFloat() + firstItemInvisibleFraction
    }
}

internal fun ScalingLazyListItemInfo.startOffset(anchorType: ScalingLazyListAnchorType) =
    offset - if (anchorType == ItemCenter) {
        (size / 2f)
    } else {
        0f
    }
