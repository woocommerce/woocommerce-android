package com.woocommerce.android.ui.bookings

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
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.component.InfiniteListHandler

@Composable
fun BookingListScreen(viewModel: BookingListViewModel) {
    viewModel.state.observeAsState().value?.let {
        BookingListScreen(it)
    }
}

@Composable
fun BookingListScreen(state: BookingListViewModel.State) {
    when {
        state.bookings.isNotEmpty() -> {
            BookingList(
                bookings = state.bookings,
                onRefresh = state.onRefresh,
                onLoadMore = state.onLoadMore,
                loadingState = state.loadingState,
            )
        }

        state.loadingState == BookingListViewModel.LoadingState.Loading -> {
            // TODO replace with shimmer
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize()
            )
        }

        else -> {
            // TODO replace with empty state
            Text(
                text = "No bookings found",
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingList(
    bookings: List<Booking>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    loadingState: BookingListViewModel.LoadingState
) {
    PullToRefreshBox(
        isRefreshing = loadingState == BookingListViewModel.LoadingState.Refreshing,
        onRefresh = onRefresh,
        state = rememberPullToRefreshState()
    ) {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            itemsIndexed(bookings) { _, booking ->
                Text(
                    text = "Booking #${booking.id.value}",
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
            }

            if (loadingState == BookingListViewModel.LoadingState.Appending) {
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
