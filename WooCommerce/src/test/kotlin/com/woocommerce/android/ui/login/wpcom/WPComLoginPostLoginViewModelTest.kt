package com.woocommerce.android.ui.login.wpcom

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.notifications.push.RegisterDevice
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.jetpack.GoToStore
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.ui.login.wpcom.WPComLoginPostLoginViewModel.ShowJetpackActivationScreen
import com.woocommerce.android.ui.login.wpcom.WPComLoginPostLoginViewModel.ShowJetpackCPInstallationScreen
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WPComLoginPostLoginViewModelTest : BaseUnitTest() {
    companion object {
        private const val SITE_URL = "https://example.com"
    }

    private val site = SiteModel().apply { url = SITE_URL }
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val jetpackActivationRepository: JetpackActivationRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val registerDevice: RegisterDevice = mock()

    private fun createViewModel() = WPComLoginPostLoginViewModel(
        savedStateHandle = SavedStateHandle(),
        selectedSite = selectedSite,
        jetpackActivationRepository = jetpackActivationRepository,
        analyticsTrackerWrapper = analyticsTrackerWrapper,
        registerDevice = registerDevice
    )

    @Test
    fun `given jetpack setup with user not connected, when login succeeds, then show activation screen`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                    siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
                    blogId = 1
                )
            )
            val viewModel = createViewModel()
            val events = viewModel.event.captureValues()

            val result = viewModel.onLoginSuccess(jetpackStatus)

            assertThat(result.isSuccess).isTrue()
            assertThat(events.last()).isEqualTo(
                ShowJetpackActivationScreen(
                    jetpackStatus = jetpackStatus,
                    siteUrl = SITE_URL
                )
            )
        }

    @Test
    fun `given jetpack setup with user connected and jetpack installed, when login succeeds, then go to store`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = true,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected(wpComEmail = "test@example.com")
            )
            whenever(jetpackActivationRepository.getJetpackSiteByUrl(SITE_URL))
                .thenReturn(SiteModel().apply { hasWooCommerce = true })
            val viewModel = createViewModel()
            val events = viewModel.event.captureValues()

            val result = viewModel.onLoginSuccess(jetpackStatus)

            assertThat(result.isSuccess).isTrue()
            verify(registerDevice).kickoff(RegisterDevice.Trigger.LOGIN_SUCCESS)
            assertThat(events.last()).isEqualTo(GoToStore)
        }

    @Test
    fun `given jetpack setup with user connected and jetpack not installed, when login succeeds, then show CP install`() =
        testBlocking {
            val jetpackStatus = JetpackStatus(
                isJetpackInstalled = false,
                jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected(wpComEmail = "test@example.com")
            )
            whenever(jetpackActivationRepository.getJetpackSiteByUrl(SITE_URL))
                .thenReturn(SiteModel().apply { hasWooCommerce = true })
            val viewModel = createViewModel()
            val events = viewModel.event.captureValues()

            val result = viewModel.onLoginSuccess(jetpackStatus)

            assertThat(result.isSuccess).isTrue()
            verify(registerDevice).kickoff(RegisterDevice.Trigger.LOGIN_SUCCESS)
            assertThat(events.last()).isEqualTo(ShowJetpackCPInstallationScreen)
        }
}
