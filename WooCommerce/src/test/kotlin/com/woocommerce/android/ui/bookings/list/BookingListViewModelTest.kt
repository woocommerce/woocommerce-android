package com.woocommerce.android.ui.bookings.list

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingMapper
import com.woocommerce.android.ui.bookings.filter.data.BookingFilterRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
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
        onBlocking {
            loadBookings(
                searchQuery = anyOrNull(),
                filters = any(),
                sortBy = any()
            )
        } doReturn Result.success(Unit)
        onBlocking { loadMore() } doReturn Result.success(Unit)
    }
    private val mockedNow = Instant.parse("2025-01-01T12:00:00Z")
    private val filtersBuilder = BookingListFiltersBuilder(Clock.fixed(mockedNow, ZoneId.of("UTC")))
    private val currencyFormatter = mock<CurrencyFormatter>()
    private val getLocations = mock<GetLocations>()
    private val bookingMapper = BookingMapper(currencyFormatter, getLocations)
    private val bookingFiltersFlow = MutableStateFlow(BookingFilters())
    private val bookingFilterRepository: BookingFilterRepository = mock {
        on { bookingFiltersFlow } doReturn bookingFiltersFlow
    }

    private lateinit var viewModel: BookingListViewModel

    suspend fun setup(
        bookings: List<Booking> = emptyList(),
        prepareMocks: suspend () -> Unit = {}
    ) {
        prepareMocks()
        whenever(bookingListHandler.bookingsFlow).thenReturn(flowOf(bookings))
        viewModel = BookingListViewModel(
            savedStateHandle = SavedStateHandle(),
            bookingFilterRepository = bookingFilterRepository,
            bookingListHandler = bookingListHandler,
            filtersBuilder = filtersBuilder,
            bookingMapper = bookingMapper,
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
                listOfNotNull(
                    with(filtersBuilder) {
                        BookingListTab.Upcoming.asDateRangeFilter()
                    }
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
            filters = eq(listOf(customerFilter)),
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
            attendanceStatus = BookingEntity.AttendanceStatus.Booked,
            order = BookingOrderInfo()
        )
    }
}
