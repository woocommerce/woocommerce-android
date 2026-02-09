package com.woocommerce.android.ui.woopos.bookings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.bookings.details.WooPosBookingDetails
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosEmptyScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreenButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

val WOO_POS_BOOKINGS_TOOLBAR_HEIGHT = 56.dp

@Composable
fun WooPosBookingsScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val viewModel: WooPosBookingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    WooPosBookingsScreen(
        state = state,
        scrollToTopEvent = viewModel.scrollToTopEvent,
        onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
        onRefresh = viewModel::onRefresh,
        onBookingSelected = viewModel::onBookingSelected,
        onEndOfBookingsListReached = viewModel::onEndOfBookingsListReached,
        onPaginationErrorTryAgain = viewModel::onPaginationErrorTryAgain,
        onBookingsEmptyActionClicked = viewModel::onBookingsEmptyActionClicked,
        onBookingsLoadingErrorRetryButtonClicked = viewModel::onBookingsLoadingErrorRetryButtonClicked,
        onUIEvent = viewModel::onUIEvent,
        onNavigationEvent = onNavigationEvent,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Suppress("UnusedParameter")
private fun WooPosBookingsScreen(
    state: WooPosBookingsState,
    scrollToTopEvent: SharedFlow<Unit>,
    onBackClicked: () -> Unit,
    onRefresh: () -> Unit,
    onBookingSelected: (Long) -> Unit,
    onEndOfBookingsListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onBookingsEmptyActionClicked: () -> Unit,
    onBookingsLoadingErrorRetryButtonClicked: () -> Unit,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    BackHandler { onBackClicked() }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        when (state) {
            is WooPosBookingsState.Content -> WooPosBookingsContent(
                state = state,
                scrollToTopEvent = scrollToTopEvent,
                onRefresh = onRefresh,
                onBookingSelected = onBookingSelected,
                onEndOfBookingsListReached = onEndOfBookingsListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                onUIEvent = onUIEvent
            )

            is WooPosBookingsState.Empty -> WooPosBookingsEmpty(
                onActionClicked = onBookingsEmptyActionClicked,
                modifier = Modifier.statusBarsPadding()
            )

            is WooPosBookingsState.Error -> WooPosBookingsError(
                onRetryClicked = onBookingsLoadingErrorRetryButtonClicked,
                modifier = Modifier.statusBarsPadding()
            )

            is WooPosBookingsState.Loading -> WooPosBookingsLoadingScreen()
        }

        WooPosToolbar(
            titleText = stringResource(R.string.woopos_bookings_title),
            onBackClicked = onBackClicked,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        )
    }
}

