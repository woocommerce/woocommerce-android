package com.woocommerce.android.ui.bookings.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isTwoPanesShouldBeUsed
import com.woocommerce.android.ui.bookings.PaymentStatus
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummary
import com.woocommerce.android.ui.bookings.compose.BookingSummaryLoading
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import com.woocommerce.android.ui.bookings.details.AttendanceUpdateStatus
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCPrimaryTabRow
import com.woocommerce.android.ui.compose.component.WCPullToRefreshBox
import com.woocommerce.android.ui.compose.component.WCSearchField
import com.woocommerce.android.ui.compose.component.getText
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
                title = state.toolbarTitle.getText(),
                // When search is active, the SearchSection already shows a back button
                navigationIcon = if (state.showBackButton && !state.searchState.isSearchActive) {
                    ImageVector.vectorResource(R.drawable.ic_back_24dp)
                } else {
                    null
                },
                onNavigationButtonClick = state.onBackClick,
                actions = {
                    SearchSection(
                        searchState = state.searchState,
                        areFiltersActive = state.controlsState.areFiltersActive
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
            HorizontalDivider(thickness = 0.5.dp)

            when {
                state.contentState.isNotEmpty() -> {
                    BookingList(
                        state = state.contentState,
                        controlsState = state.controlsState,
                        listState = lazyListState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                state.contentState.loadingState == BookingListLoadingState.Loading -> {
                    BookingListLoading(state.controlsState)
                }

                else -> {
                    WCPullToRefreshBox(
                        isRefreshing = state.contentState.loadingState ==
                            BookingListLoadingState.Refreshing,
                        onRefresh = state.contentState.onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        EmptyView(
                            state = state,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .fillMaxSize()
                        )
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
                    imageVector = ImageVector.vectorResource(R.drawable.ic_search_24dp),
                    contentDescription = stringResource(R.string.search)
                )
            }
        } else {
            IconButton(onClick = {
                searchState.onQueryChanged(null)
            }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_back_24dp),
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
    controlsState: BookingListControlsState,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
            stickyHeader {
                BookingListControls(controlsState)
            }

            itemsIndexed(state.bookings) { _, booking ->
                val backgroundColor = if (context.isTwoPanesShouldBeUsed && booking.id == state.selectedBooking) {
                    colorResource(R.color.color_item_selected)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
                Column(
                    modifier = Modifier.background(backgroundColor)
                ) {
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
private fun BookingListLoading(controlsState: BookingListControlsState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        BookingListControls(controlsState)
        repeat(7) {
            BookingSummaryLoading()
            HorizontalDivider(Modifier.padding(start = 16.dp))
        }
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
                containerColor = if (state.areFiltersActive) {
                    colorResource(R.color.primary_colored_button_background)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (state.areFiltersActive) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            ),
            onClick = state.onFilterClick,
        ) {
            Text(
                text = if (state.areFiltersActive) {
                    stringResource(
                        id = R.string.bookings_filters_enabled_title,
                        state.enabledFiltersCount
                    )
                } else {
                    stringResource(R.string.bookings_filters_default_title)
                },
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
        modifier
            .verticalScroll(rememberScrollState())
            .height(IntrinsicSize.Max)
    ) {
        BookingListControls(state.controlsState)

        val innerEmptyViewModifier = Modifier
            .fillMaxSize()
            .padding(32.dp)

        if (state.searchState.query?.isNotEmpty() == true) {
            EmptySearchResultsView(
                areFiltersActive = state.controlsState.areFiltersActive,
                onClearFiltersClick = state.controlsState.onClearFiltersClick,
                modifier = innerEmptyViewModifier
            )
        } else {
            EmptyListView(
                selectedTab = state.tabState.selectedTab,
                areFiltersActive = state.controlsState.areFiltersActive,
                onClearFiltersClick = state.controlsState.onClearFiltersClick,
                modifier = innerEmptyViewModifier
            )
        }
    }
}

@Composable
private fun EmptyListView(
    selectedTab: BookingListTab,
    areFiltersActive: Boolean,
    onClearFiltersClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(
                if (areFiltersActive) R.drawable.img_empty_search else R.drawable.img_calendar_grey
            ),
            contentDescription = null,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (areFiltersActive) {
                stringResource(R.string.bookings_empty_state_title_default)
            } else {
                when (selectedTab) {
                    BookingListTab.Today -> stringResource(R.string.bookings_empty_state_title_today)
                    BookingListTab.Upcoming -> stringResource(R.string.bookings_empty_state_title_upcoming)
                    BookingListTab.All -> stringResource(R.string.bookings_empty_state_title_all)
                }
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (areFiltersActive) {
                stringResource(R.string.bookings_filtered_empty_state_description)
            } else {
                when (selectedTab) {
                    BookingListTab.Today -> stringResource(R.string.bookings_empty_state_description_today_v2)
                    BookingListTab.Upcoming -> stringResource(R.string.bookings_empty_state_description_upcoming_v2)
                    BookingListTab.All -> stringResource(R.string.bookings_empty_state_description_all)
                }
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (areFiltersActive) {
            Spacer(Modifier.height(24.dp))
            WCColoredButton(
                text = stringResource(R.string.bookings_empty_state_clear_filters_button),
                onClick = onClearFiltersClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptySearchResultsView(
    areFiltersActive: Boolean,
    onClearFiltersClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(R.drawable.img_empty_search),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.bookings_empty_state_title_default),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(
                id = if (areFiltersActive) {
                    R.string.bookings_search_no_results_with_filters
                } else {
                    R.string.bookings_search_no_results_without_filters
                }
            ),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (areFiltersActive) {
            Spacer(Modifier.height(24.dp))
            WCColoredButton(
                text = stringResource(R.string.bookings_empty_state_clear_filters_button),
                onClick = onClearFiltersClick,
                modifier = Modifier.fillMaxWidth()
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
                            summary = BookingSummaryModel(
                                date = "Aug 20, 2024",
                                name = "Women’s Haircut",
                                customerName = "Margarita Nikolaevna",
                                attendanceStatus = BookingAttendanceStatus.Attended,
                                paymentStatus = PaymentStatus.PAID,
                                isCancelled = false,
                                attendanceUpdateStatus = AttendanceUpdateStatus.Idle,
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
                    enabledFiltersCount = 0,
                    onSortClick = {},
                    onFilterClick = {},
                    onClearFiltersClick = {},
                ),
                sortBottomSheetState = null,
                searchState = BookingListSearchState(
                    query = null,
                    onQueryChanged = {}
                ),
                showBackButton = false,
                onBackClick = {}
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
                    enabledFiltersCount = 0,
                    onSortClick = {},
                    onFilterClick = {},
                    onClearFiltersClick = {},
                ),
                sortBottomSheetState = null,
                searchState = BookingListSearchState(
                    query = null,
                    onQueryChanged = {}
                ),
                showBackButton = false,
                onBackClick = {}
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
                    enabledFiltersCount = 0,
                    onSortClick = {},
                    onFilterClick = {},
                    onClearFiltersClick = {},
                ),
                sortBottomSheetState = null,
                searchState = BookingListSearchState(
                    query = "Haircut",
                    onQueryChanged = {}
                ),
                showBackButton = false,
                onBackClick = {}
            )
        )
    }
}
