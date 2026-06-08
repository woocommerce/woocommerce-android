package com.woocommerce.android.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status
import com.woocommerce.android.notifications.push.ShouldShowEnablePushNotificationsUi
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetUiModel.ConfigurableWidget
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetUiModel.NewWidgetsCard
import com.woocommerce.android.ui.dashboard.data.AnalyticsScheduledImportRepository
import com.woocommerce.android.ui.dashboard.data.DashboardRepository
import com.woocommerce.android.ui.prefs.privacy.banner.domain.ShouldShowPrivacyBanner
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
        val site = SiteModel().apply {
            url = "https://example.com"
        }
        on { get() } doReturn site
        on { getIfExists() } doReturn site
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val myStoreTransactionLauncher: DashboardTransactionLauncher = mock()
    private val shouldShowPrivacyBanner: ShouldShowPrivacyBanner = mock {
        on { invoke() } doReturn true
    }
    private val dashboardRepository: DashboardRepository = mock {
        on { widgets } doReturn flowOf(
            DashboardWidget.Type.entries.map {
                DashboardWidget(
                    it,
                    true,
                    DashboardWidget.Status.Available
                )
            }
        )
        on { hasNewWidgets } doReturn flowOf(false)
    }

    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus = mock {
        on { observe(any()) } doReturn flowOf(Status.REGISTERED_WPCOM_ONLY)
    }
    private val feedbackPrefs: FeedbackPrefs = mock {
        on { userFeedbackIsDueObservable } doReturn flowOf(false)
    }
    private val shouldShowEnablePushNotificationsUi: ShouldShowEnablePushNotificationsUi = mock {
        on { invoke() } doReturn flowOf(false)
    }
    private val analyticsScheduledImportRepository: AnalyticsScheduledImportRepository = mock {
        on { observeIsEnabled() } doReturn flowOf(false)
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
            analyticsScheduledImportRepository = analyticsScheduledImportRepository,
            pushNotificationRegistrationStatus = pushNotificationRegistrationStatus,
            shouldShowEnablePushNotificationsUi = shouldShowEnablePushNotificationsUi,
            feedbackPrefs = feedbackPrefs,
        )
    }

    @Test
    fun `given ai assistant is missing, when dashboard starts, then repository inserts ai assistant at top`() =
        testBlocking {
            // GIVEN
            val widgetsFlow = MutableStateFlow(DEFAULT_WIDGETS_WITHOUT_AI)
            setup {
                whenever(dashboardRepository.widgets).thenReturn(widgetsFlow)
            }

            // WHEN
            viewModel.dashboardCardsState.captureValues().last()

            // THEN
            verify(dashboardRepository).insertAIAssistantWidgetAtTopIfMissing()
        }

    @Test
    fun `given upgraded config is only missing ai assistant, when insertion emission lands, then new widgets card is hidden`() =
        testBlocking {
            // GIVEN
            val widgets = MutableStateFlow(DEFAULT_WIDGETS_WITHOUT_AI)
            val hasNewWidgets = MutableStateFlow(true)
            setup {
                whenever(dashboardRepository.widgets).thenReturn(widgets)
                whenever(dashboardRepository.hasNewWidgets).thenReturn(hasNewWidgets)
            }

            // WHEN
            val states = viewModel.dashboardCardsState.captureValues()
            widgets.value = listOf(dashboardWidget(DashboardWidget.Type.AI_ASSISTANT)) + DEFAULT_WIDGETS_WITHOUT_AI
            hasNewWidgets.value = false

            // THEN
            verify(dashboardRepository).insertAIAssistantWidgetAtTopIfMissing()
            val newWidgetsCard = states.last().widgets
                .filterIsInstance<NewWidgetsCard>()
                .single()
            assertThat(newWidgetsCard.isVisible).isFalse()
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
    fun `given a Jetpack CP site with PN setup available, when screen starts, then hide the Jetpack Benefits banner`() =
        testBlocking {
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
                whenever(pushNotificationRegistrationStatus.observe(any()))
                    .thenReturn(flowOf(Status.UNREGISTERED))
                whenever(shouldShowEnablePushNotificationsUi.invoke()).thenReturn(flowOf(true))
            }

            val jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()

            assertThat(jetpackBenefitsBanner).isNotNull()
            assertThat(jetpackBenefitsBanner!!.show).isFalse()
        }

    @Test
    fun `given a Jetpack CP site with PN setup not available, when screen starts, then show the Jetpack Benefits banner`() =
        testBlocking {
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
                whenever(pushNotificationRegistrationStatus.observe(any()))
                    .thenReturn(flowOf(Status.UNREGISTERED))
                whenever(shouldShowEnablePushNotificationsUi.invoke()).thenReturn(flowOf(false))
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
                whenever(pushNotificationRegistrationStatus.observe(any())).thenReturn(flowOf(Status.UNREGISTERED))
            }

            val jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()

            assertThat(jetpackBenefitsBanner).isNotNull()
            assertThat(jetpackBenefitsBanner!!.show).isTrue()
        }

    @Test
    fun `given an Application Passwords site, when jetpack benefits dismissed, then update prefs`() = testBlocking {
        setup {
            whenever(selectedSite.observe()).thenReturn(
                flowOf(
                    SiteModel().apply {
                        origin = SiteModel.ORIGIN_WPAPI
                    }
                )
            )
            whenever(pushNotificationRegistrationStatus.observe(any())).thenReturn(flowOf(Status.UNREGISTERED))
        }

        val jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()
        jetpackBenefitsBanner!!.onDismiss()

        verify(appPrefsWrapper).recordJetpackBenefitsDismissal()
    }

    @Test
    fun `given an Application Passwords site, when jetpack benefits dismissed recently, then hide banner`() =
        testBlocking {
            setup {
                whenever(selectedSite.observe()).thenReturn(
                    flowOf(
                        SiteModel().apply {
                            origin = SiteModel.ORIGIN_WPAPI
                        }
                    )
                )
                whenever(pushNotificationRegistrationStatus.observe(any())).thenReturn(flowOf(Status.UNREGISTERED))
                whenever(appPrefsWrapper.getJetpackBenefitsDismissalDate())
                    .thenReturn(System.currentTimeMillis() - 1000)
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

        val viewState = viewModel.dashboardCardsState.captureValues().last()

        val shareStoreCard =
            viewState.widgets.first { it is DashboardViewModel.DashboardWidgetUiModel.ShareStoreWidget }
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

        val viewState = viewModel.dashboardCardsState.captureValues().last()

        val shareStoreCard =
            viewState.widgets.first { it is DashboardViewModel.DashboardWidgetUiModel.ShareStoreWidget }
        assertThat(shareStoreCard.isVisible).isFalse()
    }

    @Test
    fun `when feedback is due, then show the feedback card`() = testBlocking {
        setup {
            whenever(feedbackPrefs.userFeedbackIsDueObservable).thenReturn(flowOf(true))
        }

        val viewState = viewModel.dashboardCardsState.captureValues().last()

        val feedbackCard = viewState.widgets.filter { it.isVisible }[1]
        assertThat(feedbackCard).isInstanceOf(DashboardViewModel.DashboardWidgetUiModel.FeedbackWidget::class.java)
        assertThat(feedbackCard.isVisible).isTrue()
    }

    @Test
    fun `when feedback is not due, then hide the feedback card`() = testBlocking {
        setup {
            whenever(feedbackPrefs.userFeedbackIsDueObservable).thenReturn(flowOf(false))
        }

        val viewState = viewModel.dashboardCardsState.captureValues().last()

        val feedbackCard = viewState.widgets.first { it is DashboardViewModel.DashboardWidgetUiModel.FeedbackWidget }
        assertThat(feedbackCard.isVisible).isFalse()
    }

    @Test
    fun `given ai assistant is available, when screen starts, then dashboard state shows assistant widget`() =
        testBlocking {
            // GIVEN
            val aiAssistant = dashboardWidget(DashboardWidget.Type.AI_ASSISTANT)
            setup {
                whenever(dashboardRepository.widgets).thenReturn(
                    flowOf(
                        listOf(
                            aiAssistant,
                            dashboardWidget(DashboardWidget.Type.STATS)
                        )
                    )
                )
            }

            // WHEN
            val viewState = viewModel.dashboardCardsState.captureValues().last()

            // THEN
            val assistantWidget = viewState.widgets
                .filterIsInstance<ConfigurableWidget>()
                .single { it.widget.type == DashboardWidget.Type.AI_ASSISTANT }
            assertThat(assistantWidget.isVisible).isTrue()
        }

    @Test
    fun `given ai assistant is hidden, when screen starts, then dashboard state hides assistant widget`() =
        testBlocking {
            // GIVEN
            val aiAssistant = dashboardWidget(
                type = DashboardWidget.Type.AI_ASSISTANT,
                status = DashboardWidget.Status.Hidden
            )
            setup {
                whenever(dashboardRepository.widgets).thenReturn(
                    flowOf(
                        listOf(
                            aiAssistant,
                            dashboardWidget(DashboardWidget.Type.STATS)
                        )
                    )
                )
            }

            // WHEN
            val viewState = viewModel.dashboardCardsState.captureValues().last()

            // THEN
            val assistantWidget = viewState.widgets
                .filterIsInstance<ConfigurableWidget>()
                .single { it.widget.type == DashboardWidget.Type.AI_ASSISTANT }
            assertThat(assistantWidget.isVisible).isFalse()
        }

    @Test
    fun `given ai assistant and feedback are visible, when screen starts, then assistant is first visible card`() =
        testBlocking {
            // GIVEN
            val orderedWidgets = listOf(
                DashboardWidget(
                    type = DashboardWidget.Type.AI_ASSISTANT,
                    isSelected = true,
                    status = DashboardWidget.Status.Available
                ),
                DashboardWidget(
                    type = DashboardWidget.Type.ORDERS,
                    isSelected = true,
                    status = DashboardWidget.Status.Available
                ),
                DashboardWidget(
                    type = DashboardWidget.Type.STATS,
                    isSelected = true,
                    status = DashboardWidget.Status.Available
                )
            )
            setup {
                whenever(feedbackPrefs.userFeedbackIsDueObservable).thenReturn(flowOf(true))
                whenever(dashboardRepository.widgets).thenReturn(flowOf(orderedWidgets))
            }

            // WHEN
            val visibleWidgets = viewModel.dashboardCardsState.captureValues().last()
                .widgets
                .filter { it.isVisible }

            // THEN
            assertThat(visibleWidgets[0]).isInstanceOf(ConfigurableWidget::class.java)
            assertThat((visibleWidgets[0] as ConfigurableWidget).widget.type)
                .isEqualTo(DashboardWidget.Type.AI_ASSISTANT)
            assertThat(visibleWidgets[1])
                .isInstanceOf(DashboardViewModel.DashboardWidgetUiModel.FeedbackWidget::class.java)
            assertThat(visibleWidgets.filterIsInstance<ConfigurableWidget>().map { it.widget.type })
                .containsExactly(
                    DashboardWidget.Type.AI_ASSISTANT,
                    DashboardWidget.Type.ORDERS,
                    DashboardWidget.Type.STATS
                )
        }

    @Test
    fun `given ai assistant card is visible, when card tapped, then open assistant event is emitted`() =
        testBlocking {
            // GIVEN
            setup {}

            // WHEN
            val event = viewModel.event.runAndCaptureValues {
                viewModel.onAiAssistantCardClicked()
            }.last()

            // THEN
            assertThat(event).isEqualTo(DashboardViewModel.DashboardEvent.OpenAiAssistant)
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.DYNAMIC_DASHBOARD_CARD_INTERACTED,
                mapOf(AnalyticsTracker.KEY_TYPE to "ai_assistant")
            )
        }

    @Test
    fun `given ai assistant is hidden, when dashboard state is built, then configurable widget order is unchanged`() =
        testBlocking {
            // GIVEN
            val orderedWidgets = listOf(
                DashboardWidget(
                    type = DashboardWidget.Type.ORDERS,
                    isSelected = true,
                    status = DashboardWidget.Status.Available
                ),
                DashboardWidget(
                    type = DashboardWidget.Type.STATS,
                    isSelected = true,
                    status = DashboardWidget.Status.Available
                ),
                DashboardWidget(
                    type = DashboardWidget.Type.COUPONS,
                    isSelected = true,
                    status = DashboardWidget.Status.Available
                )
            )
            setup {
                whenever(dashboardRepository.widgets).thenReturn(flowOf(orderedWidgets))
            }

            // WHEN
            val viewState = viewModel.dashboardCardsState.captureValues().last()

            // THEN
            val visibleConfigurableTypes = viewState.widgets
                .filter { it.isVisible }
                .filterIsInstance<ConfigurableWidget>()
                .map { it.widget.type }
            assertThat(visibleConfigurableTypes).containsExactly(
                DashboardWidget.Type.ORDERS,
                DashboardWidget.Type.STATS,
                DashboardWidget.Type.COUPONS
            )
        }

    @Test
    fun `given stored ai assistant is below stats, when dashboard state is built, then visible order follows storage`() =
        testBlocking {
            // GIVEN
            val orderedWidgets = listOf(
                dashboardWidget(DashboardWidget.Type.STATS, isSelected = true),
                dashboardWidget(DashboardWidget.Type.AI_ASSISTANT, isSelected = true),
                dashboardWidget(DashboardWidget.Type.ORDERS, isSelected = true)
            )
            setup {
                whenever(dashboardRepository.widgets).thenReturn(flowOf(orderedWidgets))
            }

            // WHEN
            val visibleConfigurableTypes = viewModel.dashboardCardsState.captureValues().last()
                .widgets
                .filter { it.isVisible }
                .filterIsInstance<ConfigurableWidget>()
                .map { it.widget.type }

            // THEN
            assertThat(visibleConfigurableTypes).containsExactly(
                DashboardWidget.Type.STATS,
                DashboardWidget.Type.AI_ASSISTANT,
                DashboardWidget.Type.ORDERS
            )
        }

    @Test
    fun `given stored ai assistant is unselected, when dashboard state is built, then ai assistant is not visible`() =
        testBlocking {
            // GIVEN
            setup {
                whenever(dashboardRepository.widgets).thenReturn(
                    flowOf(
                        listOf(
                            dashboardWidget(DashboardWidget.Type.AI_ASSISTANT, isSelected = false),
                            dashboardWidget(DashboardWidget.Type.STATS, isSelected = true)
                        )
                    )
                )
            }

            // WHEN
            val visibleTypes = viewModel.dashboardCardsState.captureValues().last()
                .widgets
                .filter { it.isVisible }
                .filterIsInstance<ConfigurableWidget>()
                .map { it.widget.type }

            // THEN
            assertThat(visibleTypes).doesNotContain(DashboardWidget.Type.AI_ASSISTANT)
        }

    @Test
    fun `given feedback card is shown, when positive button is tapped, then handle click`() = testBlocking {
        setup {
            whenever(feedbackPrefs.userFeedbackIsDueObservable).thenReturn(flowOf(true))
        }

        val event = viewModel.event.runAndCaptureValues {
            val viewState = viewModel.dashboardCardsState.captureValues().last()
            val feedbackCard =
                viewState.widgets.filterIsInstance<DashboardViewModel.DashboardWidgetUiModel.FeedbackWidget>()
                    .first()
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
            val viewState = viewModel.dashboardCardsState.captureValues().last()
            val feedbackCard =
                viewState.widgets.filterIsInstance<DashboardViewModel.DashboardWidgetUiModel.FeedbackWidget>()
                    .first()
            feedbackCard.onNegativeClick.invoke()
        }.last()

        verify(feedbackPrefs).lastFeedbackDate = any()
        assertThat(event).isEqualTo(DashboardViewModel.DashboardEvent.FeedbackNegativeAction)
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.APP_FEEDBACK_PROMPT,
            mapOf(AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_NOT_LIKED)
        )
    }

    @Test
    fun `given there are new widgets, when screen starts, then show the new widgets card`() = testBlocking {
        setup {
            whenever(dashboardRepository.hasNewWidgets).thenReturn(flowOf(true))
        }

        val viewState = viewModel.dashboardCardsState.getOrAwaitValue()

        val newWidgetsCard = viewState.widgets.first { it is DashboardViewModel.DashboardWidgetUiModel.NewWidgetsCard }
        assertThat(newWidgetsCard.isVisible).isTrue()
    }

    @Test
    fun `given there are no new widgets, when screen starts, then hide the new widgets card`() = testBlocking {
        setup {
            whenever(dashboardRepository.hasNewWidgets).thenReturn(flowOf(false))
        }

        val viewState = viewModel.dashboardCardsState.getOrAwaitValue()

        val newWidgetsCard = viewState.widgets.first { it is DashboardViewModel.DashboardWidgetUiModel.NewWidgetsCard }
        assertThat(newWidgetsCard.isVisible).isFalse()
    }

    @Test
    fun `given site is WPCom suspended, when visitor stats placeholder, then hide Jetpack benefits banner`() =
        testBlocking {
            setup {
                whenever(selectedSite.observe())
                    .thenReturn(flowOf(SiteModel().apply { origin = SiteModel.ORIGIN_WPAPI }))
                whenever(appPrefsWrapper.isSiteWPComSuspended).thenReturn(true)
            }

            val jetpackBenefitsBannerState = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()

            assertThat(jetpackBenefitsBannerState).isNull()
        }

    @Test
    fun `given push notification token registered, when screen starts, then hide Jetpack benefits banner`() =
        testBlocking {
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
                whenever(pushNotificationRegistrationStatus.observe(any()))
                    .thenReturn(flowOf(Status.REGISTERED_WPCOM_ONLY))
            }

            val jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()

            assertThat(jetpackBenefitsBanner).isNotNull()
            assertThat(jetpackBenefitsBanner!!.show).isFalse()
        }

    @Test
    fun `given enable push notifications UI is available, when screen starts, then hide Jetpack benefits banner`() =
        testBlocking {
            setup {
                whenever(selectedSite.observe()).thenReturn(
                    flowOf(
                        SiteModel().apply {
                            origin = SiteModel.ORIGIN_WPAPI
                        }
                    )
                )
                whenever(shouldShowEnablePushNotificationsUi.invoke()).thenReturn(flowOf(true))
            }

            val jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()

            assertThat(jetpackBenefitsBanner).isNotNull()
            assertThat(jetpackBenefitsBanner!!.show).isFalse()
        }

    @Test
    fun `given enable push notifications UI is not available, when screen starts, then show Jetpack benefits banner`() =
        testBlocking {
            setup {
                whenever(selectedSite.observe()).thenReturn(
                    flowOf(
                        SiteModel().apply {
                            origin = SiteModel.ORIGIN_WPAPI
                        }
                    )
                )
                whenever(pushNotificationRegistrationStatus.observe(any()))
                    .thenReturn(flowOf(Status.UNREGISTERED))
                whenever(shouldShowEnablePushNotificationsUi.invoke()).thenReturn(flowOf(false))
            }

            val jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()

            assertThat(jetpackBenefitsBanner).isNotNull()
            assertThat(jetpackBenefitsBanner!!.show).isTrue()
        }

    @Test
    fun `given registration status changes, when observing flow, then updates occur`() = testBlocking {
        // This test verifies the reactive nature of the banner visibility
        val statusFlow = MutableStateFlow(Status.UNREGISTERED)
        setup {
            whenever(selectedSite.observe()).thenReturn(
                flowOf(
                    SiteModel().apply {
                        origin = SiteModel.ORIGIN_WPAPI
                    }
                )
            )
            whenever(pushNotificationRegistrationStatus.observe(any())).thenReturn(statusFlow)
            whenever(shouldShowEnablePushNotificationsUi.invoke()).thenReturn(flowOf(false))
        }

        // Initially Unregistered -> Banner Shown
        var jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()
        assertThat(jetpackBenefitsBanner).isNotNull()
        assertThat(jetpackBenefitsBanner!!.show).isTrue()

        // Change to Registered -> Banner Hidden
        statusFlow.value = Status.REGISTERED_WOO_ONLY
        jetpackBenefitsBanner = viewModel.jetpackBenefitsBannerState.getOrAwaitValue()
        assertThat(jetpackBenefitsBanner!!.show).isFalse()
    }

    @Test
    fun `given scheduled import is enabled, when dashboard starts, then isScheduledImportEnabled is true`() =
        testBlocking {
            // GIVEN
            setup {
                whenever(analyticsScheduledImportRepository.observeIsEnabled()).thenReturn(flowOf(true))
            }

            // THEN
            assertThat(viewModel.isScheduledImportEnabled.getOrAwaitValue()).isTrue()
        }

    @Test
    fun `given no cached scheduled import setting, when dashboard starts, then isScheduledImportEnabled is false`() =
        testBlocking {
            // GIVEN
            setup {
                whenever(analyticsScheduledImportRepository.observeIsEnabled()).thenReturn(flowOf(false))
            }

            // THEN
            assertThat(viewModel.isScheduledImportEnabled.getOrAwaitValue()).isFalse()
        }

    @Test
    fun `when dashboard starts, then scheduled import setting is refreshed from remote`() =
        testBlocking {
            // GIVEN
            setup {}

            // THEN
            verify(analyticsScheduledImportRepository).refresh()
        }

    @Test
    fun `given import enabled, when delayed stats info clicked, then info event is emitted with current state`() =
        testBlocking {
            // GIVEN
            setup {
                whenever(analyticsScheduledImportRepository.observeIsEnabled()).thenReturn(flowOf(true))
            }

            // WHEN
            val event = viewModel.event.runAndCaptureValues {
                viewModel.onDelayedStatsInfoClicked()
            }.last()

            // THEN
            assertThat(event)
                .isEqualTo(DashboardViewModel.DashboardEvent.OpenScheduledImportInfo(isEnabled = true))
        }

    @Test
    fun `when delayed stats info is clicked, then the info sheet is marked as seen`() = testBlocking {
        // GIVEN
        setup {}

        // WHEN
        viewModel.onDelayedStatsInfoClicked()

        // THEN
        verify(appPrefsWrapper).hasSeenAnalyticsScheduledImportInfo = true
    }

    @Test
    fun `given import enabled and info not seen, when pull to refresh, then scheduled import notice is shown`() =
        testBlocking {
            // GIVEN
            setup {
                whenever(analyticsScheduledImportRepository.observeIsEnabled()).thenReturn(flowOf(true))
                whenever(appPrefsWrapper.hasSeenAnalyticsScheduledImportInfo).thenReturn(false)
            }

            // WHEN
            val event = viewModel.event.runAndCaptureValues {
                viewModel.onPullToRefresh()
            }.last()

            // THEN
            assertThat(event).isEqualTo(DashboardViewModel.DashboardEvent.ShowScheduledImportNotice)
        }

    @Test
    fun `given the info sheet has been seen, when pull to refresh, then scheduled import notice is not shown`() =
        testBlocking {
            // GIVEN
            setup {
                whenever(analyticsScheduledImportRepository.observeIsEnabled()).thenReturn(flowOf(true))
                whenever(appPrefsWrapper.hasSeenAnalyticsScheduledImportInfo).thenReturn(true)
            }

            // WHEN
            val events = viewModel.event.runAndCaptureValues {
                viewModel.onPullToRefresh()
            }

            // THEN
            assertThat(events).doesNotContain(DashboardViewModel.DashboardEvent.ShowScheduledImportNotice)
        }

    @Test
    fun `given import disabled, when pull to refresh, then scheduled import notice is not shown`() =
        testBlocking {
            // GIVEN
            setup {
                whenever(analyticsScheduledImportRepository.observeIsEnabled()).thenReturn(flowOf(false))
            }

            // WHEN
            val events = viewModel.event.runAndCaptureValues {
                viewModel.onPullToRefresh()
            }

            // THEN
            assertThat(events).doesNotContain(DashboardViewModel.DashboardEvent.ShowScheduledImportNotice)
        }

    @Test
    fun `given no delayed-stats card is visible, when pull to refresh, then scheduled import notice is not shown`() =
        testBlocking {
            // GIVEN
            setup {
                whenever(analyticsScheduledImportRepository.observeIsEnabled()).thenReturn(flowOf(true))
                whenever(dashboardRepository.widgets).thenReturn(
                    flowOf(listOf(dashboardWidget(DashboardWidget.Type.ORDERS)))
                )
            }

            // WHEN
            val events = viewModel.event.runAndCaptureValues {
                viewModel.onPullToRefresh()
            }

            // THEN
            assertThat(events).doesNotContain(DashboardViewModel.DashboardEvent.ShowScheduledImportNotice)
        }

    private companion object {
        fun dashboardWidget(
            type: DashboardWidget.Type,
            isSelected: Boolean = true,
            status: DashboardWidget.Status = DashboardWidget.Status.Available
        ) = DashboardWidget(type = type, isSelected = isSelected, status = status)

        val DEFAULT_WIDGETS_WITHOUT_AI = listOf(
            dashboardWidget(DashboardWidget.Type.STATS),
            dashboardWidget(DashboardWidget.Type.POPULAR_PRODUCTS),
            dashboardWidget(DashboardWidget.Type.ONBOARDING),
            dashboardWidget(DashboardWidget.Type.BLAZE),
            dashboardWidget(DashboardWidget.Type.GOOGLE_ADS),
            dashboardWidget(DashboardWidget.Type.PUSH_NOTIFICATIONS)
        )
    }
}
