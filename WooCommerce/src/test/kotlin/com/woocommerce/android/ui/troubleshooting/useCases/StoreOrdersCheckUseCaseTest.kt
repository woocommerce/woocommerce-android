package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.troubleshooting.FailureType
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
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
    private val siteModel = SiteModel().apply {
        origin = SiteModel.ORIGIN_WPCOM_REST
        setIsJetpackConnected(true)
    }

    @Before
    fun setUp() {
        orderStore = mock()
        selectedSite = mock {
            on { get() }.thenReturn(siteModel)
        }
        sut = StoreOrdersCheckUseCase(orderStore, selectedSite)
    }

    @Test
    fun `when fetchHasOrders returns success then emit Success`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(
            orderStore.fetchHasOrders(
                site = siteModel,
                status = null
            )
        ).thenReturn(HasOrdersResult.Success(hasOrders = true))

        // WHEN
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // THEN
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Success::class.java)
    }

    @Test
    fun `when fetchHasOrders returns PLUGIN_NOT_ACTIVE failure, then emit GENERIC Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(
            orderStore.fetchHasOrders(
                site = siteModel,
                status = null
            )
        ).thenReturn(HasOrdersResult.Failure(OrderError(PLUGIN_NOT_ACTIVE)))

        // WHEN
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // THEN
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).error).isEqualTo(FailureType.GENERIC)
        assertThat((stateEvents[1] as Failure).technicalDetails).isNotNull()
    }

    @Test
    fun `when fetchHasOrders returns unknown_token error, then emit JETPACK Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val wpapiNetworkError = WPAPINetworkError(
            baseError = org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError(GenericErrorType.UNKNOWN),
            errorCode = "unknown_token"
        )
        whenever(
            orderStore.fetchHasOrders(
                site = siteModel,
                status = null
            )
        ).thenReturn(HasOrdersResult.Failure(OrderError(GENERIC_ERROR, networkError = wpapiNetworkError)))

        // WHEN
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // THEN
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).error).isEqualTo(FailureType.JETPACK)
        assertThat((stateEvents[1] as Failure).technicalDetails).isNotNull()
    }

    @Test
    fun `when fetchHasOrders returns invalid_blog error, then emit JETPACK Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val wpapiNetworkError = WPAPINetworkError(
            baseError = org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError(GenericErrorType.UNKNOWN),
            errorCode = "invalid_blog"
        )
        whenever(
            orderStore.fetchHasOrders(
                site = siteModel,
                status = null
            )
        ).thenReturn(HasOrdersResult.Failure(OrderError(GENERIC_ERROR, networkError = wpapiNetworkError)))

        // WHEN
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // THEN
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).error).isEqualTo(FailureType.JETPACK)
        assertThat((stateEvents[1] as Failure).technicalDetails).isNotNull()
    }

    @Test
    fun `when fetchHasOrders returns GENERIC_ERROR failure then emit GENERIC Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(
            orderStore.fetchHasOrders(
                site = siteModel,
                status = null
            )
        ).thenReturn(HasOrdersResult.Failure(OrderError(GENERIC_ERROR)))

        // WHEN
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // THEN
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).error).isEqualTo(FailureType.GENERIC)
        assertThat((stateEvents[1] as Failure).technicalDetails).isNotNull()
    }

    @Test
    fun `when fetchHasOrders returns TIMEOUT_ERROR failure then emit GENERIC Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(
            orderStore.fetchHasOrders(
                site = siteModel,
                status = null
            )
        ).thenReturn(HasOrdersResult.Failure(OrderError(TIMEOUT_ERROR)))

        // WHEN
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // THEN
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).error).isEqualTo(FailureType.TIMEOUT)
        assertThat((stateEvents[1] as Failure).technicalDetails).isNotNull()
    }

    @Test
    fun `when fetchHasOrders returns PARSE_ERROR failure then emit PARSE Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(
            orderStore.fetchHasOrders(
                site = siteModel,
                status = null
            )
        ).thenReturn(HasOrdersResult.Failure(OrderError(PARSE_ERROR)))

        // WHEN
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // THEN
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).error).isEqualTo(FailureType.PARSE)
        assertThat((stateEvents[1] as Failure).technicalDetails).isNotNull()
    }

    @Test
    fun `given app passwords site, when fetchHasOrders returns unknown_token error, then emit GENERIC Failure`() = testBlocking {
        // GIVEN
        val appPasswordSite = SiteModel()
        whenever(selectedSite.get()).thenReturn(appPasswordSite)
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val wpapiNetworkError = WPAPINetworkError(
            baseError = org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError(GenericErrorType.UNKNOWN),
            errorCode = "unknown_token"
        )
        whenever(
            orderStore.fetchHasOrders(
                site = appPasswordSite,
                status = null
            )
        ).thenReturn(HasOrdersResult.Failure(OrderError(GENERIC_ERROR, networkError = wpapiNetworkError)))

        // WHEN
        sut.invoke().onEach {
            stateEvents.add(it)
        }.launchIn(this)

        // THEN
        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).error).isEqualTo(FailureType.GENERIC)
    }
}
