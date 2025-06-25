package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.OrderNotFoundException
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.OrderShipmentProvidersFetchException
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.OrderShipmentProvidersFetchResult.CarriersFetched
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.OrderShipmentProvidersFetchResult.NoCarriersFound
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError

@ExperimentalCoroutinesApi
class OrderShipmentProvidersRepositoryTest : BaseUnitTest() {
    private companion object Companion {
        const val ORDER_ID = 1L

        val testSite = SiteModel()
        val testOrder = OrderTestUtils.generateOrder()
        val testProviders = OrderTestUtils.generateOrderShipmentProviders()
    }

    private lateinit var sut: OrderShipmentProvidersRepository
    private lateinit var orderStore: WCOrderStore
    private lateinit var selectedSite: SelectedSite

    @Before
    fun setup() {
        // Create mocks with minimal configuration
        orderStore = mock()
        selectedSite = mock { on { get() } doReturn testSite }

        // Initialize repository with mocks
        sut = OrderShipmentProvidersRepository(selectedSite, orderStore)
    }

    @Test
    fun `fetching shipment providers returns OrderNotFoundException when order is not found`() = runTest {
        // Given
        doReturn(null)
            .whenever(orderStore)
            .getOrderByIdAndSite(ORDER_ID, testSite)

        // When
        val result = sut.fetchOrderShipmentProviders(ORDER_ID)

        // Then
        assertThat(result.exceptionOrNull())
            .isInstanceOf(OrderNotFoundException::class.java)
    }

    @Test
    fun `fetching shipment providers returns CarriersFetched when providers found`() = runTest {
        // Given
        doReturn(testOrder)
            .whenever(orderStore)
            .getOrderByIdAndSite(ORDER_ID, testSite)
        doReturn(OnOrderShipmentProvidersChanged(testProviders.size))
            .whenever(orderStore)
            .fetchOrderShipmentProviders(any())

        // When
        val result = sut.fetchOrderShipmentProviders(ORDER_ID)

        // Then
        assertThat(result.getOrNull()).isEqualTo(CarriersFetched)
    }

    @Test
    fun `fetching shipment providers returns NoCarriersFound when no providers found`() = runTest {
        // Given
        doReturn(testOrder)
            .whenever(orderStore)
            .getOrderByIdAndSite(ORDER_ID, testSite)
        doReturn(OnOrderShipmentProvidersChanged(0))
            .whenever(orderStore)
            .fetchOrderShipmentProviders(any())

        // When
        val result = sut.fetchOrderShipmentProviders(ORDER_ID)

        // Then
        assertThat(result.getOrNull()).isEqualTo(NoCarriersFound)
    }

    @Test
    fun `fetchOrderShipmentProviders returns exception when error occurs`() = testBlocking {
        // Given
        doReturn(testOrder)
            .whenever(orderStore)
            .getOrderByIdAndSite(ORDER_ID, testSite)
        val errorEvent = OnOrderShipmentProvidersChanged(0).apply {
            error = OrderError(message = "Network error")
        }
        doReturn(errorEvent)
            .whenever(orderStore)
            .fetchOrderShipmentProviders(any())

        // When
        val result = sut.fetchOrderShipmentProviders(ORDER_ID)

        // Then
        assertThat(result.exceptionOrNull())
            .isInstanceOf(OrderShipmentProvidersFetchException::class.java)
            .hasMessage("Error fetching order shipment providers: Network error")
    }
}
