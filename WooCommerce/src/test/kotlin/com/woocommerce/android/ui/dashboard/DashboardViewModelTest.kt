package com.woocommerce.android.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.dashboard.data.DashboardRepository
import com.woocommerce.android.ui.prefs.privacy.banner.domain.ShouldShowPrivacyBanner
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class DashboardViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel().apply {
            url = "https://example.com"
        }
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val myStoreTransactionLauncher: DashboardTransactionLauncher = mock()
    private val shouldShowPrivacyBanner: ShouldShowPrivacyBanner = mock {
        onBlocking { invoke() } doReturn true
    }
    private val dashboardRepository: DashboardRepository = mock {
        onBlocking { widgets } doReturn flowOf(
            DashboardWidget.Type.entries.map {
                DashboardWidget(
                    it,
                    true,
                    DashboardWidget.Status.Available
                )
            }
        )
    }
    private val feedbackPrefs: FeedbackPrefs = mock {
        onBlocking { userFeedbackIsDueObservable } doReturn flowOf(false)
    }

    private lateinit var viewModel: DashboardViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit) {
        prepareMocks()

        viewModel = DashboardViewModel(
            savedState = SavedStateHandle(),
            appPrefsWrapper = appPrefsWrapper,
            dashboardTransactionLauncher = myStoreTransactionLauncher,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            usageTracksEventEmitter = usageTracksEventEmitter,
            resourceProvider = resourceProvider,
            selectedSite = selectedSite,
            shouldShowPrivacyBanner = shouldShowPrivacyBanner,
            dashboardRepository = dashboardRepository,
            feedbackPrefs = feedbackPrefs,
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

    @Test
    fun `when the stats card is unavailable, then show the share store card`() = testBlocking {
        setup {
            whenever(dashboardRepository.widgets).thenReturn(
                flowOf(
                    listOf(
                        DashboardWidget(
                            type = DashboardWidget.Type.STATS,
                            isSelected = true,
                            status = DashboardWidget.Status.Unavailable(0)
                        )
                    )
                )
            )
        }

        val widgets = viewModel.dashboardWidgets.captureValues().last()

        val shareStoreCard = widgets.first { it is DashboardViewModel.DashboardWidgetUiModel.ShareStoreWidget }
        assertThat(shareStoreCard.isVisible).isTrue()
    }

    @Test
    fun `when the stats card is available, then hide the share store card`() = testBlocking {
        setup {
            whenever(dashboardRepository.widgets).thenReturn(
                flowOf(
                    listOf(
                        DashboardWidget(
                            type = DashboardWidget.Type.STATS,
                            isSelected = true,
                            status = DashboardWidget.Status.Available
                        )
                    )
                )
            )
        }

        val widgets = viewModel.dashboardWidgets.captureValues().last()

        val shareStoreCard = widgets.first { it is DashboardViewModel.DashboardWidgetUiModel.ShareStoreWidget }
        assertThat(shareStoreCard.isVisible).isFalse()
    }

    @Test
    fun `when feedback is due, then show the feedback card`() = testBlocking {
        setup {
            whenever(feedbackPrefs.userFeedbackIsDueObservable).thenReturn(flowOf(true))
        }

        val widgets = viewModel.dashboardWidgets.captureValues().last()

        val feedbackCard = widgets.filter { it.isVisible }[1]
        assertThat(feedbackCard).isInstanceOf(DashboardViewModel.DashboardWidgetUiModel.FeedbackWidget::class.java)
        assertThat(feedbackCard.isVisible).isTrue()
    }

    @Test
    fun `when feedback is not due, then hide the feedback card`() = testBlocking {
        setup {
            whenever(feedbackPrefs.userFeedbackIsDueObservable).thenReturn(flowOf(false))
        }

        val widgets = viewModel.dashboardWidgets.captureValues().last()

        val feedbackCard = widgets.first { it is DashboardViewModel.DashboardWidgetUiModel.FeedbackWidget }
        assertThat(feedbackCard.isVisible).isFalse()
    }

    @Test
    fun `given feedback card is shown, when positive button is tapped, then handle click`() = testBlocking {
        setup {
            whenever(feedbackPrefs.userFeedbackIsDueObservable).thenReturn(flowOf(true))
        }

        val event = viewModel.event.runAndCaptureValues {
            val widgets = viewModel.dashboardWidgets.captureValues().last()
            val feedbackCard = widgets.filterIsInstance(
                DashboardViewModel.DashboardWidgetUiModel.FeedbackWidget::class.java
            ).first()
            feedbackCard.onPositiveClick.invoke()
        }.last()

        verify(feedbackPrefs).lastFeedbackDate = any()
        assertThat(event).isEqualTo(DashboardViewModel.DashboardEvent.FeedbackPositiveAction)
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.APP_FEEDBACK_PROMPT,
            mapOf(AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_LIKED)
        )
    }

    @Test
    fun `given feedback card is shown, when negative button is tapped, then handle click`() = testBlocking {
        setup {
            whenever(feedbackPrefs.userFeedbackIsDueObservable).thenReturn(flowOf(true))
        }

        val event = viewModel.event.runAndCaptureValues {
            val widgets = viewModel.dashboardWidgets.captureValues().last()
            val feedbackCard = widgets.filterIsInstance(
                DashboardViewModel.DashboardWidgetUiModel.FeedbackWidget::class.java
            ).first()
            feedbackCard.onNegativeClick.invoke()
        }.last()

        verify(feedbackPrefs).lastFeedbackDate = any()
        assertThat(event).isEqualTo(DashboardViewModel.DashboardEvent.FeedbackNegativeAction)
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.APP_FEEDBACK_PROMPT,
            mapOf(AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_NOT_LIKED)
        )
    }
}
