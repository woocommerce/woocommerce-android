package com.woocommerce.android.ui.woopos.bookings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.bookings.details.WooPosBookingDetails
import com.woocommerce.android.ui.woopos.bookings.details.WooPosCancelBookingDialog
import com.woocommerce.android.ui.woopos.bookings.note.BOOKING_NOTE_RESULT_KEY
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
import com.woocommerce.android.ui.woopos.common.composeui.rememberRetained
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosIssueRefundDialog
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.wordpress.android.util.ToastUtils

val WOO_POS_BOOKINGS_TOOLBAR_HEIGHT = 56.dp

@Composable
fun WooPosBookingsScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    backStackEntry: NavBackStackEntry,
    refundReasonResult: String? = null,
) {
    val viewModel: WooPosBookingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { onNavigationEvent(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            ToastUtils.showToast(context, message, ToastUtils.Duration.LONG)
        }
    }

    val paymentFlowFinishedResult = backStackEntry.savedStateHandle
        .getStateFlow(BOOKING_PAYMENT_FLOW_FINISHED_KEY, false)
        .collectAsState()

    LaunchedEffect(paymentFlowFinishedResult.value) {
        if (paymentFlowFinishedResult.value) {
            viewModel.onPaymentCompleted()
            backStackEntry.savedStateHandle[BOOKING_PAYMENT_FLOW_FINISHED_KEY] = false
        }
    }

    val bookingNoteResult = backStackEntry.savedStateHandle
        .getStateFlow(BOOKING_NOTE_RESULT_KEY, false)
        .collectAsState()

    LaunchedEffect(bookingNoteResult.value) {
        if (bookingNoteResult.value) {
            viewModel.onBookingNoteSaved()
            backStackEntry.savedStateHandle[BOOKING_NOTE_RESULT_KEY] = false
        }
    }

    WooPosBookingsScreen(
        state = state,
        scrollToTopEvent = viewModel.scrollToTopEvent,
        onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
        onRefresh = viewModel::onPullToRefresh,
        onBookingSelected = viewModel::onBookingSelected,
        onEndOfBookingsListReached = viewModel::onEndOfBookingsListReached,
        onPaginationErrorTryAgain = viewModel::onPaginationErrorTryAgain,
        onBookingsLoadingErrorRetryButtonClicked = viewModel::onBookingsLoadingErrorRetryButtonClicked,
        onUIEvent = viewModel::onUIEvent,
        onIssueRefundDialogDismissed = viewModel::onIssueRefundDialogDismissed,
        onNavigationEvent = onNavigationEvent,
        refundReasonUpdate = refundReasonResult,
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WooPosBookingsScreen(
    state: WooPosBookingsState,
    scrollToTopEvent: SharedFlow<Unit>,
    onBackClicked: () -> Unit,
    onRefresh: () -> Unit,
    onBookingSelected: (Long) -> Unit,
    onEndOfBookingsListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onBookingsLoadingErrorRetryButtonClicked: () -> Unit,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit,
    onIssueRefundDialogDismissed: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    refundReasonUpdate: String? = null,
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

            is WooPosBookingsState.Error -> WooPosBookingsError(
                onRetryClicked = onBookingsLoadingErrorRetryButtonClicked,
                modifier = Modifier.statusBarsPadding()
            )

            is WooPosBookingsState.Loading -> WooPosBookingsLoadingScreen(
                dateSelectorState = state.dateSelectorState,
                onUIEvent = onUIEvent,
            )
        }

        WooPosToolbar(
            titleText = stringResource(R.string.woopos_bookings_title),
            onBackClicked = onBackClicked,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        )

        if (state is WooPosBookingsState.Content) {
            val dialogState = state.dialogState

            val retainedIssueRefund: WooPosBookingsState.Content.DialogState.IssueRefund? = rememberRetained(
                when (dialogState) {
                    is WooPosBookingsState.Content.DialogState.IssueRefund -> dialogState
                    is WooPosBookingsState.Content.DialogState.CancelBooking -> null
                    WooPosBookingsState.Content.DialogState.Hidden -> null
                }
            )

            retainedIssueRefund?.let { issueRefund ->
                WooPosIssueRefundDialog(
                    orderId = issueRefund.orderId,
                    isVisible = dialogState is WooPosBookingsState.Content.DialogState.IssueRefund,
                    onDismissRequest = onIssueRefundDialogDismissed,
                    onNavigationEvent = onNavigationEvent,
                    refundReasonUpdate = refundReasonUpdate,
                    disablePartialRefund = true
                )
            }
        }
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
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            WooPosBookingsListPane(
                state = state,
                scrollToTopEvent = scrollToTopEvent,
                onRefresh = onRefresh,
                isRefreshing = state.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
                onBookingSelected = onBookingSelected,
                onEndOfBookingsListReached = onEndOfBookingsListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                onUIEvent = onUIEvent,
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
                    state.items is WooPosBookingsState.Content.Items.Loading -> {
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
                            icon = WooPosIcons.BookingsEmpty,
                            title = stringResource(R.string.woopos_bookings_no_booking_selected),
                            message = "",
                            contentDescription = stringResource(R.string.woopos_bookings_empty_image_description)
                        )
                    }
                }
            }
        }

        val cancelDialog =
            state.dialogState as? WooPosBookingsState.Content.DialogState.CancelBooking
        BackHandler(enabled = cancelDialog != null) {
            onUIEvent(WooPosBookingsUIEvent.CancelBookingDismissed)
        }
        WooPosCancelBookingDialog(
            isVisible = cancelDialog != null,
            message = cancelDialog?.message.orEmpty(),
            isProcessing = cancelDialog is WooPosBookingsState.Content.DialogState.CancelBooking.Processing,
            errorMessage = (cancelDialog as? WooPosBookingsState.Content.DialogState.CancelBooking.Error)
                ?.errorMessage,
            onConfirm = { onUIEvent(WooPosBookingsUIEvent.CancelBookingConfirmed) },
            onDismiss = { onUIEvent(WooPosBookingsUIEvent.CancelBookingDismissed) },
        )
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
    onUIEvent: (WooPosBookingsUIEvent) -> Unit,
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

        state.dateSelectorState?.let { dateSelectorState ->
            WooPosBookingsDateSelector(
                dateSelectorState = dateSelectorState,
                onUIEvent = onUIEvent,
            )
        }

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
                onRetry = onRefresh,
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
    onRetry: () -> Unit,
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

        is WooPosBookingsState.Content.Items.Loading -> {
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
                        click = onRetry
                    )
                )
            }
        }

        is WooPosBookingsState.Content.Items.NothingFound -> {
            WooPosEmptyScreen(
                modifier = modifier
                    .imePadding()
                    .padding(horizontal = WooPosSpacing.XXLarge.value),
                title = items.title,
                message = items.message,
                contentDescription = stringResource(id = R.string.woopos_bookings_empty_image_description)
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
            WooPosBookingListItem(
                item = item,
                onBookingSelected = onBookingSelected,
            )
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
private fun WooPosBookingListItem(
    item: WooPosBookingsState.BookingItemViewState,
    onBookingSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        ) {
            Column(modifier = Modifier.weight(1f)) {
                WooPosText(
                    item.timeRange,
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(WooPosSpacing.XSmall.value))

                WooPosText(
                    item.subtitle,
                    style = WooPosTypography.BodySmall,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(WooPosSpacing.Small.value))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value),
                ) {
                    if (item.isCancelled) {
                        WooPosCancelledBadge()
                    }
                    item.attendanceBadge?.let { WooPosAttendanceBadge(it) }
                    WooPosPaymentStatusBadge(item.paymentStatus)
                }
            }

            item.teamMember?.let { teamMember ->
                WooPosTeamMemberAvatar(
                    teamMember = teamMember,
                    modifier = Modifier.padding(start = WooPosSpacing.Small.value),
                )
            }
        }
    }
}