@Composable
private fun WooPosBookingsContent(
    state: WooPosBookingsState.Content,
    scrollToTopEvent: SharedFlow<Unit>,
    onRefresh: () -> Unit,
    onBookingSelected: (Long) -> Unit,
    onEndOfBookingsListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        WooPosBookingsListPane(
            state = state,
            scrollToTopEvent = scrollToTopEvent,
            onRefresh = onRefresh,
            isRefreshing = state.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
            onBookingSelected = onBookingSelected,
            onEndOfBookingsListReached = onEndOfBookingsListReached,
            onPaginationErrorTryAgain = onPaginationErrorTryAgain,
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceBright)
        )

        Box(
            modifier = Modifier
                .weight(0.7f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when {
                state.selectedDetails != null -> {
                    WooPosBookingDetails(
                        modifier = Modifier
                            .fillMaxHeight(),
                        details = state.selectedDetails,
                        onUIEvent = onUIEvent
                    )
                }
                state.items is WooPosBookingsState.Content.Items.Searching -> {
                    BookingDetailsLoadingPane(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(
                                start = WooPosSpacing.Medium.value,
                                end = WooPosSpacing.Medium.value,
                                top = WooPosSpacing.XLarge.value,
                                bottom = WooPosSpacing.XLarge.value
                            )
                    )
                }
                else -> {
                    WooPosEmptyScreen(
                        modifier = Modifier.fillMaxSize(),
                        icon = WooPosIcons.OrdersEmpty,
                        title = stringResource(R.string.woopos_orders_no_order_selected),
                        message = "",
                        contentDescription = stringResource(R.string.woopos_orders_empty_list_image_description)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WooPosBookingsListPane(
    state: WooPosBookingsState.Content,
    scrollToTopEvent: SharedFlow<Unit>,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onBookingSelected: (Long) -> Unit,
    onEndOfBookingsListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = WooPosSpacing.Medium.value)
                .heightIn(min = WOO_POS_BOOKINGS_TOOLBAR_HEIGHT),
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = onRefresh
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(
                    pullRefreshState,
                    enabled = state.pullToRefreshState != WooPosPullToRefreshState.Disabled
                )
        ) {
            WooPosBookingsList(
                modifier = Modifier.fillMaxSize(),
                state = state,
                scrollToTopEvent = scrollToTopEvent,
                onBookingSelected = onBookingSelected,
                onEndOfBookingsListReached = onEndOfBookingsListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain,
            )

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = WooPosSpacing.XSmall.value),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WooPosBookingsList(
    modifier: Modifier = Modifier,
    state: WooPosBookingsState.Content,
    scrollToTopEvent: SharedFlow<Unit>,
    onBookingSelected: (Long) -> Unit,
    onEndOfBookingsListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
) {
    when (val items = state.items) {
        is WooPosBookingsState.Content.Items.Loaded -> {
            WooPosLoadedBookingsList(
                modifier = modifier,
                items = items.items,
                paginationState = state.paginationState,
                scrollToTopEvent = scrollToTopEvent,
                onBookingSelected = onBookingSelected,
                onEndOfBookingsListReached = onEndOfBookingsListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain
            )
        }

        is WooPosBookingsState.Content.Items.Searching -> {
            WooPosBookingsListLoadingPane(
                modifier = modifier.imePadding()
            )
        }

        is WooPosBookingsState.Content.Items.Error -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                WooPosErrorScreen(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = WooPosSpacing.XLarge.value),
                    message = items.title,
                    reason = items.message,
                    primaryButton = WooPosErrorScreenButtonState(
                        text = stringResource(id = R.string.retry),
                        click = { throw NotImplementedError("Retry button clicked") }
                    )
                )
            }
        }

        is WooPosBookingsState.Content.Items.NothingFound -> {
            WooPosEmptyScreen(
                modifier = modifier
                    .imePadding()
                    .padding(horizontal = WooPosSpacing.XLarge.value),
                title = items.title,
                message = items.message,
                contentDescription = stringResource(id = R.string.woopos_search_empty_image_content_description)
            )
        }
    }
}

@Composable
private fun WooPosLoadedBookingsList(
    modifier: Modifier = Modifier,
    items: Map<WooPosBookingsState.BookingItemViewState, WooPosBookingsState.BookingDetailsViewState>,
    paginationState: WooPosPaginationState,
    scrollToTopEvent: SharedFlow<Unit>,
    onBookingSelected: (Long) -> Unit,
    onEndOfBookingsListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit
) {
    val listState = rememberLazyListState()

    val loadMoreBuffer = 5
    val shouldLoadMore = remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val lastVisible = (info.visibleItemsInfo.lastOrNull()?.index ?: -1)
            lastVisible >= total - 1 - loadMoreBuffer
        }
    }
    LaunchedEffect(paginationState) {
        snapshotFlow { shouldLoadMore.value }
            .distinctUntilChanged()
            .filter { it }
            .collect { onEndOfBookingsListReached() }
    }

    LaunchedEffect(Unit) {
        scrollToTopEvent.collect {
            delay(100)
            listState.animateScrollToItem(0)
        }
    }

    WooPosLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        contentPadding = PaddingValues(WooPosSpacing.Medium.value),
        state = listState,
    ) {
        items(items.keys.toList(), key = { it.id }) { item ->
            WooPosCard(
                modifier = modifier
                    .wrapContentHeight(),
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                elevation = WooPosElevation.Medium,
                shadowType = ShadowType.Soft,
                isSelected = item.isSelected,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBookingSelected(item.id) }
                        .padding(WooPosSpacing.Medium.value),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        WooPosText(
                            item.title,
                            style = WooPosTypography.BodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(WooPosSpacing.XSmall.value))

                        WooPosText(
                            item.date,
                            style = WooPosTypography.BodySmall,
                            color = WooPosTheme.colors.onSurfaceVariantHighest,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(WooPosSpacing.XSmall.value))

                        val email = item.customerEmail.orEmpty()
                        if (email.isNotBlank()) {
                            WooPosText(
                                email,
                                style = WooPosTypography.BodySmall,
                                color = WooPosTheme.colors.onSurfaceVariantHighest,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(WooPosSpacing.Small.value))

                        WooPosBookingsStatusBadge(item.status)
                    }

                    WooPosText(
                        text = item.total,
                        style = WooPosTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }

        if (paginationState == WooPosPaginationState.Loading) {
            item {
                WooPosBookingsBookingLoadingRow()
            }
        }

        if (paginationState == WooPosPaginationState.Error) {
            item {
                WooPosBookingsPaginationErrorRow(onPaginationErrorTryAgain)
            }
        }
    }
}

