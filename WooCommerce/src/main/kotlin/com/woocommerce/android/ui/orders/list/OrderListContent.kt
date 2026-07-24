package com.woocommerce.android.ui.orders.list

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooCircularProgressIndicator
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledButton

@Suppress("LongParameterList")
@Composable
internal fun OrderListContent(
    state: OrderListContentState,
    rowState: OrderListRowState,
    itemCount: Int,
    itemKey: (index: Int) -> Any,
    itemAt: (index: Int) -> OrderListItemUiModel?,
    onOrderTapped: (orderId: Long) -> Unit,
    onOrderLongPressed: (orderId: Long) -> Unit,
    onOrderSelectionToggled: (orderId: Long) -> Boolean,
    onMarkOrderCompleted: (orderId: Long) -> Unit,
    onLearnMoreClicked: () -> Unit,
    onShowGuestOrdersClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    when (state) {
        OrderListContentState.InitialLoading -> OrderListInitialLoading(modifier)
        is OrderListContentState.Empty -> OrderListEmptyState(
            state = state.state,
            onLearnMoreClicked = onLearnMoreClicked,
            onShowGuestOrdersClicked = onShowGuestOrdersClicked,
            onRetryClicked = onRetryClicked,
            modifier = modifier,
        )
        is OrderListContentState.Content -> OrderLazyList(
            itemCount = itemCount,
            itemKey = itemKey,
            itemAt = itemAt,
            rowState = rowState,
            isAppending = state.isAppending,
            contentRevision = state.contentRevision,
            onOrderTapped = onOrderTapped,
            onOrderLongPressed = onOrderLongPressed,
            onOrderSelectionToggled = onOrderSelectionToggled,
            onMarkOrderCompleted = onMarkOrderCompleted,
            modifier = modifier,
            listState = listState,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun OrderLazyList(
    itemCount: Int,
    itemKey: (index: Int) -> Any,
    itemAt: (index: Int) -> OrderListItemUiModel?,
    rowState: OrderListRowState,
    isAppending: Boolean,
    contentRevision: Long,
    onOrderTapped: (orderId: Long) -> Unit,
    onOrderLongPressed: (orderId: Long) -> Unit,
    onOrderSelectionToggled: (orderId: Long) -> Boolean,
    onMarkOrderCompleted: (orderId: Long) -> Unit,
    modifier: Modifier,
    listState: LazyListState,
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag(OrderListTestTags.LIST),
        contentPadding = PaddingValues(bottom = WooTheme.padding.padding5),
    ) {
        items(
            count = itemCount,
            key = itemKey,
        ) { index ->
            val item = remember(index, contentRevision, itemAt) { itemAt(index) }
            when (item) {
                is OrderListItemUiModel.DateSection -> OrderListDateSection(item)
                is OrderListItemUiModel.Order -> {
                    val orderId = item.orderId
                    OrderListOrderRow(
                        order = item,
                        isBulkSelected = orderId in rowState.bulkSelectedOrderIds,
                        isDetailHighlighted = orderId == rowState.detailHighlightedOrderId,
                        isBulkSelectionActive = rowState.isBulkSelectionActive,
                        onTap = { onOrderTapped(orderId) },
                        onLongPress = { onOrderLongPressed(orderId) },
                        onSelectionToggle = { onOrderSelectionToggled(orderId) },
                        onMarkCompleted = { onMarkOrderCompleted(orderId) },
                    )
                    if (item.showDivider) {
                        WooDivider(
                            modifier = Modifier
                                .padding(horizontal = WooTheme.padding.padding7)
                                .testTag(OrderListTestTags.orderDivider(orderId))
                        )
                    }
                }
                is OrderListItemUiModel.Loading -> OrderListItemSkeleton(
                    testTag = OrderListTestTags.loadingItem(item.orderId),
                )
                null -> OrderListItemSkeleton(testTag = OrderListTestTags.NULL_PLACEHOLDER)
            }
        }
        if (isAppending) {
            item(key = APPEND_PROGRESS_KEY) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(OrderListTestTags.APPEND_PROGRESS)
                        .padding(WooTheme.padding.padding5),
                    contentAlignment = Alignment.Center,
                ) {
                    WooCircularProgressIndicator(modifier = Modifier.size(PROGRESS_SIZE))
                }
            }
        }
    }
}

@Composable
private fun OrderListDateSection(section: OrderListItemUiModel.DateSection) {
    Text(
        text = section.title,
        color = WooTheme.colors.background.onSection,
        style = WooTheme.text.titleSmall.strong,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(OrderListTestTags.DATE_SECTION)
            .semantics { heading() }
            .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding4),
    )
}

@Composable
private fun OrderListItemSkeleton(testTag: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding4),
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        SkeletonView(
            width = SKELETON_DATE_WIDTH,
            height = SKELETON_DATE_HEIGHT,
            modifier = Modifier.testTag(OrderListTestTags.SKELETON_DATE),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
        ) {
            SkeletonView(
                modifier = Modifier
                    .weight(4f)
                    .height(SKELETON_TEXT_HEIGHT)
                    .testTag(OrderListTestTags.SKELETON_TITLE),
            )
            SkeletonView(
                modifier = Modifier
                    .weight(1f)
                    .height(SKELETON_TEXT_HEIGHT)
                    .testTag(OrderListTestTags.SKELETON_TOTAL),
            )
        }
        SkeletonView(
            width = SKELETON_BADGE_WIDTH,
            height = SKELETON_BADGE_HEIGHT,
            modifier = Modifier.testTag(OrderListTestTags.SKELETON_BADGE),
        )
    }
    WooDivider(
        modifier = Modifier
            .padding(horizontal = WooTheme.padding.padding7)
            .testTag(OrderListTestTags.SKELETON_DIVIDER)
    )
}

