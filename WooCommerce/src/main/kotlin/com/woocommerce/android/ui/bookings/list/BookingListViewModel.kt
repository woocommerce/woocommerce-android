package com.woocommerce.android.ui.bookings.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import javax.inject.Inject

@HiltViewModel
class BookingListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingListHandler: BookingListHandler,
    private val filtersBuilder: BookingListFiltersBuilder,
    private val bookingMapper: BookingMapper,
) : ScopedViewModel(savedStateHandle) {
    private val loadingState = MutableStateFlow(BookingListLoadingState.Idle)
    private val selectedTab = savedStateHandle.getStateFlow(viewModelScope, BookingListTab.Today)
    private val searchQuery = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = String::class.java,
        key = "searchQuery"
    )

    private val sortOption = savedStateHandle.getStateFlow(viewModelScope, BookingListSortOption.NewestToOldest)

    private val isSortSheetVisible = MutableStateFlow(false)

    private var bookingsFetchJob: Job? = null
    private var bookingsLoadMoreJob: Job? = null

    private val contentState = combine(
        bookingListHandler.bookingsFlow.map { bookings ->
            with(bookingMapper) { bookings.map { it.toListItem() } }
        },
        loadingState
    ) { bookings, loadingState ->
        BookingListContentState(
            bookings = bookings,
            loadingState = loadingState,
            onRefresh = { fetchBookings(BookingListLoadingState.Refreshing) },
            onLoadMore = ::loadMore,
            onBookingClick = ::onBookingClick
        )
    }
    private val searchState = searchQuery.map {
        BookingListSearchState(
            query = it,
            onQueryChanged = { newQuery ->
                searchQuery.value = newQuery
            }
        )
    }

    val state = combine(
        contentState,
        selectedTab,
        sortOption,
        isSortSheetVisible,
        searchState
    ) { contentState, selectedTab, sortOption, sheetVisible, searchState ->
        BookingListViewState(
            contentState = contentState,
            tabState = BookingListTabState(
                selectedTab = selectedTab,
                onTabChanged = ::onTabChanged
            ),
            controlsState = BookingListControlsState(
                selectedSortOption = sortOption,
                isFilterButtonVisible = selectedTab == BookingListTab.All,
                onSortClick = ::onSortClicked,
                onFilterClick = ::onFilterClicked
            ),
            sortBottomSheetState = if (sheetVisible) {
                BookingListSortBottomSheetState(
                    selectedOption = sortOption,
                    onSelect = ::onSortOptionSelected,
                    onDismiss = ::onSortDismiss
                )
            } else {
                null
            },
            searchState = searchState
        )
    }.asLiveData()

    val bottomNavigationVisible = searchState.map { !it.isSearchActive }
        .asLiveData()

    init {
        monitorSearchAndFilterChanges()
    }

    @OptIn(FlowPreview::class)
    private fun monitorSearchAndFilterChanges() {
        launch {
            val queryFlow = searchQuery
                .drop(1) // Skip the initial value to avoid double fetch on init
                .debounce {
                    if (it.isNullOrEmpty()) 0L else AppConstants.SEARCH_TYPING_DELAY_MS
                }
            val sortFlow = sortOption.drop(1) // Skip the initial value to avoid double fetch on init

            merge(selectedTab, queryFlow, sortFlow).collectLatest {
                // Cancel any ongoing fetch or load more operations
                bookingsFetchJob?.cancel()
                bookingsLoadMoreJob?.cancel()

                bookingsFetchJob = fetchBookings(
                    initialLoadingState = if (it is BookingListSortOption) {
                        BookingListLoadingState.Refreshing
                    } else {
                        BookingListLoadingState.Loading
                    }
                )
            }
        }
    }

    private fun fetchBookings(initialLoadingState: BookingListLoadingState) = launch {
        loadingState.value = initialLoadingState
        bookingListHandler.loadBookings(
            searchQuery = searchQuery.value,
            filters = prepareFilters(),
            sortBy = sortOption.value
        ).onFailure {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
        }
        loadingState.value = BookingListLoadingState.Idle
    }

    private fun loadMore() {
        bookingsLoadMoreJob?.cancel()
        bookingsLoadMoreJob = launch {
            // If a fetch is already in progress, wait for it to complete before loading more
            bookingsFetchJob?.join()

            loadingState.value = BookingListLoadingState.Appending
            bookingListHandler.loadMore()
                .onFailure {
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
                }
            loadingState.value = BookingListLoadingState.Idle
        }
    }

    private fun onBookingClick(bookingId: Long) {
        triggerEvent(NavigateToBookingDetails(bookingId))
    }

    private fun onTabChanged(tab: BookingListTab) {
        selectedTab.value = tab
    }

    private fun onSortClicked() {
        isSortSheetVisible.value = true
    }

    private fun onSortOptionSelected(option: BookingListSortOption) {
        sortOption.value = option
        isSortSheetVisible.value = false
    }

    private fun onSortDismiss() {
        isSortSheetVisible.value = false
    }

    private fun onFilterClicked() {
        triggerEvent(NavigateToFilters)
    }

    fun onFiltersApplied() {
        // TODO Receive filters from filter screen and apply them
    }

    private fun prepareFilters(): List<BookingsFilterOption> = with(filtersBuilder) {
        listOfNotNull(
            selectedTab.value.asDateRangeFilter()
        )
    }

    data class NavigateToBookingDetails(val bookingId: Long) : MultiLiveEvent.Event()
    object NavigateToFilters : MultiLiveEvent.Event()
}
