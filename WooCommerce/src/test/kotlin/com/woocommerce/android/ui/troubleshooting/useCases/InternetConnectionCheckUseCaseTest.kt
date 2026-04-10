package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(networkStatus.isConnected()).thenReturn(true)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Success::class.java)
    }

    @Test
    fun `when network is not connected then emit Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(networkStatus.isConnected()).thenReturn(false)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
    }
}
