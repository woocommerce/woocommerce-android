package com.woocommerce.android.ui.bookings.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummary
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCPrimaryTabRow
import com.woocommerce.android.ui.compose.component.WCPullToRefreshBox
import com.woocommerce.android.ui.compose.component.WCSearchField
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
                navigationIcon = null,
                actions = {
                    SearchSection(
                        searchState = state.searchState,
                        areFiltersActive = state.areFiltersActive
                    )
                }
            )
        },
        contentWindowInsets = WindowInsets()
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
                }
            )
            BookingListControls(state.controlsState)
            HorizontalDivider(thickness = 0.5.dp)

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
                    val modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp)

                    if (state.searchState.isSearchActive) {
                        EmptySearchResultsView(state.searchState.query.orEmpty(), modifier)
                    } else {
                        EmptyView(state, modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSection(
    searchState: BookingListSearchState,
    areFiltersActive: Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchState.isSearchActive) {
        if (searchState.isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        if (!searchState.isSearchActive) {
            IconButton(onClick = {
                searchState.onQueryChanged("")
            }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search)
                )
            }
        } else {
            IconButton(onClick = {
                searchState.onQueryChanged(null)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            WCSearchField(
                value = searchState.query ?: "",
                onValueChange = { searchState.onQueryChanged(it) },
                hint = if (areFiltersActive) {
                    stringResource(R.string.bookings_search_with_filters_hint)
                } else {
                    stringResource(R.string.bookings_search_hint)
                },
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            modifier = Modifier.defaultMinSize(minHeight = 36.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
            onClick = state.onSortClick,
        ) {
            Text(
                text = state.selectedSortOption.shortName(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_drop_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(
            modifier = Modifier.defaultMinSize(minWidth = 88.dp, minHeight = 36.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            colors = ButtonDefaults.outlinedButtonColors().copy(
                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            onClick = state.onFilterClick,
        ) {
            Text(
                text = stringResource(R.string.bookings_filters_default_title),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun EmptyView(
    state: BookingListViewState,
    modifier: Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(R.drawable.img_calendar_grey),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = when (state.tabState.selectedTab) {
                BookingListTab.Today -> stringResource(R.string.bookings_empty_state_title_today)
                BookingListTab.Upcoming -> stringResource(R.string.bookings_empty_state_title_upcoming)
                BookingListTab.All -> stringResource(R.string.bookings_empty_state_title_default)
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (state.tabState.selectedTab) {
                BookingListTab.Today -> stringResource(R.string.bookings_empty_state_description_today)
                BookingListTab.Upcoming -> stringResource(R.string.bookings_empty_state_description_upcoming)
                else -> {
                    if (state.areFiltersActive) {
                        TODO()
                    } else {
                        stringResource(R.string.bookings_empty_state_description_default)
                    }
                }
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptySearchResultsView(query: String, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(R.drawable.search_failed_illustration),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = annotatedStringResLegacy(R.string.bookings_search_no_results, query),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
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
                            summary = BookingSummaryModel(
                                date = "Aug 20, 2024",
                                name = "Women’s Haircut",
                                customerName = "Margarita Nikolaevna",
                                attendanceStatus = BookingAttendanceStatus.BOOKED,
                                status = BookingStatus.Paid
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
                    selectedSortOption = BookingListSortOption.NewestToOldest,
                    onSortClick = {},
                    onFilterClick = {}
                ),
                sortBottomSheetState = null,
                searchState = BookingListSearchState(
                    query = null,
                    onQueryChanged = {}
                )
            )
        )
    }
}

@Composable
@LightDarkThemePreviews
private fun EmptyViewPreview() {
    WooThemeWithBackground {
        BookingListScreen(
            state = BookingListViewState(
                contentState = BookingListContentState(
                    bookings = emptyList(),
                    loadingState = BookingListLoadingState.Idle,
                    onRefresh = {},
                    onLoadMore = {},
                    onBookingClick = {}
                ),
                tabState = BookingListTabState(
                    selectedTab = BookingListTab.All,
                    onTabChanged = {}
                ),
                controlsState = BookingListControlsState(
                    selectedSortOption = BookingListSortOption.NewestToOldest,
                    onSortClick = {},
                    onFilterClick = {}
                ),
                sortBottomSheetState = null,
                searchState = BookingListSearchState(
                    query = null,
                    onQueryChanged = {}
                )
            )
        )
    }
}

@Composable
@LightDarkThemePreviews
private fun EmptySearchResultsViewPreview() {
    WooThemeWithBackground {
        BookingListScreen(
            state = BookingListViewState(
                contentState = BookingListContentState(
                    bookings = emptyList(),
                    loadingState = BookingListLoadingState.Idle,
                    onRefresh = {},
                    onLoadMore = {},
                    onBookingClick = {}
                ),
                tabState = BookingListTabState(
                    selectedTab = BookingListTab.All,
                    onTabChanged = {}
                ),
                controlsState = BookingListControlsState(
                    selectedSortOption = BookingListSortOption.NewestToOldest,
                    onSortClick = {},
                    onFilterClick = {}
                ),
                sortBottomSheetState = null,
                searchState = BookingListSearchState(
                    query = "Haircut",
                    onQueryChanged = {}
                )
            )
        )
    }
}
