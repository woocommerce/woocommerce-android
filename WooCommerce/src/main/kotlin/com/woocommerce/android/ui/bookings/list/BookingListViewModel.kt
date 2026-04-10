package com.woocommerce.android.ui.bookings.list

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.automattic.eventhorizon.BookingListBookingTapEvent
import com.automattic.eventhorizon.BookingListFiltersTapEvent
import com.automattic.eventhorizon.BookingListSearchTapEvent
import com.automattic.eventhorizon.BookingListSortByOptionTapEvent
import com.automattic.eventhorizon.BookingListSortByTapEvent
import com.automattic.eventhorizon.BookingListTabSelectEvent
import com.automattic.eventhorizon.BookingListViewEvent
import com.woocommerce.android.AppConstants
import com.woocommerce.android.BookingsArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.bookings.BookingAnalyticsHelper
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.PaymentStatusResolver
import com.woocommerce.android.ui.bookings.filter.data.BookingFilterRepository
import com.woocommerce.android.ui.bookings.toEventHorizonValue
import com.woocommerce.android.util.IsWindowClassLargeThanCompact
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
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
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private val analyticsHelper = BookingAnalyticsHelper()
    private val navArgs: BookingsArgs by savedStateHandle.navArgs()

    private val loadingState = MutableStateFlow(BookingListLoadingState.Idle)

    private val selectedTab = savedState.getStateFlow(viewModelScope, BookingListTab.Today)
    private var didUserSwitchTab: Boolean
        get() = savedState["did_user_switch_tab"] ?: false
        set(value) = savedState.set("did_user_switch_tab", value)

    private val searchQuery = savedState.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = String::class.java,
        key = "searchQuery"
    )
    private val filters = bookingFilterRepository.bookingFiltersFlow
        .shareIn(viewModelScope, started = SharingStarted.WhileSubscribed(), replay = 1)

    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val sortOption = savedState.getStateFlow(viewModelScope, BookingListSortOption.NewestToOldest)

    private val selectedBookingIdOnBigScreen = savedState.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = Long::class.java,
        key = KEY_BOOKING_SELECTED_ON_BIG_SCREEN
    )

    private val isSortSheetVisible = MutableStateFlow(false)

    private var bookingsFetchJob: Job? = null
    private var bookingsLoadMoreJob: Job? = null

    private val bookingListItems = bookingListHandler.bookingsFlow
        .distinctUntilChanged()
        .map { bookings ->
            openFirstLoadedBookingOnTablet(bookings)
            val paymentStatusesByOrderId = paymentStatusResolver.resolveAll(bookings.map { it.orderId })
            with(bookingMapper) {
                bookings.map { booking ->
                    booking.toListItem(paymentStatusesByOrderId.getValue(booking.orderId))
                }
            }
        }

    private val contentState = combine(
        bookingListItems,
        loadingState,
        selectedBookingIdOnBigScreen,
    ) { listItems, loadingState, selectedBookingId ->
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

    private val _state = combine(
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
            searchState = searchState,
            showBackButton = !navArgs.showBottomNavigation,
            onBackClick = ::onBackClick
        )
    }.shareIn(
        scope = viewModelScope,
        // Drop replayed values once no one is observing, so analytics reads fresh state on return.
        started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
        replay = 1
    )
    val state = _state.asLiveData()

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
        ).onFailure {
            with(analyticsHelper) {
                analyticsTrackerWrapper.trackError(
                    event = AnalyticsEvent.BOOKING_LIST_FAILED_TO_FETCH_BOOKINGS,
                    throwable = it,
                    errorContext = this@BookingListViewModel::class.java.simpleName
                )
            }
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
            BookingListBookingTapEvent(
                selectedTab = selectedTab.value.toEventHorizonValue(),
                isSearchActive = searchQuery.value != null,
                isFilteringActive = enabledFiltersCount > 0,
            )
        )
        if (isWindowClassLargeThanCompact()) {
            selectedBookingIdOnBigScreen.value = bookingId
        }
        triggerEvent(NavigateToBookingDetails(bookingId))
    }

    private fun onTabChanged(tab: BookingListTab) {
        analyticsTrackerWrapper.track(
            BookingListTabSelectEvent(selectedTab = tab.toEventHorizonValue())
        )
        didUserSwitchTab = true
        selectedTab.value = tab
    }

    private fun onSortClicked() {
        analyticsTrackerWrapper.track(BookingListSortByTapEvent)
        isSortSheetVisible.value = true
    }

    private fun onSortOptionSelected(option: BookingListSortOption) {
        analyticsTrackerWrapper.track(
            BookingListSortByOptionTapEvent(sortOption = option.toEventHorizonValue())
        )
        sortOption.value = option
        isSortSheetVisible.value = false
    }

    private fun onSortDismiss() {
        isSortSheetVisible.value = false
    }

    private fun onFilterClicked() {
        analyticsTrackerWrapper.track(BookingListFiltersTapEvent)
        triggerEvent(NavigateToFilters)
    }

    private fun onClearFiltersClicked() {
        launch {
            bookingFilterRepository.save(BookingFilters.EMPTY)
        }
    }

    private fun onBackClick() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun openFirstLoadedBookingOnTablet(bookings: List<BookingEntity>) {
        if (isWindowClassLargeThanCompact() && bookings.isNotEmpty() && selectedBookingIdOnBigScreen.value == null) {
            val firstId = bookings.first().id.value
            selectedBookingIdOnBigScreen.value = firstId
            triggerEvent(NavigateToBookingDetails(firstId))
        }
    }

    private fun FetchParams.prepareFilters(): BookingFilters = when (selectedTab) {
        BookingListTab.Today,
        BookingListTab.Upcoming -> filters.copy(
            dateRange = dateFilterBuilder.prepareDateFilter(selectedTab, filters.dateRange),
            excludedBookingStatuses = ExcludedBookingStatuses(setOf(Cancelled, Complete))
        )

        BookingListTab.All -> filters.copy(
            dateRange = dateFilterBuilder.prepareDateFilter(selectedTab, filters.dateRange)
        )
    }

    private fun onSearchQueryChanged(newQuery: String?) {
        if (searchQuery.value == null && newQuery != null) {
            analyticsTrackerWrapper.track(BookingListSearchTapEvent)
        }
        searchQuery.value = newQuery
    }

    fun trackBookingListView() = launch {
        val state = _state.first()

        analyticsTrackerWrapper.track(
            BookingListViewEvent(
                selectedTab = state.tabState.selectedTab.toEventHorizonValue(),
                isDefaultTab = !didUserSwitchTab,
                isListEmpty = state.contentState.bookings.isEmpty(),
                isFiltered = state.controlsState.enabledFiltersCount > 0,
            )
        )
    }

    private data class FetchParams(
        val searchQuery: String?,
        val sortOption: BookingListSortOption,
        val selectedTab: BookingListTab,
        val filters: BookingFilters
    )

    data class NavigateToBookingDetails(val bookingId: Long) : MultiLiveEvent.Event()
    object NavigateToFilters : MultiLiveEvent.Event()

    companion object {
        @VisibleForTesting
        const val KEY_BOOKING_SELECTED_ON_BIG_SCREEN = "key_booking_selected_on_big_screen"
    }
}
