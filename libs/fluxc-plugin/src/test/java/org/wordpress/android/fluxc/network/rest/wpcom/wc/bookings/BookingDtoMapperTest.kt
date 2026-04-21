package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import java.math.BigDecimal
import java.time.Instant

class BookingDtoMapperTest {
    private val productsDao: ProductsDao = mock()
    private val localSiteId = LocalId(123)
    private val mapper = BookingDtoMapper(productsDao)

    @Test
    fun `given valid booking dto, when mapping to entity, then all fields are correctly mapped`() = runTest {
        // Given
        val bookingDto = createSampleBookingDto()

        // When
        val result = with(mapper) { bookingDto.toEntity(localSiteId, null) }

        // Then
        assertThat(result.id).isEqualTo(RemoteId(1L))
        assertThat(result.localSiteId).isEqualTo(localSiteId)
        assertThat(result.start).isEqualTo(Instant.ofEpochSecond(1609459200L)) // 2021-01-01 00:00:00
        assertThat(result.end).isEqualTo(Instant.ofEpochSecond(1609462800L)) // 2021-01-01 01:00:00
        assertThat(result.allDay).isEqualTo(false)
        assertThat(result.status).isEqualTo(BookingEntity.Status.Confirmed)
        assertThat(result.cost).isEqualTo("50.00")
        assertThat(result.currency).isEqualTo("USD")
        assertThat(result.customerId).isEqualTo(456L)
        assertThat(result.userId).isEqualTo(123L)
        assertThat(result.productId).isEqualTo(789L)
        assertThat(result.resourceId).isEqualTo(101L)
        assertThat(result.dateCreated).isEqualTo(Instant.ofEpochSecond(1609455600L))
        assertThat(result.dateModified).isEqualTo(Instant.ofEpochSecond(1609459200L))
        assertThat(result.googleCalendarEventId).isEqualTo("cal-123")
        assertThat(result.orderId).isEqualTo(999L)
        assertThat(result.orderItemId).isEqualTo(111L)
        assertThat(result.parentId).isEqualTo(0L)
        assertThat(result.personCounts).containsExactly(2L, 1L)
        assertThat(result.localTimezone).isEqualTo("America/New_York")
        assertThat(result.order.productInfo).isNull()
    }

    @Test
    fun `given booking dto with null person counts, when mapping, then person counts is null`() = runTest {
        // Given
        val bookingDto = createSampleBookingDto(personCounts = null)

        // When
        val result = with(mapper) { bookingDto.toEntity(localSiteId, null) }

        // Then
        assertThat(result.personCounts).isNull()
    }

    @Test
    fun `given booking dto with empty person counts, when mapping, then person counts is empty`() = runTest {
        // Given
        val bookingDto = createSampleBookingDto(personCounts = emptyList())

        // When
        val result = with(mapper) { bookingDto.toEntity(localSiteId, null) }

        // Then
        assertThat(result.personCounts).isEmpty()
    }

    @Test
    fun `given booking dto with unknown status, when mapping, then status is Unknown`() = runTest {
        // Given
        val bookingDto = createSampleBookingDto(status = "unknown-status")

        // When
        val result = with(mapper) { bookingDto.toEntity(localSiteId, null) }

        // Then
        assertThat(result.status).isInstanceOf(BookingEntity.Status.Unknown::class.java)
        assertThat(result.status.key).isEqualTo("unknown-status")
    }

    @Test
    fun `given booking dto with all valid statuses, when mapping, then correct status enum is returned`() = runTest {
        val statusMappings = mapOf(
            "unpaid" to BookingEntity.Status.Unpaid,
            "pending-confirmation" to BookingEntity.Status.PendingConfirmation,
            "confirmed" to BookingEntity.Status.Confirmed,
            "paid" to BookingEntity.Status.Paid,
            "cancelled" to BookingEntity.Status.Cancelled,
            "complete" to BookingEntity.Status.Complete
        )

        statusMappings.forEach { (statusKey, expectedStatus) ->
            // Given
            val bookingDto = createSampleBookingDto(status = statusKey)

            // When
            val result = with(mapper) { bookingDto.toEntity(localSiteId, null) }

            // Then
            assertThat(result.status).isEqualTo(expectedStatus)
        }
    }

