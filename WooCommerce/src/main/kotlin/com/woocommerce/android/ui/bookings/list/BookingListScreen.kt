package com.woocommerce.android.ui.bookings.list

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.compose.BookingSummary
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.Toolbar
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
        when {
            state.bookings.isNotEmpty() -> {
                BookingList(
                    bookings = state.bookings,
                    onRefresh = state.onRefresh,
                    onLoadMore = state.onLoadMore,
                    loadingState = state.loadingState,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            state.loadingState == BookingListViewState.LoadingState.Loading -> {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingList(
    bookings: List<BookingListItem>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    loadingState: BookingListViewState.LoadingState,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = loadingState == BookingListViewState.LoadingState.Refreshing,
        onRefresh = onRefresh,
        state = rememberPullToRefreshState(),
        modifier = modifier
    ) {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            itemsIndexed(bookings) { _, booking ->
                BookingSummary(
                    model = booking.summary,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
            }

            if (loadingState == BookingListViewState.LoadingState.Appending) {
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

        InfiniteListHandler(listState = listState) {
            onLoadMore()
        }
    }
}

@Composable
@LightDarkThemePreviews
private fun BookingListPreview() {
    WooThemeWithBackground {
        BookingListScreen(
            state = BookingListViewState(
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
                loadingState = BookingListViewState.LoadingState.Idle,
                onRefresh = {},
                onLoadMore = {}
            )
        )
    }
}
