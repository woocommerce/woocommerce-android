package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.dashboard.stats.GetStats
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.troubleshooting.FailureType
import com.woocommerce.android.util.LocalizedDatePatternsTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class StoreAnalyticsCheckUseCaseTest : BaseUnitTest() {
    @get:Rule
    val localizedDatePatterns = LocalizedDatePatternsTestRule()

    private lateinit var sut: StoreAnalyticsCheckUseCase
    private lateinit var getStats: GetStats
    private lateinit var selectedSite: SelectedSite
    private lateinit var wooCommerceStore: WooCommerceStore

    private val site = SiteModel()

    @Before
    fun setUp() {
        getStats = mock()
        selectedSite = mock {
            on { get() }.thenReturn(site)
        }
        wooCommerceStore = mock()
        sut = StoreAnalyticsCheckUseCase(getStats, selectedSite, wooCommerceStore)
    }

    @Test
    fun `given analytics setting is enabled, when revenue stats load, then emit success`() = testBlocking {
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(wooCommerceStore.fetchAnalyticsEnabled(site)).thenReturn(WooResult(true))
        whenever(getStats.invoke(any(), any(), anyOrNull())).thenReturn(
            flowOf(GetStats.LoadStatsResult.RevenueStatsSuccess(null))
        )

        sut().onEach { stateEvents.add(it) }.launchIn(this)

        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Success::class.java)
    }

    @Test
    fun `given cached analytics stats load first, when revenue stats refresh, then emit refreshed result`() =
        testBlocking {
            val stateEvents = mutableListOf<ConnectivityCheckStatus>()
            whenever(wooCommerceStore.fetchAnalyticsEnabled(site)).thenReturn(WooResult(true))
            whenever(getStats.invoke(any(), any(), anyOrNull())).thenReturn(
                flowOf(
                    GetStats.LoadStatsResult.RevenueStatsSuccess(null, isOutdated = true),
                    GetStats.LoadStatsResult.RevenueStatsSuccess(null)
                )
            )

            sut().onEach { stateEvents.add(it) }.launchIn(this)

            assertThat(stateEvents).hasSize(2)
            assertThat(stateEvents[0]).isEqualTo(InProgress)
            assertThat(stateEvents[1]).isInstanceOf(Success::class.java)
        }

    @Test
    fun `given analytics setting is disabled, when check runs, then emit plugin inactive failure`() = testBlocking {
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(wooCommerceStore.fetchAnalyticsEnabled(site)).thenReturn(WooResult(false))

        sut().onEach { stateEvents.add(it) }.launchIn(this)

        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).error).isEqualTo(FailureType.GENERIC)
        assertThat((stateEvents[1] as Failure).technicalDetails)
            .contains(StoreAnalyticsCheckUseCase.PLUGIN_NOT_ACTIVE_ERROR_TYPE)
        verify(getStats, never()).invoke(any(), any(), anyOrNull())
    }

    @Test
    fun `given analytics setting request fails, when check runs, then emit generic failure`() = testBlocking {
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(wooCommerceStore.fetchAnalyticsEnabled(site)).thenReturn(
            WooResult(WooError(GENERIC_ERROR, UNKNOWN, "Server error"))
        )

        sut().onEach { stateEvents.add(it) }.launchIn(this)

        assertThat(stateEvents).hasSize(2)
        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).error).isEqualTo(FailureType.GENERIC)
        assertThat((stateEvents[1] as Failure).technicalDetails)
            .contains(GENERIC_ERROR.name)
            .contains("Server error")
        verify(getStats, never()).invoke(any(), any(), anyOrNull())
    }

    @Test
    fun `given analytics setting is enabled, when stats report plugin inactive, then emit plugin inactive failure`() =
        testBlocking {
            val stateEvents = mutableListOf<ConnectivityCheckStatus>()
            whenever(wooCommerceStore.fetchAnalyticsEnabled(site)).thenReturn(WooResult(true))
            whenever(getStats.invoke(any(), any(), anyOrNull())).thenReturn(
                flowOf(GetStats.LoadStatsResult.PluginNotActive)
            )

            sut().onEach { stateEvents.add(it) }.launchIn(this)

            assertThat(stateEvents).hasSize(2)
            assertThat(stateEvents[0]).isEqualTo(InProgress)
            assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
            assertThat((stateEvents[1] as Failure).technicalDetails)
                .contains(StoreAnalyticsCheckUseCase.PLUGIN_NOT_ACTIVE_ERROR_TYPE)
        }

    @Test
    fun `given analytics setting is enabled successfully, when enableAnalytics is called, then result is success`() =
        testBlocking {
            whenever(wooCommerceStore.enableAnalytics(site)).thenReturn(WooResult(true))

            val result = sut.enableAnalytics()

            assertThat(result.isSuccess).isTrue()
        }

    @Test
    fun `given analytics setting is not enabled, when enableAnalytics is called, then result is failure`() =
        testBlocking {
            whenever(wooCommerceStore.enableAnalytics(site)).thenReturn(WooResult(false))

            val result = sut.enableAnalytics()

            assertThat(result.isFailure).isTrue()
        }

    @Test
    fun `given analytics setting request fails, when enableAnalytics is called, then error is propagated`() =
        testBlocking {
            val error = WooError(GENERIC_ERROR, UNKNOWN, "Server error")
            whenever(wooCommerceStore.enableAnalytics(site)).thenReturn(WooResult(error))

            val result = sut.enableAnalytics()

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).hasMessage("Server error")
        }
}
