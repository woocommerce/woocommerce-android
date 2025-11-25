package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.dao.BookingsDao
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.BookingEntity.AttendanceStatus
import org.wordpress.android.fluxc.persistence.entity.BookingEntity.Status
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.HeadersParser
import java.time.Clock
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

class BookingsStoreTest {

    private lateinit var sut: BookingsStore

    private val bookingsRestClient: BookingsRestClient = mock()
    private val orderStore: WCOrderStore = mock()
    private val bookingsDao: BookingsDao = mock()
    private val bookingDtoMapper: BookingDtoMapper = BookingDtoMapper(mock())
    private val headersParser: HeadersParser = mock()
    private val clock: Clock = Clock.systemUTC()

    @Before
    fun setUp() {
        sut = BookingsStore(
            bookingsRestClient = bookingsRestClient,
            orderStore = orderStore,
            bookingsDao = bookingsDao,
            bookingDtoMapper = bookingDtoMapper,
            headersParser = headersParser,
            coroutineEngine = CoroutineEngine(EmptyCoroutineContext, mock()),
            clock = clock,
        )
    }

    @Test
    fun `given refreshOrder is false, when updateBooking succeeds, then inserts mapped entity and returns it`(): Unit =
        runBlocking {
            // given
            val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID.value }
            val dto = sampleBookingDto()
            val storedBooking = sampleBookingEntity(order = BookingOrderInfo(productInfo = null))
            whenever(bookingsDao.getBooking(TEST_LOCAL_SITE_ID, dto.id)).thenReturn(storedBooking)
            whenever(bookingsRestClient.updateBooking(site, dto.id, BookingUpdatePayload(note = "n")))
                .thenReturn(WooPayload(dto))
            whenever(bookingsDao.insertOrReplace(any<BookingEntity>())).thenReturn(1L)

            // when
            val result = sut.updateBooking(
                site = site,
                bookingId = dto.id,
                bookingUpdatePayload = BookingUpdatePayload(note = "n"),
                refreshOrder = false
            )

