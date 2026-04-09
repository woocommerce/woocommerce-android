package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.troubleshooting.FailureType
import com.woocommerce.android.util.WCSSRModelCachingFetcher
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
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@OptIn(ExperimentalCoroutinesApi::class)
class StoreConnectionCheckUseCaseTest : BaseUnitTest() {
    private lateinit var sut: StoreConnectionCheckUseCase
    private lateinit var selectedSite: SelectedSite
    private lateinit var ssrFetcher: WCSSRModelCachingFetcher
    private val siteModel = SiteModel().apply {
        origin = SiteModel.ORIGIN_WPCOM_REST
        setIsJetpackConnected(true)
    }

    @Before
    fun setUp() {
        selectedSite = mock {
            on { get() }.thenReturn(siteModel)
        }
        ssrFetcher = mock()
        sut = StoreConnectionCheckUseCase(selectedSite, ssrFetcher)
    }

    @Test
    fun `when fetchSSR returns an GENERIC_ERROR error then emit GENERIC Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult<WCSSRModel>(
            WooError(
                type = WooErrorType.GENERIC_ERROR,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR
            )
        )
        whenever(ssrFetcher.load(siteModel, true)).thenReturn(response)

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
    fun `when fetchSSR returns API_NOT_FOUND error, then emit GENERIC Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult<WCSSRModel>(
            WooError(
                type = WooErrorType.API_NOT_FOUND,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR
            )
        )
        whenever(ssrFetcher.load(siteModel, true)).thenReturn(response)

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
    fun `when fetchSSR returns unknown_token error, then emit JETPACK Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult<WCSSRModel>(
            WooError(
                type = WooErrorType.GENERIC_ERROR,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR,
                apiErrorCode = "unknown_token"
            )
        )
        whenever(ssrFetcher.load(siteModel, true)).thenReturn(response)

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
    fun `when fetchSSR returns invalid_blog error, then emit JETPACK Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult<WCSSRModel>(
            WooError(
                type = WooErrorType.GENERIC_ERROR,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR,
                apiErrorCode = "invalid_blog"
            )
        )
        whenever(ssrFetcher.load(siteModel, true)).thenReturn(response)

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
    fun `when fetchSSR returns an INVALID_RESPONSE error then emit PARSE Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult<WCSSRModel>(
            WooError(
                type = WooErrorType.INVALID_RESPONSE,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR
            )
        )
        whenever(ssrFetcher.load(siteModel, true)).thenReturn(response)

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
    fun `when fetchSSR returns an TIMEOUT error then emit TIMEOUT Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult<WCSSRModel>(
            WooError(
                type = WooErrorType.TIMEOUT,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR
            )
        )
        whenever(ssrFetcher.load(siteModel, true)).thenReturn(response)

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
    fun `when fetchSSR returns no error then emit Success`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult(WCSSRModel(remoteSiteId = 123L))
        whenever(ssrFetcher.load(siteModel, true)).thenReturn(response)

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
    fun `given app passwords site, when fetchSSR returns unknown_token error, then emit GENERIC Failure`() = testBlocking {
        // GIVEN
        val appPasswordSite = SiteModel()
        whenever(selectedSite.get()).thenReturn(appPasswordSite)
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        val response = WooResult<WCSSRModel>(
            WooError(
                type = WooErrorType.GENERIC_ERROR,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR,
                apiErrorCode = "unknown_token"
            )
        )
        whenever(ssrFetcher.load(appPasswordSite, true)).thenReturn(response)

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
