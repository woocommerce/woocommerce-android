package com.woocommerce.android.ui.pushnotifications.introduction

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.notifications.push.CheckWooPluginPushNotificationsSupport
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus.JetpackStatusFetchResponse
import com.woocommerce.android.ui.pushnotifications.introduction.WooPushNotificationsIntroductionViewModel.ViewState
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPushNotificationsIntroductionViewModelTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()

    private val fetchJetpackStatus: FetchJetpackStatus = mock {
        on { invoke(any(), any(), anyOrNull()) } doReturn Result.success(
            JetpackStatusFetchResponse.Success(
                JetpackStatus(
                    isJetpackInstalled = false,
                    jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                        siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
                        blogId = null
                    )
                )
            )
        )
    }
    private val checkWCPluginSupport: CheckWooPluginPushNotificationsSupport = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: WooPushNotificationsIntroductionViewModel

    private fun setup(isJetpackCPSite: Boolean = false) {
        val site = SiteModel().apply {
            url = "https://example.com"
            origin = if (isJetpackCPSite) {
                SiteModel.ORIGIN_WPCOM_REST
            } else {
                SiteModel.ORIGIN_WPAPI
            }
            setIsJetpackCPConnected(isJetpackCPSite)
            setIsJetpackConnected(false)
        }
        whenever(selectedSite.get()).thenReturn(site)

        viewModel = WooPushNotificationsIntroductionViewModel(
            savedStateHandle = SavedStateHandle(),
            fetchJetpackStatus = fetchJetpackStatus,
            checkWCPluginSupport = checkWCPluginSupport,
            selectedSite = selectedSite,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )
    }

    @Test
    fun `given site is not registered, when screen opens, then NotConnected state is shown`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = false,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
                    blogId = null
                )
            )
            whenever(fetchJetpackStatus(any(), any(), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))

            setup()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState).isEqualTo(ViewState.NotConnected)
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_VIEW),
                eq(mapOf(AnalyticsTracker.KEY_STATE to "not_connected"))
            )
        }

    @Test
    fun `given site registration status is unknown, when screen opens, then NotConnected state is shown`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
                    blogId = null
                )
            )
            whenever(fetchJetpackStatus(any(), any(), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))

            setup()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState).isEqualTo(ViewState.NotConnected)
        }

    @Test
    fun `given site is registered but user not connected and WC version is incompatible, when screen opens, then UpdateRequired state is shown`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
                    blogId = 123L
                )
            )
            whenever(fetchJetpackStatus(any(), any(), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.UpdateRequired("9.0.0"))

            setup()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState).isEqualTo(ViewState.UpdateRequired)
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_VIEW),
                eq(mapOf(AnalyticsTracker.KEY_STATE to "update_required"))
            )
        }

    @Test
    fun `given user is connected and WC version is incompatible, when screen opens, then UpdateRequired state is shown`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected(
                    wpComEmail = "test@test.com"
                )
            )
            whenever(fetchJetpackStatus(any(), any(), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.UpdateRequired("9.0.0"))

            setup()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState).isEqualTo(ViewState.UpdateRequired)
        }

    @Test
    fun `given user is connected and WC version is compatible, when screen opens, then Connected state is shown`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected(
                    wpComEmail = "test@test.com"
                )
            )
            whenever(fetchJetpackStatus(any(), any(), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.Compatible)

            setup()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState).isEqualTo(ViewState.Connected)
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_VIEW),
                eq(mapOf(AnalyticsTracker.KEY_STATE to "connected"))
            )
        }

    @Test
    fun `given checking WC version fails, when screen opens, then Error state is shown`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected(
                    wpComEmail = "test@test.com"
                )
            )
            whenever(fetchJetpackStatus(any(), any(), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.Error)

            setup()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState).isEqualTo(ViewState.GenericError)
        }

    @Test
    fun `given fetching jetpack status fails, when screen opens, then GenericError state is shown`() =
        testBlocking {
            whenever(fetchJetpackStatus(any(), any(), anyOrNull()))
                .thenReturn(Result.failure(Exception("Network error")))

            setup()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState).isEqualTo(ViewState.GenericError)
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_ERROR),
                eq(mapOf(AnalyticsTracker.KEY_ERROR_TYPE to "generic"))
            )
        }

    @Test
    fun `given connection is forbidden, when screen opens, then ForbiddenError state is shown`() =
        testBlocking {
            whenever(fetchJetpackStatus(any(), any(), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.ConnectionForbidden))

            setup()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState).isEqualTo(ViewState.ForbiddenError)
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_ERROR),
                eq(mapOf(AnalyticsTracker.KEY_ERROR_TYPE to "no_permission"))
            )
        }

    @Test
    fun `given UpdateRequired state, when continue is clicked, then NavigateToConnectionSteps with update required`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
                    blogId = 123L
                )
            )
            whenever(fetchJetpackStatus(any(), any(), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.UpdateRequired("9.0.0"))

            setup()

            viewModel.onContinueClick()

            val event = viewModel.event.value
            assertThat(event).isEqualTo(
                WooPushNotificationsIntroductionViewModel.NavigateToConnectionSteps(
                    isSiteConnectedToJetpack = true,
                    shouldAutoOpenUpdatePlugin = true
                )
            )
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_BUTTON_TAP),
                eq(mapOf(AnalyticsTracker.KEY_BUTTON_LABEL to "update_plugin"))
            )
        }

    @Test
    fun `given NotConnected state, when continue is clicked, then NavigateToConnectionSteps is triggered`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = false,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
                    blogId = null
                )
            )
            whenever(fetchJetpackStatus(any(), any(), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))

            setup()

            viewModel.onContinueClick()

            val event = viewModel.event.value
            assertThat(event).isEqualTo(
                WooPushNotificationsIntroductionViewModel.NavigateToConnectionSteps(
                    isSiteConnectedToJetpack = false,
                    shouldAutoOpenUpdatePlugin = false
                )
            )
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_BUTTON_TAP),
                eq(mapOf(AnalyticsTracker.KEY_BUTTON_LABEL to "continue"))
            )
        }

    @Test
    fun `given Connected state and compatible plugin, when continue is clicked, then NavigateToConnectionSteps without auto update`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected(
                    wpComEmail = "test@test.com"
                )
            )
            whenever(fetchJetpackStatus(any(), any(), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.Compatible)

            setup()

            viewModel.onContinueClick()

            val event = viewModel.event.value
            assertThat(event).isEqualTo(
                WooPushNotificationsIntroductionViewModel.NavigateToConnectionSteps(
                    isSiteConnectedToJetpack = true,
                    shouldAutoOpenUpdatePlugin = false
                )
            )
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_BUTTON_TAP),
                eq(mapOf(AnalyticsTracker.KEY_BUTTON_LABEL to "continue"))
            )
        }

    @Test
    fun `when continue is clicked on not connected state, then continue label is tracked`() = testBlocking {
        setup()

        viewModel.onContinueClick()

        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_BUTTON_TAP),
            eq(mapOf(AnalyticsTracker.KEY_BUTTON_LABEL to "continue"))
        )
    }

    @Test
    fun `when contact support is clicked, then NavigateToHelpScreen event is triggered`() {
        setup()

        viewModel.onContactSupportClick()

        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_BUTTON_TAP),
            eq(mapOf(AnalyticsTracker.KEY_BUTTON_LABEL to "support"))
        )
        val event = viewModel.event.value
        assertThat(event).isInstanceOf(NavigateToHelpScreen::class.java)
        assertThat((event as NavigateToHelpScreen).origin)
            .isEqualTo(HelpOrigin.WOO_PUSH_NOTIFICATIONS_SETUP)
    }

    @Test
    fun `when not now is clicked, then Exit event is triggered`() {
        setup()

        viewModel.onNotNowClick()

        val event = viewModel.event.value
        assertThat(event).isEqualTo(Exit)
        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_BUTTON_TAP),
            eq(mapOf(AnalyticsTracker.KEY_BUTTON_LABEL to "not_now"))
        )
    }

    @Test
    fun `when close is clicked, then introduction close is tracked and Exit event is triggered`() {
        setup()

        viewModel.onCloseClick()

        val event = viewModel.event.value
        assertThat(event).isEqualTo(Exit)
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_CLOSE)
    }

    @Test
    fun `when What is WordPress_com is clicked, then OpenUrlEvent is triggered`() {
        setup()

        viewModel.onWhatIsWPComClick()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_LINK_TAP)
        val event = viewModel.event.value
        assertThat(event).isInstanceOf(WooPushNotificationsIntroductionViewModel.OpenUrlEvent::class.java)
        assertThat((event as WooPushNotificationsIntroductionViewModel.OpenUrlEvent).url)
            .isEqualTo(AppUrls.LOGIN_WITH_EMAIL_WHAT_IS_WORDPRESS_COM_ACCOUNT)
    }

    @Test
    fun `given a Jetpack CP site with incompatible WC version, when screen opens, then UpdateRequired state is shown`() =
        testBlocking {
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.UpdateRequired("9.0.0"))

            setup(isJetpackCPSite = true)

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState).isEqualTo(ViewState.UpdateRequired)
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_VIEW),
                eq(mapOf(AnalyticsTracker.KEY_STATE to "update_required"))
            )
        }

    @Test
    fun `given a Jetpack CP site with compatible WC version, when screen opens, then Connected state is shown`() =
        testBlocking {
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.Compatible)

            setup(isJetpackCPSite = true)

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState).isEqualTo(ViewState.Connected)
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_VIEW),
                eq(mapOf(AnalyticsTracker.KEY_STATE to "connected"))
            )
        }

    @Test
    fun `given a Jetpack CP site with WC plugin check error, when screen opens, then GenericError state is shown`() =
        testBlocking {
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.Error)

            setup(isJetpackCPSite = true)

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState).isEqualTo(ViewState.GenericError)
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.PUSH_NOTIFICATIONS_SETUP_INTRODUCTION_ERROR),
                eq(mapOf(AnalyticsTracker.KEY_ERROR_TYPE to "generic"))
            )
        }

    @Test
    fun `given a Jetpack CP site, when screen opens, then fetchJetpackStatus is not called`() =
        testBlocking {
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.Compatible)

            setup(isJetpackCPSite = true)

            verify(fetchJetpackStatus, never()).invoke(any(), any(), anyOrNull())
        }

    @Test
    fun `given a Jetpack CP site, when continue is clicked, then isSiteConnectedToJetpack is true`() =
        testBlocking {
            whenever(checkWCPluginSupport(forceRefresh = true))
                .thenReturn(CheckWooPluginPushNotificationsSupport.Result.UpdateRequired("9.0.0"))

            setup(isJetpackCPSite = true)
            viewModel.onContinueClick()

            val event = viewModel.event.value
            assertThat(event).isEqualTo(
                WooPushNotificationsIntroductionViewModel.NavigateToConnectionSteps(
                    isSiteConnectedToJetpack = true,
                    shouldAutoOpenUpdatePlugin = true
                )
            )
        }
}