@Composable
private fun OrderListInitialLoading(modifier: Modifier) {
    OrderListMessage(
        title = stringResource(R.string.orderlist_loading),
        message = null,
        image = R.drawable.img_empty_orders_loading,
        actionText = null,
        onActionClicked = null,
        modifier = modifier.testTag(OrderListTestTags.INITIAL_LOADING),
    )
}

@Composable
private fun OrderListEmptyState(
    state: OrderListEmptyState,
    onLearnMoreClicked: () -> Unit,
    onShowGuestOrdersClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    modifier: Modifier,
) {
    val presentation = when (state) {
        OrderListEmptyState.NoOrders -> OrderListMessagePresentation(
            title = stringResource(R.string.empty_order_list_title),
            message = stringResource(R.string.empty_order_list_message),
            image = R.drawable.img_empty_orders_no_orders,
            actionText = stringResource(R.string.learn_more),
            action = OrderListEmptyAction.LearnMore,
        )
        OrderListEmptyState.Filtered -> OrderListMessagePresentation(
            title = stringResource(R.string.orders_empty_message_for_filtered_orders),
            message = null,
            image = R.drawable.img_empty_search,
        )
        is OrderListEmptyState.Search -> OrderListMessagePresentation(
            title = stringResource(R.string.empty_message_with_search, state.query),
            message = null,
            image = R.drawable.img_empty_search,
        )
        is OrderListEmptyState.GuestSearch -> OrderListMessagePresentation(
            title = stringResource(R.string.empty_message_with_search, state.query),
            message = stringResource(R.string.empty_message_with_search_guest),
            image = R.drawable.img_empty_search,
            actionText = stringResource(R.string.empty_search_guest_orders_button),
            action = OrderListEmptyAction.ShowGuestOrders,
        )
        OrderListEmptyState.Offline -> OrderListMessagePresentation(
            title = stringResource(R.string.offline_error),
            message = null,
            image = R.drawable.ic_woo_error_state,
            actionText = stringResource(R.string.retry),
            action = OrderListEmptyAction.Retry,
        )
        OrderListEmptyState.NetworkError -> OrderListMessagePresentation(
            title = stringResource(R.string.error_generic_network),
            message = null,
            image = R.drawable.ic_woo_error_state,
            actionText = stringResource(R.string.retry),
            action = OrderListEmptyAction.Retry,
        )
    }
    val onActionClicked = when (presentation.action) {
        OrderListEmptyAction.LearnMore -> onLearnMoreClicked
        OrderListEmptyAction.ShowGuestOrders -> onShowGuestOrdersClicked
        OrderListEmptyAction.Retry -> onRetryClicked
        null -> null
    }

    OrderListMessage(
        title = presentation.title,
        message = presentation.message,
        image = presentation.image,
        actionText = presentation.actionText,
        onActionClicked = onActionClicked,
        modifier = modifier.testTag(OrderListTestTags.EMPTY),
    )
}

@Composable
private fun OrderListMessage(
    title: String,
    message: String?,
    @DrawableRes image: Int,
    actionText: String?,
    onActionClicked: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val showImage = maxHeight >= MIN_MESSAGE_IMAGE_HEIGHT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding8),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = WooTheme.colors.background.onSection,
                style = WooTheme.text.titleMedium.strong,
                textAlign = TextAlign.Center,
            )
            if (showImage) {
                Image(
                    painter = painterResource(image),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = WooTheme.padding.padding8)
                        .size(MESSAGE_IMAGE_SIZE),
                )
            }
            message?.let {
                Text(
                    text = it,
                    color = WooTheme.colors.background.onSectionVariant,
                    style = WooTheme.text.bodyMedium.regular,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = WooTheme.padding.padding8),
                )
            }
            if (actionText != null && onActionClicked != null) {
                WooFilledButton(
                    text = actionText,
                    onClick = onActionClicked,
                    modifier = Modifier
                        .padding(top = WooTheme.padding.padding8)
                        .testTag(OrderListTestTags.EMPTY_ACTION),
                )
            }
        }
    }
}

private data class OrderListMessagePresentation(
    val title: String,
    val message: String?,
    @DrawableRes val image: Int,
    val actionText: String? = null,
    val action: OrderListEmptyAction? = null,
)

private enum class OrderListEmptyAction {
    LearnMore,
    ShowGuestOrders,
    Retry,
}

private const val APPEND_PROGRESS_KEY = "order-list-append-progress"
private val PROGRESS_SIZE = 24.dp
private val SKELETON_DATE_WIDTH = 80.dp
private val SKELETON_DATE_HEIGHT = 12.dp
private val SKELETON_TEXT_HEIGHT = 16.dp
private val SKELETON_BADGE_WIDTH = 100.dp
private val SKELETON_BADGE_HEIGHT = 24.dp
private val MESSAGE_IMAGE_SIZE = 160.dp
private val MIN_MESSAGE_IMAGE_HEIGHT = 400.dp
