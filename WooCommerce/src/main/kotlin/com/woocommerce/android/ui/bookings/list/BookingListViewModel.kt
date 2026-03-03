package com.woocommerce.android.ui.bookings.list

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.PaymentStatusResolver
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.ExcludedBookingStatuses
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.BookingEntity.Status.Cancelled
import org.wordpress.android.fluxc.persistence.entity.BookingEntity.Status.Complete
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookingFilterRepository: BookingFilterRepository,
    private val bookingListHandler: BookingListHandler,
    private val dateFilterBuilder: BookingListDateFilterBuilder,
    private val bookingMapper: BookingMapper,
    private val isWindowClassLargeThanCompact: IsWindowClassLargeThanCompact,
    private val paymentStatusResolver: PaymentStatusResolver,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
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

    private val selectedBookingIdFlow: Flow<Long?> =
        stateHandle.getStateFlow(KEY_BOOKING_SELECTED_ON_BIG_SCREEN, null)

    private val isSortSheetVisible = MutableStateFlow(false)

    private var bookingsFetchJob: Job? = null
    private var bookingsLoadMoreJob: Job? = null

    private val contentState = combine(
        bookingListHandler.bookingsFlow,
        loadingState,
        selectedBookingIdFlow,
    ) { bookings, loadingState, selectedBookingId ->
        openFirstLoadedBookingOnTablet(bookings)
        val listItems = with(bookingMapper) {
            bookings.map {
                val paymentStatus = paymentStatusResolver.resolve(it.orderId)
                it.toListItem(paymentStatus)
            }
        }
        BookingListContentState(
            bookings = listItems,
            loadingState = loadingState,
            selectedBooking = selectedBookingId,
            onRefresh = { refreshTrigger.tryEmit(Unit) },
            onLoadMore = ::loadMore,
            onBookingClick = ::onBookingClick
        )
    }
    private val searchState = searchQuery.map {
        BookingListSearchState(
            query = it,
            onQueryChanged = ::onSearchQueryChanged
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
                    searchQuery = query?.takeUnless { it.isEmpty() },
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

                if (!isRefreshing && lastFetchParams == fetchParams) return@collectLatest

                val initialLoadingState = if (isRefreshing ||
                    (lastFetchParams != null && lastFetchParams?.sortOption != fetchParams.sortOption)
                ) {
                    BookingListLoadingState.Refreshing
                } else {
                    BookingListLoadingState.Loading
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
        ).onSuccess {
            trackBookingListView(fetchParams)
        }.onFailure {
            analyticsTrackerWrapper.track(
                stat = AnalyticsEvent.BOOKING_LIST_FAILED_TO_FETCH_BOOKINGS,
                properties = mapOf("error_code" to (it::class.java.simpleName ?: "")),
                errorContext = this::class.java.simpleName,
                errorType = null,
                errorDescription = it.message
            )
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
        val enabledFiltersCount = state.value?.controlsState?.enabledFiltersCount ?: 0
        analyticsTrackerWrapper.track(
            AnalyticsEvent.BOOKING_LIST_BOOKING_TAP,
            mapOf(
                "is_search_active" to (searchQuery.value != null).toString(),
                "is_filtering_active" to (enabledFiltersCount > 0).toString(),
                "selected_tab" to selectedTab.value.toAnalyticsValue()
            )
        )
        if (isWindowClassLargeThanCompact()) {
            selectedBookingIdOnBigScreen = bookingId
        }
        triggerEvent(NavigateToBookingDetails(bookingId))
    }

    private fun onTabChanged(tab: BookingListTab) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.BOOKING_LIST_TAB_SELECT,
            mapOf("selected_tab" to tab.toAnalyticsValue())
        )
        selectedTab.value = tab
    }

    private fun onSortClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.BOOKING_LIST_SORT_BY_TAP)
        isSortSheetVisible.value = true
    }

    private fun onSortOptionSelected(option: BookingListSortOption) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.BOOKING_LIST_SORT_BY_OPTION_TAP,
            mapOf("sort_option" to option.toAnalyticsValue())
        )
        sortOption.value = option
        isSortSheetVisible.value = false
    }

    private fun onSortDismiss() {
        isSortSheetVisible.value = false
    }

    private fun onFilterClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.BOOKING_LIST_FILTERS_TAP)
        triggerEvent(NavigateToFilters)
    }

    private fun onClearFiltersClicked() {
        launch {
            bookingFilterRepository.save(BookingFilters.EMPTY)
        }
    }

    private fun openFirstLoadedBookingOnTablet(bookings: List<BookingEntity>) {
        if (isWindowClassLargeThanCompact() && bookings.isNotEmpty() && selectedBookingIdOnBigScreen == null) {
            val firstId = bookings.first().id.value
            selectedBookingIdOnBigScreen = firstId
            triggerEvent(NavigateToBookingDetails(firstId))
        }
    }

    private fun FetchParams.prepareFilters(): BookingFilters = when (selectedTab) {
        BookingListTab.Today,
        BookingListTab.Upcoming -> BookingFilters(
            dateRange = dateFilterBuilder.prepareDateFilter(selectedTab, filters.dateRange),
            excludedBookingStatuses = ExcludedBookingStatuses(setOf(Cancelled, Complete))
        )

        BookingListTab.All -> filters.copy(
            dateRange = dateFilterBuilder.prepareDateFilter(selectedTab, filters.dateRange)
        )
    }

    private fun onSearchQueryChanged(newQuery: String?) {
        if (searchQuery.value == null && newQuery != null) {
            analyticsTrackerWrapper.track(AnalyticsEvent.BOOKING_LIST_SEARCH_TAP)
        }
        searchQuery.value = newQuery
    }

    private suspend fun trackBookingListView(fetchParams: FetchParams) {
        val bookings = bookingListHandler.bookingsFlow.first()
        analyticsTrackerWrapper.track(
            AnalyticsEvent.BOOKING_LIST_VIEW,
            mapOf(
                "selected_tab" to fetchParams.selectedTab.toAnalyticsValue(),
                "is_default_tab" to (fetchParams.selectedTab == BookingListTab.Today).toString(),
                "is_list_empty" to bookings.isEmpty().toString(),
                "is_filtered" to
                    (fetchParams.filters.enabledFiltersCount > 0).toString()
            )
        )
    }

    private fun BookingListTab.toAnalyticsValue(): String = when (this) {
        BookingListTab.Today -> "today"
        BookingListTab.Upcoming -> "upcoming"
        BookingListTab.All -> "all"
    }

    private fun BookingListSortOption.toAnalyticsValue(): String = when (this) {
        BookingListSortOption.NewestToOldest -> "newest_first"
        BookingListSortOption.OldestToNewest -> "oldest_first"
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
