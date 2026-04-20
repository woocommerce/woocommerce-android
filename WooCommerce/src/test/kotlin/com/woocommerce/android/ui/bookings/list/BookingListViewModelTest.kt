package com.woocommerce.android.ui.bookings.list

import androidx.lifecycle.SavedStateHandle
import com.automattic.eventhorizon.BookingListBookingTapEvent
import com.automattic.eventhorizon.BookingListFiltersTapEvent
import com.automattic.eventhorizon.BookingListSearchTapEvent
import com.automattic.eventhorizon.BookingListSortByOptionTapEvent
import com.automattic.eventhorizon.BookingListSortByTapEvent
import com.automattic.eventhorizon.BookingListTabSelectEvent
import com.automattic.eventhorizon.BookingListViewEvent
import com.automattic.eventhorizon.BookingTabValue
import com.automattic.eventhorizon.Trackable
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.PaymentStatus
import com.woocommerce.android.ui.bookings.PaymentStatusResolver
import com.woocommerce.android.ui.bookings.filter.data.BookingFilterRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateFormatter
import com.woocommerce.android.util.IsWindowClassLargeThanCompact
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingOrderInfo
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingListViewModelTest : BaseUnitTest() {
    private val bookingListHandler: BookingListHandler = mock {
        on { bookingsFlow } doReturn flowOf(emptyList())
        on {
            loadBookings(
                searchQuery = anyOrNull(),
                filters = any(),
                sortBy = any()
            )
        } doReturn Result.success(0)
        on { loadMore() } doReturn Result.success(0)
    }
    private val mockedNow = Instant.parse("2025-01-01T12:00:00Z")
    private val filtersBuilder = BookingListDateFilterBuilder(Clock.fixed(mockedNow, ZoneId.of("UTC")))
    private val dateFormatter = mock<DateFormatter>()

    private val currencyFormatter = mock<CurrencyFormatter>()
    private val getLocations = mock<GetLocations>()
    private val resourceProvider = mock<ResourceProvider>()
    private val bookingMapper = BookingMapper(dateFormatter, currencyFormatter, getLocations, resourceProvider)
    private val bookingFiltersFlow = MutableStateFlow(BookingFilters())
    private val bookingFilterRepository: BookingFilterRepository = mock {
        on { bookingFiltersFlow } doReturn bookingFiltersFlow
    }
    private val isWindowClassLargeThanCompact: IsWindowClassLargeThanCompact = mock()
    private val paymentStatusResolver: PaymentStatusResolver = mock {
        on { resolveAll(any()) } doReturn mapOf(1L to PaymentStatus.UNPAID)
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: BookingListViewModel
    private lateinit var savedStateHandle: SavedStateHandle

    suspend fun setup(
        bookings: List<Booking> = emptyList(),
        prepareMocks: suspend () -> Unit = {}
    ) {
        whenever(bookingListHandler.bookingsFlow).thenReturn(flowOf(bookings))
        whenever(isWindowClassLargeThanCompact()).thenReturn(false)
        whenever(dateFormatter.formatDateTime(any<Instant>())).thenReturn("Dec 12, 2025, 11:00 AM")
        prepareMocks()
        savedStateHandle = SavedStateHandle()
        viewModel = BookingListViewModel(
            bookingFilterRepository = bookingFilterRepository,
            savedStateHandle = savedStateHandle,
            bookingListHandler = bookingListHandler,
            dateFilterBuilder = filtersBuilder,
            bookingMapper = bookingMapper,
            isWindowClassLargeThanCompact = isWindowClassLargeThanCompact,
            paymentStatusResolver = paymentStatusResolver,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
        )
    }

    @Test
    fun `when viewmodel is initialized, then bookings are fetched`() = testBlocking {
        // GIVEN
        val bookings = listOf(getSampleBooking(1))
        setup(bookings = bookings)

        // WHEN
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler).loadBookings(searchQuery = eq(null), filters = any(), sortBy = any())

        val state = viewModel.state.getOrAwaitValue().contentState
        assertThat(state.bookings).hasSize(1)
        assertThat(state.loadingState).isEqualTo(BookingListLoadingState.Idle)
    }

    @Test
    fun `given multiple bookings, when state is observed, then bookings are converted to UI models`() =
        testBlocking {
            // GIVEN
            val booking1 = getSampleBooking(1)
            val booking2 = getSampleBooking(2)
            val bookings = listOf(booking1, booking2)
            setup(bookings = bookings)

            // WHEN
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.getOrAwaitValue().contentState
            assertThat(state.bookings).hasSize(2)
            assertThat(state.bookings[0].id).isEqualTo(booking1.id.value)
            assertThat(state.bookings[1].id).isEqualTo(booking2.id.value)
        }

    @Test
    fun `when onRefresh is called, then bookings are refreshed`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val state = viewModel.state.getOrAwaitValue().contentState
        state.onRefresh()
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler, times(2)).loadBookings(
            searchQuery = eq(null),
            filters = any(),
            sortBy = any()
        )
    }

    @Test
    fun `when onLoadMore is called, then handler loadMore is invoked`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val state = viewModel.state.getOrAwaitValue().contentState
        state.onLoadMore()
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler).loadMore()
    }

    @Test
    fun `when onBookingClick is called, then NavigateToBookingDetails event is triggered`() = testBlocking {
        // GIVEN
        val bookingId = 123L
        setup()

        // WHEN
        val state = viewModel.state.getOrAwaitValue().contentState
        val events = viewModel.event.captureValues()
        state.onBookingClick(bookingId)

        // THEN
        assertThat(events).hasSize(1)
        val event = events.first()
        assertThat(event).isInstanceOf(BookingListViewModel.NavigateToBookingDetails::class.java)
        assertThat((event as BookingListViewModel.NavigateToBookingDetails).bookingId).isEqualTo(bookingId)
    }

    @Test
    fun `when booking handler fails to load, then show error snackbar`() = testBlocking {
        // GIVEN
        setup {
            whenever(bookingListHandler.loadBookings(searchQuery = anyOrNull(), filters = any(), sortBy = any()))
                .thenReturn(Result.failure(Exception("Network error")))
        }

        // WHEN
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.getOrAwaitValue().contentState
        val event = viewModel.event.getOrAwaitValue()
        assertThat(state.loadingState).isEqualTo(BookingListLoadingState.Idle)
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
    }

    @Test
    fun `when load more fails, then show error snackbar`() = testBlocking {
        // GIVEN
        setup {
            whenever(bookingListHandler.loadMore())
                .thenReturn(Result.failure(Exception("Network error")))
        }

        // WHEN
        val state = viewModel.state.getOrAwaitValue().contentState
        state.onLoadMore()
        advanceUntilIdle()

        // THEN
        val finalState = viewModel.state.getOrAwaitValue().contentState
        val event = viewModel.event.getOrAwaitValue()
        assertThat(finalState.loadingState).isEqualTo(BookingListLoadingState.Idle)
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
    }

    @Test
    fun `when tab is changed, then state is updated`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val initialState = viewModel.state.getOrAwaitValue()
        initialState.tabState.onTabChanged(BookingListTab.Upcoming)

        // THEN
        val updatedState = viewModel.state.getOrAwaitValue()
        assertThat(updatedState.tabState.selectedTab).isEqualTo(BookingListTab.Upcoming)
    }

    @Test
    fun `when tab is changed, then bookings are refetched`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val initialState = viewModel.state.getOrAwaitValue()
        initialState.tabState.onTabChanged(BookingListTab.Upcoming)
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler).loadBookings(
            searchQuery = eq(null),
            filters = eq(
                BookingFilters().copy(
                    dateRange = filtersBuilder.prepareDateFilter(
                        BookingListTab.Upcoming,
                        BookingsFilterOption.DateRange.DEFAULT
                    ),
                    excludedBookingStatuses = BookingsFilterOption.ExcludedBookingStatuses(
                        setOf(BookingEntity.Status.Cancelled, BookingEntity.Status.Complete)
                    )
                )
            ),
            sortBy = any()
        )
    }

    @Test
    fun `when onSortClick is called, then bottom sheet is shown`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val initialState = viewModel.state.getOrAwaitValue()
        initialState.controlsState.onSortClick()

        // THEN
        val withSheet = viewModel.state.getOrAwaitValue()
        assertThat(withSheet.sortBottomSheetState).isNotNull()
    }

    @Test
    fun `when search query is changed, then state is updated`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val initialState = viewModel.state.getOrAwaitValue()
        initialState.searchState.onQueryChanged("test query")

        // THEN
        val updatedState = viewModel.state.getOrAwaitValue()
        assertThat(updatedState.searchState.query).isEqualTo("test query")
        assertThat(updatedState.searchState.isSearchActive).isTrue()
    }

    @Test
    fun `when search query is changed, then bookings are refetched`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val initialState = viewModel.state.getOrAwaitValue()
        initialState.searchState.onQueryChanged("test query")
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler).loadBookings(
            searchQuery = eq("test query"),
            filters = any(),
            sortBy = any()
        )
    }

    @Test
    fun `when search query is cleared, then isSearchActive becomes false and bookings are refetched`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val initialState = viewModel.state.getOrAwaitValue()
        initialState.searchState.onQueryChanged("test query")
        advanceTimeBy(AppConstants.SEARCH_TYPING_DELAY_MS + 1)
        val stateWithQuery = viewModel.state.getOrAwaitValue()
        stateWithQuery.searchState.onQueryChanged(null)
        advanceUntilIdle()

        // THEN
        val clearedState = viewModel.state.getOrAwaitValue()
        assertThat(clearedState.searchState.query).isNull()
        assertThat(clearedState.searchState.isSearchActive).isFalse()
        verify(bookingListHandler, times(2)).loadBookings(
            searchQuery = eq(null),
            filters = any(),
            sortBy = any()
        )
    }

    @Test
    fun `given All tab, when filters are changed, then bookings are refetched`() = testBlocking {
        // GIVEN
        setup()
        val initialState = viewModel.state.getOrAwaitValue()
        initialState.tabState.onTabChanged(BookingListTab.All)

        // WHEN
        val customerFilter = BookingsFilterOption.Customer(1L, "Customer")
        bookingFiltersFlow.emit(
            BookingFilters(customer = customerFilter)
        )

        // THEN
        verify(bookingListHandler).loadBookings(
            searchQuery = anyOrNull(),
            filters = argThat { customer == customerFilter },
            sortBy = any()
        )
    }

    @Test
    fun `given Today tab, when filters are changed, then bookings are refetched with filters`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val customerFilter = BookingsFilterOption.Customer(1L, "Customer")
        bookingFiltersFlow.emit(
            BookingFilters(customer = customerFilter)
        )
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler).loadBookings(
            searchQuery = anyOrNull(),
            filters = argThat { customer == customerFilter },
            sortBy = any()
        )
    }

    @Test
    fun `given Upcoming tab, when filters are changed, then bookings are refetched with filters`() = testBlocking {
        // GIVEN
        setup()
        val initialState = viewModel.state.getOrAwaitValue()
        initialState.tabState.onTabChanged(BookingListTab.Upcoming)
        advanceUntilIdle()

        // WHEN
        val customerFilter = BookingsFilterOption.Customer(1L, "Customer")
        bookingFiltersFlow.emit(
            BookingFilters(customer = customerFilter)
        )
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler).loadBookings(
            searchQuery = anyOrNull(),
            filters = argThat {
                customer == customerFilter &&
                    excludedBookingStatuses == BookingsFilterOption.ExcludedBookingStatuses(
                        setOf(BookingEntity.Status.Cancelled, BookingEntity.Status.Complete)
                    )
            },
            sortBy = any()
        )
    }

    @Test
    fun `given enabled filters, when controls state is observed, then enabledFiltersCount is correct`() = testBlocking {
        // GIVEN
        setup()
        val customerFilter = BookingsFilterOption.Customer(1L, "Customer")
        bookingFiltersFlow.emit(
            BookingFilters(customer = customerFilter)
        )

        // WHEN
        val initialState = viewModel.state.getOrAwaitValue()
        initialState.tabState.onTabChanged(BookingListTab.All)

        // THEN
        val controlsState = viewModel.state.getOrAwaitValue().controlsState
        assertThat(controlsState.enabledFiltersCount).isEqualTo(1)
    }

    @Test
    fun `given tablet, when bookings are loaded and none selected yet, then first is auto opened`() = testBlocking {
        // GIVEN
        val booking1 = getSampleBooking(11)
        val booking2 = getSampleBooking(22)
        setup(bookings = listOf(booking1, booking2)) {
            whenever(isWindowClassLargeThanCompact()).thenReturn(true)
        }

        // WHEN
        val events = viewModel.event.captureValues()
        viewModel.state.getOrAwaitValue()
        advanceUntilIdle()

        // THEN
        val navigations = events.filterIsInstance<BookingListViewModel.NavigateToBookingDetails>()
        assertThat(navigations).hasSize(1)
        assertThat(navigations.first().bookingId).isEqualTo(booking1.id.value)
    }

    @Test
    fun `given phone, when bookings are loaded, then no auto navigation happens`() = testBlocking {
        // GIVEN
        val booking = getSampleBooking(33)
        setup(bookings = listOf(booking))

        // WHEN
        val events = viewModel.event.captureValues()
        viewModel.state.getOrAwaitValue()
        advanceUntilIdle()

        // THEN
        val navigations = events.filterIsInstance<BookingListViewModel.NavigateToBookingDetails>()
        assertThat(navigations).isEmpty()
    }

    @Test
    fun `given tablet, when booking is clicked, then selection is saved and navigation emitted`() = testBlocking {
        // GIVEN
        setup(bookings = emptyList()) {
            whenever(isWindowClassLargeThanCompact()).thenReturn(true)
        }
        val events = viewModel.event.captureValues()
        val state = viewModel.state.getOrAwaitValue()

        // WHEN
        state.contentState.onBookingClick(1234L)
        advanceUntilIdle()

        // THEN
        val navigations = events.filterIsInstance<BookingListViewModel.NavigateToBookingDetails>()
        assertThat(navigations.last().bookingId).isEqualTo(1234L)
        assertThat(savedStateHandle.get<Long>(BookingListViewModel.KEY_BOOKING_SELECTED_ON_BIG_SCREEN)).isEqualTo(1234L)
    }

    @Test
    fun `when tab is changed, then BOOKING_LIST_TAB_SELECT is tracked`() = testBlocking {
        setup()

        val state = viewModel.state.getOrAwaitValue()
        state.tabState.onTabChanged(BookingListTab.Upcoming)

        verify(analyticsTrackerWrapper).track(
            argThat<Trackable> {
                this is BookingListTabSelectEvent &&
                    this.selectedTab == BookingTabValue.Upcoming
            }
        )
    }

    @Test
    fun `when booking is clicked, then BOOKING_LIST_BOOKING_TAP is tracked`() = testBlocking {
        setup()

        val state = viewModel.state.getOrAwaitValue()
        state.contentState.onBookingClick(123L)

        verify(analyticsTrackerWrapper).track(
            argThat<Trackable> {
                this is BookingListBookingTapEvent &&
                    this.isSearchActive == false &&
                    this.isFilteringActive == false &&
                    this.selectedTab == BookingTabValue.Today
            }
        )
    }

    @Test
    fun `given active filters, when booking is clicked, then BOOKING_LIST_BOOKING_TAP tracks is_filtering_active as true`() =
        testBlocking {
            bookingFiltersFlow.value = BookingFilters(
                bookingType = BookingsFilterOption.BookingType(BookingsFilterOption.BookingType.Type.SERVICE)
            )
            setup()

            val state = viewModel.state.getOrAwaitValue()
            state.contentState.onBookingClick(123L)

            verify(analyticsTrackerWrapper).track(
                argThat<Trackable> {
                    this is BookingListBookingTapEvent &&
                        this.isFilteringActive == true
                }
            )
        }

    @Test
    fun `when sort is clicked, then BOOKING_LIST_SORT_BY_TAP is tracked`() = testBlocking {
        setup()

        val state = viewModel.state.getOrAwaitValue()
        state.controlsState.onSortClick()

        verify(analyticsTrackerWrapper).track(argThat<Trackable> { this is BookingListSortByTapEvent })
    }

    @Test
    fun `when sort option is selected, then BOOKING_LIST_SORT_BY_OPTION_TAP is tracked`() = testBlocking {
        setup()
        val state = viewModel.state.getOrAwaitValue()
        state.controlsState.onSortClick()
        val stateWithSheet = viewModel.state.getOrAwaitValue()

        stateWithSheet.sortBottomSheetState?.onSelect?.invoke(BookingListSortOption.OldestToNewest)

        verify(analyticsTrackerWrapper).track(
            argThat<Trackable> {
                this is BookingListSortByOptionTapEvent &&
                    this.sortOption == com.automattic.eventhorizon.BookingSortValue.OldestFirst
            }
        )
    }

    @Test
    fun `when filter is clicked, then BOOKING_LIST_FILTERS_TAP is tracked`() = testBlocking {
        setup()

        val state = viewModel.state.getOrAwaitValue()
        state.controlsState.onFilterClick()

        verify(analyticsTrackerWrapper).track(argThat<Trackable> { this is BookingListFiltersTapEvent })
    }

    @Test
    fun `when search is activated, then BOOKING_LIST_SEARCH_TAP is tracked`() = testBlocking {
        setup()

        val state = viewModel.state.getOrAwaitValue()
        state.searchState.onQueryChanged("")

        verify(analyticsTrackerWrapper).track(argThat<Trackable> { this is BookingListSearchTapEvent })
    }

    @Test
    fun `when fetch fails, then BOOKING_LIST_FAILED_TO_FETCH_BOOKINGS is tracked`() = testBlocking {
        val error = WooError(WooErrorType.API_ERROR, GenericErrorType.NETWORK_ERROR, "Network error", "rest_error")
        setup {
            whenever(bookingListHandler.loadBookings(searchQuery = anyOrNull(), filters = any(), sortBy = any()))
                .thenReturn(Result.failure(WooException(error)))
        }

        advanceUntilIdle()

        verify(analyticsTrackerWrapper).track(
            stat = eq(AnalyticsEvent.BOOKING_LIST_FAILED_TO_FETCH_BOOKINGS),
            properties = argThat<Map<String, Any>> { this["error_code"] == "rest_error" },
            errorContext = eq("BookingListViewModel"),
            errorType = eq("API_ERROR"),
            errorDescription = eq("Network error")
        )
    }

    @Test
    fun `when screen is foregrounded, then BOOKING_LIST_VIEW is tracked`() = testBlocking {
        val bookings = listOf(getSampleBooking(1))
        setup(bookings = bookings)
        viewModel.trackBookingListView()

        advanceUntilIdle()

        verify(analyticsTrackerWrapper).track(
            argThat<Trackable> {
                this is BookingListViewEvent &&
                    this.selectedTab == BookingTabValue.Today &&
                    this.isDefaultTab == true &&
                    this.isListEmpty == false &&
                    this.isFiltered == false
            }
        )
    }

    @Test
    fun `given active filters, when bookings loaded, then BOOKING_LIST_VIEW tracks is_filtered as true`() = testBlocking {
        bookingFiltersFlow.value = BookingFilters(
            bookingType = BookingsFilterOption.BookingType(BookingsFilterOption.BookingType.Type.SERVICE)
        )
        val bookings = listOf(getSampleBooking(1))
        setup(bookings = bookings)
        viewModel.trackBookingListView()

        advanceUntilIdle()

        verify(analyticsTrackerWrapper).track(
            argThat<Trackable> {
                this is BookingListViewEvent &&
                    this.isFiltered == true
            }
        )
    }

    @Test
    fun `when search query is opened and closed without typing, then bookings are NOT refetched`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val initialState = viewModel.state.getOrAwaitValue()
        // Search opened -> query starts empty
        initialState.searchState.onQueryChanged("")
        advanceUntilIdle()

        val stateWithEmptyQuery = viewModel.state.getOrAwaitValue()
        // Search closed -> query becomes null
        stateWithEmptyQuery.searchState.onQueryChanged(null)
        advanceUntilIdle()

        // THEN
        // Should only be called once for the initial load
        verify(bookingListHandler, times(1)).loadBookings(
            searchQuery = anyOrNull(),
            filters = any(),
            sortBy = any()
        )
    }

    private fun getSampleBooking(id: Int): Booking {
        return BookingEntity(
            id = LocalOrRemoteId.RemoteId(id.toLong()),
            localSiteId = LocalOrRemoteId.LocalId(1),
            start = Instant.now(),
            end = Instant.now() + Duration.ofDays(1),
            allDay = false,
            status = BookingEntity.Status.Confirmed,
            cost = "100.00",
            currency = "USD",
            customerId = 1L,
            productId = 1L,
            resourceId = 1L,
            dateCreated = Instant.now(),
            dateModified = Instant.now(),
            googleCalendarEventId = "",
            orderId = 1L,
            orderItemId = 1L,
            parentId = 0L,
            personCounts = listOf(1L),
            localTimezone = "",
            attendanceStatus = BookingEntity.AttendanceStatus.Attended,
            order = BookingOrderInfo(),
            customerNote = ""
        )
    }
}
