package com.woocommerce.android.ui.bookings.list

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
                forceRefresh = any(),
                filters = any()
            )
        } doReturn Result.success(Unit)
        onBlocking { loadMore() } doReturn Result.success(Unit)
    }
    private val mockedNow = Instant.parse("2025-01-01T12:00:00Z")
    private val filtersBuilder = BookingListFiltersBuilder(Clock.fixed(mockedNow, ZoneId.of("UTC")))

    private lateinit var viewModel: BookingListViewModel

    suspend fun setup(
        bookings: List<Booking> = emptyList(),
        prepareMocks: suspend () -> Unit = {}
    ) {
        prepareMocks()
        whenever(bookingListHandler.bookingsFlow).thenReturn(flowOf(bookings))
        viewModel = BookingListViewModel(
            savedStateHandle = SavedStateHandle(),
            bookingListHandler = bookingListHandler,
            filtersBuilder = filtersBuilder
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
        verify(bookingListHandler).loadBookings(searchQuery = eq(null), forceRefresh = eq(true), filters = any())

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
            forceRefresh = eq(true),
            filters = any()
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
            whenever(bookingListHandler.loadBookings(searchQuery = anyOrNull(), forceRefresh = any(), filters = any()))
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
            forceRefresh = eq(true),
            filters = eq(
                listOfNotNull(
                    with(filtersBuilder) {
                        BookingListTab.Upcoming.asDateRangeFilter()
                    }
                )
            )
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
    fun `when selecting sort in one tab, then other tabs keep their own selection`() = testBlocking {
        // GIVEN
        setup()

        // Open sheet on Today and select OldestToNewest
        val s1 = viewModel.state.getOrAwaitValue()
        s1.controlsState.onSortClick()
        val sheet1 = viewModel.state.getOrAwaitValue().sortBottomSheetState!!
        sheet1.onSelect(BookingListSortOption.OldestToNewest)

        // Sheet should be hidden after selection
        val afterSelectToday = viewModel.state.getOrAwaitValue()
        assertThat(afterSelectToday.sortBottomSheetState).isNull()

        // Re-open and ensure Today remembers OldestToNewest
        afterSelectToday.controlsState.onSortClick()
        val sheetTodayAgain = viewModel.state.getOrAwaitValue().sortBottomSheetState!!
        assertThat(sheetTodayAgain.selectedOption).isEqualTo(BookingListSortOption.OldestToNewest)
        sheetTodayAgain.onDismiss()

        // Switch to Upcoming and verify default is NewestToOldest
        val stateAfterDismiss = viewModel.state.getOrAwaitValue()
        stateAfterDismiss.tabState.onTabChanged(BookingListTab.Upcoming)
        val upcomingState = viewModel.state.getOrAwaitValue()
        upcomingState.controlsState.onSortClick()
        val sheetUpcoming = viewModel.state.getOrAwaitValue().sortBottomSheetState!!
        assertThat(sheetUpcoming.selectedOption).isEqualTo(BookingListSortOption.NewestToOldest)

        // Switch back to Today and ensure it still holds its own selection (OldestToNewest)
        upcomingState.tabState.onTabChanged(BookingListTab.Today)
        val backToToday = viewModel.state.getOrAwaitValue()
        backToToday.controlsState.onSortClick()
        val sheetBackToToday = viewModel.state.getOrAwaitValue().sortBottomSheetState!!
        assertThat(sheetBackToToday.selectedOption).isEqualTo(BookingListSortOption.OldestToNewest)
    }

    private fun getSampleBooking(id: Int): Booking {
        return BookingEntity(
            id = LocalOrRemoteId.RemoteId(id.toLong()),
            localSiteId = LocalOrRemoteId.LocalId(1),
            start = Instant.now(),
            end = Instant.now() + Duration.ofDays(1),
            allDay = false,
            status = "confirmed",
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
            localTimezone = ""
        )
    }
}
