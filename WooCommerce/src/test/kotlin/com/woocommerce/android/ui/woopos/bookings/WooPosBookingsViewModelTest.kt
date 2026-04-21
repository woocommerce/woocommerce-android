package com.woocommerce.android.ui.woopos.bookings

import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.bookings.list.BookingListHandler
import com.woocommerce.android.ui.bookings.list.BookingListSortOption
import com.woocommerce.android.ui.woopos.cardpayment.CardPaymentSource
import com.woocommerce.android.ui.woopos.common.util.WooPosClipboardHelper
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.localcatalog.DateTimeProvider
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
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
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingOrderInfo
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingProductInfo
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosBookingsViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule(StandardTestDispatcher())

    private val bookingListHandler: BookingListHandler = mock()
    private val bookingsRepository: BookingsRepository = mock {
        on { observeResources() } doAnswer { flowOf(emptyList()) }
        on { fetchResources() } doAnswer { Result.success(Unit) }
    }
    private val dateTimeProvider: DateTimeProvider = mock()
    private val formatPrice: WooPosFormatPrice = mock {
        on { invoke(any<BigDecimal>(), any()) } doAnswer { invocation ->
            val price = invocation.arguments[0] as BigDecimal
            "$${price.toPlainString()}"
        }
    }
    private val timeRangeFormatter: WooPosBookingTimeRangeFormatter = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getQuantityString(any(), any(), anyOrNull(), anyOrNull()) } doAnswer { invocation ->
            val quantity = invocation.arguments[0] as Int
            val default = invocation.arguments[1] as Int
            val one = invocation.arguments[3] as Int?
            when (quantity) {
                1 -> when (one) {
                    R.string.booking_duration_second -> "1 second"
                    R.string.booking_duration_minute -> "1 minute"
                    R.string.booking_duration_hour -> "1 hour"
                    R.string.booking_duration_day -> "1 day"
                    else -> ""
                }
                else -> when (default) {
                    R.string.booking_duration_seconds -> "$quantity seconds"
                    R.string.booking_duration_minutes -> "$quantity minutes"
                    R.string.booking_duration_hours -> "$quantity hours"
                    R.string.booking_duration_days -> "$quantity days"
                    else -> ""
                }
            }
        }
        on { getString(any()) } doAnswer { "" }
        on { getString(any(), any(), any(), any(), any(), any()) } doAnswer {
            "Cancel dialog message"
        }
    }
    private val clipboardHelper: WooPosClipboardHelper = mock()
    private val paymentStatusResolver: WooPosPaymentStatusResolver = mock()
    private val analyticsTracker: WooPosBookingsAnalyticsTracker = mock()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-02-19T10:00:00Z"), ZoneOffset.UTC)
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

    private fun createSelectedSite(siteTimezone: String = "0"): SelectedSite {
        val siteModel = SiteModel().apply { timezone = siteTimezone }
        return mock {
            on { get() } doAnswer { siteModel }
        }
    }

    private fun createViewModel(
        siteTimezone: String = "0",
        clock: Clock = this.clock,
    ): WooPosBookingsViewModel {
        val selectedSite = createSelectedSite(siteTimezone)
        return WooPosBookingsViewModel(
            bookingListHandler = bookingListHandler,
            bookingsRepository = bookingsRepository,
            dateTimeProvider = dateTimeProvider,
            mapper = WooPosBookingViewStateMapper(
                resourceProvider,
                formatPrice,
                paymentStatusResolver,
                timeRangeFormatter,
            ),
            clipboardHelper = clipboardHelper,
            resourceProvider = resourceProvider,
            clock = clock,
            analyticsTracker = analyticsTracker,
            selectedSite = selectedSite,
        )
    }

    @Before
    fun setUp() = runTest {
        whenever(formatPrice(anyOrNull())).doAnswer { invocation ->
            val amount = invocation.arguments[0] as? java.math.BigDecimal
            amount?.let { "$${it.toPlainString()}" } ?: "$0.00"
        }
        whenever(timeRangeFormatter.format(any(), any())).thenReturn("10:13 AM – 11:13 AM")
        whenever(bookingListHandler.bookingsFlow).thenReturn(
            flowOf(listOf(booking(1), booking(2)))
        )
        whenever(
            bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest))
        ).thenReturn(Result.success(0))
        whenever(bookingListHandler.loadMore()).thenReturn(Result.success(0))
        whenever(bookingListHandler.hasMorePages).thenReturn(true)
        whenever(dateTimeProvider.now()).thenReturn(0L)
        whenever(paymentStatusResolver.resolve(any())).thenReturn(PaymentStatus.UNPAID)
        whenever(bookingsRepository.fetchBooking(any())).thenReturn(Result.success(booking()))
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
        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .thenReturn(Result.failure(RuntimeException("Network error")))

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosBookingsState.Error::class.java)
    }

    @Test
    fun `given no bookings exist, when init completes, then state is Content with NothingFound`() = runTest {
        // GIVEN
        whenever(bookingListHandler.bookingsFlow).thenReturn(flowOf(emptyList()))
        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .thenReturn(Result.success(0))

        // WHEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosBookingsState.Content
        assertThat(state.items).isInstanceOf(WooPosBookingsState.Content.Items.NothingFound::class.java)
    }

    @Test
    fun `given empty bookings after content was shown, when flow emits empty, then state is Content with NothingFound`() =
        runTest {
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
            val state = viewModel.state.value as WooPosBookingsState.Content
            assertThat(state.items).isInstanceOf(WooPosBookingsState.Content.Items.NothingFound::class.java)
        }

    @Test
    fun `given empty bookings while Loading, when flow emits empty, then state stays Loading`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)
        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(0)
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
    fun `given content state, when PTR, then pullToRefreshState is Refreshing`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)

        viewModel = createViewModel()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1)))
        advanceUntilIdle()

        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(0)
            }

        // WHEN
        viewModel.onPullToRefresh()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Refreshing)
    }

    @Test
    fun `given non-content state, when PTR, then state becomes Loading`() = runTest {
        // GIVEN
        whenever(bookingListHandler.bookingsFlow).thenReturn(MutableSharedFlow())
        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .thenReturn(Result.failure(RuntimeException("error")))

        viewModel = createViewModel()
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Error::class.java)

        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(0)
            }

        // WHEN
        viewModel.onPullToRefresh()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Loading::class.java)
    }

    @Test
    fun `given refresh fails on content, when PTR, then pullToRefreshState returns to Enabled`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .thenReturn(Result.failure(RuntimeException("error")))

        // WHEN
        viewModel.onPullToRefresh()
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.pullToRefreshState).isEqualTo(WooPosPullToRefreshState.Enabled)
    }

    @Test
    fun `given refresh fails on non-content, when PTR, then state is Error`() = runTest {
        // GIVEN
        whenever(bookingListHandler.bookingsFlow).thenReturn(MutableSharedFlow())
        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .thenReturn(Result.failure(RuntimeException("first error")))

        viewModel = createViewModel()
        advanceUntilIdle()

        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .thenReturn(Result.failure(RuntimeException("refresh error")))

        // WHEN
        viewModel.onPullToRefresh()
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value as WooPosBookingsState.Error
        assertThat(state.message).isEqualTo("refresh error")
    }

    @Test
    fun `given content state, when PTR, then cancels previous fetch job`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onPullToRefresh()
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler, times(2))
            .loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest))
    }

    @Test
    fun `given content state, when onEndOfBookingsListReached succeeds, then paginationState is None`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)

        viewModel = createViewModel()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1), booking(2)))
        advanceUntilIdle()

        // WHEN
        viewModel.onEndOfBookingsListReached()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1), booking(2), booking(3)))
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
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)

        viewModel = createViewModel()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1), booking(2)))
        advanceUntilIdle()

        whenever(bookingListHandler.loadMore()).thenReturn(Result.failure(RuntimeException("error")))
        viewModel.onEndOfBookingsListReached()
        advanceUntilIdle()

        whenever(bookingListHandler.loadMore()).thenReturn(Result.success(0))

        // WHEN
        viewModel.onPaginationErrorTryAgain()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1), booking(2), booking(3)))
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
        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .doSuspendableAnswer {
                delay(1000)
                Result.success(0)
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
            whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
                .thenReturn(Result.failure(RuntimeException("error")))

            viewModel = createViewModel()
            advanceUntilIdle()
            assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Error::class.java)

            whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
                .doSuspendableAnswer {
                    delay(Long.MAX_VALUE)
                    Result.success(0)
                }

            // WHEN
            viewModel.onBookingsLoadingErrorRetryButtonClicked()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Loading::class.java)
            verify(bookingListHandler, times(2)).loadBookings(
                anyOrNull(),
                any(),
                eq(BookingListSortOption.OldestToNewest)
            )
        }

    @Test
    fun `given empty state, when onBookingsEmptyActionClicked, then resets to Loading and fetches`() = runTest {
        // GIVEN
        whenever(bookingListHandler.bookingsFlow).thenReturn(flowOf(emptyList()))

        viewModel = createViewModel()
        advanceUntilIdle()

        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(0)
            }

        // WHEN
        viewModel.onBookingsEmptyActionClicked()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Loading::class.java)
        verify(bookingListHandler, times(2)).loadBookings(
            anyOrNull(),
            any(),
            eq(BookingListSortOption.OldestToNewest)
        )
    }

    @Test
    fun `given non-content state, when onIssueRefundDialogDismissed, then state remains unchanged`() = runTest {
        // GIVEN
        whenever(bookingListHandler.bookingsFlow).thenReturn(MutableSharedFlow())
        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
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
        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
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

    @Test
    fun `given selected booking, when CollectPaymentClicked, then navigation event emitted with correct orderId and BOOKINGS source`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.navigationEvent.test {
                // WHEN
                viewModel.onUIEvent(WooPosBookingsUIEvent.CollectPaymentClicked)

                // THEN
                val event = awaitItem()
                assertThat(event).isInstanceOf(WooPosNavigationEvent.OpenCardPayment::class.java)
                val cardEvent = event as WooPosNavigationEvent.OpenCardPayment
                assertThat(cardEvent.orderId).isEqualTo(10L)
                assertThat(cardEvent.source).isEqualTo(CardPaymentSource.BOOKINGS)
                assertThat(cardEvent.showCashPaymentButton).isTrue()
            }
        }

    @Test
    fun `given no selected booking, when CollectPaymentClicked, then no navigation event emitted`() =
        runTest {
            // GIVEN
            whenever(bookingListHandler.bookingsFlow).thenReturn(MutableSharedFlow())
            whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
                .thenReturn(Result.failure(RuntimeException("error")))

            viewModel = createViewModel()
            advanceUntilIdle()
            assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Error::class.java)

            viewModel.navigationEvent.test {
                // WHEN
                viewModel.onUIEvent(WooPosBookingsUIEvent.CollectPaymentClicked)

                // THEN
                expectNoEvents()
            }
        }

    @Test
    fun `given content state, when attendance toggled to attended, then selection updates optimistically`() = runTest {
        // GIVEN
        whenever(bookingsRepository.updateAttendanceStatus(any(), any()))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(true))
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        val details = content.selectedDetails!!
        val attendanceSection = details.attendanceSection as WooPosBookingsState.AttendanceSection.Visible
        assertThat(attendanceSection.selection)
            .isEqualTo(WooPosBookingsState.AttendanceState.ATTENDED)
        assertThat(details.attendanceBadge)
            .isEqualTo(WooPosBookingsState.AttendanceState.ATTENDED)
    }

    @Test
    fun `given content state, when attendance toggled to unattended, then selection updates optimistically`() = runTest {
        // GIVEN
        whenever(bookingsRepository.updateAttendanceStatus(any(), any()))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(Unit)
            }
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(false))
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        val details = content.selectedDetails!!
        val attendanceSection = details.attendanceSection as WooPosBookingsState.AttendanceSection.Visible
        assertThat(attendanceSection.selection)
            .isEqualTo(WooPosBookingsState.AttendanceState.UNATTENDED)
        assertThat(details.attendanceBadge)
            .isEqualTo(WooPosBookingsState.AttendanceState.UNATTENDED)
    }

    @Test
    fun `given content state, when attendance toggle API fails, then selection reverts to previous value`() = runTest {
        // GIVEN
        whenever(bookingsRepository.updateAttendanceStatus(any(), any()))
            .thenReturn(Result.failure(RuntimeException("network error")))
        viewModel = createViewModel()
        advanceUntilIdle()

        val contentBefore = viewModel.state.value as WooPosBookingsState.Content
        val previousSection = contentBefore.selectedDetails!!.attendanceSection
            as WooPosBookingsState.AttendanceSection.Visible
        val previousSelection = previousSection.selection

        // WHEN
        viewModel.onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(false))
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        val attendanceSection = content.selectedDetails!!.attendanceSection
            as WooPosBookingsState.AttendanceSection.Visible
        assertThat(attendanceSection.selection).isEqualTo(previousSelection)
    }

    @Test
    fun `given content state, when attendance toggled to attended, then repository called with correct params`() =
        runTest {
            // GIVEN
            whenever(bookingsRepository.updateAttendanceStatus(any(), any()))
                .thenReturn(Result.success(Unit))
            viewModel = createViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(true))
            advanceUntilIdle()

            // THEN
            verify(bookingsRepository).updateAttendanceStatus(
                bookingId = 1L,
                attendanceStatus = BookingEntity.AttendanceStatus.Attended
            )
        }

    @Test
    fun `given content loaded, when IssueRefund action clicked, then issue refund dialog is shown`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.IssueRefund(orderId = 10L)
                )
            )
            advanceUntilIdle()

            // THEN
            val state = viewModel.state.value as WooPosBookingsState.Content
            assertThat(state.dialogState)
                .isInstanceOf(WooPosBookingsState.Content.DialogState.IssueRefund::class.java)
            val dialogState = state.dialogState as WooPosBookingsState.Content.DialogState.IssueRefund
            assertThat(dialogState.orderId).isEqualTo(10L)
        }

    @Test
    fun `given non-Content state, when IssueRefund action clicked, then state remains unchanged`() =
        runTest {
            // GIVEN
            whenever(bookingListHandler.bookingsFlow).thenReturn(MutableSharedFlow())
            whenever(
                bookingListHandler.loadBookings(
                    filters = anyOrNull(),
                    sortBy = anyOrNull(),
                    searchQuery = anyOrNull()
                )
            ).thenReturn(Result.failure(RuntimeException("error")))

            viewModel = createViewModel()
            advanceUntilIdle()
            val beforeState = viewModel.state.value

            // WHEN
            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.IssueRefund(orderId = 10L)
                )
            )
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.state.value).isEqualTo(beforeState)
            assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Error::class.java)
        }

    @Test
    fun `given IssueRefund dialog visible, when onIssueRefundDialogDismissed, then dialog is hidden`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.IssueRefund(orderId = 10L)
                )
            )
            advanceUntilIdle()
            val state = viewModel.state.value as WooPosBookingsState.Content
            assertThat(state.dialogState)
                .isInstanceOf(WooPosBookingsState.Content.DialogState.IssueRefund::class.java)

            // WHEN
            viewModel.onIssueRefundDialogDismissed()
            advanceUntilIdle()

            // THEN
            val updatedState = viewModel.state.value as WooPosBookingsState.Content
            assertThat(updatedState.dialogState)
                .isInstanceOf(WooPosBookingsState.Content.DialogState.Hidden::class.java)
        }

    @Test
    fun `given cancel action clicked, when handling event, then dialog state is Confirmation`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            // WHEN
            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.CancelBooking(
                        bookingId = bookingId,
                        orderId = bookingId * 10
                    )
                )
            )
            advanceUntilIdle()

            // THEN
            val updatedContent = viewModel.state.value as WooPosBookingsState.Content
            assertThat(updatedContent.dialogState)
                .isInstanceOf(
                    WooPosBookingsState.Content.DialogState.CancelBooking.PendingConfirmation::class.java
                )
        }

    @Test
    fun `given cancel confirmed successfully, when handling event, then hides dialog`() =
        runTest {
            // GIVEN
            whenever(bookingsRepository.cancelBooking(any()))
                .thenReturn(Result.success(Unit))
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.CancelBooking(
                        bookingId = bookingId,
                        orderId = bookingId * 10
                    )
                )
            )
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosBookingsUIEvent.CancelBookingConfirmed)
            advanceUntilIdle()

            // THEN
            verify(bookingsRepository).cancelBooking(bookingId)
            val updatedContent =
                viewModel.state.value as WooPosBookingsState.Content
            assertThat(updatedContent.dialogState)
                .isInstanceOf(
                    WooPosBookingsState.Content.DialogState.Hidden::class.java
                )
        }

    @Test
    fun `given cancel confirmed with failure, when handling event, then dialog state is Error`() =
        runTest {
            // GIVEN
            whenever(bookingsRepository.cancelBooking(any()))
                .thenReturn(Result.failure(RuntimeException("Network error")))
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.CancelBooking(
                        bookingId = bookingId,
                        orderId = bookingId * 10
                    )
                )
            )
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosBookingsUIEvent.CancelBookingConfirmed)
            advanceUntilIdle()

            // THEN
            val updatedContent =
                viewModel.state.value as WooPosBookingsState.Content
            assertThat(updatedContent.dialogState)
                .isInstanceOf(
                    WooPosBookingsState.Content.DialogState.CancelBooking.Error::class.java
                )
        }

    @Test
    fun `given cancel dismissed, when handling event, then hides dialog`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.CancelBooking(
                        bookingId = bookingId,
                        orderId = bookingId * 10
                    )
                )
            )
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosBookingsUIEvent.CancelBookingDismissed)
            advanceUntilIdle()

            // THEN
            val updatedContent =
                viewModel.state.value as WooPosBookingsState.Content
            assertThat(updatedContent.dialogState)
                .isInstanceOf(
                    WooPosBookingsState.Content.DialogState.Hidden::class.java
                )
        }

    @Test
    fun `given ViewOrder action, when BookingMenuActionClicked, then OpenOrderDetails event emitted with correct orderId`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val orderId = content.selectedDetails!!.orderId

            viewModel.navigationEvent.test {
                // WHEN
                viewModel.onUIEvent(
                    WooPosBookingsUIEvent.BookingMenuActionClicked(
                        WooPosBookingsState.BookingAction.ViewOrder(orderId = orderId)
                    )
                )

                // THEN
                val event = awaitItem()
                assertThat(event).isInstanceOf(WooPosNavigationEvent.OpenOrderDetails::class.java)
                val orderDetailsEvent = event as WooPosNavigationEvent.OpenOrderDetails
                assertThat(orderDetailsEvent.orderId).isEqualTo(orderId)
            }
        }

    @Test
    fun `given EmailReceipt action, when BookingMenuActionClicked, then OpenEmailReceipt event emitted with correct orderId`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val orderId = content.selectedDetails!!.orderId

            viewModel.navigationEvent.test {
                // WHEN
                viewModel.onUIEvent(
                    WooPosBookingsUIEvent.BookingMenuActionClicked(
                        WooPosBookingsState.BookingAction.EmailReceipt(orderId = orderId)
                    )
                )

                // THEN
                val event = awaitItem()
                assertThat(event).isInstanceOf(WooPosNavigationEvent.OpenEmailReceipt::class.java)
                val emailEvent = event as WooPosNavigationEvent.OpenEmailReceipt
                assertThat(emailEvent.orderId).isEqualTo(orderId)
            }
        }

    @Test
    fun `given cancel confirmed successfully, when handling event, then pullToRefreshState stays Enabled`() =
        runTest {
            // GIVEN
            whenever(bookingsRepository.cancelBooking(any()))
                .thenReturn(Result.success(Unit))
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.CancelBooking(
                        bookingId = bookingId,
                        orderId = bookingId * 10
                    )
                )
            )
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosBookingsUIEvent.CancelBookingConfirmed)
            advanceUntilIdle()

            // THEN
            val updatedContent = viewModel.state.value as WooPosBookingsState.Content
            assertThat(updatedContent.pullToRefreshState)
                .isEqualTo(WooPosPullToRefreshState.Enabled)
        }

    @Test
    fun `given cancel confirmed successfully, when handling event, then single booking is refreshed`() =
        runTest {
            // GIVEN
            whenever(bookingsRepository.cancelBooking(any()))
                .thenReturn(Result.success(Unit))
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.CancelBooking(
                        bookingId = bookingId,
                        orderId = bookingId * 10
                    )
                )
            )
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosBookingsUIEvent.CancelBookingConfirmed)
            advanceUntilIdle()

            // THEN
            verify(bookingsRepository).fetchBooking(bookingId)
        }

    @Test
    fun `given IssueRefund dialog dismissed, when refreshing, then pullToRefreshState stays Enabled`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.IssueRefund(orderId = 10L)
                )
            )
            advanceUntilIdle()

            // WHEN
            viewModel.onIssueRefundDialogDismissed()
            advanceUntilIdle()

            // THEN
            val updatedState = viewModel.state.value as WooPosBookingsState.Content
            assertThat(updatedState.pullToRefreshState)
                .isEqualTo(WooPosPullToRefreshState.Enabled)
        }

    @Test
    fun `given booking note saved, when refreshing, then pullToRefreshState stays Enabled`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onBookingNoteSaved()
            advanceUntilIdle()

            // THEN
            val content = viewModel.state.value as WooPosBookingsState.Content
            assertThat(content.pullToRefreshState)
                .isEqualTo(WooPosPullToRefreshState.Enabled)
        }

    @Test
    fun `when payment completed, then single booking is fetched instead of full refresh`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            // WHEN
            viewModel.onPaymentCompleted()
            advanceUntilIdle()

            // THEN
            verify(bookingsRepository).fetchBooking(bookingId)
            verify(bookingListHandler, times(1))
                .loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest))
        }

    @Test
    fun `when booking note saved, then single booking is fetched instead of full refresh`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            // WHEN
            viewModel.onBookingNoteSaved()
            advanceUntilIdle()

            // THEN
            verify(bookingsRepository).fetchBooking(bookingId)
            verify(bookingListHandler, times(1))
                .loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest))
        }

    @Test
    fun `when issue refund dialog dismissed, then single booking is fetched instead of full refresh`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.IssueRefund(orderId = 10L)
                )
            )
            advanceUntilIdle()

            // WHEN
            viewModel.onIssueRefundDialogDismissed()
            advanceUntilIdle()

            // THEN
            verify(bookingsRepository).fetchBooking(bookingId)
            verify(bookingListHandler, times(1))
                .loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest))
        }

    @Test
    fun `when single booking refresh fails, then error toast is shown`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()

            whenever(bookingsRepository.fetchBooking(any()))
                .thenReturn(Result.failure(RuntimeException("Network error")))

            // WHEN
            viewModel.onPaymentCompleted()
            advanceUntilIdle()

            // THEN
            verify(resourceProvider).getString(R.string.something_went_wrong_try_again)
        }

    @Test
    fun `given processing state, when dismiss event, then dialog stays open`() =
        runTest {
            // GIVEN
            whenever(bookingsRepository.cancelBooking(any()))
                .thenReturn(Result.success(Unit))
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.CancelBooking(
                        bookingId = bookingId,
                        orderId = bookingId * 10
                    )
                )
            )
            advanceUntilIdle()

            viewModel.onUIEvent(WooPosBookingsUIEvent.CancelBookingConfirmed)

            // WHEN
            viewModel.onUIEvent(WooPosBookingsUIEvent.CancelBookingDismissed)

            // THEN
            val updatedContent =
                viewModel.state.value as WooPosBookingsState.Content
            assertThat(updatedContent.dialogState)
                .isInstanceOf(
                    WooPosBookingsState.Content.DialogState.CancelBooking.Processing::class.java
                )
        }

    @Test
    fun `given date changed, when state becomes Content, then dateSelectorState is updated`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        val contentBefore = viewModel.state.value as WooPosBookingsState.Content
        val dateBefore = contentBefore.dateSelectorState?.formattedDate

        // WHEN
        viewModel.onUIEvent(WooPosBookingsUIEvent.NextDayClicked)
        advanceUntilIdle()

        // THEN
        val contentAfter = viewModel.state.value as WooPosBookingsState.Content
        assertThat(contentAfter.dateSelectorState).isNotNull
        assertThat(contentAfter.dateSelectorState?.formattedDate).isNotEqualTo(dateBefore)
    }

    @Test
    fun `given date changed, when previous day clicked, then fetch called with new date`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosBookingsUIEvent.PreviousDayClicked)
        advanceUntilIdle()

        // THEN
        verify(bookingListHandler, times(2))
            .loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest))
    }

    @Test
    fun `given date changed and cache empty, when fetch in progress, then state stays Loading`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)

        viewModel = createViewModel()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1)))
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Content::class.java)

        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(0)
            }

        // WHEN
        viewModel.onUIEvent(WooPosBookingsUIEvent.NextDayClicked)
        advanceUntilIdle()

        bookingsFlow.emit(emptyList())
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.items).isInstanceOf(WooPosBookingsState.Content.Items.Loading::class.java)
    }

    @Test
    fun `given date changed and cache has data, when observe emits, then cached data shown immediately`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)

        viewModel = createViewModel()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1)))
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Content::class.java)

        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .doSuspendableAnswer {
                delay(Long.MAX_VALUE)
                Result.success(0)
            }

        // WHEN
        viewModel.onUIEvent(WooPosBookingsUIEvent.NextDayClicked)
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(5)))
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.items).isInstanceOf(WooPosBookingsState.Content.Items.Loaded::class.java)
        val loaded = content.items as WooPosBookingsState.Content.Items.Loaded
        assertThat(loaded.items.keys.first().id).isEqualTo(5L)
    }

    @Test
    fun `given date changed and cache empty, when fetch succeeds with no data, then state is NothingFound`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)

        viewModel = createViewModel()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1)))
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Content::class.java)

        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .thenReturn(Result.success(0))

        // WHEN
        viewModel.onUIEvent(WooPosBookingsUIEvent.NextDayClicked)
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.items).isInstanceOf(WooPosBookingsState.Content.Items.NothingFound::class.java)
    }

    @Test
    fun `given date changed and cache empty, when fetch fails, then state is Error`() = runTest {
        // GIVEN
        val bookingsFlow = MutableSharedFlow<List<BookingEntity>>()
        whenever(bookingListHandler.bookingsFlow).thenReturn(bookingsFlow)

        viewModel = createViewModel()
        advanceUntilIdle()

        bookingsFlow.emit(listOf(booking(1)))
        advanceUntilIdle()
        assertThat(viewModel.state.value).isInstanceOf(WooPosBookingsState.Content::class.java)

        whenever(bookingListHandler.loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest)))
            .thenReturn(Result.failure(RuntimeException("network error")))

        // WHEN
        viewModel.onUIEvent(WooPosBookingsUIEvent.NextDayClicked)
        advanceUntilIdle()

        // THEN
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.items).isInstanceOf(WooPosBookingsState.Content.Items.Error::class.java)
    }

    @Test
    fun `given rapid date changes, when multiple next day clicks, then intermediate fetches are cancelled`() = runTest {
        // GIVEN
        viewModel = createViewModel()
        advanceUntilIdle()

        val initialDate = (viewModel.state.value as WooPosBookingsState.Content)
            .dateSelectorState?.formattedDate

        // WHEN - rapid tapping next 3 times without advancing the dispatcher
        viewModel.onUIEvent(WooPosBookingsUIEvent.NextDayClicked)
        viewModel.onUIEvent(WooPosBookingsUIEvent.NextDayClicked)
        viewModel.onUIEvent(WooPosBookingsUIEvent.NextDayClicked)
        advanceUntilIdle()

        // THEN - only 2 loadBookings calls: initial + last date change (intermediate ones cancelled)
        verify(bookingListHandler, times(2))
            .loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest))
        val content = viewModel.state.value as WooPosBookingsState.Content
        assertThat(content.dateSelectorState?.formattedDate).isNotEqualTo(initialDate)
    }

    @Test
    fun `given date selected from calendar picker, when DateSelected dispatched, then fetch called for selected date`() =
        runTest {
            // GIVEN
            viewModel = createViewModel()
            advanceUntilIdle()

            val selectedDateMillis = Instant.parse("2026-03-15T00:00:00Z").toEpochMilli()

            // WHEN
            viewModel.onUIEvent(WooPosBookingsUIEvent.DateSelected(selectedDateMillis))
            advanceUntilIdle()

            // THEN
            verify(bookingListHandler, times(2))
                .loadBookings(anyOrNull(), any(), eq(BookingListSortOption.OldestToNewest))
            val content = viewModel.state.value as WooPosBookingsState.Content
            assertThat(content.dateSelectorState?.selectedDateMillis).isEqualTo(selectedDateMillis)
        }

    @Test
    fun `given store in UTC-11, when clock is 2026-02-19T10-00Z, then today is Feb 18 in store timezone`() =
        runTest {
            // GIVEN
            val clockAt10amUtc = Clock.fixed(Instant.parse("2026-02-19T10:00:00Z"), ZoneOffset.UTC)
            viewModel = createViewModel(siteTimezone = "-11", clock = clockAt10amUtc)
            advanceUntilIdle()

            // THEN
            val content = viewModel.state.value as WooPosBookingsState.Content
            val dateSelectorState = content.dateSelectorState!!
            assertThat(dateSelectorState.formattedDate).startsWith("18")
            assertThat(dateSelectorState.formattedDate).doesNotContain("19")
        }

    @Test
    fun `given store in UTC-5, when date selected via DatePicker, then API filter boundaries use UTC`() =
        runTest {
            // GIVEN
            val filtersCaptor = argumentCaptor<BookingFilters>()
            viewModel = createViewModel(siteTimezone = "-5")
            advanceUntilIdle()

            // WHEN
            val march15MidnightUtc = Instant.parse("2026-03-15T00:00:00Z")
            viewModel.onUIEvent(WooPosBookingsUIEvent.DateSelected(march15MidnightUtc.toEpochMilli()))
            advanceUntilIdle()

            // THEN
            verify(bookingListHandler, times(2))
                .loadBookings(anyOrNull(), filtersCaptor.capture(), eq(BookingListSortOption.OldestToNewest))
            val dateRange = filtersCaptor.lastValue.dateRange as BookingsFilterOption.DateRange
            assertThat(dateRange.after).isEqualTo(Instant.parse("2026-03-15T00:00:00Z"))
            assertThat(dateRange.before).isEqualTo(Instant.parse("2026-03-15T23:59:59.999999999Z"))
        }

    @Test
    fun `given store in UTC+5, when date selected, then API filter boundaries use UTC not store timezone`() =
        runTest {
            // GIVEN
            val filtersCaptor = argumentCaptor<BookingFilters>()
            viewModel = createViewModel(siteTimezone = "5")
            advanceUntilIdle()

            // WHEN
            val march15MidnightUtc = Instant.parse("2026-03-15T00:00:00Z")
            viewModel.onUIEvent(WooPosBookingsUIEvent.DateSelected(march15MidnightUtc.toEpochMilli()))
            advanceUntilIdle()

            // THEN
            verify(bookingListHandler, times(2))
                .loadBookings(anyOrNull(), filtersCaptor.capture(), eq(BookingListSortOption.OldestToNewest))
            val dateRange = filtersCaptor.lastValue.dateRange as BookingsFilterOption.DateRange
            assertThat(dateRange.after).isEqualTo(Instant.parse("2026-03-15T00:00:00Z"))
            assertThat(dateRange.before).isEqualTo(Instant.parse("2026-03-15T23:59:59.999999999Z"))
        }

    @Test
    fun `given cancel confirmed successfully, when handling event, then tracks booking cancelled`() =
        runTest {
            // GIVEN
            whenever(bookingsRepository.cancelBooking(any()))
                .thenReturn(Result.success(Unit))
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.CancelBooking(
                        bookingId = bookingId,
                        orderId = bookingId * 10
                    )
                )
            )
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosBookingsUIEvent.CancelBookingConfirmed)
            advanceUntilIdle()

            // THEN
            verify(analyticsTracker).trackBookingCancelled()
        }

    @Test
    fun `given cancel confirmed with failure, when handling event, then tracks cancel failed`() =
        runTest {
            // GIVEN
            whenever(bookingsRepository.cancelBooking(any()))
                .thenReturn(Result.failure(RuntimeException("Network error")))
            viewModel = createViewModel()
            advanceUntilIdle()
            val content = viewModel.state.value as WooPosBookingsState.Content
            val bookingId = content.selectedDetails!!.id

            viewModel.onUIEvent(
                WooPosBookingsUIEvent.BookingMenuActionClicked(
                    WooPosBookingsState.BookingAction.CancelBooking(
                        bookingId = bookingId,
                        orderId = bookingId * 10
                    )
                )
            )
            advanceUntilIdle()

            // WHEN
            viewModel.onUIEvent(WooPosBookingsUIEvent.CancelBookingConfirmed)
            advanceUntilIdle()

            // THEN
            verify(analyticsTracker).trackBookingCancelFailed(any(), any())
        }

    @Test
    fun `given attendance toggled successfully, when handling event, then tracks attendance changed`() = runTest {
        // GIVEN
        whenever(bookingsRepository.updateAttendanceStatus(any(), any()))
            .thenReturn(Result.success(Unit))
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(true))
        advanceUntilIdle()

        // THEN
        verify(analyticsTracker).trackAttendanceChanged()
    }

    @Test
    fun `given attendance toggle fails, when handling event, then tracks attendance change failed`() = runTest {
        // GIVEN
        whenever(bookingsRepository.updateAttendanceStatus(any(), any()))
            .thenReturn(Result.failure(RuntimeException("network error")))
        viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(false))
        advanceUntilIdle()

        // THEN
        verify(analyticsTracker).trackAttendanceChangeFailed(any(), any())
    }

    @Test
    fun `when payment completed, then collect payment button is hidden immediately`() = runTest {
        // GIVEN
        whenever(bookingsRepository.fetchBooking(any())).doSuspendableAnswer {
            delay(5000)
            Result.success(booking())
        }
        viewModel = createViewModel()
        advanceUntilIdle()
        val contentBefore = viewModel.state.value as WooPosBookingsState.Content
        assertThat(contentBefore.selectedDetails!!.paymentSection.collectPaymentLabel).isNotNull()

        // WHEN
        viewModel.onPaymentCompleted()

        // THEN
        val contentAfter = viewModel.state.value as WooPosBookingsState.Content
        assertThat(contentAfter.selectedDetails!!.paymentSection.collectPaymentLabel).isNull()
    }
}
