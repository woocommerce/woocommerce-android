package com.woocommerce.android.ui.bookings

import com.woocommerce.android.util.InlineClassesAnswer
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.intThat
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingListHandlerTest : BaseUnitTest() {
    private val availablePages = 3
    private val bookingsRepository: BookingsRepository = mock {
        val results = MutableStateFlow(emptyList<Booking>())
        on { observeBookings(any()) } doAnswer { invocation ->
            val limit = invocation.getArgument<Int>(0)
            results.map { it.take(limit) }
        }
        onBlocking { fetchBookings(any(), any()) } doAnswer InlineClassesAnswer { invocation ->
            val page = invocation.getArgument<Int>(0)
            val perPage = invocation.getArgument<Int>(1)
            val canLoadMore = page < availablePages
            when (page) {
                1 -> results.update { List(perPage) { getSampleBooking(it) } }
                availablePages -> results.update { list -> list + List(5) { getSampleBooking(it + perPage) } }
                else -> results.update { list -> list + List(perPage) { getSampleBooking(it + perPage) } }
            }
            Result.success(canLoadMore)
        }
    }

    private val bookingListHandler: BookingListHandler = BookingListHandler(bookingsRepository)

    @Test
    fun `given repository returns bookings, when observing bookings flow, then returns bookings`() = testBlocking {
        val sampleBookings = List(10) { getSampleBooking(it) }
        given(bookingsRepository.observeBookings(any())).willReturn(flowOf(sampleBookings))

        val bookings = bookingListHandler.bookingsFlow.first()

        assertThat(bookings).isEqualTo(sampleBookings)
    }

    @Test
    fun `given no search query and force refresh, when loading bookings, then fetches from repository`() =
        testBlocking {
            val result = bookingListHandler.loadBookings(searchQuery = null, forceRefresh = true)
            val bookings = bookingListHandler.bookingsFlow.first()

            assertThat(result.isSuccess).isTrue()
            assertThat(bookings).hasSize(BookingListHandler.PAGE_SIZE)
        }

    @Test
    fun `given repository fetch fails, when loading bookings with force refresh, then returns failure`() =
        testBlocking {
            val exception = Exception("Network error")
            given(bookingsRepository.fetchBookings(page = any(), perPage = any()))
                .willReturn(Result.failure(exception))

            val result = bookingListHandler.loadBookings(searchQuery = null, forceRefresh = true)

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isEqualTo(exception)
        }

    @Test
    fun `when load more is called and can load more is false, then returns success without fetching`() =
        testBlocking {
            val result = bookingListHandler.loadMore()

            assertThat(result.isSuccess).isTrue()
            verify(bookingsRepository, never()).fetchBookings(any(), any())
        }

    @Test
    fun `when load more is called and can load more is true, then fetches next page`() = testBlocking {
        bookingListHandler.loadBookings(forceRefresh = true)

        val result = bookingListHandler.loadMore()
        val bookings = bookingListHandler.bookingsFlow.first()

        assertThat(result.isSuccess).isTrue()
        assertThat(bookings).hasSize(2 * BookingListHandler.PAGE_SIZE)
    }

    @Test
    fun `when last page is reached, then can load more becomes false`() = testBlocking {
        bookingListHandler.loadBookings(forceRefresh = true)

        var result: Result<Unit>? = null
        repeat(availablePages - 1) {
            result = bookingListHandler.loadMore()
        }

        assertThat(result?.isSuccess).isTrue()
        verify(bookingsRepository, never()).fetchBookings(page = intThat { it > availablePages }, perPage = any())
    }

    @Test
    fun `when load bookings is called, then pagination resets`() = testBlocking {
        // First load and load more to advance page
        bookingListHandler.loadBookings(forceRefresh = true)
        bookingListHandler.loadMore()

        // Load bookings again - should reset to page 1
        bookingListHandler.loadBookings(forceRefresh = true)
        val bookings = bookingListHandler.bookingsFlow.first()

        assertThat(bookings).hasSize(BookingListHandler.PAGE_SIZE)
    }

    @Test
    fun `when bookings flow is observed with pagination, then limit increases correctly`() = testBlocking {
        bookingListHandler.loadBookings(forceRefresh = true)

        val initialBookings = bookingListHandler.bookingsFlow.first()
        assertThat(initialBookings).hasSize(BookingListHandler.PAGE_SIZE)

        bookingListHandler.loadMore()
        val moreBookings = bookingListHandler.bookingsFlow.first()

        @Suppress("UnusedFlow")
        verify(bookingsRepository).observeBookings(limit = eq(2 * BookingListHandler.PAGE_SIZE))
        assertThat(moreBookings).hasSize(2 * BookingListHandler.PAGE_SIZE)
    }

    @Test
    fun `when concurrent load operations occur, then operations are synchronized`() = testBlocking {
        // Launch multiple concurrent load operations
        val job1 = launch { bookingListHandler.loadBookings(forceRefresh = true) }
        val job2 = launch { bookingListHandler.loadBookings(forceRefresh = true) }
        val job3 = launch { bookingListHandler.loadMore() }

        job1.join()
        job2.join()
        job3.join()

        val bookings = bookingListHandler.bookingsFlow.first()
        assertThat(bookings).hasSize(BookingListHandler.PAGE_SIZE * 2)
    }

    private fun getSampleBooking(id: Int) = BookingEntity(
        id = RemoteId(id.toLong()),
        localSiteId = LocalId(1),
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
