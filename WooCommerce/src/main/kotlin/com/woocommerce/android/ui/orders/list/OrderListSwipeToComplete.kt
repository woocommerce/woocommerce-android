package com.woocommerce.android.ui.orders.list

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.icons.CheckSmall
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import kotlin.math.roundToInt

@Composable
internal fun OrderListSwipeToComplete(
    orderId: Long,
    isCompleted: Boolean,
    isEnabled: Boolean,
    onMarkCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    var widthPx by remember(orderId) { mutableIntStateOf(0) }
    var offsetPx by remember(orderId) { mutableFloatStateOf(0f) }
    val dragState = rememberDraggableState { delta ->
        if (widthPx == 0) return@rememberDraggableState

        val maximumTravel = maximumOrderSwipeTravel(widthPx, isCompleted)
        offsetPx = (offsetPx + delta).coerceIn(
            minimumValue = -maximumTravel,
            maximumValue = 0f,
        )
    }

    LaunchedEffect(isEnabled, orderId) {
        if (!isEnabled) {
            dragState.drag(MutatePriority.PreventUserInput) {
                offsetPx = 0f
            }
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { widthPx = it.width }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = isEnabled,
                startDragImmediately = false,
                reverseDirection = false,
                onDragStopped = {
                    val shouldMarkCompleted = !isCompleted &&
                        shouldCompleteOrderSwipe(offsetPx, widthPx)
                    if (shouldMarkCompleted) {
                        onMarkCompleted()
                    }
                    dragState.drag {
                        var previousOffset = offsetPx
                        animate(
                            initialValue = previousOffset,
                            targetValue = 0f,
                            animationSpec = tween(),
                        ) { value, _ ->
                            dragBy(value - previousOffset)
                            previousOffset = value
                        }
                    }
                },
            ),
    ) {
        OrderListSwipeBackground(
            orderId = orderId,
            isCompleted = isCompleted,
            showAction = isEnabled,
            modifier = Modifier.matchParentSize(),
        )
        content(
            Modifier.absoluteOffset {
                IntOffset(offsetPx.roundToInt(), 0)
            }
        )
    }
}

@Composable
private fun OrderListSwipeBackground(
    orderId: Long,
    isCompleted: Boolean,
    showAction: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (isCompleted) {
        colorResource(R.color.color_on_surface_disabled)
    } else {
        WooTheme.colors.primary
    }
    Box(
        modifier = modifier
            .background(background)
            .then(
                if (!isCompleted && showAction) {
                    Modifier.testTag(OrderListTestTags.swipeReveal(orderId))
                } else {
                    Modifier
                }
            ),
    ) {
        if (!isCompleted && showAction) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = WooTheme.padding.padding5,
                        end = WooTheme.padding.padding5,
                    )
                    .clearAndSetSemantics { },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2),
            ) {
                Icon(
                    imageVector = WooIcons.Regular.CheckSmall,
                    contentDescription = null,
                    tint = WooTheme.colors.onPrimary,
                    modifier = Modifier.size(WooTheme.iconSize.size24),
                )
                Text(
                    text = stringResource(R.string.orderlist_mark_completed),
                    color = WooTheme.colors.onPrimary,
                    style = WooTheme.text.bodyLarge.regular,
                )
            }
        }
    }
}

private const val COMPLETED_MAXIMUM_TRAVEL = 0.1f
private const val COMPLETION_THRESHOLD = 0.5f
private const val FULL_TRAVEL = 1f

internal fun shouldCompleteOrderSwipe(offsetPx: Float, widthPx: Int): Boolean {
    return widthPx > 0 && offsetPx < -(widthPx * COMPLETION_THRESHOLD)
}

internal fun maximumOrderSwipeTravel(widthPx: Int, isCompleted: Boolean): Float {
    val travelFraction = if (isCompleted) COMPLETED_MAXIMUM_TRAVEL else FULL_TRAVEL
    return widthPx * travelFraction
}
