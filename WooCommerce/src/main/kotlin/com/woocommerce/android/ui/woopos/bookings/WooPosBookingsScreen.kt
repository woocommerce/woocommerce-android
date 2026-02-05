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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosEmptyScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreenButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun WooPosBookingsScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val viewModel: WooPosBookingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    BackHandler { onNavigationEvent(WooPosNavigationEvent.GoBack) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is WooPosBookingsState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is WooPosBookingsState.Error -> {
                WooPosErrorScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    message = stringResource(R.string.woopos_bookings_error_title),
                    reason = currentState.message,
                    primaryButton = WooPosErrorScreenButtonState(
                        text = stringResource(R.string.woopos_bookings_error_retry),
                        click = viewModel::onRetryClicked
                    )
                )
            }

            is WooPosBookingsState.Empty -> {
                WooPosEmptyScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    icon = WooPosIcons.OrdersEmpty,
                    title = stringResource(R.string.woopos_bookings_empty_title),
                    message = stringResource(R.string.woopos_bookings_empty_message),
                    contentDescription = "No bookings"
                )
            }

            is WooPosBookingsState.Content -> {
                BookingsContent(
                    state = currentState,
                    onTabSelected = viewModel::onTabSelected,
                    onRefresh = viewModel::onRefresh,
                    onBookingSelected = viewModel::onBookingSelected,
                    onEndOfListReached = viewModel::onEndOfListReached,
                    onPaginationRetryClicked = viewModel::onPaginationRetryClicked,
                    onAttendanceStatusSelected = viewModel::onAttendanceStatusSelected,
                    onCancelBookingClicked = viewModel::onCancelBookingClicked,
                    onCancelConfirmed = viewModel::onCancelConfirmed,
                    onCancelDialogDismissed = viewModel::onCancelDialogDismissed,
                    onPayByCardClicked = viewModel::onPayByCardClicked,
                    onPayByCashClicked = viewModel::onPayByCashClicked,
                    onViewOrderClicked = viewModel::onViewOrderClicked,
                )
            }
        }

        WooPosToolbar(
            titleText = stringResource(R.string.woopos_bookings_title),
            onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BookingsContent(
    state: WooPosBookingsState.Content,
    onTabSelected: (BookingTab) -> Unit,
    onRefresh: () -> Unit,
    onBookingSelected: (Long) -> Unit,
    onEndOfListReached: () -> Unit,
    onPaginationRetryClicked: () -> Unit,
    onAttendanceStatusSelected: (AttendanceStatusUi) -> Unit,
    onCancelBookingClicked: () -> Unit,
    onCancelConfirmed: () -> Unit,
    onCancelDialogDismissed: () -> Unit,
    onPayByCardClicked: () -> Unit,
    onPayByCashClicked: () -> Unit,
    onViewOrderClicked: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        BookingsListPane(
            state = state,
            onTabSelected = onTabSelected,
            onRefresh = onRefresh,
            onBookingSelected = onBookingSelected,
            onEndOfListReached = onEndOfListReached,
            onPaginationRetryClicked = onPaginationRetryClicked,
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
            if (state.selectedDetail != null) {
                WooPosBookingDetailPane(
                    detail = state.selectedDetail,
                    onAttendanceStatusSelected = onAttendanceStatusSelected,
                    onCancelBookingClicked = onCancelBookingClicked,
                    onPayByCardClicked = onPayByCardClicked,
                    onPayByCashClicked = onPayByCashClicked,
                    onViewOrderClicked = onViewOrderClicked,
                    modifier = Modifier.fillMaxHeight()
                )
            } else {
                WooPosEmptyScreen(
                    modifier = Modifier.fillMaxSize(),
                    icon = WooPosIcons.OrdersEmpty,
                    title = stringResource(R.string.woopos_bookings_no_booking_selected),
                    message = "",
                    contentDescription = "No booking selected"
                )
            }
        }
    }

    val dialogState = state.dialogState
    if (dialogState is DialogState.CancelConfirmation) {
        CancelBookingDialog(
            onConfirm = onCancelConfirmed,
            onDismiss = onCancelDialogDismissed,
        )
    }
}

