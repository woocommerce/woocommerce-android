package com.woocommerce.android.model

import com.woocommerce.android.util.DateUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import java.util.Date

class OrderMapperTest {
    @Mock
    private lateinit var getLocations: GetLocations

    @Mock
    private lateinit var dateUtils: DateUtils

    private lateinit var orderMapper: OrderMapper

    private val testDate = Date()
    private val localSiteId = LocalOrRemoteId.LocalId(1)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        orderMapper = OrderMapper(getLocations, dateUtils)

        whenever(dateUtils.getDateUsingSiteTimeZone(org.mockito.kotlin.any())).thenReturn(testDate)
        whenever(getLocations.invoke(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(
            Pair(Location.EMPTY, AmbiguousLocation.Raw(""))
        )
    }

    @Test
    fun `when createdVia is pos-rest-api, then salesChannel is POS`() {
        val orderEntity = createTestOrderEntity(createdVia = "pos-rest-api")

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.salesChannel).isEqualTo(Order.SalesChannel.POS)
    }

    @Test
    fun `when createdVia is rest-api, then salesChannel is ALL_OTHER`() {
        val orderEntity = createTestOrderEntity(createdVia = "rest-api")

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.salesChannel).isEqualTo(Order.SalesChannel.ALL_OTHER)
    }

    @Test
    fun `when createdVia is empty, then salesChannel is ALL_OTHER`() {
        val orderEntity = createTestOrderEntity(createdVia = "")

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.salesChannel).isEqualTo(Order.SalesChannel.ALL_OTHER)
    }

    @Test
    fun `when createdVia is wc-admin, then salesChannel is ALL_OTHER`() {
        val orderEntity = createTestOrderEntity(createdVia = "wc-admin")

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.salesChannel).isEqualTo(Order.SalesChannel.ALL_OTHER)
    }

    @Test
    fun `when createdVia is checkout, then salesChannel is ALL_OTHER`() {
        val orderEntity = createTestOrderEntity(createdVia = "checkout")

        val result = orderMapper.toAppModel(orderEntity)

        assertThat(result.salesChannel).isEqualTo(Order.SalesChannel.ALL_OTHER)
    }

    private fun createTestOrderEntity(createdVia: String = ""): OrderEntity {
        return OrderEntity(
            localSiteId = localSiteId,
            orderId = 1L,
            number = "1",
            status = "completed",
            currency = "USD",
            dateCreated = "2023-01-01T00:00:00Z",
            dateModified = "2023-01-01T00:00:00Z",
            total = "100.00",
            createdVia = createdVia
        )
    }
}