    @Test
    fun `given booking dto with order entity, when mapping, then order info is populated from order`() = runTest {
        // Given
        val bookingDto = createSampleBookingDto()
        val orderEntity = createSampleOrderEntity()

        // When
        val result = with(mapper) { bookingDto.toEntity(localSiteId, orderEntity) }

        // Then
        with(result.order) {
            assertThat(status).isEqualTo("processing")
            assertThat(productInfo?.name).isEqualTo("Test Product")
            assertThat(customerInfo?.billingFirstName).isEqualTo("John")
            assertThat(customerInfo?.billingLastName).isEqualTo("Doe")
            assertThat(customerInfo?.billingEmail).isEqualTo("john@example.com")
            assertThat(customerInfo?.billingCompany).isNull() // empty string becomes null
            assertThat(customerInfo?.billingAddress1).isEqualTo("123 Main St")
            assertThat(paymentInfo?.subtotal).isEqualTo(BigDecimal("40.00"))
            assertThat(paymentInfo?.total).isEqualTo(BigDecimal("50.00"))
        }
    }

    @Test
    fun `given booking dto without order entity and product exists, when mapping, then order info has product name`() =
        runTest {
            // Given
            val bookingDto = createSampleBookingDto()
            val productModel = WCProductModel(name = "Test Product from DB")
            given(productsDao.getProduct(localSiteId.value, bookingDto.productId)).willReturn(productModel)

            // When
            val result = with(mapper) { bookingDto.toEntity(localSiteId, null) }

            // Then
            assertThat(result.order.productInfo?.name).isEqualTo("Test Product from DB")
            assertThat(result.order.status).isNull()
            assertThat(result.order.customerInfo).isNull()
            assertThat(result.order.paymentInfo).isNull()
        }

    @Test
    fun `given booking dto without order entity and no product, when mapping, then order info has null product info`() =
        runTest {
            // Given
            val bookingDto = createSampleBookingDto()
            given(productsDao.getProduct(any<Int>(), any<Long>())).willReturn(null)

            // When
            val result = with(mapper) { bookingDto.toEntity(localSiteId, null) }

            // Then
            assertThat(result.order.productInfo).isNull()
            assertThat(result.order.status).isNull()
            assertThat(result.order.customerInfo).isNull()
            assertThat(result.order.paymentInfo).isNull()
        }

    @Test
    fun `given order entity with empty billing fields, when mapping, then empty strings become null`() = runTest {
        // Given
        val bookingDto = createSampleBookingDto()
        val orderEntity = createSampleOrderEntity().copy(
            billingCompany = "",
            billingAddress2 = "",
            billingCity = "",
            billingState = "",
            billingPostcode = "",
            billingCountry = "",
            billingPhone = ""
        )

        // When
        val result = with(mapper) { bookingDto.toEntity(localSiteId, orderEntity) }

        // Then
        with(result.order.customerInfo!!) {
            assertThat(billingCompany).isNull()
            assertThat(billingAddress2).isNull()
            assertThat(billingCity).isNull()
            assertThat(billingState).isNull()
            assertThat(billingPostcode).isNull()
            assertThat(billingCountry).isNull()
            assertThat(billingPhone).isNull()
        }
    }

    @Test
    fun `given order entity with no matching line item, when mapping, then payment info is null`() = runTest {
        // Given
        val bookingDto = createSampleBookingDto(orderItemId = 999L) // non-matching ID
        val orderEntity = createSampleOrderEntity()

        // When
        val result = with(mapper) { bookingDto.toEntity(localSiteId, orderEntity) }

        // Then
        assertThat(result.order.paymentInfo).isNull()
    }

