package com.woocommerce.android.ui.login.sitecredentials

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.applicationpasswords.ApplicationPasswordGenerationException
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.LoggedIn
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.ShowApplicationPasswordsUnavailableScreen
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.ShowNonWooErrorScreen
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.login.LoginAnalyticsListener

@ExperimentalCoroutinesApi
class LoginSiteCredentialsViewModelTest : BaseUnitTest() {
    private val testUsername = "username"
    private val testPassword = "password"
    private val siteAddress: String = "http://site.com"
    private val siteAddressWithoutSchemeAndSuffix = "site.com"
    private val clientId = "woo_android"

    private val urlAuthBase = "$siteAddress/wp-admin/authorize-application.php"
    private val urlRedirectBase = "woocommerce://login"
    private val urlAuthFull = "$urlAuthBase?app_name=$clientId&success_url=$urlRedirectBase"
    private val urlSuccessRedirect = "$urlRedirectBase?user_login=$testUsername&password=$testPassword"
    private val urlRejectedRedirect = "$urlRedirectBase?success=false"

    private val testSite = SiteModel().apply {
        hasWooCommerce = true
    }
    private val applicationPasswordsUnavailableEvents = MutableSharedFlow<WPAPINetworkError>(extraBufferCapacity = 1)

    private val wpApiSiteRepository: WPApiSiteRepository = mock {
        onBlocking { fetchSite(eq(siteAddress), any(), any()) } doReturn Result.success(testSite)
        onBlocking { checkIfUserIsEligible(testSite) } doReturn Result.success(true)
        onBlocking { getSiteByLocalId(testSite.id) } doReturn testSite
    }
    private var isJetpackConnected: Boolean = false
    private val selectedSite: SelectedSite = mock()
    private val applicationPasswordsNotifier: ApplicationPasswordsNotifier = mock {
        on { featureUnavailableEvents } doReturn applicationPasswordsUnavailableEvents
    }
    private val loginAnalyticsListener: LoginAnalyticsListener = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val appPrefs: AppPrefsWrapper = mock()
    private val userAgent: UserAgent = mock()

    private lateinit var viewModel: LoginSiteCredentialsViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()

