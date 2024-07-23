package com.woocommerce.android.background

import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.filters.domain.GetWCOrderListDescriptorWithFilters
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateOrdersListTest : BaseUnitTest() {
    private val defaultListDescriptor = WCOrderListDescriptor(SiteModel().apply { id = 1 })
    private val defaultOrderResponse = List(5) { i ->
        OrderTestUtils.generateOrder().copy(orderId = i.toLong())
    }

    private val getWCOrderListDescriptorWithFilters: GetWCOrderListDescriptorWithFilters = mock()
    private val listStore: ListStore = mock()
    private val ordersStore: WCOrderStore = mock()

    val sut = UpdateOrdersList(
        getWCOrderListDescriptorWithFilters = getWCOrderListDescriptorWithFilters,
        listStore = listStore,
        ordersStore = ordersStore
    )

    @Test
    fun `when fetch orders succeed then persist data and return true`() = testBlocking {
        // Mock dependencies
        whenever(getWCOrderListDescriptorWithFilters.invoke()).thenReturn(defaultListDescriptor)
        whenever(ordersStore.fetchOrdersListFirstPage(defaultListDescriptor))
            .thenReturn(WooResult(defaultOrderResponse))

        val result = sut()

        // Assertions
        val expectedIds = defaultOrderResponse.map { it.orderId }
        verify(listStore).saveListFetched(defaultListDescriptor, expectedIds, false)
        assertTrue(result)
    }

    @Test
    fun `when fetch orders fails then DON'T persist data and return false`() = testBlocking {
        // Mock dependencies
        whenever(getWCOrderListDescriptorWithFilters.invoke()).thenReturn(defaultListDescriptor)
        whenever(ordersStore.fetchOrdersListFirstPage(defaultListDescriptor))
            .thenReturn(
                WooResult(
                    WooError(WooErrorType.INVALID_RESPONSE, BaseRequest.GenericErrorType.INVALID_RESPONSE)
                )
            )

        val result = sut()

        // Assertions
        val expectedIds = defaultOrderResponse.map { it.orderId }
        verify(listStore, never()).saveListFetched(defaultListDescriptor, expectedIds, false)
        assertFalse(result)
    }

    @Test
    fun `when fetch orders succeed but result is null then DON'T persist data and return false`() = testBlocking {
        // Mock dependencies
        whenever(getWCOrderListDescriptorWithFilters.invoke()).thenReturn(defaultListDescriptor)
        whenever(ordersStore.fetchOrdersListFirstPage(defaultListDescriptor))
            .thenReturn(WooResult(null))

        val result = sut()

        // Assertions
        val expectedIds = defaultOrderResponse.map { it.orderId }
        verify(listStore, never()).saveListFetched(defaultListDescriptor, expectedIds, false)
        assertFalse(result)
    }

    @Test
    fun `when fetch orders succeed but result is empty then DON'T persist data and return true`() = testBlocking {
        // Mock dependencies
        whenever(getWCOrderListDescriptorWithFilters.invoke()).thenReturn(defaultListDescriptor)
        whenever(ordersStore.fetchOrdersListFirstPage(defaultListDescriptor))
            .thenReturn(WooResult(emptyList()))

        val result = sut()

        // Assertions
        val expectedIds = defaultOrderResponse.map { it.orderId }
        verify(listStore, never()).saveListFetched(defaultListDescriptor, expectedIds, false)
        assertTrue(result)
    }
}
