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
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore

@OptIn(ExperimentalCoroutinesApi::class)
class StoreProductsCheckUseCaseTest : BaseUnitTest() {
    private lateinit var sut: StoreProductsCheckUseCase
    private lateinit var productStore: WCProductStore
    private lateinit var selectedSite: SelectedSite
    private val siteModel = SiteModel().apply {
        origin = SiteModel.ORIGIN_WPCOM_REST
        setIsJetpackConnected(true)
    }

    @Before
    fun setUp() {
        productStore = mock()
        selectedSite = mock {
            on { get() }.thenReturn(siteModel)
        }
        sut = StoreProductsCheckUseCase(productStore, selectedSite)
    }

    @Test
    fun `when fetchProducts returns success, then emit Success`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(productStore.fetchProducts(siteModel))
            .thenReturn(WooResult(emptyList()))

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
    fun `when fetchProducts returns GENERIC_ERROR, then emit GENERIC Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(productStore.fetchProducts(siteModel))
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN)))

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
    fun `when fetchProducts returns TIMEOUT, then emit TIMEOUT Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(productStore.fetchProducts(siteModel))
            .thenReturn(WooResult(WooError(WooErrorType.TIMEOUT, UNKNOWN)))

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
    fun `when fetchProducts returns INVALID_RESPONSE, then emit PARSE Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(productStore.fetchProducts(siteModel))
            .thenReturn(WooResult(WooError(WooErrorType.INVALID_RESPONSE, UNKNOWN)))

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
    fun `when fetchProducts returns API_NOT_FOUND, then emit GENERIC Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(productStore.fetchProducts(siteModel))
            .thenReturn(WooResult(WooError(WooErrorType.API_NOT_FOUND, UNKNOWN)))

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
    fun `when fetchProducts returns unknown_token error, then emit JETPACK Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(productStore.fetchProducts(siteModel))
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN, apiErrorCode = "unknown_token")))

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
    fun `when fetchProducts returns invalid_blog error, then emit JETPACK Failure`() = testBlocking {
        // GIVEN
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(productStore.fetchProducts(siteModel))
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN, apiErrorCode = "invalid_blog")))

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
    fun `given app passwords site, when fetchProducts returns unknown_token error, then emit GENERIC Failure`() = testBlocking {
        // GIVEN
        val appPasswordSite = SiteModel()
        whenever(selectedSite.get()).thenReturn(appPasswordSite)
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(productStore.fetchProducts(appPasswordSite))
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN, apiErrorCode = "unknown_token")))

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
