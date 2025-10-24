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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.dao.BookingsDao
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.BookingEntity.AttendanceStatus
import org.wordpress.android.fluxc.persistence.entity.BookingEntity.Status
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.HeadersParser
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

class BookingsStoreTest {

    private lateinit var sut: BookingsStore

    private val bookingsRestClient: BookingsRestClient = mock()
    private val orderStore: WCOrderStore = mock()
    private val bookingsDao: BookingsDao = mock()
    private val bookingDtoMapper: BookingDtoMapper = BookingDtoMapper(mock())
    private val headersParser: HeadersParser = mock()

    @Before
    fun setUp() {
        sut = BookingsStore(
            bookingsRestClient = bookingsRestClient,
            orderStore = orderStore,
            bookingsDao = bookingsDao,
            bookingDtoMapper = bookingDtoMapper,
            headersParser = headersParser,
            coroutineEngine = CoroutineEngine(EmptyCoroutineContext, mock())
        )
    }

    @Test
    fun `given refreshBooking is false, when updateBooking succeeds, then inserts mapped entity and returns it`(): Unit = runBlocking {
        // given
        val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID.value }
        val dto = sampleBookingDto()
        val storedBooking = sampleBookingEntity(order = BookingOrderInfo(productInfo = null))
        whenever(bookingsDao.getBooking(TEST_LOCAL_SITE_ID, dto.id)).thenReturn(storedBooking)
        whenever(bookingsRestClient.updateBooking(site, dto.id, BookingUpdatePayload(note = "n")))
            .thenReturn(org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload(dto))
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
    fun `given refreshBooking is true, when updateBooking succeeds, then refreshes order and inserts mapped entity`(): Unit = runBlocking {
        // given
        val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID.value }
        val dto = sampleBookingDto()

        val fetchedOrder = OrderEntity(orderId = dto.orderId, localSiteId = TEST_LOCAL_SITE_ID)
        whenever(orderStore.fetchSingleOrderSync(site, dto.orderId)).thenReturn(WooResult(fetchedOrder))
        whenever(bookingsRestClient.updateBooking(site, dto.id, BookingUpdatePayload(status = Status.Confirmed)))
            .thenReturn(org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload(dto))
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
    }

    @Test
    fun `given rest client fails, when updateBooking, then returns error and does not insert`(): Unit = runBlocking {
        // given
        val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID.value }
        val error = WooError(GENERIC_ERROR, UNKNOWN)
        whenever(bookingsRestClient.updateBooking(site, TEST_BOOKING_ID, BookingUpdatePayload(note = "n")))
            .thenReturn(org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload(error))

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
    fun `given order refresh fails, when updateBooking with refreshBooking true, then returns error and does not insert`(): Unit = runBlocking {
        // given
        val site = SiteModel().apply { id = TEST_LOCAL_SITE_ID.value }
        val dto = sampleBookingDto()
        whenever(
            bookingsRestClient.updateBooking(
                site,
                dto.id,
                BookingUpdatePayload(attendanceStatus = AttendanceStatus.CheckedIn)
            )
        )
            .thenReturn(org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload(dto))

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
        assertThat(result.isError).isTrue()
        verify(bookingsDao, never()).insertOrReplace(any<BookingEntity>())
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
        order = order
    )

    companion object {
        private const val TEST_BOOKING_ID = 42L
        private val TEST_LOCAL_SITE_ID = LocalId(999)
    }
}
