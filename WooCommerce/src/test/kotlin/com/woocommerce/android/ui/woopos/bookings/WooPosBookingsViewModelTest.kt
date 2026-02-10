package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.ui.bookings.list.BookingListHandler
import com.woocommerce.android.ui.bookings.list.BookingListSortOption
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.localcatalog.DateTimeProvider
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingOrderInfo
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingProductInfo
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosBookingsViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule(StandardTestDispatcher())

    private val bookingListHandler: BookingListHandler = mock()
    private val dateTimeProvider: DateTimeProvider = mock()
    private lateinit var viewModel: WooPosBookingsViewModel

    private fun booking(id: Long = 1L) = BookingEntity(
        id = RemoteId(id),
        localSiteId = LocalId(1),
        start = Instant.ofEpochSecond(1700000000 + id),
        end = Instant.ofEpochSecond(1700003600 + id),
        allDay = false,
        status = BookingEntity.Status.Confirmed,
        cost = "100.00",
        currency = "USD",
        customerId = 1L,
        productId = 1L,
        resourceId = 1L,
        dateCreated = Instant.ofEpochSecond(1700000000),
        dateModified = Instant.ofEpochSecond(1700000000),
        googleCalendarEventId = "",
        orderId = id * 10,
        orderItemId = 1L,
        parentId = 0L,
        personCounts = null,
        localTimezone = "UTC",
        customerNote = null,
        attendanceStatus = BookingEntity.AttendanceStatus.Attended,
        note = "",
        order = BookingOrderInfo(
            productInfo = BookingProductInfo(name = "Service $id"),
        )
    )

    private fun createViewModel(): WooPosBookingsViewModel {
        return WooPosBookingsViewModel(
            bookingListHandler = bookingListHandler,
            dateTimeProvider = dateTimeProvider,
        )
    }

    @Before
    fun setUp() = runTest {
        whenever(bookingListHandler.bookingsFlow).thenReturn(
            flowOf(listOf(booking(1), booking(2)))
        )
        whenever(
            bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest)
        ).thenReturn(Result.success(Unit))
        whenever(bookingListHandler.loadMore()).thenReturn(Result.success(Unit))
        whenever(dateTimeProvider.now()).thenReturn(0L)
    }

    @Test
    fun `given happy path, when init, then state is Content`() = runTest {
        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosBookingsState.Content::class.java)
    }

    @Test
    fun `given happy path, when init, then first booking is auto-selected`() = runTest {
        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.selectedDetails?.id).isEqualTo(1L)
    }

    @Test
    fun `given fetch fails and state is Loading, when init, then state is Error`() = runTest {
        // GIVEN
        whenever(bookingListHandler.bookingsFlow).thenReturn(MutableSharedFlow())
        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .thenReturn(Result.failure(RuntimeException("Network error")))

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosBookingsState.Error::class.java)
    }

    @Test
    fun `given no bookings exist, when init completes, then state is Empty`() = runTest {
        // GIVEN
        whenever(bookingListHandler.bookingsFlow).thenReturn(flowOf(emptyList()))
        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .thenReturn(Result.success(Unit))

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Empty::class.java)
    }

    @Test
    fun `given empty bookings after content was shown, when flow emits empty, then state is Empty`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)

        viewModel = createViewModel()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1)))
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Content::class.java)

        // WHEN
        bookingsFlow.emit(emptyList())
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Empty::class.java)
    }

    @Test
    fun `given empty bookings while Loading, when flow emits empty, then state stays Loading`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)
        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()
        bookingsFlow.emit(emptyList())
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Loading::class.java)
    }

    @Test
    fun `given bookings loaded, when onBookingSelected, then selection updates`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onBookingSelected(2L)

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.selectedDetails?.id).isEqualTo(2L)
    }

    @Test
    fun `given content state, when onRefresh, then pullToRefreshState is Refreshing`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)

        viewModel = createViewModel()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1)))
        advanceUntilIdle()

        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Refreshing)
    }

    @Test
    fun `given non-content state, when onRefresh, then state becomes Loading`() = runTest {
        // GIVEN
        whenever(bookingListHandler.bookingsFlow).thenReturn(MutableSharedFlow())
        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .thenReturn(Result.failure(RuntimeException("error")))

        viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Error::class.java)

        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Loading::class.java)
    }

    @Test
    fun `given refresh fails on content, when onRefresh, then pullToRefreshState returns to Enabled`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .thenReturn(Result.failure(RuntimeException("error")))

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
    }

    @Test
    fun `given refresh fails on non-content, when onRefresh, then state is Error`() = runTest {
        // GIVEN
        whenever(bookingListHandler.bookingsFlow).thenReturn(MutableSharedFlow())
        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .thenReturn(Result.failure(RuntimeException("first error")))

        viewModel = createViewModel()
        advanceUntilIdle()

        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .thenReturn(Result.failure(RuntimeException("refresh error")))

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosBookingsState.Error
        assertThat(state.message).isEqualTo("refresh error")
    }

    @Test
    fun `given content state, when onRefresh, then cancels previous fetch job`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onRefresh()
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler, times(2))
            .loadBookings(sortBy = BookingListSortOption.NewestToOldest)
    }

    @Test
    fun `given content state, when onEndOfBookingsListReached succeeds, then paginationState is None`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onEndOfBookingsListReached()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
        verify(bookingListHandler).loadMore()
    }

    @Test
    fun `given content state, when onEndOfBookingsListReached fails, then paginationState is Error`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        whenever(bookingListHandler.loadMore()).thenReturn(Result.failure(RuntimeException("load more failed")))

        // WHEN
        viewModel.onEndOfBookingsListReached()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.Error)
    }

    @Test
    fun `given pagination error, when onEndOfBookingsListReached, then does not retry`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        whenever(bookingListHandler.loadMore()).thenReturn(Result.failure(RuntimeException("error")))
        viewModel.onEndOfBookingsListReached()
        advanceUntilIdle()

        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.Error)

        // WHEN
        viewModel.onEndOfBookingsListReached()
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler, times(1)).loadMore()
    }

    @Test
    fun `given pagination error, when onPaginationErrorTryAgain succeeds, then paginationState is None`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        whenever(bookingListHandler.loadMore()).thenReturn(Result.failure(RuntimeException("error")))
        viewModel.onEndOfBookingsListReached()
        advanceUntilIdle()

        whenever(bookingListHandler.loadMore()).thenReturn(Result.success(Unit))

        // WHEN
        viewModel.onPaginationErrorTryAgain()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.paginationState).isEqualTo(WooPosPaginationState.None)
    }

    @Test
    fun `given fetch in progress, when onEndOfBookingsListReached, then waits for fetch to complete`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)
        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .doSuspendableAnswer {
                delay(1000)
                Result.success(Unit)
            }

        viewModel = createViewModel()
        advanceTimeBy(100)

        bookingsFlow.emit(listOf(booking(1)))
        advanceTimeBy(400)

        // WHEN
        viewModel.onEndOfBookingsListReached()
        advanceTimeBy(100)

        // THEN - loadMore should not have been called yet (fetch still running)
        verify(bookingListHandler, times(0)).loadMore()

        advanceUntilIdle()
        verify(bookingListHandler, times(1)).loadMore()
    }

    @Test
    fun `given error state, when onBookingsLoadingErrorRetryButtonClicked, then resets to Loading and fetches`() =
        runTest {
            // GIVEN
            whenever(bookingListHandler.bookingsFlow).thenReturn(MutableSharedFlow())
            whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
                .thenReturn(Result.failure(RuntimeException("error")))

            viewModel = createViewModel()
            advanceUntilIdle()
            assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Error::class.java)

            whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
                .doSuspendableAnswer {
                    delay(Long.MAX_VALUE)
                    Result.success(Unit)
                }

            // WHEN
            viewModel.onBookingsLoadingErrorRetryButtonClicked()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Loading::class.java)
            verify(bookingListHandler, times(2)).loadBookings(sortBy = BookingListSortOption.NewestToOldest)
        }

    @Test
    fun `given empty state, when onBookingsEmptyActionClicked, then resets to Loading and fetches`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)

        viewModel = createViewModel()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1)))
        advanceUntilIdle()

        bookingsFlow.emit(emptyList())
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Empty::class.java)

        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

        // WHEN
        viewModel.onBookingsEmptyActionClicked()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Loading::class.java)
        verify(bookingListHandler, times(2)).loadBookings(sortBy = BookingListSortOption.NewestToOldest)
    }

    @Test
    fun `given non-content state, when onIssueRefundDialogDismissed, then state remains unchanged`() = runTest {
        // GIVEN
        whenever(bookingListHandler.bookingsFlow).thenReturn(MutableSharedFlow())
        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .thenReturn(Result.failure(RuntimeException("error")))

        viewModel = createViewModel()
        advanceUntilIdle()
        val beforeState = viewModel.state.value

        // WHEN
        viewModel.onIssueRefundDialogDismissed()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(beforeState)
    }

    @Test
    fun `given non-content state, when onBookingSelected, then state remains unchanged`() = runTest {
        // GIVEN
        whenever(bookingListHandler.bookingsFlow).thenReturn(MutableSharedFlow())
        whenever(bookingListHandler.loadBookings(sortBy = BookingListSortOption.NewestToOldest))
            .thenReturn(Result.failure(RuntimeException("error")))

        viewModel = createViewModel()
        advanceUntilIdle()
        val beforeState = viewModel.state.value

        // WHEN
        viewModel.onBookingSelected(1L)

        // THEN
        assertThat(viewModel.state.value).isEqualTo(beforeState)
    }

    @Test
    fun `given content state, when pullToRefreshState is Enabled, then it is set correctly`() = runTest {
        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
    }

    @Test
    fun `given content loaded, when new bookings emitted and selected booking still exists, then selection preserved`() =
        runTest {
            // GIVEN
            val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
            whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)

            viewModel = createViewModel()
            advanceUntilIdle()

            bookingsFlow.emit(listOf(booking(1), booking(2), booking(3)))
            advanceUntilIdle()

            viewModel.onBookingSelected(2L)

            // WHEN
            bookingsFlow.emit(listOf(booking(1), booking(2), booking(3), booking(4)))
            advanceUntilIdle()

            // THEN
            val content = viewModel.state.value as WooPosBookingsState.Content
            assertThat(content.selectedDetails?.id).isEqualTo(2L)
        }
}
