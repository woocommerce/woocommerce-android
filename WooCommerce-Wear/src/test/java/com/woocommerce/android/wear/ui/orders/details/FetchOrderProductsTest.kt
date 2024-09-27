package com.woocommerce.android.wear.ui.orders.details

import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.wear.model.Refund
import com.woocommerce.android.wear.phone.PhoneConnectionRepository
import com.woocommerce.android.wear.system.ConnectionStatus
import com.woocommerce.android.wear.ui.orders.OrdersRepository
import com.woocommerce.android.wear.ui.orders.details.FetchOrderProducts.OrderProductsRequest
import com.woocommerce.android.wear.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Finished
import com.woocommerce.android.wear.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Waiting
import com.woocommerce.commons.WearOrderedProduct
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class FetchOrderProductsTest : BaseUnitTest() {

    private val phoneRepository: PhoneConnectionRepository = mock()
    private val ordersRepository: OrdersRepository = mock()
    private val selectedSite: SiteModel = mock()
    private val connectionStatus: ConnectionStatus = mock()

    private lateinit var sut: FetchOrderProducts

    @Before
    fun setup() {
        whenever(connectionStatus.isStoreConnected()).thenReturn(false)
        sut = FetchOrderProducts(phoneRepository, ordersRepository, connectionStatus)
    }

    @Test
    fun `returns Cache when no connection is not available`() = testBlocking {
        val cachedProducts = emptyList<Refund>()

        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(false)
        whenever(ordersRepository.getOrderFromId(selectedSite, 1L)).thenReturn(null)
        whenever(ordersRepository.getOrderRefunds(selectedSite, 1L)).thenReturn(cachedProducts)

        val result = sut.invoke(selectedSite, 1L).last()

        assertThat(result).isEqualTo(Finished(emptyList()))
    }

    @Test
    fun `returns Finished when order products are available`() = testBlocking {
        val expectedProducts = listOf<WearOrderedProduct>(mock())
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(true)
        whenever(ordersRepository.observeOrderProductsDataChanges(1L, selectedSite.siteId))
            .thenReturn(flowOf(expectedProducts))

        val result = sut.invoke(selectedSite, 1L).first()

        assertThat(result).isEqualTo(Finished(expectedProducts))
    }

    @Test
    fun `returns Waiting when no order products and not timeout`() = testBlocking {
        whenever(connectionStatus.isStoreConnected()).thenReturn(true)
        whenever(
            ordersRepository.fetchOrderRefunds(selectedSite, 1L)
        ).thenReturn(null)

        val result = sut.invoke(selectedSite, 1L).first()

        assertThat(result).isEqualTo(Waiting)
    }

    @Test
    fun `returns Error when no order products and timeout`() = testBlocking {
        whenever(connectionStatus.isStoreConnected()).thenReturn(true)
        whenever(
            ordersRepository.fetchOrderRefunds(selectedSite, 1L)
        ).thenReturn(null)

        val result = sut.invoke(selectedSite, 1L)
            .filter { it !is Waiting }
            .first()

        advanceUntilIdle()
        assertThat(result).isEqualTo(OrderProductsRequest.Error)
    }
}
