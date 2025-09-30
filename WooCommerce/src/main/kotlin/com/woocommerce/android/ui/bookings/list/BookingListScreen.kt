package com.woocommerce.android.ui.bookings.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.compose.BookingSummary
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCPrimaryTabRow
import com.woocommerce.android.ui.compose.component.WCPullToRefreshBox
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import kotlinx.coroutines.launch

@Composable
fun BookingListScreen(viewModel: BookingListViewModel) {
    viewModel.state.observeAsState().value?.let {
        BookingListScreen(it)
    }
}

@Composable
fun BookingListScreen(state: BookingListViewState) {
    state.sortBottomSheetState?.let { BookingSortBottomSheet(it) }
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(R.string.bookings_tab_title),
                navigationIcon = null
            )
        }
    ) { paddingValues ->
        val coroutineScope = rememberCoroutineScope()
        val lazyListState = rememberLazyListState()

        Column(modifier = Modifier.padding(paddingValues)) {
            WCPrimaryTabRow(
                tabs = BookingListTab.entries,
                selectedTab = state.tabState.selectedTab,
                tabName = { it.name() },
                onTabSelected = {
                    // Scroll to top when tab changes
                    coroutineScope.launch {
                        lazyListState.scrollToItem(0)
                    }
                    state.tabState.onTabChanged(it)
                },
                modifier = Modifier
            )
            if (state.contentState.isNotEmpty()) {
                BookingListControls(state.controlsState)
                HorizontalDivider()
            }
            when {
                state.contentState.isNotEmpty() -> {
                    BookingList(
                        state = state.contentState,
                        listState = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                state.contentState.loadingState == BookingListLoadingState.Loading -> {
                    // TODO replace with shimmer
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                            .wrapContentSize()
                    )
                }

                else -> {
                    // TODO replace with empty state
                    Text(
                        text = "No bookings found",
                        modifier = Modifier
                            .padding(paddingValues)
                            .wrapContentSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingList(
    state: BookingListContentState,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    WCPullToRefreshBox(
        isRefreshing = state.loadingState == BookingListLoadingState.Refreshing,
        onRefresh = state.onRefresh,
        state = rememberPullToRefreshState(),
        modifier = modifier
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
        ) {
            itemsIndexed(state.bookings) { _, booking ->
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
                    BookingSummary(
                        model = booking.summary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { state.onBookingClick(booking.id) })
                    )
                    HorizontalDivider(
                        Modifier.padding(start = 16.dp)
                    )
                }
            }

            if (state.loadingState == BookingListLoadingState.Appending) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }

        InfiniteListHandler(listState = listState, onLoadMore = state.onLoadMore)
    }
}

@Composable
private fun BookingListControls(
    state: BookingListControlsState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        WCTextButton(onClick = state.onSortClick) {
            Text(
                text = stringResource(R.string.product_list_sorting_header),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        WCTextButton(onClick = state.onFilterClick) {
            Text(
                text = stringResource(R.string.orderlist_filter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun BookingListTab.name(): String = when (this) {
    BookingListTab.Today -> stringResource(R.string.bookings_tab_today)
    BookingListTab.Upcoming -> stringResource(R.string.bookings_tab_upcoming)
    BookingListTab.All -> stringResource(R.string.bookings_tab_all)
}

@Composable
@LightDarkThemePreviews
private fun BookingListPreview() {
    WooThemeWithBackground {
        BookingListScreen(
            state = BookingListViewState(
                contentState = BookingListContentState(
                    bookings = List(20) {
                        BookingListItem(
                            id = it.toLong(),
                            summary = com.woocommerce.android.ui.bookings.compose.BookingSummaryModel(
                                date = "Aug 20, 2024",
                                name = "Women’s Haircut",
                                customerName = "Margarita Nikolaevna",
                                attendanceStatus = com.woocommerce.android.ui.bookings.compose.AttendanceStatus.BOOKED,
                                paymentStatus = com.woocommerce.android.ui.bookings.compose.BookingPaymentStatus.PAID
                            )
                        )
                    },
                    loadingState = BookingListLoadingState.Idle,
                    onRefresh = {},
                    onLoadMore = {},
                    onBookingClick = {}
                ),
                tabState = BookingListTabState(
                    selectedTab = BookingListTab.Today,
                    onTabChanged = {}
                ),
                controlsState = BookingListControlsState(
                    onSortClick = {},
                    onFilterClick = {}
                ),
                sortBottomSheetState = null
            )
        )
    }
}
