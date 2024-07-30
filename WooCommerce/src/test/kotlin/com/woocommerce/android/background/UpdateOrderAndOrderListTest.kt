package com.woocommerce.android.background

import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCOrderStore
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateOrderAndOrderListTest : BaseUnitTest() {
    private val updateOrdersListByStoreId: UpdateOrdersListByStoreId = mock()
    private val orderStore: WCOrderStore = mock()
    private val siteStore: SiteStore = mock()

    private val sut = UpdateOrderAndOrderList(
        updateOrdersListByStoreId = updateOrdersListByStoreId,
        orderStore = orderStore,
        siteStore = siteStore
    )

    @Test
    fun `when selected site is null then return false`() = testBlocking {
        val defaultSiteId = 2L
        val defaultOrderId = 5L
        whenever(siteStore.getSiteBySiteId(defaultSiteId)).thenReturn(null)
        whenever(updateOrdersListByStoreId.invoke(any(), any())).thenReturn(true)

        val result = sut(defaultSiteId, defaultOrderId)

        // Assertions
        Assert.assertFalse(result)
    }

    @Test
    fun `when fetchSingleOrder fails then return false`() = testBlocking {
        val defaultSiteId = 2L
        val defaultOrderId = 5L
        val defaultSite = SiteModel().apply { id = 3 }
        val fetchOrderError = WooResult<OrderEntity>(
            WooError(WooErrorType.INVALID_RESPONSE, BaseRequest.GenericErrorType.INVALID_RESPONSE)
        )

        whenever(siteStore.getSiteBySiteId(defaultSiteId)).thenReturn(defaultSite)
        whenever(orderStore.fetchSingleOrderSync(eq(defaultSite), eq(defaultOrderId))).thenReturn(fetchOrderError)
        whenever(updateOrdersListByStoreId.invoke(any(), any())).thenReturn(true)

        val result = sut(defaultSiteId, defaultOrderId)

        // Assertions
        Assert.assertFalse(result)
    }

    @Test
    fun `when updateOrdersList fails then return false`() = testBlocking {
        val defaultSiteId = 2L
        val defaultOrderId = 5L
        val defaultSite = SiteModel().apply { id = 3 }
        val fetchOrderResult = WooResult(OrderTestUtils.generateOrder())

        whenever(siteStore.getSiteBySiteId(defaultSiteId)).thenReturn(defaultSite)
        whenever(orderStore.fetchSingleOrderSync(eq(defaultSite), eq(defaultOrderId))).thenReturn(fetchOrderResult)
        whenever(updateOrdersListByStoreId.invoke(any(), any())).thenReturn(false)

        val result = sut(defaultSiteId, defaultOrderId)

        // Assertions
        Assert.assertFalse(result)
    }

    @Test
    fun `when everything succeed then return true`() = testBlocking {
        val defaultSiteId = 2L
        val defaultOrderId = 5L
        val defaultSite = SiteModel().apply { id = 3 }
        val fetchOrderResult = WooResult(OrderTestUtils.generateOrder())

        whenever(siteStore.getSiteBySiteId(defaultSiteId)).thenReturn(defaultSite)
        whenever(orderStore.fetchSingleOrderSync(eq(defaultSite), eq(defaultOrderId))).thenReturn(fetchOrderResult)
        whenever(updateOrdersListByStoreId.invoke(any(), any())).thenReturn(true)

        val result = sut(defaultSiteId, defaultOrderId)

        // Assertions
        Assert.assertTrue(result)
    }
}
