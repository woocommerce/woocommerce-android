package com.woocommerce.android.ui.orders.list

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooBadge
import com.woocommerce.android.ui.compose.designsystem.component.WooBadgeDefaults
import com.woocommerce.android.ui.compose.designsystem.icons.CheckSmall
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun OrderListOrderRow(
    order: OrderListItemUiModel.Order,
    isBulkSelected: Boolean,
    isDetailHighlighted: Boolean,
    isBulkSelectionActive: Boolean,
    onActivate: () -> Unit,
    onLongPress: () -> Unit,
    onSelectionToggle: () -> Boolean,
    onMarkCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    canHandleSwipeDelta: () -> Boolean = { true },
    canCommitSwipe: () -> Boolean = { true },
) {
    val focusRequester = remember { FocusRequester() }
    val selectionAction = stringResource(
        if (isBulkSelected) R.string.orderlist_deselect_order else R.string.orderlist_select_order,
        order.number,
    )
    val markCompletedAction = stringResource(R.string.orderlist_mark_completed)
    val background = if (isBulkSelected || isDetailHighlighted) {
        colorResource(R.color.color_item_selected)
    } else {
        WooTheme.colors.surface.default
    }

    OrderListSwipeToComplete(
        orderId = order.orderId,
        isCompleted = order.isCompleted,
        isEnabled = !isBulkSelectionActive,
        onMarkCompleted = onMarkCompleted,
        canHandleDelta = canHandleSwipeDelta,
        canCommit = canCommitSwipe,
        modifier = modifier.fillMaxWidth(),
    ) { swipeModifier ->
        Row(
            modifier = swipeModifier
                .fillMaxWidth()
                .heightIn(min = ORDER_ROW_MIN_HEIGHT)
                .testTag(OrderListTestTags.orderRow(order.orderId))
                .background(background)
                .focusRequester(focusRequester)
                .combinedClickable(
                    onClick = onActivate,
                    onLongClick = onLongPress,
                )
                .semantics(mergeDescendants = true) {
                    selected = isBulkSelected
                    role = Role.Button
                    requestFocus {
                        focusRequester.requestFocus()
                    }
                    customActions = buildList {
                        add(CustomAccessibilityAction(selectionAction, onSelectionToggle))
                        if (!order.isCompleted && !isBulkSelectionActive) {
                            add(
                                CustomAccessibilityAction(
                                    label = markCompletedAction,
                                    action = {
                                        onMarkCompleted()
                                        true
                                    },
                                )
                            )
                        }
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isBulkSelected) {
                BulkSelectionIndicator(orderId = order.orderId)
            }
            OrderContent(
                order = order,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OrderListSwipeToComplete(
    orderId: Long,
    isCompleted: Boolean,
    isEnabled: Boolean,
    onMarkCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    canHandleDelta: () -> Boolean = { true },
    canCommit: () -> Boolean = { true },
    content: @Composable (Modifier) -> Unit,
) {
    var widthPx by remember(orderId) { mutableIntStateOf(0) }
    var offsetPx by remember(orderId) { mutableFloatStateOf(0f) }
    val velocityThresholdPx = with(LocalDensity.current) {
        SWIPE_VELOCITY_THRESHOLD.toPx()
    }
    val dragState = rememberDraggableState { delta ->
        if (widthPx == 0 || !canHandleDelta()) return@rememberDraggableState

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
                onDragStopped = { velocity ->
                    val shouldMarkCompleted = canCommit() &&
                        !isCompleted &&
                        shouldCompleteOrderSwipe(
                            offsetPx = offsetPx,
                            widthPx = widthPx,
                            velocityPxPerSecond = velocity,
                            velocityThresholdPxPerSecond = velocityThresholdPx,
                        )
                    if (shouldMarkCompleted) {
                        onMarkCompleted()
                    }
                    dragState.drag {
                        animate(
                            initialValue = offsetPx,
                            targetValue = 0f,
                            animationSpec = tween(),
                        ) { value, _ ->
                            offsetPx = value
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
                    .align(AbsoluteAlignment.TopRight)
                    .absolutePadding(
                        top = WooTheme.padding.padding5,
                        right = WooTheme.padding.padding5,
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
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BulkSelectionIndicator(orderId: Long) {
    Box(
        modifier = Modifier
            .padding(start = SELECTION_SLOT_START_INSET)
            .size(SELECTION_INDICATOR_SIZE)
            .testTag(OrderListTestTags.selectionSlot(orderId))
            .background(
                color = WooTheme.colors.primary,
                shape = RoundedCornerShape(WooTheme.radius.small),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = WooIcons.Regular.CheckSmall,
            contentDescription = null,
            tint = WooTheme.colors.onPrimary,
            modifier = Modifier
                .size(WooTheme.iconSize.size24)
                .testTag(OrderListTestTags.selectionIndicator(orderId)),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderContent(
    order: OrderListItemUiModel.Order,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            start = WooTheme.padding.padding5,
            end = WooTheme.padding.padding5,
            top = WooTheme.padding.padding4,
            bottom = WooTheme.padding.padding5,
        ),
    ) {
        order.dateCreated?.takeIf(String::isNotBlank)?.let { dateCreated ->
            Text(
                text = dateCreated,
                color = WooTheme.colors.surface.onVariant,
                style = WooTheme.text.bodySmall.regular,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(WooTheme.spacing.space2))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = order.number,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.bodyLarge.regular,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(WooTheme.spacing.space3))
            Text(
                text = order.customerName,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.bodyLarge.regular,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(WooTheme.spacing.space5))
            Text(
                text = order.total,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.bodyLarge.regular,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (order.badges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(WooTheme.spacing.space3))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2),
            ) {
                order.badges.forEach { badge ->
                    WooBadge(
                        text = badge.text,
                        colors = WooBadgeDefaults.colors(
                            containerColor = colorResource(badge.containerColorRes),
                            contentColor = colorResource(badge.contentColorRes),
                        ),
                    )
                }
            }
        }
    }
}

private val ORDER_ROW_MIN_HEIGHT = 96.dp
private val SELECTION_SLOT_START_INSET = 12.dp
private val SELECTION_INDICATOR_SIZE = 40.dp

// Match Material 3 swipe settling for a short, intentional fling.
private val SWIPE_VELOCITY_THRESHOLD = 125.dp
private const val COMPLETED_MAXIMUM_TRAVEL = 0.1f
private const val COMPLETION_THRESHOLD = 0.5f
private const val FULL_TRAVEL = 1f

internal fun shouldCompleteOrderSwipe(
    offsetPx: Float,
    widthPx: Int,
    velocityPxPerSecond: Float,
    velocityThresholdPxPerSecond: Float,
): Boolean {
    if (widthPx <= 0) return false

    val crossedDistanceThreshold = offsetPx < -(widthPx * COMPLETION_THRESHOLD)
    return if (abs(velocityPxPerSecond) >= velocityThresholdPxPerSecond) {
        velocityPxPerSecond < 0f
    } else {
        crossedDistanceThreshold
    }
}

internal fun maximumOrderSwipeTravel(widthPx: Int, isCompleted: Boolean): Float {
    val travelFraction = if (isCompleted) COMPLETED_MAXIMUM_TRAVEL else FULL_TRAVEL
    return widthPx * travelFraction
}