@Composable
private fun WooPosBookingsEmpty(
    onActionClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WooPosEmptyScreen(
        modifier = modifier.fillMaxSize(),
        icon = WooPosIcons.OrdersEmpty,
        title = stringResource(id = R.string.woopos_bookings_empty_list_title),
        message = "",
        contentDescription = stringResource(id = R.string.woopos_bookings_empty_list_image_description),
        actionLabel = stringResource(id = R.string.woopos_bookings_empty_action_label),
        onActionClicked = onActionClicked
    )
}

@Composable
private fun WooPosBookingsError(
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WooPosErrorScreen(
        modifier = modifier,
        message = stringResource(id = R.string.woopos_orders_loading_error_title),
        reason = stringResource(id = R.string.woopos_orders_loading_error_message),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(id = R.string.woopos_orders_loading_error_retry_button),
            click = onRetryClicked
        )
    )
}

@Composable
private fun WooPosBookingsPaginationErrorRow(onPaginationErrorTryAgain: () -> Unit) {
    WooPosPaginationErrorIndicator(
        icon = null,
        message = stringResource(id = R.string.woopos_orders_pagination_error_title),
        description = stringResource(id = R.string.woopos_orders_pagination_error_content_description),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(id = R.string.woopos_orders_pagination_try_again_label),
            click = onPaginationErrorTryAgain
        ),
    )
}

