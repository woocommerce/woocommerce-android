package com.woocommerce.android.ui.login.jetpack.main

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.ui.login.jetpack.connection.JetpackActivationWebViewViewModel
import com.woocommerce.android.ui.login.jetpack.main.JetpackActivationMainViewModel.ConnectionStep
import com.woocommerce.android.ui.login.jetpack.main.JetpackActivationMainViewModel.ShowJetpackConnectionWebView
import com.woocommerce.android.ui.login.jetpack.main.JetpackActivationMainViewModel.StepState
import com.woocommerce.android.ui.login.jetpack.main.JetpackActivationMainViewModel.StepType
import com.woocommerce.android.ui.login.jetpack.main.JetpackActivationMainViewModel.ViewState
import com.woocommerce.android.ui.login.jetpack.main.JetpackActivationMainViewModel.ViewState.ProgressViewState
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

/**
 * This is still missing tests for some scenarios, for now it covers only the connection step and its flows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JetpackActivationMainViewModelTest : BaseUnitTest() {
    private val siteUrl = "example.com"
    private val site = SiteModel().apply {
        url = siteUrl
        username = "username"
        password = "password"
    }

    private lateinit var viewModel: JetpackActivationMainViewModel

    private val jetpackActivationRepository: JetpackActivationRepository = mock {
        onBlocking { getSiteByUrl(siteUrl) } doReturn site
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val pluginRepository: PluginRepository = mock()
    private val accountRepository: AccountRepository = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock()

    suspend fun setup(
        isJetpackInstalled: Boolean = false,
        prepareMocks: suspend () -> Unit = { }
    ) {
        prepareMocks()

        viewModel = JetpackActivationMainViewModel(
            savedStateHandle = JetpackActivationMainFragmentArgs(
                isJetpackInstalled = isJetpackInstalled, siteUrl = siteUrl
            ).toSavedStateHandle(),
            jetpackActivationRepository = jetpackActivationRepository,
            pluginRepository = pluginRepository,
            accountRepository = accountRepository,
            appPrefsWrapper = appPrefsWrapper,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            selectedSite = selectedSite
        )
    }

    @Test
    fun `given site using application passwords and fully disconnected, when starting Jetpack connection, then use alternative URL`() =
        testBlocking {
            setup(isJetpackInstalled = true) {
                whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
                whenever(
                    jetpackActivationRepository.fetchJetpackConnectionUrl(
                        site = site,
                        useApplicationPasswords = true
                    )
                ).thenReturn(Result.success("https://example.com"))
            }

            val event = viewModel.event.captureValues().last()

            assertThat(event).isEqualTo(
                ShowJetpackConnectionWebView(
                    url = "https://wordpress.com/jetpack/connect?url=example.com" +
                        "&mobile_redirect=${JetpackActivationMainViewModel.MOBILE_REDIRECT}&from=mobile"
                )
            )
        }

    @Test
    fun `given site using application passwords and with site-level connection, when starting Jetpack connection, then use default URL`() =
        testBlocking {
            val connectionUrl = JetpackActivationMainViewModel.JETPACK_SITE_CONNECTED_AUTH_URL_PREFIX
            setup(isJetpackInstalled = true) {
                whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
                whenever(
                    jetpackActivationRepository.fetchJetpackConnectionUrl(
                        site = site,
                        useApplicationPasswords = true
                    )
                ).thenReturn(Result.success(connectionUrl))
            }

            val event = viewModel.event.captureValues().last()

            assertThat(event).isEqualTo(
                ShowJetpackConnectionWebView(
                    url = connectionUrl
                )
            )
        }

    @Test
    fun `given site not using application passwords, when starting Jetpack connection, then use default URL`() =
        testBlocking {
            val connectionUrl = JetpackActivationMainViewModel.JETPACK_SITE_CONNECTED_AUTH_URL_PREFIX
            setup(isJetpackInstalled = true) {
                whenever(selectedSite.connectionType).thenReturn(null)
                whenever(
                    jetpackActivationRepository.fetchJetpackConnectionUrl(
                        site = site,
                        useApplicationPasswords = false
                    )
                ).thenReturn(Result.success(connectionUrl))
            }

            val event = viewModel.event.captureValues().last()

            assertThat(event).isEqualTo(
                ShowJetpackConnectionWebView(
                    url = connectionUrl
                )
            )
        }

    @Test
    fun `when connection step succeeds, then start connection validation`() = testBlocking {
        setup(isJetpackInstalled = true) {
            whenever(
                jetpackActivationRepository.fetchJetpackConnectionUrl(site = site, useApplicationPasswords = false)
            ).thenReturn(Result.success("https://example.com/connect"))

            whenever(
                jetpackActivationRepository.fetchJetpackConnectedEmail(site = site, useApplicationPasswords = false)
            ).doSuspendableAnswer {
                // The duration value is not important, it's just to make sure we suspend at this step
                delay(100)
                Result.success("")
            }
        }

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onJetpackConnectionResult(JetpackActivationWebViewViewModel.ConnectionResult.Success)
        }.last()

        assertThat((state as ProgressViewState).connectionStep).isEqualTo(ConnectionStep.Validation)
    }

    @Test
    fun `when validation step succeeds, then mark steps as done`() = testBlocking {
        setup(isJetpackInstalled = true) {
            whenever(
                jetpackActivationRepository.fetchJetpackConnectionUrl(site = site, useApplicationPasswords = false)
            ).thenReturn(Result.success("https://example.com/connect"))

            whenever(
                jetpackActivationRepository.fetchJetpackConnectedEmail(site = site, useApplicationPasswords = false)
            ).doReturn(Result.success("email"))
        }

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onJetpackConnectionResult(JetpackActivationWebViewViewModel.ConnectionResult.Success)
            advanceUntilIdle()
            runCurrent()
        }.last()

        assertThat((state as ProgressViewState).steps).allSatisfy { step ->
            assertThat(step.state).isEqualTo(StepState.Success)
        }
    }

    @Test
    fun `when validation step fails due to missing email, then retrying should restart the connection`() =
        testBlocking {
            setup(isJetpackInstalled = true) {
                whenever(
                    jetpackActivationRepository.fetchJetpackConnectionUrl(site = site, useApplicationPasswords = false)
                ).thenReturn(Result.success("https://example.com/connect"))
                whenever(
                    jetpackActivationRepository.fetchJetpackConnectedEmail(site = site, useApplicationPasswords = false)
                ).thenReturn(Result.failure(JetpackActivationRepository.JetpackMissingConnectionEmailException()))
            }

            val state = viewModel.viewState.runAndCaptureValues {
                viewModel.onJetpackConnectionResult(JetpackActivationWebViewViewModel.ConnectionResult.Success)
                viewModel.onRetryClick()
            }.last()

            verify(jetpackActivationRepository, times(2)).fetchJetpackConnectionUrl(
                site = site,
                useApplicationPasswords = false
            )
            assertThat((state as ProgressViewState).steps.single { it.state == StepState.Ongoing }.type)
                .isEqualTo(StepType.Connection)
        }

    @Test
    fun `when connection is dismissed, then trigger correct event`() = testBlocking {
        setup(isJetpackInstalled = true) {
            whenever(
                jetpackActivationRepository.fetchJetpackConnectionUrl(site = site, useApplicationPasswords = false)
            ).thenReturn(Result.success("https://example.com/connect"))
        }

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onJetpackConnectionResult(JetpackActivationWebViewViewModel.ConnectionResult.Cancel)
        }.last()

        assertThat(event).isEqualTo(JetpackActivationMainViewModel.ShowWebViewDismissedError)
    }

    @Test
    fun `when connection fails due to an error, then show correct state`() = testBlocking {
        setup(isJetpackInstalled = true) {
            whenever(
                jetpackActivationRepository.fetchJetpackConnectionUrl(site = site, useApplicationPasswords = false)
            ).thenReturn(Result.success("https://example.com/connect"))
        }

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onJetpackConnectionResult(
                JetpackActivationWebViewViewModel.ConnectionResult.Failure(404)
            )
            advanceUntilIdle()
        }.last()

        assertThat(state).isEqualTo(ViewState.ErrorViewState(StepType.Connection, 404))
    }

    @Test
    fun `given site using application passwords, when starting, then allow empty username and password`() =
        testBlocking {
            setup(isJetpackInstalled = false) {
                whenever(jetpackActivationRepository.getSiteByUrl(siteUrl)).thenReturn(
                    site.apply {
                        username = ""
                        password = ""
                    }
                )
                whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
            }

            val viewState = viewModel.viewState.getOrAwaitValue()

            assertThat(viewState).isInstanceOf(ProgressViewState::class.java)
        }

    @Test
    fun `given site not using application passwords, when starting, then require username and password`() =
        testBlocking {
            val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

            // Given that we disable the catching of non-test related exceptions in BaseUnitTest, we need to use
            // a custom handler to capture the exception thrown by the view model
            var exception: Throwable? = null
            Thread.setDefaultUncaughtExceptionHandler { _, e -> exception = e }

            setup(isJetpackInstalled = false) {
                whenever(jetpackActivationRepository.getSiteByUrl(siteUrl)).thenReturn(
                    site.apply {
                        username = ""
                        password = ""
                    }
                )
                whenever(selectedSite.connectionType).thenReturn(null)
            }

            assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)

            // Restore the original handler
            Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        }
}
