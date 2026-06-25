package com.woocommerce.android.model

import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import java.util.Date

class OrderMapperTest {
    private val getLocations: GetLocations = mock()

    private val dateUtils: DateUtils = mock()

    private lateinit var orderMapper: OrderMapper

    private val testDate = Date()
    private val localSiteId = LocalOrRemoteId.LocalId(1)

    @Before
    fun setUp() {
        orderMapper = OrderMapper(getLocations, dateUtils)

        whenever(dateUtils.getDateUsingSiteTimeZone(org.mockito.kotlin.any())).thenReturn(testDate)
        whenever(getLocations.invoke(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(
            Pair(Location.EMPTY, AmbiguousLocation.Raw(""))
        )
    }

    @Test
    fun `when createdVia is pos-rest-api, then salesChannel is POS`() = runTest {
        val orderEntity = createTestOrderEntity(createdVia = "pos-rest-api")

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.salesChannel).isEqualTo(Order.SalesChannel.POS)
    }

    @Test
    fun `when createdVia is rest-api, then salesChannel is NON_POS`() = runTest {
        val orderEntity = createTestOrderEntity(createdVia = "rest-api")

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.salesChannel).isEqualTo(Order.SalesChannel.NON_POS)
    }

    @Test
    fun `when createdVia is empty, then salesChannel is NON_POS`() = runTest {
        val orderEntity = createTestOrderEntity(createdVia = "")

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.salesChannel).isEqualTo(Order.SalesChannel.NON_POS)
    }

    @Test
    fun `when createdVia is wc-admin, then salesChannel is NON_POS`() = runTest {
        val orderEntity = createTestOrderEntity(createdVia = "wc-admin")

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.salesChannel).isEqualTo(Order.SalesChannel.NON_POS)
    }

    @Test
    fun `when createdVia is checkout, then salesChannel is NON_POS`() = runTest {
        val orderEntity = createTestOrderEntity(createdVia = "checkout")

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.salesChannel).isEqualTo(Order.SalesChannel.NON_POS)
    }

    @Test
    fun `when fulfillment status is fulfilled, then map to FULFILLED`() = runTest {
        val orderEntity = createTestOrderEntity(
            metaData = listOf(
                WCMetaData(
                    id = 1L,
                    key = WCMetaData.OrderFulfillmentMetadataKeys.FULFILLMENT_STATUS,
                    value = "fulfilled"
                )
            )
        )

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.fulfillmentStatus).isEqualTo(Order.FulfillmentStatus.FULFILLED)
    }

    @Test
    fun `when fulfillment status is partially fulfilled, then map to PARTIALLY_FULFILLED`() = runTest {
        val orderEntity = createTestOrderEntity(
            metaData = listOf(
                WCMetaData(
                    id = 1L,
                    key = WCMetaData.OrderFulfillmentMetadataKeys.FULFILLMENT_STATUS,
                    value = "partially_fulfilled"
                )
            )
        )

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.fulfillmentStatus).isEqualTo(Order.FulfillmentStatus.PARTIALLY_FULFILLED)
    }

    @Test
    fun `when fulfillment status is unfulfilled, then map to UNFULFILLED`() = runTest {
        val orderEntity = createTestOrderEntity(
            metaData = listOf(
                WCMetaData(
                    id = 1L,
                    key = WCMetaData.OrderFulfillmentMetadataKeys.FULFILLMENT_STATUS,
                    value = "unfulfilled"
                )
            )
        )

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.fulfillmentStatus).isEqualTo(Order.FulfillmentStatus.UNFULFILLED)
    }

    @Test
    fun `when fulfillment status metadata is missing, then map to NO_FULFILLMENTS`() = runTest {
        val orderEntity = createTestOrderEntity()

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.fulfillmentStatus).isEqualTo(Order.FulfillmentStatus.NO_FULFILLMENTS)
    }

    @Test
    fun `when fulfillment status is empty, then map to UNKNOWN`() = runTest {
        val orderEntity = createTestOrderEntity(
            metaData = listOf(
                WCMetaData(
                    id = 1L,
                    key = WCMetaData.OrderFulfillmentMetadataKeys.FULFILLMENT_STATUS,
                    value = ""
                )
            )
        )

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.fulfillmentStatus).isEqualTo(Order.FulfillmentStatus.UNKNOWN)
    }

    @Test
    fun `when fulfillment status is no fulfillments, then map to UNKNOWN`() = runTest {
        val orderEntity = createTestOrderEntity(
            metaData = listOf(
                WCMetaData(
                    id = 1L,
                    key = WCMetaData.OrderFulfillmentMetadataKeys.FULFILLMENT_STATUS,
                    value = "no_fulfillments"
                )
            )
        )

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.fulfillmentStatus).isEqualTo(Order.FulfillmentStatus.UNKNOWN)
    }

    @Test
    fun `when fulfillment status is unknown, then map to UNKNOWN`() = runTest {
        val orderEntity = createTestOrderEntity(
            metaData = listOf(
                WCMetaData(
                    id = 1L,
                    key = WCMetaData.OrderFulfillmentMetadataKeys.FULFILLMENT_STATUS,
                    value = "custom_future_status"
                )
            )
        )

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.fulfillmentStatus).isEqualTo(Order.FulfillmentStatus.UNKNOWN)
    }

    private fun createTestOrderEntity(
        createdVia: String = "",
        metaData: List<WCMetaData> = emptyList(),
    ): OrderEntity {
        return OrderEntity(
            localSiteId = localSiteId,
            orderId = 1L,
            number = "1",
            status = "completed",
            currency = "USD",
            dateCreated = "2023-01-01T00:00:00Z",
            dateModified = "2023-01-01T00:00:00Z",
            total = "100.00",
            createdVia = createdVia,
            metaData = metaData,
        )
    }
}
