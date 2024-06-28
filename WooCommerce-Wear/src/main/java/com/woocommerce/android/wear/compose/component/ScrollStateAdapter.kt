package com.woocommerce.android.wear.compose.component

import androidx.compose.foundation.ScrollState
import androidx.wear.compose.material.PositionIndicatorState
import androidx.wear.compose.material.PositionIndicatorVisibility

class ScrollStateAdapter(private val scrollState: ScrollState) : PositionIndicatorState {
    override val positionFraction: Float
        get() {
            return if (scrollState.maxValue == 0) {
                0f
            } else {
                scrollState.value.toFloat() / scrollState.maxValue
            }
        }

    override fun sizeFraction(scrollableContainerSizePx: Float) =
        if (scrollableContainerSizePx + scrollState.maxValue == 0.0f) {
            1.0f
        } else {
            scrollableContainerSizePx / (scrollableContainerSizePx + scrollState.maxValue)
        }

    override fun visibility(scrollableContainerSizePx: Float) = if (scrollState.maxValue == 0) {
        PositionIndicatorVisibility.Hide
    } else if (scrollState.isScrollInProgress)
        PositionIndicatorVisibility.Show
    else
        PositionIndicatorVisibility.AutoHide

    override fun equals(other: Any?): Boolean {
        return (other as? ScrollStateAdapter)?.scrollState == scrollState
    }

    override fun hashCode(): Int {
        return scrollState.hashCode()
    }
}
