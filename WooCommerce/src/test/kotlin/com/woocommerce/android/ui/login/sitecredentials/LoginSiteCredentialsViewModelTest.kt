package com.woocommerce.android.ui.login.sitecredentials

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.EndpointType
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsViewModel.LoggedIn
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticationEndpoints
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.login.LoginAnalyticsListener

private typealias ShowApplicationPasswordTutorialScreen =
    LoginSiteCredentialsViewModel.ShowApplicationPasswordTutorialScreen

@OptIn(ExperimentalCoroutinesApi::class)
class LoginSiteCredentialsViewModelTest : BaseUnitTest() {
    private val site = SiteModel().apply {
        id = SITE_ID
        url = SITE_URL
        hasWooCommerce = true
        applicationPasswordsAuthorizeUrl = "$SITE_URL/wp-admin/authorize-application.php"
    }
    private val persistedSite = SiteModel().apply {
        id = SITE_ID
        url = SITE_URL
        hasWooCommerce = true
    }
    private val repository: WPApiSiteRepository = mock {
        on { login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS) } doReturn Result.success(Unit)
        on { fetchSite(SITE_URL, USERNAME, PASSWORD) } doReturn Result.success(site)
        on { fetchSite(SITE_URL) } doReturn Result.success(site)
        on { getSiteByLocalId(SITE_ID) } doReturn site
        on { checkIfUserIsEligible(site) } doReturn Result.success(true)
        on { checkIfUserIsEligible(persistedSite) } doReturn Result.success(true)
        on { saveAuthenticationEndpoints(site, PROVEN_ENDPOINTS) } doReturn Result.success(persistedSite)
    }
    private val selectedSite: SelectedSite = mock()
    private val notifier: ApplicationPasswordsNotifier = mock {
        on { featureUnavailableEvents } doReturn MutableSharedFlow<WPAPINetworkError>()
    }
    private val analytics: AnalyticsTrackerWrapper = mock()
    private val loginAnalytics: LoginAnalyticsListener = mock()
    private val appPrefs: AppPrefsWrapper = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.error_generic) } doReturn "error"
    }
    private lateinit var savedState: SavedStateHandle
    private lateinit var viewModel: LoginSiteCredentialsViewModel

    @Test
    fun `given default native authentication succeeds, when submitting, then keep the existing login flow`() =
        testBlocking {
            setup()

            viewModel.viewState.observeForTesting { enterCredentialsAndContinue() }

            verify(repository).checkIfUserIsEligible(site)
            verify(repository, never()).saveAuthenticationEndpoints(any(), any())
            verify(selectedSite).set(site)
            assertThat(viewModel.event.value).isEqualTo(LoggedIn(0))
        }

    @Test
    fun `given default login, when credentials are invalid, then keep the browser fallback`() = testBlocking {
        val invalidCredentials = cookieNonceError(Nonce.CookieNonceErrorType.INVALID_CREDENTIALS)
        whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
            .thenReturn(Result.failure(invalidCredentials))
        setup()

        // WHEN
        viewModel.viewState.observeForTesting {
            enterCredentialsAndContinue()
            advanceUntilIdle()
        }

        // THEN
        val state = viewModel.viewState.value
        assertThat(state?.endpointRecovery).isNull()
        assertThat(state?.authenticationError?.errorMessage).isEqualTo(invalidCredentials.errorMessage)
        assertThat(state?.authenticationError?.showWpAdminFallbackOption).isTrue()
    }

    @Test
    fun `given scheme-less site, when recovering with full http or https login URL, then validate matching origin`() =
        testBlocking {
            listOf("http", "https").forEach { scheme ->
                val loginUrl = "$scheme://$SCHEMELESS_SITE/private-login"
                val expectedEndpoints = CookieNonceAuthenticationEndpoints(
                    siteUrl = "$scheme://$SCHEMELESS_SITE/",
                    loginEntryUrl = loginUrl
                )
                val invalidCredentials = cookieNonceError(
                    Nonce.CookieNonceErrorType.INVALID_CREDENTIALS,
                    loginEntryVerified = true
                )
                whenever(repository.login(eq(SCHEMELESS_SITE), eq(USERNAME), eq(PASSWORD), any()))
                    .thenReturn(Result.failure(invalidCredentials))
                setup(recoveryState(EndpointType.LOGIN, loginUrl, SCHEMELESS_SITE))

                viewModel.viewState.observeForTesting {
                    viewModel.onContinueClick()
                    advanceUntilIdle()
                }

                assertThat(viewModel.viewState.value?.endpointRecovery).isNull()
                assertThat(viewModel.viewState.value?.authenticationError?.errorMessage)
                    .isEqualTo(invalidCredentials.errorMessage)
                verify(repository).login(SCHEMELESS_SITE, USERNAME, PASSWORD, expectedEndpoints)
            }
        }

    @Test
    fun `given scheme-less site, when recovery is first shown, then prefill a safe https URL`() = testBlocking {
        val schemeLessSite = SiteModel().apply {
            id = SITE_ID
            url = SCHEMELESS_SITE
            hasWooCommerce = true
        }
        whenever(repository.login(eq(SCHEMELESS_SITE), eq(USERNAME), eq(PASSWORD), any()))
            .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
        whenever(repository.fetchSite(SCHEMELESS_SITE)).thenReturn(Result.success(schemeLessSite))
        setup(siteAddress = SCHEMELESS_SITE)

        viewModel.viewState.observeForTesting {
            enterCredentialsAndContinue()
            advanceUntilIdle()
        }

        assertThat(viewModel.viewState.value?.endpointRecovery).isEqualTo(
            LoginSiteCredentialsViewModel.EndpointRecovery(
                EndpointType.LOGIN,
                "https://$SCHEMELESS_SITE/wp-login.php"
            )
        )
        verify(repository).login(SCHEMELESS_SITE, USERNAME, PASSWORD, SCHEMELESS_ENDPOINTS)
    }

    @Test
    fun `given invalid edited recovery URL, when continuing, then show only its inline validation error`() =
        testBlocking {
            listOf(
                "not a URL" to R.string.login_site_credentials_endpoint_invalid_url_error,
                "https://other.example/private-login" to
                    R.string.login_site_credentials_endpoint_same_site_error
            ).forEach { (url, errorRes) ->
                setup(recoveryState(EndpointType.LOGIN, url))

                viewModel.viewState.observeForTesting {
                    viewModel.onContinueClick()
                    advanceUntilIdle()
                }

                val state = viewModel.viewState.value
                assertThat(state?.endpointRecovery?.errorMessage).isEqualTo(UiStringRes(errorRes))
                assertThat(state?.authenticationError).isNull()
            }
            verify(repository, never()).login(any(), any(), any(), any())
        }

    @Test
    fun `given another endpoint is invalid, when validating edited URL, then show a general error without inline blame`() =
        testBlocking {
            val state = recoveryState(EndpointType.ADMIN, ADMIN_URL).apply {
                this[LOGIN_ENTRY_URL_STATE_KEY] = "https://other.example/private-login"
            }
            setup(state)

            viewModel.viewState.observeForTesting {
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            val viewState = viewModel.viewState.value
            assertThat(viewState?.endpointRecovery?.errorMessage).isNull()
            assertThat(viewState?.authenticationError?.errorMessage)
                .isEqualTo(UiStringRes(R.string.login_site_credentials_endpoint_same_site_error))
            assertThat(viewState?.authenticationError?.showWpAdminFallbackOption).isTrue()
            verify(repository, never()).login(any(), any(), any(), any())
        }

    @Test
    fun `given canonical origin is invalid, when validating edited URL, then show fallback error without inline blame`() =
        testBlocking {
            setup(recoveryState(EndpointType.LOGIN, LOGIN_URL, siteAddress = "not a site"))

            viewModel.viewState.observeForTesting {
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            val state = viewModel.viewState.value
            assertThat(state?.endpointRecovery?.errorMessage).isNull()
            assertThat(state?.authenticationError?.errorMessage).isEqualTo(UiStringRes(R.string.error_generic))
            assertThat(state?.authenticationError?.showWpAdminFallbackOption).isTrue()
            verify(repository, never()).login(any(), any(), any(), any())
        }

    @Test
    fun `given discovery returns reconciled site URL, when native retry succeeds, then complete login`() =
        testBlocking {
            val originalSiteUrl = "http://site.example"
            val originalEndpoints = CookieNonceAuthenticationEndpoints(originalSiteUrl)
            whenever(repository.login(originalSiteUrl, USERNAME, PASSWORD, originalEndpoints))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
            whenever(repository.fetchSite(originalSiteUrl)).thenReturn(Result.success(site))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
                .thenReturn(Result.success(Unit))
            setup(siteAddress = originalSiteUrl)

            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
            }

            verify(repository).login(originalSiteUrl, USERNAME, PASSWORD, originalEndpoints)
            verify(repository).login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS)
            verify(repository).checkIfUserIsEligible(site)
            assertThat(viewModel.viewState.value?.endpointRecovery).isNull()
            assertThat(viewModel.event.value).isEqualTo(LoggedIn(0))
        }

    @Test
    fun `given discovery returns an off-host URL, when recovering endpoint, then keep original origin`() =
        testBlocking {
            val discoveredSite = siteWithUrl("https://other.example")
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
            whenever(repository.fetchSite(SITE_URL)).thenReturn(Result.success(discoveredSite))
            setup()

            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
            }

            assertThat(viewModel.viewState.value?.siteUrl).isEqualTo("site.example")
            assertThat(viewModel.viewState.value?.endpointRecovery?.url).isEqualTo("$SITE_URL/wp-login.php")
            verify(repository).login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS)
            verify(repository, never()).login(eq("https://other.example"), any(), any(), any())
        }

    @Test
    fun `given discovery changes the port, when recovering endpoint, then keep original origin`() = testBlocking {
        val originalSiteUrl = "https://site.example:8443"
        val originalEndpoints = CookieNonceAuthenticationEndpoints(originalSiteUrl)
        val discoveredSite = siteWithUrl("https://site.example:9443")
        whenever(repository.login(originalSiteUrl, USERNAME, PASSWORD, originalEndpoints))
            .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
        whenever(repository.fetchSite(originalSiteUrl)).thenReturn(Result.success(discoveredSite))
        setup(siteAddress = originalSiteUrl)

        viewModel.viewState.observeForTesting {
            enterCredentialsAndContinue()
            advanceUntilIdle()
        }

        assertThat(viewModel.viewState.value?.siteUrl).isEqualTo("site.example:8443")
        assertThat(viewModel.viewState.value?.endpointRecovery?.url)
            .isEqualTo("$originalSiteUrl/wp-login.php")
        verify(repository).login(originalSiteUrl, USERNAME, PASSWORD, originalEndpoints)
        verify(repository, never()).login(eq("https://site.example:9443"), any(), any(), any())
    }

    @Test
    fun `given discovery downgrades HTTPS to HTTP, when recovering endpoint, then keep original origin`() =
        testBlocking {
            val discoveredSite = siteWithUrl("http://site.example")
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
            whenever(repository.fetchSite(SITE_URL)).thenReturn(Result.success(discoveredSite))
            setup()

            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
            }

            assertThat(viewModel.viewState.value?.siteUrl).isEqualTo("site.example")
            assertThat(viewModel.viewState.value?.endpointRecovery?.url).isEqualTo("$SITE_URL/wp-login.php")
            verify(repository).login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS)
            verify(repository, never()).login(eq("http://site.example"), any(), any(), any())
        }

    @Test
    fun `given verified custom login, when post response fails, then retain it and offer browser fallback`() =
        testBlocking {
            val responseError = cookieNonceError(
                Nonce.CookieNonceErrorType.INVALID_RESPONSE,
                loginEntryVerified = true
            )
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, LOGIN_ENDPOINTS))
                .thenReturn(Result.failure(responseError))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, PENDING_LOGIN_ENDPOINTS))
                .thenReturn(Result.failure(responseError))
            setup()

            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
                viewModel.onEndpointUrlChanged(LOGIN_URL)
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            val state = viewModel.viewState.value
            assertThat(state?.username).isEqualTo(USERNAME)
            assertThat(state?.password).isEqualTo(PASSWORD)
            assertThat(state?.endpointRecovery).isNull()
            assertThat(state?.authenticationError?.errorMessage).isEqualTo(responseError.errorMessage)
            assertThat(state?.authenticationError?.showWpAdminFallbackOption).isTrue()
            assertThat(savedState.get<String>(LOGIN_ENTRY_URL_STATE_KEY)).isEqualTo(LOGIN_URL)
            assertThat(viewModel.event.value).isNull()
            verify(repository, never()).saveAuthenticationEndpoints(any(), any())

            viewModel.viewState.observeForTesting {
                viewModel.onErrorDialogDismissed()
                viewModel.onContinueClick()
                advanceUntilIdle()
            }
            verify(repository).login(SITE_URL, USERNAME, PASSWORD, PENDING_LOGIN_ENDPOINTS)
        }

    @Test
    fun `given verified custom login, when post network or nonce fails, then do not blame its URL`() = testBlocking {
        listOf(
            Nonce.CookieNonceErrorType.UNKNOWN,
            Nonce.CookieNonceErrorType.INVALID_NONCE
        ).forEach { errorType ->
            val authenticationError = cookieNonceError(errorType, loginEntryVerified = true)
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, LOGIN_ENDPOINTS))
                .thenReturn(Result.failure(authenticationError))
            setup()

            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
                viewModel.onEndpointUrlChanged(LOGIN_URL)
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            val state = viewModel.viewState.value
            assertThat(state?.endpointRecovery).isNull()
            assertThat(state?.authenticationError?.errorMessage).isEqualTo(authenticationError.errorMessage)
            assertThat(state?.authenticationError?.showWpAdminFallbackOption).isFalse()
        }
    }

    @Test
    fun `given custom login soft page, when preflight is unverified, then keep URL recovery semantics`() =
        testBlocking {
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, LOGIN_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.INVALID_RESPONSE)))
            setup()

            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
                viewModel.onEndpointUrlChanged(LOGIN_URL)
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            val recovery = viewModel.viewState.value?.endpointRecovery
            assertThat(recovery?.type).isEqualTo(EndpointType.LOGIN)
            assertThat(recovery?.url).isEqualTo(LOGIN_URL)
            assertThat(recovery?.errorMessage)
                .isEqualTo(UiStringRes(R.string.login_site_credentials_login_url_not_found_error))
            assertThat(viewModel.viewState.value?.authenticationError).isNull()
        }

    @Test
    fun `given active custom login prompt, when preflight server fails, then preserve text and show actual error`() =
        testBlocking {
            val serverError = cookieNonceError(Nonce.CookieNonceErrorType.GENERIC_ERROR)
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, LOGIN_ENDPOINTS))
                .thenReturn(Result.failure(serverError))
            setup()

            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
                viewModel.onEndpointUrlChanged(LOGIN_URL)
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            val state = viewModel.viewState.value
            assertThat(state?.endpointRecovery?.url).isEqualTo(LOGIN_URL)
            assertThat(state?.endpointRecovery?.errorMessage).isNull()
            assertThat(state?.authenticationError?.errorMessage).isEqualTo(serverError.errorMessage)
            assertThat(state?.authenticationError?.showWpAdminFallbackOption).isFalse()
        }

    @Test
    fun `given retained custom login, when next preflight loses network, then stay in native credentials`() =
        testBlocking {
            val invalidCredentials = cookieNonceError(
                Nonce.CookieNonceErrorType.INVALID_CREDENTIALS,
                loginEntryVerified = true
            )
            val networkError = cookieNonceError(Nonce.CookieNonceErrorType.UNKNOWN)
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, LOGIN_ENDPOINTS))
                .thenReturn(Result.failure(invalidCredentials))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, PENDING_LOGIN_ENDPOINTS))
                .thenReturn(Result.failure(networkError))
            setup()

            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
                viewModel.onEndpointUrlChanged(LOGIN_URL)
                viewModel.onContinueClick()
                advanceUntilIdle()
                viewModel.onErrorDialogDismissed()
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            val state = viewModel.viewState.value
            assertThat(state?.endpointRecovery).isNull()
            assertThat(state?.authenticationError?.errorMessage).isEqualTo(networkError.errorMessage)
            assertThat(state?.authenticationError?.showWpAdminFallbackOption).isFalse()
            verify(repository).login(SITE_URL, USERNAME, PASSWORD, PENDING_LOGIN_ENDPOINTS)
            verify(repository, times(1)).fetchSite(SITE_URL)
            verify(repository, never()).saveAuthenticationEndpoints(any(), any())
        }

    @Test
    fun `given custom login invalid credentials, when retrying and recreating, then retain it with browser fallback`() =
        testBlocking {
            val invalidCredentials = cookieNonceError(
                Nonce.CookieNonceErrorType.INVALID_CREDENTIALS,
                loginEntryVerified = true
            )
            givenCustomLoginRetriesBeforeNativeSuccess(invalidCredentials)

            // GIVEN
            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
                assertThat(viewModel.viewState.value?.endpointRecovery?.type).isEqualTo(EndpointType.LOGIN)

                viewModel.onEndpointUrlChanged(LOGIN_URL)
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            val firstFailureState = viewModel.viewState.value
            assertThat(firstFailureState?.username).isEqualTo(USERNAME)
            assertThat(firstFailureState?.password).isEqualTo(PASSWORD)
            assertThat(firstFailureState?.endpointRecovery).isNull()
            assertThat(firstFailureState?.authenticationError?.errorMessage)
                .isEqualTo(invalidCredentials.errorMessage)
            assertThat(firstFailureState?.authenticationError?.showWpAdminFallbackOption).isTrue()
            verify(repository, never()).saveAuthenticationEndpoints(any(), any())
            verify(selectedSite, never()).set(any())

            // WHEN
            viewModel.viewState.observeForTesting {
                viewModel.onErrorDialogDismissed()
                viewModel.onPasswordChanged(STILL_WRONG_PASSWORD)
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            // THEN
            val repeatedFailureState = viewModel.viewState.value
            assertThat(repeatedFailureState?.endpointRecovery).isNull()
            assertThat(repeatedFailureState?.authenticationError?.errorMessage)
                .isEqualTo(invalidCredentials.errorMessage)
            assertThat(repeatedFailureState?.authenticationError?.showWpAdminFallbackOption).isTrue()
            verify(repository).login(SITE_URL, USERNAME, STILL_WRONG_PASSWORD, PENDING_LOGIN_ENDPOINTS)

            val restoredState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) })
            setup(restoredState)
            val recreatedState = viewModel.viewState.getOrAwaitValue()
            assertThat(recreatedState.endpointRecovery).isNull()
            assertThat(recreatedState.authenticationError?.showWpAdminFallbackOption).isTrue()

            viewModel.viewState.observeForTesting {
                viewModel.onErrorDialogDismissed()
                viewModel.onPasswordChanged(CORRECT_PASSWORD)
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            assertThat(viewModel.viewState.value?.endpointRecovery?.type).isEqualTo(EndpointType.ADMIN)
            verify(repository).login(SITE_URL, USERNAME, CORRECT_PASSWORD, PENDING_LOGIN_ENDPOINTS)
            verify(repository, never()).saveAuthenticationEndpoints(any(), any())

            viewModel.viewState.observeForTesting {
                viewModel.onEndpointUrlChanged(ADMIN_URL)
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            verify(repository).login(SITE_URL, USERNAME, CORRECT_PASSWORD, ADMIN_RETRY_ENDPOINTS)
            verify(repository).saveAuthenticationEndpoints(site, PROVEN_ENDPOINTS)
            verify(selectedSite).set(persistedSite)
        }

    @Test
    fun `given login and admin recovery, when native proof succeeds, then persist endpoints before eligibility`() =
        testBlocking {
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, LOGIN_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL)))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, ADMIN_RETRY_ENDPOINTS))
                .thenReturn(Result.success(Unit))
            whenever(repository.checkIfUserIsEligible(persistedSite))
                .thenReturn(Result.failure(Exception("ineligible")))
            setup()

            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
                assertThat(viewModel.viewState.value?.endpointRecovery?.type).isEqualTo(EndpointType.LOGIN)
                viewModel.onEndpointUrlChanged(LOGIN_URL)
                viewModel.onContinueClick()
                advanceUntilIdle()
                assertThat(viewModel.viewState.value?.endpointRecovery?.type).isEqualTo(EndpointType.ADMIN)
                viewModel.onEndpointUrlChanged(ADMIN_URL)
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            inOrder(repository) {
                verify(repository).saveAuthenticationEndpoints(site, PROVEN_ENDPOINTS)
                verify(repository).checkIfUserIsEligible(persistedSite)
            }
            verify(selectedSite, never()).set(any())
        }

    @Test
    fun `given admin index URL, when native proof succeeds, then promote normalized login and admin endpoints`() =
        testBlocking {
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, ADMIN_RETRY_ENDPOINTS))
                .thenReturn(Result.success(Unit))
            setup(
                recoveryState(EndpointType.ADMIN, ADMIN_INDEX_URL).apply {
                    this[LOGIN_ENTRY_URL_STATE_KEY] = LOGIN_URL
                }
            )

            viewModel.viewState.observeForTesting {
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            verify(repository).login(SITE_URL, USERNAME, PASSWORD, ADMIN_RETRY_ENDPOINTS)
            verify(repository).saveAuthenticationEndpoints(site, PROVEN_ENDPOINTS)
            verify(repository).checkIfUserIsEligible(persistedSite)
            assertThat(savedState.get<String>(LOGIN_ENTRY_URL_STATE_KEY)).isEqualTo(LOGIN_URL)
            assertThat(savedState.get<String>(ADMIN_BASE_URL_STATE_KEY)).isEqualTo(ADMIN_URL)
        }

    @Test
    fun `given endpoint persistence fails, when native proof succeeds, then show accurate error and SiteError analytics`() =
        testBlocking {
            val siteError = SiteError(SiteErrorType.GENERIC_ERROR, PERSISTENCE_ERROR)
            val exception = OnChangedException(siteError, PERSISTENCE_ERROR)
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, PROVEN_ENDPOINTS))
                .thenReturn(Result.success(Unit))
            whenever(repository.saveAuthenticationEndpoints(site, PROVEN_ENDPOINTS))
                .thenReturn(Result.failure(exception))
            setup(
                credentialsState().apply {
                    this[LOGIN_ENTRY_URL_STATE_KEY] = LOGIN_URL
                    this[ADMIN_BASE_URL_STATE_KEY] = ADMIN_URL
                }
            )

            viewModel.viewState.observeForTesting {
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            assertThat(viewModel.event.value)
                .isEqualTo(ShowSnackbar(R.string.login_site_credentials_endpoint_persistence_failed))
            verify(repository, never()).checkIfUserIsEligible(any())
            verify(analytics).track(
                stat = AnalyticsEvent.LOGIN_SITE_CREDENTIALS_LOGIN_FAILED,
                properties = mapOf(
                    AnalyticsTracker.KEY_STEP to
                        LoginSiteCredentialsViewModel.Step.ENDPOINT_PERSISTENCE.name.lowercase(),
                    AnalyticsTracker.KEY_NETWORK_STATUS_CODE to ""
                ),
                errorContext = SiteError::class.java.simpleName,
                errorType = SiteErrorType.GENERIC_ERROR.name,
                errorDescription = PERSISTENCE_ERROR
            )
        }

    @Test
    fun `given custom endpoints, when persistence completes, then publish fetched site marker only on success`() =
        testBlocking {
            var persistenceResult = CompletableDeferred<Result<SiteModel>>()
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, LOGIN_ENDPOINTS))
                .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL)))
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, ADMIN_RETRY_ENDPOINTS))
                .thenReturn(Result.success(Unit))
            whenever(repository.saveAuthenticationEndpoints(site, PROVEN_ENDPOINTS)).doSuspendableAnswer {
                persistenceResult.await()
            }
            setup()

            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
                viewModel.onEndpointUrlChanged(LOGIN_URL)
                viewModel.onContinueClick()
                advanceUntilIdle()
                viewModel.onEndpointUrlChanged(ADMIN_URL)
                viewModel.onContinueClick()
                runCurrent()
                assertThat(savedState.get<Int>("site-id")).isEqualTo(-1)

                persistenceResult.complete(Result.failure(Exception("save failed")))
                advanceUntilIdle()
                assertThat(savedState.get<Int>("site-id")).isEqualTo(-1)
            }

            val restoredState = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) })
            persistenceResult = CompletableDeferred()
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, PROVEN_ENDPOINTS))
                .thenReturn(Result.success(Unit))
            setup(restoredState)

            viewModel.viewState.observeForTesting {
                viewModel.onContinueClick()
                runCurrent()
                assertThat(savedState.get<Int>("site-id")).isEqualTo(-1)

                persistenceResult.complete(Result.success(persistedSite))
                advanceUntilIdle()
            }

            assertThat(savedState.get<Int>("site-id")).isEqualTo(SITE_ID)
            verify(repository, times(2)).saveAuthenticationEndpoints(site, PROVEN_ENDPOINTS)
            verify(repository).checkIfUserIsEligible(persistedSite)
        }

    @Test
    fun `given edited recovery input, when recreated after process death, then restore it as pending`() = testBlocking {
        whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
            .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
        setup()
        viewModel.viewState.observeForTesting {
            enterCredentialsAndContinue()
            viewModel.onEndpointUrlChanged(LOGIN_URL)
        }
        val restored = SavedStateHandle(savedState.keys().associateWith { savedState.get<Any?>(it) })

        setup(restored)

        assertThat(viewModel.viewState.getOrAwaitValue().endpointRecovery).isEqualTo(
            LoginSiteCredentialsViewModel.EndpointRecovery(EndpointType.LOGIN, LOGIN_URL)
        )
        verify(repository, never()).saveAuthenticationEndpoints(any(), any())
    }

    @Test
    fun `given login or admin recovery, when WP Admin fallback is selected, then open the existing tutorial`() =
        testBlocking {
            EndpointType.entries.forEach { type ->
                setup(recoveryState(type, type.recoveryUrl))

                viewModel.viewState.observeForTesting {
                    viewModel.onStartWebAuthorizationClick()
                    advanceUntilIdle()
                }

                assertThat(viewModel.viewState.value?.endpointRecovery?.type).isEqualTo(type)
                assertThat(viewModel.event.value).isEqualTo(
                    ShowApplicationPasswordTutorialScreen(
                        verifiedLoginUrl = null,
                        applicationPasswordAuthorizationUrl = "$SITE_URL/wp-admin/authorize-application.php" +
                            "?app_name=woo_android&success_url=woocommerce://login",
                        errorMessage = "error"
                    )
                )
            }

            verify(repository, never()).saveAuthenticationEndpoints(any(), any())
            verify(repository, times(2)).fetchSite(SITE_URL)
        }

    @Test
    fun `given verified login and advertised authorization URLs, when fallback is selected, then pass both unchanged`() =
        testBlocking {
            val loginUrl = "$LOGIN_URL?security_token=abc&redirect_to=stale"
            val customAuthorizationUrl = "$SITE_URL/private-admin/authorize-application.php"
            val fetchedSite = SiteModel().apply {
                id = SITE_ID
                url = SITE_URL
                hasWooCommerce = true
                applicationPasswordsAuthorizeUrl = customAuthorizationUrl
            }
            whenever(repository.fetchSite(SITE_URL)).thenReturn(Result.success(fetchedSite))
            setup(
                credentialsState().apply {
                    this[LOGIN_ENTRY_URL_STATE_KEY] = loginUrl
                }
            )

            viewModel.viewState.observeForTesting {
                viewModel.onStartWebAuthorizationClick()
                advanceUntilIdle()
            }

            val event = viewModel.event.value as ShowApplicationPasswordTutorialScreen
            assertThat(event.verifiedLoginUrl).isEqualTo(loginUrl)
            assertThat(event.applicationPasswordAuthorizationUrl).isEqualTo(
                "$customAuthorizationUrl?app_name=woo_android&success_url=woocommerce://login"
            )
        }

    @Test
    fun `given login or admin recovery, when browser completes, then do not promote pending endpoints`() =
        testBlocking {
            EndpointType.entries.forEach { type ->
                setup(recoveryState(type, type.recoveryUrl))

                viewModel.viewState.observeForTesting {
                    viewModel.onStartWebAuthorizationClick()
                    advanceUntilIdle()
                    viewModel.onWebAuthorizationUrlLoaded(WEB_SUCCESS_URL)
                    advanceUntilIdle()
                }
            }

            verify(repository, times(2)).saveApplicationPassword(SITE_ID, USERNAME, PASSWORD)
            verify(repository, never()).saveAuthenticationEndpoints(any(), any())
            verify(repository, times(2)).checkIfUserIsEligible(site)
        }

    @Test
    fun `given recovery is cancelled, when credentials are retried, then use no pending manual endpoint`() =
        testBlocking {
            val customLoginError = cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)
            whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
                .thenReturn(Result.failure(customLoginError), Result.success(Unit))
            setup()

            viewModel.viewState.observeForTesting {
                enterCredentialsAndContinue()
                advanceUntilIdle()
                viewModel.onEndpointUrlChanged(LOGIN_URL)
                viewModel.onEndpointRecoveryCancelClick()
                viewModel.onContinueClick()
                advanceUntilIdle()
            }

            assertThat(viewModel.viewState.value?.endpointRecovery).isNull()
            assertThat(savedState.get<String>(LOGIN_ENTRY_URL_STATE_KEY)).isNull()
            verify(repository, times(2)).login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS)
            verify(repository, never()).saveAuthenticationEndpoints(any(), any())
            verify(repository).checkIfUserIsEligible(site)
        }

    private suspend fun setup(
        restoredState: SavedStateHandle? = null,
        siteAddress: String = SITE_URL
    ) {
        savedState = restoredState ?: SavedStateHandle(
            mapOf(
                LoginSiteCredentialsViewModel.SITE_ADDRESS_KEY to siteAddress,
                LoginSiteCredentialsViewModel.IS_JETPACK_CONNECTED_KEY to false
            )
        )
        viewModel = LoginSiteCredentialsViewModel(
            savedState,
            repository,
            selectedSite,
            loginAnalytics,
            notifier,
            analytics,
            appPrefs,
            resourceProvider,
            object : ApplicationPasswordsConfiguration {
                override val applicationName = "woo_android"
                override suspend fun isEnabledForJetpackAccess() = true
            }
        )
    }

    private suspend fun givenCustomLoginRetriesBeforeNativeSuccess(
        invalidCredentials: CookieNonceAuthenticationException
    ) {
        whenever(repository.login(SITE_URL, USERNAME, PASSWORD, DEFAULT_ENDPOINTS))
            .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_LOGIN_URL)))
        whenever(repository.login(SITE_URL, USERNAME, PASSWORD, LOGIN_ENDPOINTS))
            .thenReturn(Result.failure(invalidCredentials))
        whenever(repository.login(SITE_URL, USERNAME, STILL_WRONG_PASSWORD, PENDING_LOGIN_ENDPOINTS))
            .thenReturn(Result.failure(invalidCredentials))
        whenever(repository.login(SITE_URL, USERNAME, CORRECT_PASSWORD, PENDING_LOGIN_ENDPOINTS))
            .thenReturn(Result.failure(cookieNonceError(Nonce.CookieNonceErrorType.CUSTOM_ADMIN_URL)))
        whenever(repository.login(SITE_URL, USERNAME, CORRECT_PASSWORD, ADMIN_RETRY_ENDPOINTS))
            .thenReturn(Result.success(Unit))
        whenever(repository.fetchSite(SITE_URL, USERNAME, CORRECT_PASSWORD)).thenReturn(Result.success(site))
        setup()
    }

    private fun credentialsState(siteAddress: String = SITE_URL) = SavedStateHandle(
        mapOf(
            LoginSiteCredentialsViewModel.SITE_ADDRESS_KEY to siteAddress,
            LoginSiteCredentialsViewModel.IS_JETPACK_CONNECTED_KEY to false,
            LoginSiteCredentialsViewModel.USERNAME_KEY to USERNAME,
            LoginSiteCredentialsViewModel.PASSWORD_KEY to PASSWORD
        )
    )

    private fun recoveryState(
        type: EndpointType,
        url: String,
        siteAddress: String = SITE_URL
    ) = credentialsState(siteAddress).apply {
        this[ENDPOINT_RECOVERY_STATE_KEY] = LoginSiteCredentialsViewModel.EndpointRecovery(type, url)
    }

    private fun siteWithUrl(siteUrl: String) = SiteModel().apply {
        id = SITE_ID
        url = siteUrl
        hasWooCommerce = true
    }

    private val EndpointType.recoveryUrl: String
        get() = when (this) {
            EndpointType.LOGIN -> LOGIN_URL
            EndpointType.ADMIN -> ADMIN_URL
        }

    private fun enterCredentialsAndContinue() {
        viewModel.onUsernameChanged(USERNAME)
        viewModel.onPasswordChanged(PASSWORD)
        viewModel.viewState.getOrAwaitValue()
        viewModel.onContinueClick()
    }

    private fun cookieNonceError(
        type: Nonce.CookieNonceErrorType,
        loginEntryVerified: Boolean = false
    ) = CookieNonceAuthenticationException(
        errorMessage = UiStringText(type.name),
        errorType = type,
        networkStatusCode = null,
        loginEntryVerified = loginEntryVerified
    )

    private companion object {
        const val SITE_ID = 7
        const val SITE_URL = "https://site.example"
        const val SCHEMELESS_SITE = "site.example"
        const val LOGIN_URL = "$SITE_URL/private-login"
        const val ADMIN_URL = "$SITE_URL/private-admin/"
        const val ADMIN_INDEX_URL = "$SITE_URL/private-admin/index.php"
        const val USERNAME = "merchant"
        const val PASSWORD = "password"
        const val STILL_WRONG_PASSWORD = "still-wrong-password"
        const val CORRECT_PASSWORD = "correct-password"
        const val PERSISTENCE_ERROR = "save failed"
        const val ENDPOINT_RECOVERY_STATE_KEY = "endpoint-recovery"
        const val LOGIN_ENTRY_URL_STATE_KEY = "login-entry-url"
        const val ADMIN_BASE_URL_STATE_KEY = "admin-base-url"
        const val WEB_SUCCESS_URL = "woocommerce://login?user_login=$USERNAME&password=$PASSWORD"
        val DEFAULT_ENDPOINTS = CookieNonceAuthenticationEndpoints(SITE_URL)
        val SCHEMELESS_ENDPOINTS = CookieNonceAuthenticationEndpoints(SCHEMELESS_SITE)
        val LOGIN_ENDPOINTS = CookieNonceAuthenticationEndpoints("$SITE_URL/", loginEntryUrl = LOGIN_URL)
        val PENDING_LOGIN_ENDPOINTS = CookieNonceAuthenticationEndpoints(SITE_URL, loginEntryUrl = LOGIN_URL)
        val ADMIN_RETRY_ENDPOINTS = CookieNonceAuthenticationEndpoints(
            "$SITE_URL/",
            LOGIN_URL,
            ADMIN_URL,
            CookieNonceAuthenticationEndpoints.AdminBaseVerification.AUTHENTICATED_DASHBOARD
        )
        val PROVEN_ENDPOINTS = CookieNonceAuthenticationEndpoints(SITE_URL, LOGIN_URL, ADMIN_URL)
    }
}
