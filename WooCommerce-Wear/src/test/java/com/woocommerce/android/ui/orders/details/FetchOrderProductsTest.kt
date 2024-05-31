package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.ui.orders.OrdersRepository
import com.woocommerce.android.ui.orders.details.FetchOrderProducts.OrderProductsRequest
import com.woocommerce.android.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Finished
import com.woocommerce.android.ui.orders.details.FetchOrderProducts.OrderProductsRequest.Waiting
import com.woocommerce.commons.WearOrderedProduct
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class FetchOrderProductsTest : BaseUnitTest() {

    private val phoneRepository: PhoneConnectionRepository = mock()
    private val ordersRepository: OrdersRepository = mock()
    private val selectedSite: SiteModel = mock()

    @Test
    fun `returns Error when phone connection is not available`() = testBlocking {
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(false)

        val result = FetchOrderProducts(phoneRepository, ordersRepository)
            .invoke(selectedSite, 1L).first()

        assertThat(result).isEqualTo(OrderProductsRequest.Error)
    }

    @Test
    fun `returns Finished when order products are available`() = testBlocking {
        val expectedProducts = listOf<WearOrderedProduct>(mock())
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(true)
        whenever(ordersRepository.observeOrderProductsDataChanges(1L, selectedSite.siteId))
            .thenReturn(flowOf(expectedProducts))

        val result = FetchOrderProducts(phoneRepository, ordersRepository)
            .invoke(selectedSite, 1L).first()

        assertThat(result).isEqualTo(Finished(expectedProducts))
    }

    @Test
    fun `returns Waiting when no order products and not timeout`() = testBlocking {
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(true)
        whenever(
            ordersRepository.observeOrderProductsDataChanges(1L, selectedSite.siteId)
        ).thenReturn(flowOf(emptyList()))

        val result = FetchOrderProducts(phoneRepository, ordersRepository)
            .invoke(selectedSite, 1L).first()

        assertThat(result).isEqualTo(Waiting)
    }

    @Test
    fun `returns Error when no order products and timeout`() = testBlocking {
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(true)
        whenever(
            ordersRepository.observeOrderProductsDataChanges(1L, selectedSite.siteId)
        ).thenReturn(flowOf(emptyList()))

        val result = FetchOrderProducts(phoneRepository, ordersRepository)
            .invoke(selectedSite, 1L)
            .filter { it !is Waiting }
            .first()

        advanceUntilIdle()
        assertThat(result).isEqualTo(OrderProductsRequest.Error)
    }
}
