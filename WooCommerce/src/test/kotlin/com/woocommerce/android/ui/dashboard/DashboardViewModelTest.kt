package com.woocommerce.android.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.dashboard.stats.GetSelectedDateRange
import com.woocommerce.android.ui.mystore.data.CustomDateRangeDataStore
import com.woocommerce.android.ui.prefs.privacy.banner.domain.ShouldShowPrivacyBanner
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class DashboardViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock()
    private val selectedSite: SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val myStoreTransactionLauncher: DashboardTransactionLauncher = mock()
    private val customDateRangeDataStore: CustomDateRangeDataStore = mock()
    private val shouldShowPrivacyBanner: ShouldShowPrivacyBanner = mock {
        onBlocking { invoke() } doReturn true
    }
    private val dateUtils: DateUtils = mock()

    private lateinit var viewModel: DashboardViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit) {
        prepareMocks()

        val getSelectedDateRange = GetSelectedDateRange(
            customDateRangeDataStore = customDateRangeDataStore,
            appPrefs = appPrefsWrapper,
            dateUtils = dateUtils
        )

        viewModel = DashboardViewModel(
            savedState = SavedStateHandle(),
            getSelectedDateRange = getSelectedDateRange,
            appPrefsWrapper = appPrefsWrapper,
            dashboardTransactionLauncher = myStoreTransactionLauncher,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            usageTracksEventEmitter = usageTracksEventEmitter,
            resourceProvider = resourceProvider,
            selectedSite = selectedSite,
            shouldShowPrivacyBanner = shouldShowPrivacyBanner,
        )
    }

    @Test
    fun `given a Jetpack site, when screen starts, then hide the Jetpack Benefits banner`() = testBlocking {
        setup {
            whenever(selectedSite.observe()).thenReturn(
                flowOf(
                    SiteModel().apply {
                        origin = SiteModel.ORIGIN_WPCOM_REST
                        setIsJetpackConnected(true)
                    }
                )
            )
        }

        val jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()

        assertThat(jetpackBenefitsBanner).isNull()
    }

    @Test
    fun `given a Jetpack CP site, when screen starts, then show the Jetpack Benefits banner`() = testBlocking {
        setup {
            whenever(selectedSite.observe()).thenReturn(
                flowOf(
                    SiteModel().apply {
                        origin = SiteModel.ORIGIN_WPCOM_REST
                        setIsJetpackCPConnected(true)
                        setIsJetpackConnected(false)
                    }
                )
            )
        }

        val jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()

        assertThat(jetpackBenefitsBanner).isNotNull()
        assertThat(jetpackBenefitsBanner!!.show).isTrue()
    }

    @Test
    fun `given an Application Passwords site, when screen starts, then show the Jetpack Benefits banner`() =
        testBlocking {
            setup {
                whenever(selectedSite.observe()).thenReturn(
                    flowOf(
                        SiteModel().apply {
                            origin = SiteModel.ORIGIN_WPAPI
                        }
                    )
                )
            }

            val jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()

            assertThat(jetpackBenefitsBanner).isNotNull()
            assertThat(jetpackBenefitsBanner!!.show).isTrue()
        }

    @Test
    fun `given a Jetpack CP site, when jetpack benefits dismissed, then update prefs`() = testBlocking {
        setup {
            whenever(selectedSite.observe()).thenReturn(
                flowOf(
                    SiteModel().apply {
                        origin = SiteModel.ORIGIN_WPCOM_REST
                        setIsJetpackCPConnected(true)
                        setIsJetpackConnected(false)
                    }
                )
            )
        }

        val jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()
        jetpackBenefitsBanner!!.onDismiss()

        verify(appPrefsWrapper).recordJetpackBenefitsDismissal()
    }

    @Test
    fun `given a Jetpack CP site, when jetpack benefits dismissed recently, then hide banner`() = testBlocking {
        setup {
            whenever(selectedSite.observe()).thenReturn(
                flowOf(
                    SiteModel().apply {
                        origin = SiteModel.ORIGIN_WPCOM_REST
                        setIsJetpackCPConnected(true)
                        setIsJetpackConnected(false)
                    }
                )
            )
            whenever(appPrefsWrapper.getJetpackBenefitsDismissalDate()).thenReturn(System.currentTimeMillis() - 1000)
        }

        val jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()

        assertThat(jetpackBenefitsBanner!!.show).isFalse()
    }
}
