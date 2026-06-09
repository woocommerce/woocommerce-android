package com.woocommerce.android.ui.login.wpcom

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.notifications.push.RegisterDevice
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.ui.login.wpcom.WPComLoginPasswordViewModel.ShowMagicLinkScreen
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType
import org.wordpress.android.login.MagicLinkFallbackButton
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WPComLoginPasswordViewModelTest : BaseUnitTest() {
    companion object {
        const val EMAIL = "user@example.com"
        const val PASSWORD = "password123"
        val JETPACK_STATUS = JetpackStatus(
            isJetpackInstalled = true,
            jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
                blogId = 1
            )
        )
    }

    private val savedStateHandle = WPComLoginPasswordFragmentArgs(
        jetpackStatus = JETPACK_STATUS,
        emailOrUsername = EMAIL
    ).toSavedStateHandle()
    private val wpComLoginRepository: WPComLoginRepository = mock()
    private val accountRepository: AccountRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val selectedSite: SelectedSite = mock()
    private val jetpackActivationRepository: JetpackActivationRepository = mock()
    private val registerDevice: RegisterDevice = mock()

    private lateinit var viewModel: WPComLoginPasswordViewModel

    private fun setup() {
        savedStateHandle["password"] = PASSWORD
        viewModel = WPComLoginPasswordViewModel(
            savedStateHandle = savedStateHandle,
            selectedSite = selectedSite,
            jetpackAccountRepository = jetpackActivationRepository,
            wpComLoginRepository = wpComLoginRepository,
            accountRepository = accountRepository,
            resourceProvider = resourceProvider,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            registerDevice = registerDevice
        )
    }

    @Test
    fun `given email login not allowed, when onContinueClick, then show magic link screen with UsernameAndPassword fallback`() =
        testBlocking {
            setup()
            whenever(wpComLoginRepository.login(EMAIL, PASSWORD)).thenReturn(
                WPComLoginRepository.LoginResult.Error(
                    AuthenticationError(AuthenticationErrorType.EMAIL_LOGIN_NOT_ALLOWED, "")
                )
            )

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onContinueClick()
            }.last()

            assertThat(event).isEqualTo(
                ShowMagicLinkScreen(
                    emailOrUsername = EMAIL,
                    jetpackStatus = JETPACK_STATUS,
                    magicLinkFallbackButton = MagicLinkFallbackButton.UsernameAndPassword,
                    requestAtStart = false
                )
            )
        }
}
