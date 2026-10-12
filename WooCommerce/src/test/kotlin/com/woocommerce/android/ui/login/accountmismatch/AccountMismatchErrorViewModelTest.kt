package com.woocommerce.android.ui.login.accountmismatch

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.AccountMismatchPrimaryButton
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.AccountMismatchPrimaryButton.CONNECT_JETPACK
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.AccountMismatchPrimaryButton.CONNECT_WPCOM_SITE
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.AccountMismatchPrimaryButton.NONE
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.NavigateToSiteAddressLogin
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.network.UserAgent
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountMismatchErrorViewModelTest : BaseUnitTest() {
    private val accountRepository: AccountRepository = mock {
        on { getUserAccount() } doReturn null
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.login_jetpack_not_connected, SITE_URL) } doReturn "Jetpack not connected"
        on { getString(R.string.login_wpcom_account_mismatch, SITE_URL) } doReturn "WPCom account mismatch"
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val webViewAuthenticator: WebViewAuthenticator = mock()
    private val userAgent: UserAgent = mock()

    private lateinit var viewModel: AccountMismatchErrorViewModel

    @Test
    fun `given logged in user, when logging in with another account, then open site address login`() =
        testBlocking {
            setup(isUserLoggedIn = true, logoutResult = true)
            val viewState = viewModel.viewState.captureValues().last()

            val event = viewModel.event.runAndCaptureValues {
                viewState.secondaryButtonAction()
            }.last()

            assertThat(event).isEqualTo(NavigateToSiteAddressLogin(SITE_URL))
            verify(accountRepository).logout()
        }

    @Test
    fun `given logged out user, when logging in with another account, then open site address login`() =
        testBlocking {
            setup(isUserLoggedIn = false)
            val viewState = viewModel.viewState.captureValues().last()

            val event = viewModel.event.runAndCaptureValues {
                viewState.secondaryButtonAction()
            }.last()

            assertThat(event).isEqualTo(NavigateToSiteAddressLogin(SITE_URL))
            verify(accountRepository, never()).logout()
        }

    @Test
    fun `given WordPress com site, when screen is shown, then account mismatch message is displayed`() =
        testBlocking {
            setup(isUserLoggedIn = true, primaryButton = CONNECT_WPCOM_SITE)

            val viewState = viewModel.viewState.captureValues().last()

            assertThat(viewState.message).isEqualTo("WPCom account mismatch")
        }

    @Test
    fun `given self-hosted site, when screen is shown, then Jetpack not connected message is displayed`() =
        testBlocking {
            setup(isUserLoggedIn = true, primaryButton = CONNECT_JETPACK)

            val viewState = viewModel.viewState.captureValues().last()

            assertThat(viewState.message).isEqualTo("Jetpack not connected")
        }

    private suspend fun setup(
        isUserLoggedIn: Boolean,
        logoutResult: Boolean = false,
        primaryButton: AccountMismatchPrimaryButton = NONE
    ) {
        doReturn(isUserLoggedIn).`when`(accountRepository).isUserLoggedIn()
        doReturn(logoutResult).`when`(accountRepository).logout()

        viewModel = AccountMismatchErrorViewModel(
            savedStateHandle = AccountMismatchErrorFragmentArgs(
                siteUrl = SITE_URL,
                primaryButton = primaryButton
            ).toSavedStateHandle(),
            accountRepository = accountRepository,
            resourceProvider = resourceProvider,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            webViewAuthenticator = webViewAuthenticator,
            userAgent = userAgent
        )
    }

    private companion object {
        const val SITE_URL = "https://example.com"
    }
}
