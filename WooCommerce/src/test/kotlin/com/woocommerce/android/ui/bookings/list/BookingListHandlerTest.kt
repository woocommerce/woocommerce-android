package com.woocommerce.android.ui.bookings.list

import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.util.InlineClassesAnswer
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.intThat
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.willSuspendableAnswer
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingOrderInfo
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Duration
import java.time.Instant
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookingListHandlerTest : BaseUnitTest() {
    private val availablePages = 3
    private val bookingsRepository: BookingsRepository = mock {
        val results = MutableStateFlow(emptyList<Booking>())
        on { observeBookings(any(), anyOrNull(), any()) } doAnswer { invocation ->
            val limit = invocation.getArgument<Int>(0)
            results.map { it.take(limit) }
        }
        on { getBookingsList(any(), anyOrNull(), any()) } doAnswer { invocation ->
            val limit = invocation.getArgument<Int>(0)
            results.value.take(limit)
        }
        on {
            fetchBookings(
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                any()
            )
        } doAnswer InlineClassesAnswer { invocation ->
            val page = invocation.getArgument<Int>(0)
            val perPage = invocation.getArgument<Int>(1)
            val canLoadMore = page < availablePages
            val bookings = when (page) {
                1 -> List(perPage) { getSampleBooking(it) }
                availablePages -> List(5) { getSampleBooking(it + (page - 1) * perPage) }
                else -> List(perPage) { getSampleBooking(it + (page - 1) * perPage) }
            }
            if (page == 1) results.update { emptyList() }
            results.update { it + bookings }
            Result.success(BookingsRepository.FetchResult(bookings, canLoadMore))
        }
    }

    private val bookingListHandler: BookingListHandler = BookingListHandler(bookingsRepository)

    @Test
    fun `given repository returns bookings, when observing bookings flow, then returns bookings`() = testBlocking {
        val sampleBookings = List(10) { getSampleBooking(it) }
        given(bookingsRepository.getBookingsList(any(), anyOrNull(), any())).willReturn(sampleBookings)
        given(bookingsRepository.observeBookings(any(), anyOrNull(), any())).willReturn(flowOf(sampleBookings))

        val bookings = bookingListHandler.bookingsFlow.first()

        assertThat(bookings).isEqualTo(sampleBookings)
    }

    @Test
    fun `given no search query and force refresh, when loading bookings, then fetches from repository`() =
        testBlocking {
            val result = bookingListHandler.loadBookings(
                searchQuery = null,
                sortBy = BookingListSortOption.NewestToOldest
            )
            val bookings = bookingListHandler.bookingsFlow.first()

            assertThat(result.isSuccess).isTrue()
            assertThat(bookings).hasSize(BookingListHandler.PAGE_SIZE)
        }

    @Test
    fun `given repository fetch fails, when loading bookings with force refresh, then returns failure`() =
        testBlocking {
            val exception = Exception("Network error")
            given(
                bookingsRepository.fetchBookings(
                    page = any(),
                    perPage = any(),
                    query = anyOrNull(),
                    filters = anyOrNull(),
                    order = any()
                )
            )
                .willReturn(Result.failure(exception))

            val result = bookingListHandler.loadBookings(
                searchQuery = null,
                sortBy = BookingListSortOption.NewestToOldest
            )

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isEqualTo(exception)
        }

    @Test
    fun `when load more is called and can load more is false, then returns success without fetching`() =
        testBlocking {
            val result = bookingListHandler.loadMore()

            assertThat(result.isSuccess).isTrue()
            verify(bookingsRepository, never()).fetchBookings(
                page = any(),
                perPage = any(),
                query = anyOrNull(),
                filters = anyOrNull(),
                order = any()
            )
        }

    @Test
    fun `when load more is called and can load more is true, then fetches next page`() = testBlocking {
        bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest)

        val result = bookingListHandler.loadMore()
        val bookings = bookingListHandler.bookingsFlow.first()

        assertThat(result.isSuccess).isTrue()
        assertThat(bookings).hasSize(2 * BookingListHandler.PAGE_SIZE)
    }

    @Test
    fun `when last page is reached, then can load more becomes false`() = testBlocking {
        bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest)

        var result: Result<Int>? = null
        repeat(availablePages - 1) {
            result = bookingListHandler.loadMore()
        }

        assertThat(result?.isSuccess).isTrue()
        verify(bookingsRepository, never()).fetchBookings(
            page = intThat { it > availablePages },
            perPage = any(),
            query = anyOrNull(),
            filters = anyOrNull(),
            order = any()
        )
    }

    @Test
    fun `when load bookings is called, then pagination resets`() = testBlocking {
        // First load and load more to advance page
        bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest)
        bookingListHandler.loadMore()

        // Load bookings again - should reset to page 1
        bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest)
        val bookings = bookingListHandler.bookingsFlow.first()

        assertThat(bookings).hasSize(BookingListHandler.PAGE_SIZE)
    }

    @Test
    fun `when bookings flow is observed with pagination, then limit increases correctly`() = testBlocking {
        bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest)

        val initialBookings = bookingListHandler.bookingsFlow.first()
        assertThat(initialBookings).hasSize(BookingListHandler.PAGE_SIZE)

        bookingListHandler.loadMore()
        val moreBookings = bookingListHandler.bookingsFlow.first()

        @Suppress("UnusedFlow")
        verify(bookingsRepository).observeBookings(
            limit = eq(2 * BookingListHandler.PAGE_SIZE),
            filters = anyOrNull(),
            order = any()
        )
        assertThat(moreBookings).hasSize(2 * BookingListHandler.PAGE_SIZE)
    }

    @Test
    fun `when concurrent load operations occur, then operations are synchronized`() = testBlocking {
        // Launch multiple concurrent load operations
        val job1 = launch { bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest) }
        val job2 = launch { bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest) }
        val job3 = launch { bookingListHandler.loadMore() }

        job1.join()
        job2.join()
        job3.join()

        val bookings = bookingListHandler.bookingsFlow.first()
        assertThat(bookings).hasSize(BookingListHandler.PAGE_SIZE * 2)
    }

    @Test
    fun `given search, when refreshing, then keep previous search results during the fetch`() = testBlocking {
        val searchQuery = "Sample"
        val firstResults = List(BookingListHandler.PAGE_SIZE) { getSampleBooking(it) }
        val refreshedResults = List(BookingListHandler.PAGE_SIZE - 2) { getSampleBooking(it) }
        given(bookingsRepository.fetchBookings(any(), any(), eq(searchQuery), anyOrNull(), any()))
            .willReturn(
                Result.success(
                    BookingsRepository.FetchResult(firstResults, false)
                )
            )
            .willSuspendableAnswer {
                delay(10) // To allow checking intermediate state
                Result.success(
                    BookingsRepository.FetchResult(refreshedResults, false)
                )
            }

        bookingListHandler.loadBookings(
            searchQuery = searchQuery,
            sortBy = BookingListSortOption.NewestToOldest
        )
        val bookingsAfterFirstLoad = bookingListHandler.bookingsFlow.first()
        val refreshJob = launch {
            bookingListHandler.loadBookings(
                searchQuery = searchQuery,
                sortBy = BookingListSortOption.NewestToOldest
            )
        }
        val bookingsDuringRefresh = bookingListHandler.bookingsFlow.first()
        refreshJob.join()
        val bookingsAfterRefresh = bookingListHandler.bookingsFlow.first()

        assertThat(bookingsAfterFirstLoad).isEqualTo(firstResults)
        assertThat(bookingsDuringRefresh).isEqualTo(firstResults)
        assertThat(bookingsAfterRefresh).isEqualTo(refreshedResults)
    }

    @Test
    fun `given search, when changing search query, then reset search results`() = testBlocking {
        val initialQuery = "Initial"
        val newQuery = "New"

        val firstResults = List(BookingListHandler.PAGE_SIZE) { getSampleBooking(it) }
        val newResults = List(BookingListHandler.PAGE_SIZE - 3) { getSampleBooking(it) }

        given(bookingsRepository.fetchBookings(any(), any(), eq(initialQuery), anyOrNull(), any()))
            .willReturn(
                Result.success(
                    BookingsRepository.FetchResult(firstResults, false)
                )
            )
        given(bookingsRepository.fetchBookings(any(), any(), eq(newQuery), anyOrNull(), any()))
            .willSuspendableAnswer {
                delay(10) // To allow checking intermediate state
                Result.success(
                    BookingsRepository.FetchResult(newResults, false)
                )
            }

        bookingListHandler.loadBookings(
            searchQuery = initialQuery,
            sortBy = BookingListSortOption.NewestToOldest
        )
        val bookingsAfterInitialSearch = bookingListHandler.bookingsFlow.first()

        val refreshJob = launch {
            bookingListHandler.loadBookings(
                searchQuery = newQuery,
                sortBy = BookingListSortOption.NewestToOldest
            )
        }
        val bookingsDuringNewSearch = bookingListHandler.bookingsFlow.first()
        refreshJob.join()
        val bookingsAfterNewSearch = bookingListHandler.bookingsFlow.first()

        assertThat(bookingsAfterInitialSearch).isEqualTo(firstResults)
        assertThat(bookingsDuringNewSearch).isEmpty()
        assertThat(bookingsAfterNewSearch).isEqualTo(newResults)
    }

    private fun getSampleBooking(id: Int) = BookingEntity(
        id = RemoteId(id.toLong()),
        localSiteId = LocalId(1),
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
