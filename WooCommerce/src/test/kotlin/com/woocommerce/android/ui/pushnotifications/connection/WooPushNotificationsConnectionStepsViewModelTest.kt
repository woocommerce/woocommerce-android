package com.woocommerce.android.ui.pushnotifications.connection

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.notifications.push.CheckWooPluginPushNotificationsSupport
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.StepState
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.StepType
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndGetValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.JetpackStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPushNotificationsConnectionStepsViewModelTest : BaseUnitTest() {
    private val site = SiteModel().apply {
        name = "coffeebeans.com"
        url = "https://coffeebeans.com"
    }

    private lateinit var viewModel: WooPushNotificationsConnectionStepsViewModel

    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val pushNotificationRepository: PushNotificationRepository = mock()
    private val jetpackActivationRepository: JetpackActivationRepository = mock()
    private val checkWCPluginSupport: CheckWooPluginPushNotificationsSupport = mock {
        on { invoke(forceRefresh = true) } doReturn CheckWooPluginPushNotificationsSupport.Result.Compatible
    }
    private val stringUtils: StringUtils = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private suspend fun setup(
        isStoreAlreadyConnected: Boolean = false,
        shouldAutoOpenUpdatePlugin: Boolean = false,
        prepareMocks: suspend () -> Unit = {}
    ) {
        whenever(jetpackActivationRepository.registerSite(any(), any()))
            .thenReturn(Result.success(1L))
        whenever(stringUtils.getSiteDomainAndPath(site))
            .thenReturn(site.name)
        prepareMocks()
        viewModel = WooPushNotificationsConnectionStepsViewModel(
            selectedSite = selectedSite,
            appPrefsWrapper = appPrefsWrapper,
            pushNotificationRepository = pushNotificationRepository,
            jetpackActivationRepository = jetpackActivationRepository,
            checkWCPluginSupport = checkWCPluginSupport,
            stringUtils = stringUtils,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            savedStateHandle = WooPushNotificationsConnectionStepsFragmentArgs(
                isSiteConnectedToJetpack = isStoreAlreadyConnected,
                shouldAutoOpenUpdatePlugin = shouldAutoOpenUpdatePlugin
            ).toSavedStateHandle()
        )
    }

    @Test
    fun `when initialized, then steps are shown in expected order`() {
        testBlocking {
            setup()

            val state = viewModel.viewState.getOrAwaitValue()

            assertThat(state.steps).hasSize(3)
            assertThat(state.steps[0].type).isEqualTo(StepType.CheckPluginCompatibility)
            assertThat(state.steps[1].type).isEqualTo(StepType.ConnectStore)
            assertThat(state.steps[2].type).isEqualTo(StepType.EnablePushNotifications)
        }
    }

    @Test
    fun `when initialized, then isDone is false`() {
        testBlocking {
            setup()

            val state = viewModel.viewState.getOrAwaitValue()

            assertThat(state.isDone).isFalse()
        }
    }

    @Test
    fun `when initialized, then site address is set`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
            whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any(), any()))
                .thenReturn(Result.success(Unit))
        }

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.siteAddress).isEqualTo("coffeebeans.com")
    }

    @Test
    fun `given site not connected, when initialized, then connect strings are used`() = testBlocking {
        setup()

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.titleRes)
            .isEqualTo(R.string.woo_push_notifications_connection_steps_title_connect)
        assertThat(state.bodyRes)
            .isEqualTo(R.string.woo_push_notifications_connection_steps_body_connect)
    }

    @Test
    fun `given site already connected, when initialized, then setup strings are used`() = testBlocking {
        setup(isStoreAlreadyConnected = true)

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.titleRes)
            .isEqualTo(R.string.woo_push_notifications_connection_steps_title_setup)
        assertThat(state.bodyRes)
            .isEqualTo(R.string.woo_push_notifications_connection_steps_body_setup)
    }

    @Test
    fun `when close is clicked, then Exit event is triggered`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
            whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any(), any()))
                .thenReturn(Result.success(Unit))
        }

        viewModel.onCloseClick()

        val event = viewModel.event.value
        assertThat(event).isEqualTo(Exit)
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_CLOSE)
    }

    @Test
    fun `when go to store is clicked, then tracks tap and close events`() = testBlocking {
        setup()

        viewModel.onGoToStoreClick()

        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_BUTTON_TAP),
            eq(mapOf(AnalyticsTracker.KEY_BUTTON_LABEL to "go_to_my_store"))
        )
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_CLOSE)
    }

    @Test
    fun `when retry is clicked, then try again is tracked`() = testBlocking {
        setup()

        viewModel.onRetryClick()

        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_BUTTON_TAP),
            eq(mapOf(AnalyticsTracker.KEY_BUTTON_LABEL to "try_again"))
        )
    }

    @Test
    fun `when contact support is clicked, then NavigateToHelpScreen event is triggered`() = testBlocking {
        setup()

        viewModel.onContactSupportClick()

        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_BUTTON_TAP),
            eq(mapOf(AnalyticsTracker.KEY_BUTTON_LABEL to "support"))
        )
        val event = viewModel.event.value
        assertThat(event).isInstanceOf(NavigateToHelpScreen::class.java)
        assertThat((event as NavigateToHelpScreen).origin)
            .isEqualTo(HelpOrigin.WOO_PUSH_NOTIFICATIONS_SETUP)
    }

    @Test
    fun `given plugin check returns UpdateRequired, when CheckPluginCompatibility runs, then step is Error`() =
        testBlocking {
            setup {
                whenever(checkWCPluginSupport(forceRefresh = true))
                    .thenReturn(CheckWooPluginPushNotificationsSupport.Result.UpdateRequired("9.0.0"))
            }

            val state = viewModel.viewState.runAndGetValue {
                advanceUntilIdle()
            }

            assertThat(state.steps[0].type).isEqualTo(StepType.CheckPluginCompatibility)
            assertThat(state.steps[0].state).isInstanceOf(StepState.Error::class.java)
            assertThat(state.isPluginUpdateRequired).isTrue()
        }

    @Test
    fun `given plugin check returns Error, when CheckPluginCompatibility runs, then step is Error`() =
        testBlocking {
            setup {
                whenever(checkWCPluginSupport(forceRefresh = true))
                    .thenReturn(CheckWooPluginPushNotificationsSupport.Result.Error)
            }

            val state = viewModel.viewState.runAndGetValue {
                advanceUntilIdle()
            }

            assertThat(state.steps[0].type).isEqualTo(StepType.CheckPluginCompatibility)
            assertThat(state.steps[0].state).isInstanceOf(StepState.Error::class.java)
            assertThat(state.isPluginUpdateRequired).isFalse()
        }

    @Test
    fun `given register site returns forbidden, when ConnectStore runs, then permission error is shown`() =
        testBlocking {
            setup {
                whenever(jetpackActivationRepository.registerSite(any(), any()))
                    .thenReturn(
                        Result.failure(
                            OnChangedException(
                                JetpackStore.JetpackError(
                                    message = "Forbidden",
                                    errorCode = 403
                                )
                            )
                        )
                    )
            }

            val state = viewModel.viewState.runAndGetValue {
                advanceUntilIdle()
            }

            assertThat(state.steps[1].type).isEqualTo(StepType.ConnectStore)
            assertThat(state.steps[1].state).isEqualTo(
                StepState.Error(
                    UiStringRes(
                        R.string.woo_push_notifications_connection_steps_error_connection_permission_message
                    )
                )
            )
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_ERROR),
                eq(
                    mapOf(
                        AnalyticsTracker.KEY_STEP to "connect_wpcom",
                        AnalyticsTracker.KEY_ERROR_DESC to "Forbidden",
                        AnalyticsTracker.KEY_ERROR_CODE to "403",
                        AnalyticsTracker.KEY_ERROR_TYPE to "JetpackError"
                    )
                )
            )
        }

    @Test
    fun `given register site fails, when ConnectStore runs, then generic error is shown`() = testBlocking {
        setup {
            whenever(jetpackActivationRepository.registerSite(any(), any()))
                .thenReturn(Result.failure(Exception("registration failed")))
        }

        val state = viewModel.viewState.runAndGetValue {
            advanceUntilIdle()
        }

        assertThat(state.steps[1].type).isEqualTo(StepType.ConnectStore)
        assertThat(state.steps[1].state)
            .isEqualTo(StepState.Error(UiStringRes(R.string.woo_push_notifications_connection_steps_generic_error)))
    }

    @Test
    fun `when connect store succeeds, then flow advances to enable push notifications step`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("")
        }

        val state = viewModel.viewState.runAndGetValue {
            advanceUntilIdle()
        }

        assertThat(state.steps[0].type).isEqualTo(StepType.CheckPluginCompatibility)
        assertThat(state.steps[0].state).isEqualTo(StepState.Success)
        assertThat(state.steps[1].type).isEqualTo(StepType.ConnectStore)
        assertThat(state.steps[1].state).isEqualTo(StepState.Success)
        assertThat(state.steps[2].type).isEqualTo(StepType.EnablePushNotifications)
        assertThat(state.steps[2].state).isInstanceOf(StepState.Error::class.java)
    }

    @Test
    fun `given push registration fails, when EnablePushNotifications runs, then step is Error`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
            whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any(), any()))
                .thenReturn(Result.failure(Exception("registration failed")))
        }

        val state = viewModel.viewState.runAndGetValue {
            advanceUntilIdle()
        }

        assertThat(state.steps[2].type).isEqualTo(StepType.EnablePushNotifications)
        assertThat(state.steps[2].state).isInstanceOf(StepState.Error::class.java)
    }

    @Test
    fun `given push registration fails with WooException, when EnablePushNotifications runs, then tracks error details`() =
        testBlocking {
            setup {
                whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
                whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any(), any()))
                    .thenReturn(
                        Result.failure(
                            WooException(
                                WooError(
                                    type = WooErrorType.API_ERROR,
                                    original = BaseRequest.GenericErrorType.SERVER_ERROR,
                                    message = "Server error",
                                    apiErrorCode = "rest_forbidden"
                                )
                            )
                        )
                    )
            }

            viewModel.viewState.runAndGetValue {
                advanceUntilIdle()
            }

            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_ERROR),
                eq(
                    mapOf(
                        AnalyticsTracker.KEY_STEP to "enable_push_notifications",
                        AnalyticsTracker.KEY_ERROR_DESC to "Server error",
                        AnalyticsTracker.KEY_ERROR_CODE to "rest_forbidden",
                        AnalyticsTracker.KEY_ERROR_TYPE to "API_ERROR"
                    )
                )
            )
        }

    @Test
    fun `given empty FCM token, when EnablePushNotifications runs, then step is Error`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("")
        }

        val state = viewModel.viewState.runAndGetValue {
            advanceUntilIdle()
        }

        assertThat(state.steps[2].type).isEqualTo(StepType.EnablePushNotifications)
        assertThat(state.steps[2].state).isInstanceOf(StepState.Error::class.java)
    }

    @Test
    fun `given push registration fails then succeeds on retry, when retry clicked, then isDone is true`() =
        testBlocking {
            setup {
                whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
                whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any(), any()))
                    .thenReturn(Result.failure(Exception("registration failed")))
            }

            val errorState = viewModel.viewState.runAndGetValue {
                advanceUntilIdle()
            }
            assertThat(errorState.steps[2].state).isInstanceOf(StepState.Error::class.java)

            whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any(), any()))
                .thenReturn(Result.success(Unit))
            viewModel.onRetryClick()

            val successState = viewModel.viewState.getOrAwaitValue()
            assertThat(successState.isDone).isTrue()
        }

    @Test
    fun `when all steps succeed, then CheckPluginCompatibility and ConnectStore show as Success`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
            whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any(), any()))
                .thenReturn(Result.success(Unit))
        }

        val state = viewModel.viewState.runAndGetValue {
            advanceUntilIdle()
        }

        assertThat(state.steps[0].type).isEqualTo(StepType.CheckPluginCompatibility)
        assertThat(state.steps[0].state).isEqualTo(StepState.Success)
        assertThat(state.steps[1].type).isEqualTo(StepType.ConnectStore)
        assertThat(state.steps[1].state).isEqualTo(StepState.Success)
        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_FLOW_SUCCESS),
            eq(mapOf(AnalyticsTracker.KEY_STEP to "enable_push_notifications"))
        )
    }

    @Test
    fun `given site already connected, when initialized, then ConnectStore step is hidden`() = testBlocking {
        setup(isStoreAlreadyConnected = true)

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.steps).hasSize(2)
        assertThat(state.steps[0].type).isEqualTo(StepType.CheckPluginCompatibility)
        assertThat(state.steps[1].type).isEqualTo(StepType.EnablePushNotifications)
    }

    @Test
    fun `given shouldAutoOpenUpdatePlugin, when initialized, then auto-opens plugin update page`() = testBlocking {
        site.adminUrl = "https://coffeebeans.com/wp-admin/"
        setup(
            isStoreAlreadyConnected = true,
            shouldAutoOpenUpdatePlugin = true
        )

        advanceUntilIdle()

        val event = viewModel.event.value
        assertThat(event)
            .isInstanceOf(WooPushNotificationsConnectionStepsViewModel.NavigateToPluginUpdatePage::class.java)
        val url = (event as WooPushNotificationsConnectionStepsViewModel.NavigateToPluginUpdatePage).url
        assertThat(url).contains(WooPushNotificationsConnectionStepsViewModel.WC_PLUGIN_UPDATE_PATH)
    }

    @Test
    fun `given shouldAutoOpenUpdatePlugin, when web view dismissed, then plugin check runs normally`() = testBlocking {
        setup(
            isStoreAlreadyConnected = true,
            shouldAutoOpenUpdatePlugin = true
        ) {
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.Compatible)
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
            whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any(), any()))
                .thenReturn(Result.success(Unit))
        }

        advanceUntilIdle()

        viewModel.onPluginUpdateWebViewDismissed()

        val state = viewModel.viewState.runAndGetValue {
            advanceUntilIdle()
        }

        assertThat(state.steps[0].state).isEqualTo(StepState.Success)
    }

    @Test
    fun `when onUpdatePluginClick, then NavigateToPluginUpdatePage is triggered`() = testBlocking {
        site.adminUrl = "https://coffeebeans.com/wp-admin/"
        setup {
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.UpdateRequired("9.0.0"))
        }

        advanceUntilIdle()

        viewModel.onUpdatePluginClick()

        val event = viewModel.event.value
        assertThat(event)
            .isInstanceOf(WooPushNotificationsConnectionStepsViewModel.NavigateToPluginUpdatePage::class.java)
        val url = (event as WooPushNotificationsConnectionStepsViewModel.NavigateToPluginUpdatePage).url
        assertThat(url).contains(WooPushNotificationsConnectionStepsViewModel.WC_PLUGIN_UPDATE_PATH)
    }
}
