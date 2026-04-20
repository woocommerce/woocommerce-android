package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.model.OrderFulfillment
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.ShippingLabelMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.wc.order.OrderTestUtils

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailRepositoryTest : BaseUnitTest() {
    private val site = SiteModel().apply { id = 6 }
    private val orderStore: WCOrderStore = mock()
    private val productStore: WCProductStore = mock()
    private val refundStore: WCRefundStore = mock()
    private val shippingLabelStore: WCShippingLabelStore = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val orderMapper: OrderMapper = mock()
    private val shippingLabelMapper: ShippingLabelMapper = mock()

    private lateinit var sut: OrderDetailRepository

    @Before
    fun setUp() {
        sut = OrderDetailRepository(
            orderStore = orderStore,
            productStore = productStore,
            refundStore = refundStore,
            shippingLabelStore = shippingLabelStore,
            selectedSite = selectedSite,
            wooCommerceStore = wooCommerceStore,
            dispatchers = coroutinesTestRule.testDispatchers,
            orderMapper = orderMapper,
            shippingLabelMapper = shippingLabelMapper
        )
    }

    @Test
    fun `when getOrderFulfillments is called, then store models are mapped to app models`() = testBlocking {
        val orderId = 123L
        val storedFulfillment = OrderTestUtils.generateOrderFulfillment(site.id, orderId)
        val expectedFulfillment = OrderFulfillment(
            localSiteId = site.id,
            orderId = orderId,
            fulfillmentId = 42L,
            status = "fulfilled",
            isFulfilled = true,
            dateUpdated = "2026-03-18 21:00:00",
            dateFulfilled = "2026-03-18 14:30:00",
            trackingNumber = "1Z999AA10123456784",
            shipmentProvider = "ups",
            trackingUrl = "https://www.ups.com/track?tracknum=1Z999AA10123456784"
        )
        doReturn(listOf(storedFulfillment)).whenever(orderStore).getOrderFulfillmentsForOrder(site, orderId)

        val result = sut.getOrderFulfillments(orderId)

        assertThat(result).containsExactly(expectedFulfillment)
    }
}
