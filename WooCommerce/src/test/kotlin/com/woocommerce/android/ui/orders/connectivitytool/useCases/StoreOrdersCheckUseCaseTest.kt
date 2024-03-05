package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.Success
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError

@OptIn(ExperimentalCoroutinesApi::class)
class StoreOrdersCheckUseCaseTest : BaseUnitTest() {
    private lateinit var sut: StoreOrdersCheckUseCase
    private lateinit var orderStore: WCOrderStore
    private lateinit var selectedSite: SelectedSite

    @Before
    fun setUp() {
        orderStore = mock()
        selectedSite = mock()
        sut = StoreOrdersCheckUseCase(orderStore, selectedSite)
    }

    @Test
    fun `when fetchHasOrders returns success then emit Success`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<OrderConnectivityToolViewModel.ConnectivityTestStatus>()
        whenever(orderStore.fetchHasOrders(
            site = selectedSite.get(),
            status = null
        )).thenReturn(HasOrdersResult.Success(hasOrders = true))

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Success))
    }

    @Test
    fun `when fetchHasOrders returns failure then emit Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<OrderConnectivityToolViewModel.ConnectivityTestStatus>()
        whenever(orderStore.fetchHasOrders(
            site = selectedSite.get(),
            status = null
        )).thenReturn(HasOrdersResult.Failure(OrderError()))

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, OrderConnectivityToolViewModel.ConnectivityTestStatus.Failure))
    }
}