        viewModel = LoginSiteCredentialsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    LoginSiteCredentialsViewModel.SITE_ADDRESS_KEY to siteAddress,
                    LoginSiteCredentialsViewModel.IS_JETPACK_CONNECTED_KEY to isJetpackConnected
                )
            ),
            wpApiSiteRepository = wpApiSiteRepository,
            selectedSite = selectedSite,
            loginAnalyticsListener = loginAnalyticsListener,
            applicationPasswordsNotifier = applicationPasswordsNotifier,
            analyticsTracker = analyticsTracker,
            appPrefs = appPrefs,
            userAgent = userAgent,
            applicationPasswordsClientId = clientId,
            resourceProvider = mock()
        )
    }

    @Test
    fun `when displaying site credentials, then show native login form`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            // Do nothing, this is just to ensure the viewState is initialized
        }.last()

        assertThat(state).isEqualTo(
            LoginSiteCredentialsViewModel.ViewState.NativeLoginViewState(
                siteUrl = siteAddressWithoutSchemeAndSuffix,
                username = "",
                password = ""
            )
        )
    }

    @Test
    fun `given shown login error dialog, when user chooses wp-admin login, then show login webview`() = testBlocking {
        setup {
            whenever(wpApiSiteRepository.getSiteByLocalId(testSite.id)).thenReturn(
                testSite.apply { applicationPasswordsAuthorizeUrl = urlAuthBase }
            )
        }

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onStartWebAuthorizationClick()
        }.last()

        assertThat(state).isEqualTo(
            LoginSiteCredentialsViewModel.ViewState.WebAuthorizationViewState(
                authorizationUrl = urlAuthFull,
                userAgent = userAgent
            )
        )
    }

    @Test
    fun `when changing username, then update state`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onUsernameChanged(testUsername)
        }.last() as LoginSiteCredentialsViewModel.ViewState.NativeLoginViewState

        assertThat(state.username).isEqualTo(testUsername)
    }

    @Test
    fun `when changing password, then update state`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onUsernameChanged(testPassword)
        }.last() as LoginSiteCredentialsViewModel.ViewState.NativeLoginViewState

        assertThat(state.username).isEqualTo(testPassword)
    }

    @Test
    fun `when username is empty, then mark input as invalid`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onUsernameChanged("")
        }.last() as LoginSiteCredentialsViewModel.ViewState.NativeLoginViewState

        assertThat(state.isValid).isFalse()
    }

    @Test
    fun `when password is empty, then mark input as invalid`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onPasswordChanged("")
        }.last() as LoginSiteCredentialsViewModel.ViewState.NativeLoginViewState

        assertThat(state.isValid).isFalse()
    }

    @Test
    fun `given login successful, when submitting login, then log the user successfully`() = testBlocking {
        setup()

        viewModel.viewState.observeForTesting {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
        }

        assertThat(viewModel.event.value).isEqualTo(LoggedIn(testSite.id))
        verify(loginAnalyticsListener).trackSubmitClicked()
        verify(loginAnalyticsListener).trackAnalyticsSignIn(false)
    }

    @Test
    fun `given successful webview login, when user is eligible, then log the user successfully`() = testBlocking {
        setup {
            whenever(wpApiSiteRepository.getSiteByLocalId(any())).thenReturn(testSite)
            whenever(wpApiSiteRepository.checkIfUserIsEligible(testSite)).thenReturn(Result.success(true))
        }

        viewModel.viewState.observeForTesting {
            viewModel.onWebAuthorizationUrlLoaded(urlSuccessRedirect)
        }

        assertThat(viewModel.event.value).isEqualTo(LoggedIn(testSite.id))
        verify(loginAnalyticsListener).trackAnalyticsSignIn(false)
    }

    @Test
    fun `given webview login, when user rejected application password creation, then show error`() = testBlocking {
        setup()

        viewModel.viewState.observeForTesting {
            viewModel.onWebAuthorizationUrlLoaded(urlRejectedRedirect)
        }

        assertThat(viewModel.event.value).isEqualTo(
            ShowSnackbar(R.string.login_site_credentials_web_authorization_connection_rejected)
        )
    }

    @Test
    fun `given incorrect credentials, when submitting, then show error`() = testBlocking {
        val expectedError = CookieNonceAuthenticationException(
            errorMessage = UiStringText("Username or password incorrect"),
            errorType = Nonce.CookieNonceErrorType.INVALID_CREDENTIALS,
            networkStatusCode = null
        )
        setup {
            whenever(wpApiSiteRepository.login(siteAddress, testUsername, testPassword)).thenReturn(
                Result.failure(expectedError)
            )
        }

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
        }.last() as LoginSiteCredentialsViewModel.ViewState.NativeLoginViewState

        assertThat(state.errorDialogMessage).isEqualTo(expectedError.errorMessage)
        verify(analyticsTracker).track(
            stat = eq(AnalyticsEvent.LOGIN_SITE_CREDENTIALS_LOGIN_FAILED),
            properties = argThat {
                get(AnalyticsTracker.KEY_STEP) == LoginSiteCredentialsViewModel.Step.AUTHENTICATION.name.lowercase()
            },
            errorContext = anyOrNull(),
            errorType = eq(expectedError.errorType.name),
            errorDescription = eq((expectedError.errorMessage as UiStringText).text)
        )
        verify(loginAnalyticsListener).trackFailure(anyOrNull())
    }

    @Test
    fun `given site without Woo, when submitting, then show error screen`() = testBlocking {
        setup {
            whenever(wpApiSiteRepository.fetchSite(siteAddress, testUsername, testPassword))
                .thenReturn(Result.success(testSite.apply { hasWooCommerce = false }))
        }

        viewModel.viewState.observeForTesting {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
        }

        assertThat(viewModel.event.value).isEqualTo(ShowNonWooErrorScreen(siteAddress))
    }

    @Test
    fun `given application passwords generation fails, when submitting, then show snackbar`() = testBlocking {
        setup {
            val networkError = WPAPINetworkError(BaseNetworkError(GenericErrorType.UNKNOWN))
            whenever(wpApiSiteRepository.checkIfUserIsEligible(testSite))
                .thenReturn(Result.failure(ApplicationPasswordGenerationException(networkError)))
        }

        viewModel.viewState.observeForTesting {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
        }

        assertThat(viewModel.event.value).isEqualTo(ShowSnackbar(R.string.error_generic))
        verify(analyticsTracker).track(
            stat = eq(AnalyticsEvent.LOGIN_SITE_CREDENTIALS_LOGIN_FAILED),
            properties = argThat {
                get(AnalyticsTracker.KEY_STEP) ==
                    LoginSiteCredentialsViewModel.Step.APPLICATION_PASSWORD_GENERATION.name.lowercase()
            },
            errorContext = anyOrNull(),
            errorType = anyOrNull(),
            errorDescription = anyOrNull()
        )
        verify(loginAnalyticsListener).trackFailure(anyOrNull())
    }

    @Test
    fun `given site without Woo, when attempting Woo installation, then retry fetching site`() = testBlocking {
        setup()

        viewModel.viewState.observeForTesting {
            viewModel.onWooInstallationAttempted()
        }

        verify(wpApiSiteRepository).fetchSite(any(), any(), any())
    }

    @Test
    fun `given application pwd disabled and wp-login-php accessible, when submitting native login, then show error screen`() = testBlocking {
        setup {
            whenever(wpApiSiteRepository.checkIfUserIsEligible(testSite)).thenReturn(Result.failure(Exception()))
        }

        viewModel.viewState.observeForTesting {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
            applicationPasswordsUnavailableEvents.tryEmit(mock())
        }

        assertThat(viewModel.event.value)
            .isEqualTo(ShowApplicationPasswordsUnavailableScreen(siteAddress, isJetpackConnected))
    }

    @Test
    fun `given application pwd disabled and wp-login-php inaccessible, when choosing webview login, then show error`() = testBlocking {
        setup()

        viewModel.viewState.observeForTesting {
            viewModel.onStartWebAuthorizationClick()
            applicationPasswordsUnavailableEvents.tryEmit(mock())
        }

        assertThat(viewModel.event.value)
            .isEqualTo(ShowApplicationPasswordsUnavailableScreen(siteAddress, isJetpackConnected))
    }

    @Test
    fun `give user role fetch fails, when submitting login, then show a snackbar`() = testBlocking {
        setup {
            whenever(wpApiSiteRepository.checkIfUserIsEligible(testSite)).thenReturn(Result.failure(Exception()))
        }

        viewModel.viewState.observeForTesting {
            viewModel.onUsernameChanged(testUsername)
            viewModel.onPasswordChanged(testPassword)
            viewModel.onContinueClick()
        }

        assertThat(viewModel.event.value).isEqualTo(ShowSnackbar(R.string.error_generic))
        verify(analyticsTracker).track(
            stat = eq(AnalyticsEvent.LOGIN_SITE_CREDENTIALS_LOGIN_FAILED),
            properties = argThat {
                get(AnalyticsTracker.KEY_STEP) == LoginSiteCredentialsViewModel.Step.USER_ROLE.name.lowercase()
            },
            errorContext = anyOrNull(),
            errorType = anyOrNull(),
            errorDescription = anyOrNull()
        )
        verify(loginAnalyticsListener).trackFailure(anyOrNull())
    }
}
