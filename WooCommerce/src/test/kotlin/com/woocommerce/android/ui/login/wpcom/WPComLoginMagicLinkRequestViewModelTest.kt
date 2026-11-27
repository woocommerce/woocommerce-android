package com.woocommerce.android.ui.login.wpcom

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.ui.login.MagicLinkFlow
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.login.MagicLinkFallbackButton

@OptIn(ExperimentalCoroutinesApi::class)
class WPComLoginMagicLinkRequestViewModelTest : BaseUnitTest() {
    companion object {
        private const val EMAIL = "email@example.com"
        private val JetpackStatus = JetpackStatus(
            isJetpackInstalled = true,
            jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
                blogId = 1
            )
        )
    }

    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val wpComLoginRepository: WPComLoginRepository = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private lateinit var viewModel: WPComLoginMagicLinkRequestViewModel

    fun setup(
        fallbackButton: MagicLinkFallbackButton,
        requestEmailAtStart: Boolean = true,
        jetpackStatus: JetpackStatus = JetpackStatus
    ) {
        viewModel = WPComLoginMagicLinkRequestViewModel(
            WPComLoginMagicLinkRequestFragmentArgs(
                jetpackStatus = jetpackStatus,
                emailOrUsername = EMAIL,
                fallbackButton = fallbackButton,
                requestAtStart = requestEmailAtStart,
                isNewWpComAccount = false
            ).toSavedStateHandle(),
            resourceProvider,
            wpComLoginRepository,
            analyticsTrackerWrapper,
            appPrefsWrapper
        )
    }

    @Test
    fun `given request email at start is true, when view model is initialized, then request magic link`() =
        testBlocking {
            setup(MagicLinkFallbackButton.None, requestEmailAtStart = true)

            verify(wpComLoginRepository).requestMagicLink(
                emailOrUsername = EMAIL,
                flow = MagicLinkFlow.JetpackConnection,
                isSignup = false
            )
        }

    @Test
    fun `given request email at start is false, when view model is initialized, then do not request magic link`() =
        testBlocking {
            setup(MagicLinkFallbackButton.None, requestEmailAtStart = false)

            verify(wpComLoginRepository, never()).requestMagicLink(
                emailOrUsername = EMAIL,
                flow = MagicLinkFlow.JetpackConnection,
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
                flow = MagicLinkFlow.JetpackConnection,
                isSignup = false
            )
        }

    @Test
    fun `given request succeeds, when requesting magic link, then save request time`() = testBlocking {
        setup(MagicLinkFallbackButton.None, requestEmailAtStart = false)
        whenever(
            wpComLoginRepository.requestMagicLink(
                emailOrUsername = EMAIL,
                flow = MagicLinkFlow.JetpackConnection,
                isSignup = false
            )
        ).thenReturn(Result.success(Unit))
        val beforeRequest = System.currentTimeMillis()

        viewModel.onRequestMagicLinkClick()

        val afterRequest = System.currentTimeMillis()
        val requestedAt = argumentCaptor<Long>()
        verify(appPrefsWrapper).jetpackSetupMagicLinkRequestedAt = requestedAt.capture()
        assertThat(requestedAt.firstValue).isBetween(beforeRequest, afterRequest)
    }

    @Test
    fun `given request fails, when requesting magic link, then do not save request time`() = testBlocking {
        setup(MagicLinkFallbackButton.None, requestEmailAtStart = false)
        whenever(
            wpComLoginRepository.requestMagicLink(
                emailOrUsername = EMAIL,
                flow = MagicLinkFlow.JetpackConnection,
                isSignup = false
            )
        ).thenReturn(Result.failure(Exception()))

        viewModel.onRequestMagicLinkClick()

        verifyNoInteractions(appPrefsWrapper)
    }

    @Test
    fun `when open email client is clicked, then trigger open email client event`() {
        setup(MagicLinkFallbackButton.None)

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onOpenEmailClientClick()
        }.last()

        assertThat(event).isEqualTo(WPComLoginMagicLinkRequestViewModel.OpenEmailClient)
    }

    @Test
    fun `given fallback using password, when fallback button is clicked, then trigger show password screen event`() {
        setup(MagicLinkFallbackButton.Password)

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onFallbackButtonClick()
        }.last()

        assertThat(event).isEqualTo(
            WPComLoginMagicLinkRequestViewModel.ShowPasswordScreen(
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
            WPComLoginMagicLinkRequestViewModel.ShowUsernameScreen(
                jetpackStatus = JetpackStatus
            )
        )
    }

}