@Composable
private fun WooPosTeamMemberAvatar(
    teamMember: WooPosBookingsState.BookingItemViewState.TeamMember,
    modifier: Modifier = Modifier,
) {
    val avatarSize = 24.dp
    var imageLoaded by remember { mutableStateOf(false) }
    val borderModifier = if (imageLoaded) {
        Modifier.border(1.dp, WooPosTheme.colors.outline, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(avatarSize)
            .then(borderModifier)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceDim),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageLoaded) {
            WooPosText(
                text = teamMember.initials,
                style = WooPosTypography.Caption,
                color = WooPosTheme.colors.onSurfaceVariantHighest,
                maxLines = 1,
            )
        }
        if (teamMember.avatarUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(teamMember.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(avatarSize),
                onSuccess = { imageLoaded = true },
                onError = { imageLoaded = false },
            )
        }
    }
}

@Composable
private fun WooPosBookingsError(
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WooPosErrorScreen(
        modifier = modifier,
        message = stringResource(id = R.string.woopos_bookings_loading_error_title),
        reason = stringResource(id = R.string.woopos_bookings_loading_error_message),
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
        message = stringResource(id = R.string.woopos_bookings_pagination_error_title),
        description = stringResource(id = R.string.woopos_bookings_pagination_error_description),
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
        timeRange = "10:00 - 10:30 AM",
        subtitle = "Women's Haircut \u00B7 John Doe",
        isSelected = true,
        paymentStatus = PaymentStatus.PAID,
        isCancelled = false,
        attendanceBadge = WooPosBookingsState.AttendanceState.ATTENDED,
        teamMember = WooPosBookingsState.BookingItemViewState.TeamMember(
            initials = "JD",
            avatarUrl = null,
        ),
    )
    val item2 = WooPosBookingsState.BookingItemViewState(
        id = 2,
        timeRange = "10:30 - 11:30 AM",
        subtitle = "Women's Haircut \u00B7 Jane Smith",
        isSelected = false,
        paymentStatus = PaymentStatus.UNPAID,
        isCancelled = true,
        attendanceBadge = null,
        teamMember = WooPosBookingsState.BookingItemViewState.TeamMember(
            initials = "JS",
            avatarUrl = null,
        ),
    )

    val details1 = sampleBookingDetails(id = 1L, number = "#014")
    val details2 = sampleBookingDetails(id = 2L, number = "#013")

    WooPosTheme {
        WooPosBookingsScreen(
            state = WooPosBookingsState.Content(
                items = WooPosBookingsState.Content.Items.Loaded(
                    items = mapOf(
                        item1 to details1,
                        item2 to details2
                    )
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                dateSelectorState = DateSelectorState(
                    formattedDate = "19 Feb, Wed",
                    selectedDateMillis = System.currentTimeMillis(),
                ),
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
            onBookingsLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onIssueRefundDialogDismissed = {},
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
                    title = stringResource(R.string.woopos_bookings_no_bookings_for_date),
                    message = stringResource(R.string.woopos_bookings_no_bookings_for_date_message)
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                dateSelectorState = DateSelectorState(
                    formattedDate = "19 Feb, Wed",
                    selectedDateMillis = System.currentTimeMillis(),
                ),
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
            onBookingsLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onIssueRefundDialogDismissed = {},
            onNavigationEvent = {}
        )
    }
}

@Suppress("MagicNumber")
private fun sampleBookingDetails(
    id: Long = 1L,
    number: String = "#014"
) = WooPosBookingsState.BookingDetailsViewState(
    id = id,
    orderId = id * 10,
    number = number,
    paymentStatus = PaymentStatus.PAID,
    isCancelled = false,
    actionsState = WooPosBookingsState.BookingActionsState.Loaded(
        listOf(WooPosBookingsState.BookingAction.EmailReceipt(id))
    ),
    headerTitle = "10:30 AM - 11:30 AM",
    headerSubtitle = "Women's Haircut \u00B7 John Doe",
    attendanceBadge = WooPosBookingsState.AttendanceState.ATTENDED,
    bookingName = "Women's Haircut",
    appointmentDate = "Thursday, August 28, 2025",
    appointmentTime = "10:30 AM - 11:30 AM",
    duration = "1 hr",
    teamMember = null,
    location = null,
    customerSection = WooPosBookingsState.CustomerSection(
        name = "John Doe",
        email = "johndoe@mail.com",
        phone = "+1 555-123-4567",
        billingAddress = null,
        note = null,
        isGuest = false,
    ),
    attendanceSection = WooPosBookingsState.AttendanceSection.Visible(
        selection = WooPosBookingsState.AttendanceState.ATTENDED,
    ),
    paymentSection = WooPosBookingsState.PaymentSection(
        serviceAmount = "$55.00",
        taxAmount = "$4.50",
        discountAmount = "-",
        totalAmount = "$59.50",
        paidWithLabel = "WooCommerce In-Person Payments",
        collectPaymentLabel = null,
    ),
    bookingNote = null,
)
