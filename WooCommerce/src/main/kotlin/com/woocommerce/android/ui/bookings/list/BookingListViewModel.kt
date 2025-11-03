package com.woocommerce.android.ui.bookings.list

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.filter.data.BookingFilterRepository
import com.woocommerce.android.util.IsWindowClassLargeThanCompact
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingFilterRepository: BookingFilterRepository,
    private val bookingListHandler: BookingListHandler,
    private val filtersBuilder: BookingListFiltersBuilder,
    private val bookingMapper: BookingMapper,
    private val isWindowClassLargeThanCompact: IsWindowClassLargeThanCompact,
) : ScopedViewModel(savedStateHandle) {

    companion object {
        @VisibleForTesting
        const val KEY_BOOKING_SELECTED_ON_BIG_SCREEN = "key_booking_selected_on_big_screen"
    }

    private val stateHandle = savedStateHandle
    private val loadingState = MutableStateFlow(BookingListLoadingState.Idle)
    private val selectedTab = savedStateHandle.getStateFlow(viewModelScope, BookingListTab.Today)
    private val searchQuery = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = String::class.java,
        key = "searchQuery"
    )
    private val filters = bookingFilterRepository.bookingFiltersFlow
        .shareIn(viewModelScope, started = SharingStarted.WhileSubscribed(), replay = 1)

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val sortOption = savedStateHandle.getStateFlow(viewModelScope, BookingListSortOption.NewestToOldest)

    private var selectedBookingIdOnBigScreen: Long?
        get() = stateHandle[KEY_BOOKING_SELECTED_ON_BIG_SCREEN]
        set(value) = stateHandle.set(KEY_BOOKING_SELECTED_ON_BIG_SCREEN, value)

    private val isSortSheetVisible = MutableStateFlow(false)

    private var bookingsFetchJob: Job? = null
    private var bookingsLoadMoreJob: Job? = null

    private fun openFirstLoadedBookingOnTablet(bookings: List<BookingEntity>) {
        if (isWindowClassLargeThanCompact() && bookings.isNotEmpty() && selectedBookingIdOnBigScreen == null) {
            val firstId = bookings.first().id.value
            selectedBookingIdOnBigScreen = firstId
            triggerEvent(NavigateToBookingDetails(firstId))
        }
    }

    private val contentState = combine(
        bookingListHandler.bookingsFlow.map { bookings ->
            openFirstLoadedBookingOnTablet(bookings)
            with(bookingMapper) { bookings.map { it.toListItem() } }
        },
        loadingState
    ) { bookings, loadingState ->
        BookingListContentState(
            bookings = bookings,
            loadingState = loadingState,
            onRefresh = { refreshTrigger.tryEmit(Unit) },
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
    private val tabsState = selectedTab.map {
        BookingListTabState(
            selectedTab = it,
            onTabChanged = ::onTabChanged
        )
    }
    private val controlsState = combine(
        selectedTab,
        sortOption,
        filters
    ) { tab, sort, filters ->
        BookingListControlsState(
            selectedSortOption = sort,
            isFilterButtonVisible = tab == BookingListTab.All,
            enabledFiltersCount = filters.enabledFiltersCount,
            onSortClick = ::onSortClicked,
            onFilterClick = ::onFilterClicked,
            onClearFiltersClick = ::onClearFiltersClicked
        )
    }
    private val listSortBottomSheetState = combine(
        sortOption,
        isSortSheetVisible
    ) { sortOption, sheetVisible ->
        if (sheetVisible) {
            BookingListSortBottomSheetState(
                selectedOption = sortOption,
                onSelect = ::onSortOptionSelected,
                onDismiss = ::onSortDismiss
            )
        } else {
            null
        }
    }

    val state = combine(
        contentState,
        tabsState,
        controlsState,
        listSortBottomSheetState,
        searchState
    ) { contentState, tabsState, controlsState, listSortBottomSheetState, searchState ->
        BookingListViewState(
            contentState = contentState,
            tabState = tabsState,
            controlsState = controlsState,
            sortBottomSheetState = listSortBottomSheetState,
            searchState = searchState
        )
    }.asLiveData()

    val bottomNavigationVisible = searchState.map { !it.isSearchActive }
        .asLiveData()

    init {
        monitorSearchAndFilterChanges()
    }

    private fun monitorSearchAndFilterChanges() {
        launch {
            var lastFetchParams: FetchParams? = null
            val queryFlow = searchQuery
                .withIndex()
                .debounce { (index, query) ->
                    // Skip debounce for the initial value or when the query is cleared
                    if (index == 0 || query.isNullOrEmpty()) 0L else AppConstants.SEARCH_TYPING_DELAY_MS
                }
                .map { it.value }

            combine(
                selectedTab,
                queryFlow,
                sortOption,
                filters
            ) { tab, query, sort, filters ->
                FetchParams(
                    searchQuery = query,
                    sortOption = sort,
                    selectedTab = tab,
                    filters = filters
                )
            }.flatMapLatest { fetchParams ->
                refreshTrigger.map { true }
                    .onStart { emit(false) }
                    .map { isRefreshing -> Pair(fetchParams, isRefreshing) }
            }.collectLatest { (fetchParams, isRefreshing) ->
                // Cancel any ongoing fetch or load more operations
                bookingsFetchJob?.cancel()
                bookingsLoadMoreJob?.cancel()

                val initialLoadingState = if (isRefreshing) {
                    BookingListLoadingState.Refreshing
                } else {
                    lastFetchParams.let { lastFetchParams ->
                        if (lastFetchParams != null && lastFetchParams.sortOption != fetchParams.sortOption) {
                            // When sort option changes, force refreshing state to indicate data reload
                            BookingListLoadingState.Refreshing
                        } else {
                            BookingListLoadingState.Loading
                        }
                    }
                }

                lastFetchParams = fetchParams
                fetchBookings(
                    initialLoadingState = initialLoadingState,
                    fetchParams = fetchParams
                )
            }
        }
    }

    private suspend fun fetchBookings(
        initialLoadingState: BookingListLoadingState,
        fetchParams: FetchParams,
    ) {
        loadingState.value = initialLoadingState
        bookingListHandler.loadBookings(
            searchQuery = fetchParams.searchQuery,
            filters = fetchParams.prepareFilters(),
            sortBy = fetchParams.sortOption
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
        if (isWindowClassLargeThanCompact()) {
            selectedBookingIdOnBigScreen = bookingId
        }
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

    private fun onClearFiltersClicked() {
        launch {
            bookingFilterRepository.save(BookingFilters.EMPTY)
        }
    }

    private fun FetchParams.prepareFilters(): List<BookingsFilterOption> = with(filtersBuilder) {
        when (selectedTab) {
            BookingListTab.Today,
            BookingListTab.Upcoming -> listOfNotNull(
                selectedTab.asDateRangeFilter()
            )

            BookingListTab.All -> filters.asList()
        }
    }

    private data class FetchParams(
        val searchQuery: String?,
        val sortOption: BookingListSortOption,
        val selectedTab: BookingListTab,
        val filters: BookingFilters
    )

    data class NavigateToBookingDetails(val bookingId: Long) : MultiLiveEvent.Event()
    object NavigateToFilters : MultiLiveEvent.Event()
}
