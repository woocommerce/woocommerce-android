package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore

@ExperimentalCoroutinesApi
class OrderShipmentProvidersRepositoryTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
    }

    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val siteModel = SiteModel()
    private val order = OrderTestUtils.generateOrder()
    private val providers = OrderTestUtils.generateOrderShipmentProviders()

    private lateinit var repository: OrderShipmentProvidersRepository

    @Before
    fun setup() = testBlocking {
        repository = OrderShipmentProvidersRepository(selectedSite, orderStore)
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(orderStore.getOrderByIdAndSite(ORDER_ID, siteModel)).thenReturn(order)
    }

    @Test
    fun `Given data in local db, when fetch shipment providers invoked, then cached data returned`() = testBlocking {
        // When there are shipment providers in local db
        doReturn(providers).whenever(orderStore).getShipmentProvidersForSite(siteModel)

        val result = repository.fetchOrderShipmentProviders(ORDER_ID)

        // Then should return the local db shipment providers
        assertThat(result).isNotEmpty
        verify(orderStore, times(1)).getShipmentProvidersForSite(siteModel)
        // And won't fetch shipment providers from the network
        verify(orderStore, times(0)).fetchOrderShipmentProviders(any())
        assertThat(providers.map { it.toAppModel() }).isEqualTo(result)
    }

    @Test
    fun `If NO data in local db, data is fetched from the network and loaded from the db`() = testBlocking {
        // When there are NO shipment providers in local db
        whenever(orderStore.getShipmentProvidersForSite(siteModel)).thenReturn(emptyList(), providers)

        // Then should fetch shipment providers from the network
        val onChanged = WCOrderStore.OnOrderShipmentProvidersChanged(providers.size)
        whenever(orderStore.fetchOrderShipmentProviders(any())).thenReturn(onChanged)

        val result = repository.fetchOrderShipmentProviders(ORDER_ID)

        // And return the updated shipment providers from the local db
        verify(orderStore, times(1)).fetchOrderShipmentProviders(any())
        verify(orderStore, times(2)).getShipmentProvidersForSite(siteModel)
        assertThat(providers.map { it.toAppModel() }).isEqualTo(result)
    }

    @Test
    fun `If there are NO rows affected, an empty list is returned`() = testBlocking {
        // When there are NO rows affected
        whenever(orderStore.getShipmentProvidersForSite(siteModel)).thenReturn(emptyList())

        val onChanged = WCOrderStore.OnOrderShipmentProvidersChanged(0)
        whenever(orderStore.fetchOrderShipmentProviders(any())).thenReturn(onChanged)

        val result = repository.fetchOrderShipmentProviders(ORDER_ID)

        // Then return an empty list
        verify(orderStore, times(1)).fetchOrderShipmentProviders(any())
        assertThat(result).isEmpty()
    }

    @Test
    fun `If an error occurs, a null result is returned`() = testBlocking {
        // When something goes wrong
        whenever(orderStore.getShipmentProvidersForSite(siteModel)).thenReturn(emptyList())

        val onChanged = WCOrderStore.OnOrderShipmentProvidersChanged(0).also {
            it.error = WCOrderStore.OrderError(message = "Something goes wrong")
        }
        whenever(orderStore.fetchOrderShipmentProviders(any())).thenReturn(onChanged)

        val result = repository.fetchOrderShipmentProviders(ORDER_ID)

        // Then return null
        verify(orderStore, times(1)).fetchOrderShipmentProviders(any())
        assertThat(result).isNull()
    }
}