@Composable
private fun CancelBookingDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            WooPosText(
                text = stringResource(R.string.woopos_bookings_cancel_dialog_title),
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            WooPosText(
                text = stringResource(R.string.woopos_bookings_cancel_dialog_message),
                style = WooPosTypography.BodySmall,
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                WooPosText(
                    text = stringResource(R.string.woopos_bookings_cancel_dialog_confirm),
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                WooPosText(
                    text = stringResource(R.string.woopos_bookings_cancel_dialog_dismiss),
                    style = WooPosTypography.BodySmall,
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BookingsListPane(
    state: WooPosBookingsState.Content,
    onTabSelected: (BookingTab) -> Unit,
    onRefresh: () -> Unit,
    onBookingSelected: (Long) -> Unit,
    onEndOfListReached: () -> Unit,
    onPaginationRetryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.statusBarsPadding()
    ) {
        @Suppress("WooPosDesignSystemSpacingUsageRule")
        Spacer(modifier = Modifier.height(56.dp))

        BookingTabChips(
            selectedTab = state.selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WooPosSpacing.Medium.value)
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        val pullRefreshState = rememberPullRefreshState(
            refreshing = state.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
            onRefresh = onRefresh
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(
                    pullRefreshState,
                    enabled = state.pullToRefreshState != WooPosPullToRefreshState.Disabled &&
                        !state.isLoadingList
                )
        ) {
            if (state.isLoadingList) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                BookingsList(
                    items = state.items,
                    paginationState = state.paginationState,
                    onBookingSelected = onBookingSelected,
                    onEndOfListReached = onEndOfListReached,
                    onPaginationRetryClicked = onPaginationRetryClicked,
                    modifier = Modifier.fillMaxSize()
                )
            }

            PullRefreshIndicator(
                refreshing = state.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
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
private fun BookingTabChips(
    selectedTab: BookingTab,
    onTabSelected: (BookingTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
    ) {
        BookingTab.entries.forEach { tab ->
            val label = when (tab) {
                BookingTab.Today -> stringResource(R.string.woopos_bookings_tab_today)
                BookingTab.Upcoming -> stringResource(R.string.woopos_bookings_tab_upcoming)
                BookingTab.Canceled -> stringResource(R.string.woopos_bookings_tab_canceled)
                BookingTab.All -> stringResource(R.string.woopos_bookings_tab_all)
            }
            FilterChip(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                label = {
                    WooPosText(
                        text = label,
                        style = WooPosTypography.Caption,
                        fontWeight = if (tab == selectedTab) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun BookingsList(
    items: List<BookingListItem>,
    paginationState: WooPosPaginationState,
    onBookingSelected: (Long) -> Unit,
    onEndOfListReached: () -> Unit,
    onPaginationRetryClicked: () -> Unit,
    modifier: Modifier = Modifier,
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
            .collect { onEndOfListReached() }
    }

    WooPosLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        contentPadding = PaddingValues(WooPosSpacing.Medium.value),
        state = listState,
    ) {
        items(items, key = { it.id }) { item ->
            BookingListCard(
                item = item,
                onBookingSelected = onBookingSelected,
            )
        }

        if (paginationState == WooPosPaginationState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WooPosSpacing.Medium.value),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (paginationState == WooPosPaginationState.Error) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WooPosSpacing.Medium.value),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.TextButton(onClick = onPaginationRetryClicked) {
                        WooPosText(
                            text = stringResource(R.string.woopos_bookings_error_retry),
                            style = WooPosTypography.BodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingListCard(
    item: BookingListItem,
    onBookingSelected: (Long) -> Unit,
) {
    WooPosCard(
        modifier = Modifier.wrapContentHeight(),
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
            Column(modifier = Modifier.weight(1f)) {
                WooPosText(
                    text = "#${item.id}",
                    style = WooPosTypography.Caption,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    maxLines = 1,
                )

                Spacer(Modifier.height(WooPosSpacing.XSmall.value))

                WooPosText(
                    text = item.customerName,
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(WooPosSpacing.XSmall.value))

                WooPosText(
                    text = item.serviceName,
                    style = WooPosTypography.BodySmall,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(WooPosSpacing.XSmall.value))

                WooPosText(
                    text = item.startTime,
                    style = WooPosTypography.BodySmall,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(WooPosSpacing.Small.value))

                Row(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
                    BookingStatusBadge(item.bookingStatus)
                    item.attendanceStatus?.let { AttendanceBadge(it) }
                }
            }

            WooPosText(
                text = item.amount,
                style = WooPosTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BookingStatusBadge(status: BookingStatusUi) {
    val bgColor = when (status) {
        BookingStatusUi.Paid, BookingStatusUi.Complete -> WooPosTheme.colors.infoLowest
        BookingStatusUi.Cancelled -> WooPosTheme.colors.errorLowest
        else -> WooPosTheme.colors.default
    }
    val textColor = when (status) {
        BookingStatusUi.Paid, BookingStatusUi.Complete -> WooPosTheme.colors.onInfoLowest
        BookingStatusUi.Cancelled -> WooPosTheme.colors.onErrorLowest
        else -> WooPosTheme.colors.onDefault
    }

    WooPosText(
        text = status.label,
        style = WooPosTypography.Caption,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(WooPosSpacing.Small.value))
            .background(bgColor)
            .padding(
                horizontal = WooPosSpacing.Small.value,
                vertical = WooPosSpacing.XSmall.value,
            )
    )
}

@Composable
private fun AttendanceBadge(status: AttendanceStatusUi) {
    val bgColor = when (status) {
        AttendanceStatusUi.CheckedIn -> WooPosTheme.colors.infoLowest
        AttendanceStatusUi.NoShow -> WooPosTheme.colors.errorLowest
        else -> WooPosTheme.colors.default
    }
    val textColor = when (status) {
        AttendanceStatusUi.CheckedIn -> WooPosTheme.colors.onInfoLowest
        AttendanceStatusUi.NoShow -> WooPosTheme.colors.onErrorLowest
        else -> WooPosTheme.colors.onDefault
    }

    WooPosText(
        text = status.label,
        style = WooPosTypography.Caption,
        color = textColor,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(WooPosSpacing.Small.value))
            .background(bgColor)
            .padding(
                horizontal = WooPosSpacing.Small.value,
                vertical = WooPosSpacing.XSmall.value,
            )
    )
}
