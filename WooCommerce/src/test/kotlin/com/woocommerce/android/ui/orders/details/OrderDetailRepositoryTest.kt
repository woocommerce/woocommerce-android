package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.model.Order
import com.woocommerce.android.notifications.push.NewOrderNotificationSuppressionCache
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult

@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailRepositoryTest : BaseUnitTest() {
    private val site: SiteModel = mock { on { siteId } doReturn SITE_ID }
    private val selectedSite: SelectedSite = mock { on { get() } doReturn site }
    private val orderStore: WCOrderStore = mock()
    private val newOrderNotificationSuppressionCache: NewOrderNotificationSuppressionCache = mock()

    private val sut = OrderDetailRepository(
        orderStore = orderStore,
        productStore = mock(),
        refundStore = mock(),
        shippingLabelStore = mock(),
        selectedSite = selectedSite,
        wooCommerceStore = mock(),
        dispatchers = coroutinesTestRule.testDispatchers,
        orderMapper = mock(),
        shippingLabelMapper = mock(),
        newOrderNotificationSuppressionCache = newOrderNotificationSuppressionCache,
    )

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

    private suspend fun givenUpdateResult(result: UpdateOrderResult) {
        whenever(orderStore.getOrderStatusForSiteAndKey(any(), any())).thenReturn(null)
        whenever(orderStore.updateOrderStatus(any(), any(), any())).thenReturn(flowOf(result))
    }

    private companion object {
        const val SITE_ID = 999L
        const val ORDER_ID = 123L
    }
}
