package com.woocommerce.android.ui.pushnotifications.introduction

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus.JetpackStatusFetchResponse
import com.woocommerce.android.ui.pushnotifications.introduction.WooPushNotificationsIntroductionViewModel.ErrorType
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPushNotificationsIntroductionViewModelTest : BaseUnitTest() {
    private val site = SiteModel()

    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }

    private val fetchJetpackStatus: FetchJetpackStatus = mock()
    private val fetchActiveWCPluginVersion: FetchActiveWCPluginVersion = mock()

    private lateinit var viewModel: WooPushNotificationsIntroductionViewModel

    private fun setup() {
        viewModel = WooPushNotificationsIntroductionViewModel(
            savedStateHandle = SavedStateHandle(),
            fetchJetpackStatus = fetchJetpackStatus,
            fetchActiveWCPluginVersion = fetchActiveWCPluginVersion,
            selectedSite = selectedSite
        )
    }

    @Test
    fun `given site is not registered, when continue is clicked, then StartWPComLogin event is triggered`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = false,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
                    blogId = null
                )
            )
            whenever(fetchJetpackStatus(eq(site), eq(true), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))

            setup()
            viewModel.onContinueClick()

            val event = viewModel.event.value
            assertThat(event).isEqualTo(WooPushNotificationsIntroductionViewModel.StartWPComLogin)
        }

    @Test
    fun `given site registration status is unknown, when continue is clicked, then StartWPComLogin event is triggered`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
                    blogId = null
                )
            )
            whenever(fetchJetpackStatus(eq(site), eq(true), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))

            setup()
            viewModel.onContinueClick()

            val event = viewModel.event.value
            assertThat(event).isEqualTo(WooPushNotificationsIntroductionViewModel.StartWPComLogin)
        }

    @Test
    fun `given site is registered but user not connected and WC version is incompatible, when continue is clicked, then NavigateToConnectionSteps event is triggered`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
                    blogId = 123L
                )
            )
            whenever(fetchJetpackStatus(eq(site), eq(true), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))
            whenever(fetchActiveWCPluginVersion()).thenReturn("9.0.0")

            setup()
            viewModel.onContinueClick()

            val event = viewModel.event.value
            assertThat(event).isEqualTo(WooPushNotificationsIntroductionViewModel.NavigateToConnectionSteps)
        }

    @Test
    fun `given user is connected and WC version is incompatible, when continue is clicked, then NavigateToConnectionSteps event is triggered`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected(
                    wpComEmail = "test@test.com"
                )
            )
            whenever(fetchJetpackStatus(eq(site), eq(true), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))
            whenever(fetchActiveWCPluginVersion()).thenReturn("9.0.0")

            setup()
            viewModel.onContinueClick()

            val event = viewModel.event.value
            assertThat(event).isEqualTo(WooPushNotificationsIntroductionViewModel.NavigateToConnectionSteps)
        }

    @Test
    fun `given user is connected and WC version is compatible, when continue is clicked, then generic error state is shown`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected(
                    wpComEmail = "test@test.com"
                )
            )
            whenever(fetchJetpackStatus(eq(site), eq(true), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))
            whenever(fetchActiveWCPluginVersion())
                .thenReturn(WooPushNotificationsIntroductionViewModel.PUSH_NOTIFICATIONS_MIN_WC_VERSION)

            setup()
            viewModel.onContinueClick()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState.errorType).isEqualTo(ErrorType.Generic)
            assertThat(viewState.isLoading).isFalse()
        }

    @Test
    fun `given user is connected and WC version is null, when continue is clicked, then NavigateToConnectionSteps event is triggered`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected(
                    wpComEmail = "test@test.com"
                )
            )
            whenever(fetchJetpackStatus(eq(site), eq(true), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.Success(jetpackStatus)))
            whenever(fetchActiveWCPluginVersion()).thenReturn(null)

            setup()
            viewModel.onContinueClick()

            val event = viewModel.event.value
            assertThat(event).isEqualTo(WooPushNotificationsIntroductionViewModel.NavigateToConnectionSteps)
        }

    @Test
    fun `given fetching jetpack status fails, when continue is clicked, then generic error state is shown`() =
        testBlocking {
            whenever(fetchJetpackStatus(eq(site), eq(true), anyOrNull()))
                .thenReturn(Result.failure(Exception("Network error")))

            setup()
            viewModel.onContinueClick()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState.errorType).isEqualTo(ErrorType.Generic)
            assertThat(viewState.isLoading).isFalse()
        }

    @Test
    fun `given connection is forbidden, when continue is clicked, then forbidden error state is shown`() =
        testBlocking {
            whenever(fetchJetpackStatus(eq(site), eq(true), anyOrNull()))
                .thenReturn(Result.success(JetpackStatusFetchResponse.ConnectionForbidden))

            setup()
            viewModel.onContinueClick()

            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState.errorType).isEqualTo(ErrorType.Forbidden)
            assertThat(viewState.isLoading).isFalse()
        }

    @Test
    fun `when contact support is clicked, then NavigateToHelpScreen event is triggered`() {
        setup()

        viewModel.onContactSupportClick()

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
    }

    @Test
    fun `when What is WordPress_com is clicked, then OpenUrlEvent is triggered`() {
        setup()

        viewModel.onWhatIsWPComClick()

        val event = viewModel.event.value
        assertThat(event).isInstanceOf(WooPushNotificationsIntroductionViewModel.OpenUrlEvent::class.java)
        assertThat((event as WooPushNotificationsIntroductionViewModel.OpenUrlEvent).url)
            .isEqualTo(AppUrls.LOGIN_WITH_EMAIL_WHAT_IS_WORDPRESS_COM_ACCOUNT)
    }
}
