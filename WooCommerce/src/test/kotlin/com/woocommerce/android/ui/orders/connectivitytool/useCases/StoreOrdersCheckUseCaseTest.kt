package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.orders.connectivitytool.FailureType
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
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.PLUGIN_NOT_ACTIVE
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.TIMEOUT_ERROR

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
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(
            orderStore.fetchHasOrders(
                site = selectedSite.get(),
                status = null
            )
        ).thenReturn(HasOrdersResult.Success(hasOrders = true))

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Success))
    }

    @Test
    fun `when fetchHasOrders returns PLUGIN_NOT_ACTIVE failure then emit JETPACK Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(
            orderStore.fetchHasOrders(
                site = selectedSite.get(),
                status = null
            )
        ).thenReturn(HasOrdersResult.Failure(OrderError(PLUGIN_NOT_ACTIVE)))

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Failure(FailureType.JETPACK)))
    }

    @Test
    fun `when fetchHasOrders returns GENERIC_ERROR failure then emit GENERIC Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(
            orderStore.fetchHasOrders(
                site = selectedSite.get(),
                status = null
            )
        ).thenReturn(HasOrdersResult.Failure(OrderError(GENERIC_ERROR)))

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Failure(FailureType.GENERIC)))
    }

    @Test
    fun `when fetchHasOrders returns TIMEOUT_ERROR failure then emit GENERIC Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(
            orderStore.fetchHasOrders(
                site = selectedSite.get(),
                status = null
            )
        ).thenReturn(HasOrdersResult.Failure(OrderError(TIMEOUT_ERROR)))

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Failure(FailureType.TIMEOUT)))
    }

    @Test
    fun `when fetchHasOrders returns PARSE_ERROR failure then emit PARSE Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(
            orderStore.fetchHasOrders(
                site = selectedSite.get(),
                status = null
            )
        ).thenReturn(HasOrdersResult.Failure(OrderError(PARSE_ERROR)))

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Failure(FailureType.PARSE)))
    }
}
