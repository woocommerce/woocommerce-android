package com.woocommerce.android.ui.login.jetpack.wpcom

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.ui.login.MagicLinkFlow
import com.woocommerce.android.ui.login.MagicLinkSource
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.login.MagicLinkFallbackButton

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackActivationMagicLinkRequestViewModelTest : BaseUnitTest() {
    companion object {
        private const val EMAIL = "email@example.com"
        private val JetpackStatus = JetpackStatus(
            isJetpackInstalled = true,
            isJetpackConnected = false,
            wpComEmail = null
        )
    }

    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val wpComLoginRepository: WPComLoginRepository = mock()
    private val resourceProvider: ResourceProvider = mock()

    private lateinit var viewModel: JetpackActivationMagicLinkRequestViewModel

    fun setup(
        fallbackButton: MagicLinkFallbackButton,
        requestEmailAtStart: Boolean = true
    ) {
        viewModel = JetpackActivationMagicLinkRequestViewModel(
            JetpackActivationMagicLinkRequestFragmentArgs(
                jetpackStatus = JetpackStatus,
                emailOrUsername = EMAIL,
                fallbackButton = fallbackButton,
                requestAtStart = requestEmailAtStart,
                isNewWpComAccount = false
            ).toSavedStateHandle(),
            resourceProvider,
            wpComLoginRepository,
            analyticsTrackerWrapper
        )
    }

    @Test
    fun `given request email at start is true, when view model is initialized, then request magic link`() =
        testBlocking {
            setup(MagicLinkFallbackButton.None, requestEmailAtStart = true)

            verify(wpComLoginRepository).requestMagicLink(
                emailOrUsername = EMAIL,
                flow = MagicLinkFlow.SiteCredentialsToWPCom,
                source = MagicLinkSource.JetpackConnection,
                isSignup = false
            )
        }

    @Test
    fun `given request email at start is false, when view model is initialized, then do not request magic link`() =
        testBlocking {
            setup(MagicLinkFallbackButton.None, requestEmailAtStart = false)

            verify(wpComLoginRepository, never()).requestMagicLink(
                emailOrUsername = EMAIL,
                flow = MagicLinkFlow.SiteCredentialsToWPCom,
                source = MagicLinkSource.JetpackConnection,
                isSignup = false
            )
        }

    @Test
    fun `when request magic link is called, then request magic link`() =
        testBlocking {
            setup(MagicLinkFallbackButton.None, requestEmailAtStart = false)

            viewModel.onRequestMagicLinkClick()

            verify(wpComLoginRepository).requestMagicLink(
                emailOrUsername = EMAIL,
                flow = MagicLinkFlow.SiteCredentialsToWPCom,
                source = MagicLinkSource.JetpackConnection,
                isSignup = false
            )
        }

    @Test
    fun `when open email client is clicked, then trigger open email client event`() {
        setup(MagicLinkFallbackButton.None)

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onOpenEmailClientClick()
        }.last()

        assertThat(event).isEqualTo(JetpackActivationMagicLinkRequestViewModel.OpenEmailClient)
    }

    @Test
    fun `given fallback using password, when fallback button is clicked, then trigger show password screen event`() {
        setup(MagicLinkFallbackButton.Password)

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onFallbackButtonClick()
        }.last()

        assertThat(event).isEqualTo(
            JetpackActivationMagicLinkRequestViewModel.ShowPasswordScreen(
                emailOrUsername = EMAIL,
                jetpackStatus = JetpackStatus
            )
        )
    }

    @Test
    fun `given fallback using username and password, when fallback button is clicked, then trigger show username screen event`() {
        setup(MagicLinkFallbackButton.UsernameAndPassword)

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onFallbackButtonClick()
        }.last()

        assertThat(event).isEqualTo(
            JetpackActivationMagicLinkRequestViewModel.ShowUsernameScreen(
                jetpackStatus = JetpackStatus
            )
        )
    }
}
