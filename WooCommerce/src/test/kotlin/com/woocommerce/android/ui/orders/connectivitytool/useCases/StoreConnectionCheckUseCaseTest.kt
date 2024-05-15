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
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class StoreConnectionCheckUseCaseTest : BaseUnitTest() {
    private lateinit var sut: StoreConnectionCheckUseCase
    private lateinit var wooCommerceStore: WooCommerceStore
    private lateinit var selectedSite: SelectedSite

    @Before
    fun setUp() {
        wooCommerceStore = mock()
        selectedSite = mock()
        sut = StoreConnectionCheckUseCase(wooCommerceStore, selectedSite)
    }

    @Test
    fun `when fetchSSR returns an GENERIC_ERROR error then emit GENERIC Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult<WCSSRModel>(
            WooError(
                type = WooErrorType.GENERIC_ERROR,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR
            )
        )
        whenever(wooCommerceStore.fetchSSR(selectedSite.get())).thenReturn(response)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Failure(FailureType.GENERIC)))
    }

    @Test
    fun `when fetchSSR returns an PLUGIN_NOT_ACTIVE error then emit JETPACK Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult<WCSSRModel>(
            WooError(
                type = WooErrorType.API_NOT_FOUND,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR
            )
        )
        whenever(wooCommerceStore.fetchSSR(selectedSite.get())).thenReturn(response)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Failure(FailureType.JETPACK)))
    }

    @Test
    fun `when fetchSSR returns an INVALID_RESPONSE error then emit PARSE Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult<WCSSRModel>(
            WooError(
                type = WooErrorType.INVALID_RESPONSE,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR
            )
        )
        whenever(wooCommerceStore.fetchSSR(selectedSite.get())).thenReturn(response)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Failure(FailureType.PARSE)))
    }

    @Test
    fun `when fetchSSR returns an TIMEOUT error then emit TIMEOUT Failure`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult<WCSSRModel>(
            WooError(
                type = WooErrorType.TIMEOUT,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR
            )
        )
        whenever(wooCommerceStore.fetchSSR(selectedSite.get())).thenReturn(response)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Failure(FailureType.TIMEOUT)))
    }

    @Test
    fun `when fetchSSR returns no error then emit Success`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult(WCSSRModel(remoteSiteId = 123L))
        whenever(wooCommerceStore.fetchSSR(selectedSite.get())).thenReturn(response)

        // When
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // Then
        assertThat(stateEvents).isEqualTo(listOf(InProgress, Success))
    }
}
