package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.OrderTestUtils.ORDER_IDENTIFIER
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.store.WCOrderStore

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class OrderShipmentProvidersRepositoryTest : BaseUnitTest() {

    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val siteModel = SiteModel()
    private val order = OrderTestUtils.generateOrder()
    private val providers = OrderTestUtils.generateOrderShipmentProviders()

    private lateinit var repository: OrderShipmentProvidersRepository

    @Before
    fun setup() {
        repository = OrderShipmentProvidersRepository(selectedSite, orderStore)
        doReturn(siteModel).whenever(selectedSite).get()
        doReturn(order).whenever(orderStore).getOrderByIdentifier(ORDER_IDENTIFIER)
    }

    @Test
    fun `When there are shipment providers in local db`() = runBlockingTest {
        // When there are shipment providers in local db
        doReturn(providers).whenever(orderStore).getShipmentProvidersForSite(siteModel)

        val result = repository.fetchOrderShipmentProviders(ORDER_IDENTIFIER)

        // Then should return the local db shipment providers
        assertThat(result).isNotEmpty
        verify(orderStore, times(1)).getShipmentProvidersForSite(siteModel)
        // And won't fetch shipment providers from the network
        verify(orderStore, times(0)).fetchOrderShipmentProviders(any())
        assertThat(providers.map { it.toAppModel() }).isEqualTo(result)
    }

    @Test
    fun `When there are NO shipment providers in local db`() = runBlockingTest {
        // When there are NO shipment providers in local db
        doReturn(
            emptyList<WCOrderShipmentProviderModel>(),
            providers
        ).whenever(orderStore).getShipmentProvidersForSite(siteModel)

        // Then should fetch shipment providers from the network
        val onChanged = WCOrderStore.OnOrderShipmentProvidersChanged(providers.size)
        doReturn(onChanged).whenever(orderStore).fetchOrderShipmentProviders(any())

        val result = repository.fetchOrderShipmentProviders(ORDER_IDENTIFIER)

        // And return the updated shipment providers from the local db
        verify(orderStore, times(1)).fetchOrderShipmentProviders(any())
        verify(orderStore, times(2)).getShipmentProvidersForSite(siteModel)
        assertThat(providers.map { it.toAppModel() }).isEqualTo(result)
    }

    @Test
    fun `When there are NO rows affected`() = runBlockingTest {
        // When there are NO rows affected
        doReturn(emptyList<WCOrderShipmentProviderModel>()).whenever(orderStore).getShipmentProvidersForSite(siteModel)

        val onChanged = WCOrderStore.OnOrderShipmentProvidersChanged(0)
        doReturn(onChanged).whenever(orderStore).fetchOrderShipmentProviders(any())

        val result = repository.fetchOrderShipmentProviders(ORDER_IDENTIFIER)

        // Then return an empty list
        verify(orderStore, times(1)).fetchOrderShipmentProviders(any())
        assertThat(result).isEmpty()
    }

    @Test
    fun `When something goes wrong`() = runBlockingTest {
        // When something goes wrong
        doReturn(emptyList<WCOrderShipmentProviderModel>()).whenever(orderStore).getShipmentProvidersForSite(siteModel)

        val onChanged = WCOrderStore.OnOrderShipmentProvidersChanged(0).also {
            it.error = WCOrderStore.OrderError(message = "Something goes wrong")
        }
        doReturn(onChanged).whenever(orderStore).fetchOrderShipmentProviders(any())

        val result = repository.fetchOrderShipmentProviders(ORDER_IDENTIFIER)

        // Then return null
        verify(orderStore, times(1)).fetchOrderShipmentProviders(any())
        assertThat(result).isNull()
    }
}
