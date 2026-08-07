package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.RefreshProductsSignal
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.OptimisticUpdateResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult

@ExperimentalCoroutinesApi
class OrderDetailRepositoryTest : BaseUnitTest() {
    private val site = SiteModel()
    private val orderStore: WCOrderStore = mock()
    private val orderMapper: OrderMapper = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val refreshProductsSignal: RefreshProductsSignal = mock()

    private lateinit var sut: OrderDetailRepository

    @Before
    fun setUp() {
        sut = OrderDetailRepository(
            orderStore = orderStore,
            productStore = mock(),
            refundStore = mock(),
            shippingLabelStore = mock(),
            selectedSite = selectedSite,
            wooCommerceStore = mock(),
            dispatchers = coroutinesTestRule.testDispatchers,
            orderMapper = orderMapper,
            shippingLabelMapper = mock(),
            refreshProductsSignal = refreshProductsSignal,
        )
    }

    @Test
    fun `given the status update is confirmed remotely, when updateOrderStatus, then products refresh is signalled`() =
        testBlocking {
            // GIVEN
            val orderEntity: OrderEntity = mock()
            val order: Order = mock {
                on { getProductIds() } doReturn listOf(101L, 102L)
            }
            whenever(orderStore.getOrderByIdAndSite(ORDER_ID, site)).thenReturn(orderEntity)
            whenever(orderMapper.toAppModel(orderEntity)).thenReturn(order)
            whenever(orderStore.updateOrderStatus(any(), any(), any()))
                .thenReturn(flowOf(RemoteUpdateResult(OnOrderChanged())))

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
                .thenReturn(flowOf(OptimisticUpdateResult(OnOrderChanged())))

            // WHEN
            sut.updateOrderStatus(ORDER_ID, "completed").toList()

            // THEN
            verify(refreshProductsSignal, never()).notifyProductsChanged(any())
        }

    private companion object {
        const val ORDER_ID = 1L
    }
}
