package com.woocommerce.android.ui.orders.connectivitytool.useCases

import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.Success
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InternetConnectionCheckUseCaseTest : BaseUnitTest() {
    private lateinit var sut: InternetConnectionCheckUseCase
    private lateinit var networkStatus: NetworkStatus

    @Before
    fun setUp() {
        networkStatus = mock()
        sut = InternetConnectionCheckUseCase(networkStatus)
    }

    @Test
    fun `when network is connected then emit Success`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityTestStatus>()
        whenever(networkStatus.isConnected()).thenReturn(true)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Success))
    }

    @Test
    fun `when network is not connected then emit Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityTestStatus>()
        whenever(networkStatus.isConnected()).thenReturn(false)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Failure))
    }
}
