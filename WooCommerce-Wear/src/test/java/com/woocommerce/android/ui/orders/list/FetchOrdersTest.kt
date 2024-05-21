package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.system.NetworkStatus
import com.woocommerce.android.ui.orders.OrdersRepository
import com.woocommerce.android.ui.orders.list.FetchOrders.OrdersRequest
import com.woocommerce.android.ui.orders.list.FetchOrders.OrdersRequest.Error
import com.woocommerce.android.ui.orders.list.FetchOrders.OrdersRequest.Finished
import com.woocommerce.android.ui.orders.list.FetchOrders.OrdersRequest.Waiting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCWearableStore.OrdersForWearablesResult.Success

@ExperimentalCoroutinesApi
class FetchOrdersTest : BaseUnitTest() {

    private val phoneRepository: PhoneConnectionRepository = mock()
    private val ordersRepository: OrdersRepository = mock()
    private val networkStatus: NetworkStatus = mock()
    private val selectedSite: SiteModel = mock()

    @Test
    fun `returns Finished when orders are available`() = testBlocking {
        val expectedOrders = listOf<OrderEntity>(mock())
        whenever(ordersRepository.fetchOrders(selectedSite)).thenReturn(Success(expectedOrders))
        whenever(networkStatus.isConnected()).thenReturn(true)
        val events = mutableListOf<OrdersRequest>()

        FetchOrders(phoneRepository, ordersRepository, networkStatus)
            .invoke(selectedSite)
            .onEach { events.add(it) }
            .launchIn(this)

        advanceUntilIdle()

        assertThat(events).containsExactly(Finished(expectedOrders))
    }

    @Test
    fun `returns Waiting when no orders and not timeout`() = testBlocking {
        whenever(ordersRepository.fetchOrders(selectedSite)).thenReturn(Success(emptyList()))
        whenever(networkStatus.isConnected()).thenReturn(true)
        val events = mutableListOf<OrdersRequest>()

        FetchOrders(phoneRepository, ordersRepository, networkStatus)
            .invoke(selectedSite)
            .onEach { events.add(it) }
            .launchIn(this)

        advanceUntilIdle()

        assertThat(events).containsExactly(Waiting, Error)
    }

    @Test
    fun `returns Error when no orders and timeout`() = testBlocking {
        whenever(ordersRepository.fetchOrders(selectedSite)).thenReturn(Success(emptyList()))
        whenever(networkStatus.isConnected()).thenReturn(true)
        val events = mutableListOf<OrdersRequest>()

        FetchOrders(phoneRepository, ordersRepository, networkStatus)
            .invoke(selectedSite)
            .onEach { events.add(it) }
            .launchIn(this)

        advanceUntilIdle()

        assertThat(events).containsExactly(Waiting, Error)
    }

    @Test
    fun `returns orders from phone when no network connection`() = testBlocking {
        val expectedOrders = listOf<OrderEntity>(mock())
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(true)
        whenever(networkStatus.isConnected()).thenReturn(false)
        whenever(ordersRepository.observeOrdersDataChanges(selectedSite.siteId))
            .thenReturn(flowOf(expectedOrders))
        val events = mutableListOf<OrdersRequest>()

        FetchOrders(phoneRepository, ordersRepository, networkStatus)
            .invoke(selectedSite)
            .onEach { events.add(it) }
            .launchIn(this)

        advanceUntilIdle()

        assertThat(events).containsExactly(Finished(expectedOrders))
    }

    @Test
    fun `returns stored orders when no network and phone connection`() = testBlocking {
        val expectedOrders = listOf<OrderEntity>(mock())
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(false)
        whenever(networkStatus.isConnected()).thenReturn(false)
        whenever(ordersRepository.getStoredOrders(selectedSite)).thenReturn(expectedOrders)
        val events = mutableListOf<OrdersRequest>()

        FetchOrders(phoneRepository, ordersRepository, networkStatus)
            .invoke(selectedSite)
            .onEach { events.add(it) }
            .launchIn(this)

        advanceUntilIdle()

        assertThat(events).containsExactly(Finished(expectedOrders))
    }
}
