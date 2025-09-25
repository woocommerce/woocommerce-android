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
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingListViewModelTest : BaseUnitTest() {
    private val bookingListHandler: BookingListHandler = mock {
        on { bookingsFlow } doReturn flowOf(emptyList())
        onBlocking { loadBookings(searchQuery = anyOrNull(), forceRefresh = any()) } doReturn Result.success(Unit)
        onBlocking { loadMore() } doReturn Result.success(Unit)
    }

    private val savedStateHandle: SavedStateHandle = SavedStateHandle()

    suspend fun setup(
        bookings: List<Booking> = emptyList(),
        prepareMocks: suspend () -> Unit = {}
    ) {
        prepareMocks()
        whenever(bookingListHandler.bookingsFlow).thenReturn(flowOf(bookings))
    }

    @Test
    fun `when viewmodel is initialized, then bookings are fetched`() = testBlocking {
        // GIVEN
        val bookings = listOf(getSampleBooking(1))
        setup(bookings = bookings)

        // WHEN
        val viewModel = BookingListViewModel(
            savedStateHandle = savedStateHandle,
            bookingListHandler = bookingListHandler
        )
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler).loadBookings(searchQuery = eq(null), forceRefresh = eq(true))

        val state = viewModel.state.getOrAwaitValue()
        assertThat(state.bookings).hasSize(1)
        assertThat(state.loadingState).isEqualTo(BookingListViewState.LoadingState.Idle)
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
            val viewModel = BookingListViewModel(
                savedStateHandle = savedStateHandle,
                bookingListHandler = bookingListHandler
            )
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.getOrAwaitValue()
            assertThat(state.bookings).hasSize(2)
            assertThat(state.bookings[0].id).isEqualTo(booking1.id.value)
            assertThat(state.bookings[1].id).isEqualTo(booking2.id.value)
        }

    @Test
    fun `when onRefresh is called, then bookings are refreshed`() = testBlocking {
        // GIVEN
        setup()
        val viewModel = BookingListViewModel(
            savedStateHandle = savedStateHandle,
            bookingListHandler = bookingListHandler
        )
        advanceUntilIdle()

        // WHEN
        val state = viewModel.state.getOrAwaitValue()
        state.onRefresh()
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler, times(2)).loadBookings(
            searchQuery = eq(null),
            forceRefresh = eq(true)
        )
    }

    @Test
    fun `when onLoadMore is called, then handler loadMore is invoked`() = testBlocking {
        // GIVEN
        setup()
        val viewModel = BookingListViewModel(
            savedStateHandle = savedStateHandle,
            bookingListHandler = bookingListHandler
        )
        advanceUntilIdle()

        // WHEN
        val state = viewModel.state.getOrAwaitValue()
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
        val viewModel = BookingListViewModel(
            savedStateHandle = savedStateHandle,
            bookingListHandler = bookingListHandler
        )
        advanceUntilIdle()

        // WHEN
        val state = viewModel.state.getOrAwaitValue()
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
            whenever(bookingListHandler.loadBookings(searchQuery = anyOrNull(), forceRefresh = any()))
                .thenReturn(Result.failure(Exception("Network error")))
        }

        // WHEN
        val viewModel = BookingListViewModel(
            savedStateHandle = savedStateHandle,
            bookingListHandler = bookingListHandler
        )
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.getOrAwaitValue()
        val event = viewModel.event.getOrAwaitValue()
        assertThat(state.loadingState).isEqualTo(BookingListViewState.LoadingState.Idle)
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
    }

    @Test
    fun `when load more fails, then show error snackbar`() = testBlocking {
        // GIVEN
        setup {
            whenever(bookingListHandler.loadMore())
                .thenReturn(Result.failure(Exception("Network error")))
        }
        val viewModel = BookingListViewModel(
            savedStateHandle = savedStateHandle,
            bookingListHandler = bookingListHandler
        )
        advanceUntilIdle()

        // WHEN
        val state = viewModel.state.getOrAwaitValue()
        state.onLoadMore()
        advanceUntilIdle()

        // THEN
        val finalState = viewModel.state.getOrAwaitValue()
        val event = viewModel.event.getOrAwaitValue()
        assertThat(finalState.loadingState).isEqualTo(BookingListViewState.LoadingState.Idle)
        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.bookings_fetch_error))
    }

    private fun getSampleBooking(id: Int): Booking {
        return BookingEntity(
            id = LocalOrRemoteId.RemoteId(id.toLong()),
            localSiteId = LocalOrRemoteId.LocalId(1),
            start = System.currentTimeMillis(),
            end = System.currentTimeMillis() + 3600000,
            allDay = false,
            status = "confirmed",
            cost = "100.00",
            currency = "USD",
            customerId = 1L,
            productId = 1L,
            resourceId = 1L,
            dateCreated = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis(),
            googleCalendarEventId = "",
            orderId = 1L,
            orderItemId = 1L,
            parentId = 0L,
            personCounts = listOf(1L),
            localTimezone = "UTC"
        )
    }
}