            // then
            assertThat(result.isError).isFalse()
            assertThat(result.model).isNotNull
            // The store preserves the stored order on the mapped entity
            verify(bookingsDao).insertOrReplace(argThat<BookingEntity> { this.order == storedBooking.order })
        }

    @Test
    fun `given refreshOrder is true, when updateBooking succeeds, then refreshes order and inserts mapped entity`(): Unit =
        runBlocking {
            // given
            val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID.value }
            val dto = sampleBookingDto()

            val fetchedOrder = OrderEntity(orderId = dto.orderId, localSiteId = TEST_LOCAL_SITE_ID)
            whenever(orderStore.fetchSingleOrderSync(site, dto.orderId)).thenReturn(WooResult(fetchedOrder))
            whenever(bookingsRestClient.updateBooking(site, dto.id, BookingUpdatePayload(status = Status.Confirmed)))
                .thenReturn(WooPayload(dto))
            whenever(bookingsDao.insertOrReplace(any<BookingEntity>())).thenReturn(1L)

            // when
            val result = sut.updateBooking(
                site = site,
                bookingId = dto.id,
                bookingUpdatePayload = BookingUpdatePayload(status = Status.Confirmed),
                refreshOrder = true
            )

            // then
            assertThat(result.isError).isFalse()
            val expected = with(bookingDtoMapper) { dto.toEntity(TEST_LOCAL_SITE_ID, fetchedOrder) }
            assertThat(result.model).isEqualTo(expected)
            verify(bookingsDao).insertOrReplace(expected)
            verify(bookingsDao, never()).getBooking(TEST_LOCAL_SITE_ID, dto.id)
        }

    @Test
    fun `given rest client fails, when updateBooking, then returns error and does not insert`(): Unit = runBlocking {
        // given
        val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID.value }
        val error = WooError(GENERIC_ERROR, UNKNOWN)
        whenever(bookingsRestClient.updateBooking(site, TEST_BOOKING_ID, BookingUpdatePayload(note = "n")))
            .thenReturn(WooPayload(error))

        // when
        val result = sut.updateBooking(
            site = site,
            bookingId = TEST_BOOKING_ID,
            bookingUpdatePayload = BookingUpdatePayload(note = "n"),
            refreshOrder = false
        )

        // then
        assertThat(result.isError).isTrue()
        verify(bookingsDao, never()).insertOrReplace(any<BookingEntity>())
    }

    @Test
    fun `given order refresh fails, when updateBooking with refreshOrder true, then fallbacks to local entity and inserts it`(): Unit =
        runBlocking {
            // given
            val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID.value }
            val dto = sampleBookingDto()
            val storedBooking = sampleBookingEntity(
                order = BookingOrderInfo(
                    productInfo = null,
                    status = "paid",
                )
            )
            whenever(bookingsDao.getBooking(TEST_LOCAL_SITE_ID, dto.id)).thenReturn(storedBooking)
            whenever(
                bookingsRestClient.updateBooking(
                    site,
                    dto.id,
                    BookingUpdatePayload(attendanceStatus = AttendanceStatus.CheckedIn)
                )
            ).thenReturn(WooPayload(dto))

            whenever(
                orderStore.fetchSingleOrderSync(
                    site,
                    dto.orderId
                )
            ).thenReturn(WooResult(error = WooError(GENERIC_ERROR, UNKNOWN)))

            // when
            val result = sut.updateBooking(
                site = site,
                bookingId = dto.id,
                bookingUpdatePayload = BookingUpdatePayload(attendanceStatus = AttendanceStatus.CheckedIn),
                refreshOrder = true
            )

            // then
            assertThat(result.isError).isFalse
            verify(bookingsDao).getBooking(TEST_LOCAL_SITE_ID, dto.id)
            // The store preserves the stored order on the mapped entity
            verify(bookingsDao).insertOrReplace(argThat<BookingEntity> { this.order == storedBooking.order })
        }

    @Test
    fun `given page 1 and today date filter and no query, when fetchBookings, then delete matching and insert is called`(): Unit =
        runBlocking {
            // given
            val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID.value }
            val dto = sampleBookingDto().copy(orderId = 0L) // avoid order fetch
            val filters = BookingFilters(
                dateRange = BookingsDateRangePresets.today(clock)
            )
            whenever(
                bookingsRestClient.fetchBookings(
                    site,
                    perPage = 25,
                    page = 1,
                    query = null,
                    filters = filters,
                    order = BookingsOrderOption.DESC
                )
            )
                .thenReturn(WooPayload(arrayOf(dto)))
            whenever(headersParser.getTotalPages(any())).thenReturn(null)

            // when
            val result = sut.fetchBookings(
                site = site,
                perPage = 25,
                page = 1,
                query = null,
                filters = filters,
                order = BookingsOrderOption.DESC
            )

            // then
            assertThat(result.isError).isFalse()
            // We delete only matching filtered rows then insert the fetched page
            verify(bookingsDao).deleteForSiteWithDateRangeFilter(
                any(),
                any<BookingsFilterOption.DateRange>(),
                any<List<Long>>()
            )
            verify(bookingsDao).insertOrReplace(any<List<BookingEntity>>())
            verify(bookingsDao, never()).replaceAllForSite(any(), any())
        }

    @Test
    fun `given page greater than 1 with multiple filters, when fetchBookings, then insertOrReplace is called`(): Unit =
        runBlocking {
            // given
            val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID.value }
            val dto = sampleBookingDto().copy(orderId = 0L)
            val filters = BookingFilters(
                dateRange = BookingsFilterOption.DateRange(before = Instant.now(), after = Instant.now()),
                customer = BookingsFilterOption.Customer(customerId = 1L, customerName = "name")
            )
            whenever(
                bookingsRestClient.fetchBookings(
                    site,
                    perPage = 25,
                    page = 2,
                    query = null,
                    filters = filters,
                    order = BookingsOrderOption.DESC
                )
            )
                .thenReturn(WooPayload(arrayOf(dto)))
            whenever(headersParser.getTotalPages(any())).thenReturn(null)

            // when
            val result = sut.fetchBookings(
                site = site,
                perPage = 25,
                page = 2,
                query = null,
                filters = filters,
                order = BookingsOrderOption.DESC
            )

            // then
            assertThat(result.isError).isFalse()
            verify(bookingsDao).insertOrReplace(any<List<BookingEntity>>())
            verify(bookingsDao, never()).replaceAllForSite(any(), any())
            verify(bookingsDao, never())
                .deleteForSiteWithDateRangeFilter(any(), any<BookingsFilterOption.DateRange>(), any<List<Long>>())
        }

    @Test
    fun `given page 1 with no filters and no query, when fetchBookings, then replaceAllForSite is called`(): Unit =
        runBlocking {
            // given
            val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID.value }
            val dto = sampleBookingDto().copy(orderId = 0L)
            val filters: BookingFilters = BookingFilters.EMPTY
            whenever(
                bookingsRestClient.fetchBookings(
                    site,
                    perPage = 25,
                    page = 1,
                    query = null,
                    filters = filters,
                    order = BookingsOrderOption.DESC
                )
            ).thenReturn(WooPayload(arrayOf(dto)))
            whenever(headersParser.getTotalPages(any())).thenReturn(null)

            // when
            val result = sut.fetchBookings(
                site = site,
                perPage = 25,
                page = 1,
                query = null,
                filters = filters,
                order = BookingsOrderOption.DESC
            )

            // then
            assertThat(result.isError).isFalse()
            verify(bookingsDao).replaceAllForSite(any(), any())
            verify(bookingsDao, never()).deleteForSiteWithDateRangeFilter(
                any(),
                any<BookingsFilterOption.DateRange>(),
                any<List<Long>>()
            )
        }

    private fun sampleBookingDto(): BookingDto = BookingDto(
        id = TEST_BOOKING_ID,
        start = Instant.now().epochSecond,
        end = Instant.now().plusSeconds(3600).epochSecond,
        allDay = false,
        status = Status.Unpaid.key,
        cost = "10.00",
        currency = "USD",
        customerId = 1L,
        productId = 2L,
        resourceId = 3L,
        dateCreated = Instant.now().epochSecond,
        dateModified = Instant.now().epochSecond,
        googleCalendarEventId = "",
        orderId = 100L,
        orderItemId = 200L,
        parentId = 0L,
        personCounts = null,
        localTimezone = "UTC",
        attendanceStatus = AttendanceStatus.Booked.key,
        note = null
    )

    private fun sampleBookingEntity(order: BookingOrderInfo): BookingEntity = BookingEntity(
        id = RemoteId(TEST_BOOKING_ID),
        localSiteId = TEST_LOCAL_SITE_ID,
        start = Instant.now(),
        end = Instant.now().plusSeconds(3600),
        allDay = false,
        status = Status.Unpaid,
        cost = "10.00",
        currency = "USD",
        customerId = 1L,
        productId = 2L,
        resourceId = 3L,
        dateCreated = Instant.now(),
        dateModified = Instant.now(),
        googleCalendarEventId = "",
        orderId = 100L,
        orderItemId = 200L,
        parentId = 0L,
        personCounts = null,
        localTimezone = "UTC",
        attendanceStatus = AttendanceStatus.Booked,
        note = "",
        order = order,
        customerNote = "Customer Note"
    )

    companion object {
        private const val TEST_BOOKING_ID = 42L
        private val TEST_LOCAL_SITE_ID = LocalId(999)
    }
}