@WooPosPreview
@Composable
fun WooPosBookingsScreenPreview() {
    val item1 = WooPosBookingsState.BookingItemViewState(
        id = 1,
        title = "#014",
        date = "Aug 28, 2025 at 10:31 AM",
        total = "$17.00",
        customerEmail = "johndoe@mail.com",
        isSelected = true,
        status = WooPosBookingStatus(
            text = "Completed",
            colorKey = WooPosBookingStatusColorKey.COMPLETED
        ),
        statusSlug = "Completed",
        createdAtMillis = 1
    )
    val item2 = WooPosBookingsState.BookingItemViewState(
        id = 2,
        title = "#013",
        date = "Jul 28, 2025 at 10:31 AM",
        total = "$43.90",
        customerEmail = "johndoe@mail.com",
        isSelected = false,
        status = WooPosBookingStatus(
            text = "Processing",
            colorKey = WooPosBookingStatusColorKey.PROCESSING
        ),
        statusSlug = "Completed",
        createdAtMillis = 1
    )

    val details1 = sampleBookingDetails(id = 1L, number = "#014")
    val details2 = sampleBookingDetails(id = 2L, number = "#013")

    WooPosTheme {
        WooPosBookingsScreen(
            state = WooPosBookingsState.Content(
                items = WooPosBookingsState.Content.Items.Loaded(
                    items = mapOf(
                        item1 to WooPosBookingsState.BookingDetailsViewState.Computed(orderId = 1L, details = details1),
                        item2 to WooPosBookingsState.BookingDetailsViewState.Computed(orderId = 2L, details = details2)
                    )
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                selectedDetails = details1,
                paginationState = WooPosPaginationState.None,
                dialogState = WooPosBookingsState.Content.DialogState.Hidden
            ),
            scrollToTopEvent = MutableSharedFlow(),
            onBackClicked = {},
            onRefresh = {},
            onBookingSelected = {},
            onEndOfBookingsListReached = {},
            onPaginationErrorTryAgain = {},
            onBookingsEmptyActionClicked = {},
            onBookingsLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onNavigationEvent = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosBookingsNothingFoundStatePreview() {
    val details = sampleBookingDetails()
    WooPosTheme {
        WooPosBookingsScreen(
            state = WooPosBookingsState.Content(
                items = WooPosBookingsState.Content.Items.NothingFound(
                    title = stringResource(R.string.woopos_search_orders_empty_title),
                    message = stringResource(R.string.woopos_search_orders_empty_description)
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                selectedDetails = details,
                paginationState = WooPosPaginationState.None,
                dialogState = WooPosBookingsState.Content.DialogState.Hidden
            ),
            scrollToTopEvent = MutableSharedFlow(),
            onBackClicked = {},
            onRefresh = {},
            onBookingSelected = {},
            onEndOfBookingsListReached = {},
            onPaginationErrorTryAgain = {},
            onBookingsEmptyActionClicked = {},
            onBookingsLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onNavigationEvent = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosBookingsEmptyStatePreview() {
    WooPosTheme {
        WooPosBookingsScreen(
            state = WooPosBookingsState.Empty(
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
            ),
            scrollToTopEvent = MutableSharedFlow(),
            onBackClicked = {},
            onRefresh = {},
            onBookingSelected = {},
            onEndOfBookingsListReached = {},
            onPaginationErrorTryAgain = {},
            onBookingsEmptyActionClicked = {},
            onBookingsLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Suppress("MagicNumber")
private fun sampleBookingDetails(
    id: Long = 1L,
    number: String = "#014"
) = WooPosBookingsState.BookingDetailsViewState.Computed.Details(
    id = id,
    number = number,
    dateTime = "Aug 28, 2025 at 10:31 AM",
    customerEmail = "johndoe@mail.com",
    status = WooPosBookingStatus(text = "Completed", colorKey = WooPosBookingStatusColorKey.COMPLETED),
    lineItems = listOf(
        WooPosBookingsState.BookingDetailsViewState.Computed.Details.LineItemRow(
            id = 101,
            name = "Cup",
            attributesDescription = null,
            qtyAndUnitPrice = "1 x $8.50",
            lineTotal = "$15.00",
            imageUrl = null
        ),
        WooPosBookingsState.BookingDetailsViewState.Computed.Details.LineItemRow(
            id = 102,
            name = "Coffee Container",
            attributesDescription = "Blue, Large",
            qtyAndUnitPrice = "1 x $10.00",
            lineTotal = "$8.00",
            imageUrl = null
        ),
        WooPosBookingsState.BookingDetailsViewState.Computed.Details.LineItemRow(
            id = 103,
            name = "Paper Filter",
            attributesDescription = null,
            qtyAndUnitPrice = "1 x $4.50",
            lineTotal = "$8.00",
            imageUrl = null
        )
    ),
    breakdown = WooPosBookingsState.BookingDetailsViewState.Computed.Details.TotalsBreakdown(
        products = "$23.00",
        discount = "-$5.00",
        discountCode = "8qew4mnq",
        taxes = "$0.00",
        shipping = null,
        refunds = listOf("-$3.00", "-$2.00"),
        netPayment = "$12.00"
    ),
    total = "$17.00",
    totalPaid = "$17.00",
    paymentMethodTitle = "WooCommerce In-Person Payments",
    actionsState = WooPosBookingsState.BookingActionsState.Loaded(
        listOf(
            WooPosBookingsState.BookingAction.EmailReceipt(id)
        )
    )
)
