package com.woocommerce.android.ui.orders.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooBadge
import com.woocommerce.android.ui.compose.designsystem.icons.CheckSmall
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun OrderListOrderRow(
    order: OrderListItemUiModel.Order,
    isBulkSelected: Boolean,
    isDetailHighlighted: Boolean,
    isBulkSelectionActive: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSelectionToggle: () -> Boolean,
    onMarkCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val selectionAction = stringResource(
        if (isBulkSelected) R.string.orderlist_deselect_order else R.string.orderlist_select_order,
        order.number,
    )
    val markCompletedAction = stringResource(R.string.orderlist_mark_completed)
    val activate: () -> Unit = if (isBulkSelectionActive) {
        { onSelectionToggle() }
    } else {
        onTap
    }
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
        modifier = modifier.fillMaxWidth(),
    ) { swipeModifier ->
        Row(
            modifier = swipeModifier
                .fillMaxWidth()
                .heightIn(min = ORDER_ROW_MIN_HEIGHT)
                .testTag(OrderListTestTags.orderRow(order.orderId))
                .background(background)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.DirectionCenter -> {
                            activate()
                            true
                        }
                        Key.Spacebar -> {
                            onSelectionToggle()
                            true
                        }
                        else -> false
                    }
                }
                .combinedClickable(
                    onClick = activate,
                    onLongClick = onLongPress,
                )
                .semantics(mergeDescendants = true) {
                    selected = isBulkSelected
                    role = Role.Button
                    requestFocus {
                        focusRequester.requestFocus()
                        true
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
                }
                .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isBulkSelectionActive) {
                BulkSelectionIndicator(
                    isSelected = isBulkSelected,
                    orderId = order.orderId,
                )
                Spacer(modifier = Modifier.width(WooTheme.spacing.space4))
            }
            OrderContent(
                order = order,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BulkSelectionIndicator(
    isSelected: Boolean,
    orderId: Long,
) {
    Box(
        modifier = Modifier
            .size(SELECTION_INDICATOR_SIZE)
            .testTag(OrderListTestTags.selectionSlot(orderId)),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(SELECTION_INDICATOR_SIZE)
                    .testTag(OrderListTestTags.selectionIndicator(orderId))
                    .background(
                        color = WooTheme.colors.primary,
                        shape = RoundedCornerShape(WooTheme.radius.medium),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = WooIcons.Regular.CheckSmall,
                    contentDescription = null,
                    tint = WooTheme.colors.onPrimary,
                    modifier = Modifier.size(WooTheme.iconSize.size18),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderContent(
    order: OrderListItemUiModel.Order,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
        ) {
            Text(
                text = order.number,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.bodyLarge.emphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = order.total,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.bodyLarge.emphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = order.customerName,
            color = WooTheme.colors.surface.onDefault,
            style = WooTheme.text.bodyMedium.regular,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        order.dateCreated?.takeIf(String::isNotBlank)?.let { dateCreated ->
            Text(
                text = dateCreated,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.bodySmall.regular,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (order.badges.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2),
            ) {
                order.badges.forEach { badge ->
                    WooBadge(
                        text = badge.text,
                        tone = badge.tone,
                    )
                }
            }
        }
    }
}

private val ORDER_ROW_MIN_HEIGHT = 96.dp
private val SELECTION_INDICATOR_SIZE = 24.dp