    @Test
    fun `given order entity with line item having invalid amounts, when mapping, then defaults to zero`() = runTest {
        // Given
        val bookingDto = createSampleBookingDto()
        val orderEntity = createSampleOrderEntity().copy(
            lineItems = "[{\"id\":111,\"name\":\"Test Product\",\"subtotal\":\"invalid\",\"total\":\"also-invalid\",\"subtotal_tax\":null,\"total_tax\":\"\"}]"
        )

        // When
        val result = with(mapper) { bookingDto.toEntity(localSiteId, orderEntity) }

        // Then
        with(result.order.paymentInfo!!) {
            assertThat(paymentMethodId).isEqualTo("cod")
            assertThat(paymentMethodTitle).isEqualTo("Cash on Delivery")
            assertThat(subtotal).isEqualTo(BigDecimal.ZERO)
            assertThat(total).isEqualTo(BigDecimal.ZERO)
            assertThat(subtotalTax).isEqualTo(BigDecimal.ZERO)
            assertThat(totalTax).isEqualTo(BigDecimal.ZERO)
        }
    }

    private fun createSampleBookingDto(
        id: Long = 1L,
        start: Long = 1609459200L, // 2021-01-01 00:00:00
        end: Long = 1609462800L, // 2021-01-01 01:00:00
        allDay: Boolean = false,
        status: String = "confirmed",
        cost: String = "50.00",
        currency: String = "USD",
        customerId: Long = 456L,
        userId: Long = 123L,
        productId: Long = 789L,
        resourceId: Long = 101L,
        dateCreated: Long = 1609455600L,
        dateModified: Long = 1609459200L,
        googleCalendarEventId: String = "cal-123",
        orderId: Long = 999L,
        orderItemId: Long = 111L,
        parentId: Long = 0L,
        personCounts: List<Int>? = listOf(2, 1),
        localTimezone: String = "America/New_York"
    ) = BookingDto(
        id = id,
        start = start,
        end = end,
        allDay = allDay,
        status = status,
        cost = cost,
        currency = currency,
        customerId = customerId,
        userId = userId,
        productId = productId,
        resourceId = resourceId,
        dateCreated = dateCreated,
        dateModified = dateModified,
        googleCalendarEventId = googleCalendarEventId,
        orderId = orderId,
        orderItemId = orderItemId,
        parentId = parentId,
        personCounts = personCounts,
        localTimezone = localTimezone,
        attendanceStatus = null,
    )

    private fun createSampleOrderEntity() = OrderEntity(
        localSiteId = localSiteId,
        orderId = 999L,
        number = "1001",
        status = "processing",
        currency = "USD",
        orderKey = "",
        dateCreated = "",
        dateModified = "",
        total = "50.00",
        totalTax = "",
        shippingTotal = "",
        paymentMethod = "cod",
        paymentMethodTitle = "Cash on Delivery",
        datePaid = "",
        pricesIncludeTax = false,
        customerNote = "",
        discountTotal = "",
        discountCodes = "",
        customerId = 456L,
        billingFirstName = "John",
        billingLastName = "Doe",
        billingCompany = "",
        billingAddress1 = "123 Main St",
        billingAddress2 = "Apt 1",
        billingCity = "New York",
        billingState = "NY",
        billingPostcode = "10001",
        billingCountry = "US",
        billingEmail = "john@example.com",
        billingPhone = "555-1234",
        shippingFirstName = "",
        shippingLastName = "",
        shippingCompany = "",
        shippingAddress1 = "",
        shippingAddress2 = "",
        shippingCity = "",
        shippingState = "",
        shippingPostcode = "",
        shippingCountry = "",
        shippingPhone = "",
        lineItems = "[{\"id\":111,\"name\":\"Test Product\",\"subtotal\":\"40.00\",\"subtotal_tax\":\"5.00\",\"total\":\"50.00\",\"total_tax\":\"7.50\"}]",
        shippingLines = "[]",
        feeLines = "[]",
        taxLines = "[]",
        couponLines = "[]",
        metaData = emptyList(),
        paymentUrl = "",
        isEditable = true,
        needsPayment = null,
        needsProcessing = null,
        giftCardCode = "",
        giftCardAmount = "",
        shippingTax = "",
        createdVia = ""
    )
}
