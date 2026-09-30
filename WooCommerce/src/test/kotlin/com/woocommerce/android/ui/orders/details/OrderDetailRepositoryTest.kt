package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.notifications.push.NewOrderNotificationSuppressionCache
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.products.RefreshProductsSignal
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_SERVICES
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_SHIPPING

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailRepositoryTest : BaseUnitTest() {
    private val site: SiteModel = mock { on { siteId } doReturn SITE_ID }
    private val selectedSite: SelectedSite = mock { on { get() } doReturn site }
    private val orderStore: WCOrderStore = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val orderMapper: OrderMapper = mock()
    private val refreshProductsSignal: RefreshProductsSignal = mock()
    private val newOrderNotificationSuppressionCache: NewOrderNotificationSuppressionCache = mock()

    private val sut = OrderDetailRepository(
        orderStore = orderStore,
        productStore = mock(),
        refundStore = mock(),
        shippingLabelStore = mock(),
        selectedSite = selectedSite,
        wooCommerceStore = wooCommerceStore,
        dispatchers = coroutinesTestRule.testDispatchers,
        orderMapper = orderMapper,
        shippingLabelMapper = mock(),
        refreshProductsSignal = refreshProductsSignal,
        newOrderNotificationSuppressionCache = newOrderNotificationSuppressionCache,
    )

    @Test
    fun `given the status update is confirmed remotely, when updateOrderStatus, then products refresh is signalled`() =
        testBlocking {
            // GIVEN
            val orderEntity = OrderTestUtils.generateOrder()
            val order = OrderTestUtils.generateTestOrder().copy(
                items = listOf(
                    OrderTestUtils.generateTestOrder().items.first().copy(productId = 101L),
                    OrderTestUtils.generateTestOrder().items.first().copy(productId = 102L),
                )
            )
            whenever(orderStore.getOrderByIdAndSite(ORDER_ID, site)).thenReturn(orderEntity)
            whenever(orderMapper.toAppModel(orderEntity)).thenReturn(order)
            whenever(orderStore.updateOrderStatus(any(), any(), any()))
                .thenReturn(flowOf(UpdateOrderResult.RemoteUpdateResult(OnOrderChanged())))

            // WHEN
            sut.updateOrderStatus(ORDER_ID, "completed").toList()

            // THEN
            verify(refreshProductsSignal).notifyProductsChanged(listOf(101L, 102L))
        }

    @Test
    fun `given only an optimistic update, when updateOrderStatus, then products refresh is not signalled`() =
        testBlocking {
            // GIVEN
            whenever(orderStore.updateOrderStatus(any(), any(), any()))
                .thenReturn(flowOf(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged())))

            // WHEN
            sut.updateOrderStatus(ORDER_ID, "completed").toList()

            // THEN
            verify(refreshProductsSignal, never()).notifyProductsChanged(any())
        }

    @Test
    fun `given an order in a non-notifiable status, when the remote update succeeds, then the transition is recorded`() =
        testBlocking {
            // GIVEN
            givenUpdateResult(UpdateOrderResult.RemoteUpdateResult(OnOrderChanged()))
            whenever(orderStore.getOrderByIdAndSite(ORDER_ID, site))
                .thenReturn(OrderTestUtils.generateOrder().copy(status = "pending"))

            // WHEN
            sut.updateOrderStatus(ORDER_ID, Order.Status.Completed.value).collect { }

            // THEN
            verify(newOrderNotificationSuppressionCache).onOrderStatusChanged(
                siteId = SITE_ID,
                orderId = ORDER_ID,
                previousStatusKey = "pending",
                newStatusKey = Order.Status.Completed.value,
            )
        }

    @Test
    fun `given the remote update fails, when the status changes, then the order is not recorded`() =
        testBlocking {
            // GIVEN
            givenUpdateResult(
                UpdateOrderResult.RemoteUpdateResult(OnOrderChanged(orderError = WCOrderStore.OrderError()))
            )

            // WHEN
            sut.updateOrderStatus(ORDER_ID, Order.Status.Completed.value).collect { }

            // THEN
            verifyNoInteractions(newOrderNotificationSuppressionCache)
        }

    @Test
    fun `given Woo Shipping plugin is present, when reading plugin info, then map stored plugin`() = testBlocking {
        val plugin = SitePluginModel(
            siteId = LocalId(SITE_ID.toInt()),
            name = WOO_SHIPPING.pluginName,
            version = "1.2.3",
            slug = WOO_SHIPPING.pluginName,
            authorName = "",
            isActive = true
        )
        whenever(wooCommerceStore.getSitePlugins(site, listOf(WOO_SHIPPING)))
            .thenReturn(listOf(plugin))

        val result = sut.getWooShippingPluginInfo()

        assertThat(result).isEqualTo(WooPlugin(true, true, "1.2.3"))
        verify(wooCommerceStore).getSitePlugins(site, listOf(WOO_SHIPPING))
        verify(wooCommerceStore, never()).getSitePlugin(any(), any())
    }

    @Test
    fun `given Woo Shipping plugin is absent, when reading plugin info, then map not installed`() = testBlocking {
        whenever(wooCommerceStore.getSitePlugins(site, listOf(WOO_SHIPPING)))
            .thenReturn(emptyList())

        val result = sut.getWooShippingPluginInfo()

        assertThat(result).isEqualTo(WooPlugin(false, false, null))
        verify(wooCommerceStore).getSitePlugins(site, listOf(WOO_SHIPPING))
    }

    @Test
    fun `given legacy shipping plugin is present, when reading plugin info, then map stored plugin`() = testBlocking {
        val plugin = SitePluginModel(
            siteId = LocalId(SITE_ID.toInt()),
            name = WOO_SERVICES.pluginName,
            version = "1.2.3",
            slug = WOO_SERVICES.pluginName,
            authorName = "",
            isActive = false
        )
        whenever(wooCommerceStore.getSitePlugins(site, listOf(WOO_SERVICES)))
            .thenReturn(listOf(plugin))

        val result = sut.getWooServicesPluginInfo()

        assertThat(result).isEqualTo(WooPlugin(true, false, "1.2.3"))
        verify(wooCommerceStore).getSitePlugins(site, listOf(WOO_SERVICES))
        verify(wooCommerceStore, never()).getSitePlugin(any(), any())
    }

    @Test
    fun `given legacy shipping plugin is absent, when reading plugin info, then map not installed`() = testBlocking {
        whenever(wooCommerceStore.getSitePlugins(site, listOf(WOO_SERVICES)))
            .thenReturn(emptyList())

        val result = sut.getWooServicesPluginInfo()

        assertThat(result).isEqualTo(WooPlugin(false, false, null))
        verify(wooCommerceStore).getSitePlugins(site, listOf(WOO_SERVICES))
    }

    private suspend fun givenUpdateResult(result: UpdateOrderResult) {
        whenever(orderStore.getOrderStatusForSiteAndKey(any(), any())).thenReturn(null)
        whenever(orderStore.updateOrderStatus(any(), any(), any())).thenReturn(flowOf(result))
    }

    private companion object {
        const val SITE_ID = 999L
        const val ORDER_ID = 123L
    }
}
