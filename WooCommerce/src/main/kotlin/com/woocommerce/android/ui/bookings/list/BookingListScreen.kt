package com.woocommerce.android.ui.bookings.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.compose.BookingSummary
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCPullToRefreshBox
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingListScreen(viewModel: BookingListViewModel) {
    viewModel.state.observeAsState().value?.let {
        BookingListScreen(it)
    }
}

@Composable
fun BookingListScreen(state: BookingListViewState) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(R.string.bookings_tab_title),
                navigationIcon = null
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            BookingListTabs(
                tabState = state.tabState,
                modifier = Modifier
            )
            when {
                state.contentState.isNotEmpty() -> {
                    BookingList(
                        state = state.contentState,
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
    modifier: Modifier = Modifier
) {
    WCPullToRefreshBox(
        isRefreshing = state.loadingState == BookingListLoadingState.Refreshing,
        onRefresh = state.onRefresh,
        state = rememberPullToRefreshState(),
        modifier = modifier
    ) {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
        ) {
            itemsIndexed(state.bookings) { _, booking ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingListTabs(
    tabState: BookingListTabState,
    modifier: Modifier = Modifier
) {
    val selectedTabIndex = BookingListTab.entries.indexOf(tabState.selectedTab)
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = colorResource(id = R.color.color_toolbar),
        modifier = modifier
    ) {
        BookingListTab.entries.forEach { tab ->
            Tab(
                selected = tab == tabState.selectedTab,
                onClick = { tabState.onTabChanged(tab) },
                text = {
                    Text(
                        text = tab.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
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
                )
            )
        )
    }
}
