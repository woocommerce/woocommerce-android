package com.woocommerce.android.ui.pushnotifications.connection

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.model.UiString
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.jetpack.FetchJetpackStatus
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.ui.login.jetpack.connection.JetpackActivationWebViewViewModel
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.StepState
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPushNotificationsConnectionStepsViewModelTest : BaseUnitTest() {
    companion object {
        private const val SITE_URL = "https://example.com"
    }

    private val site = SiteModel().apply {
        url = SITE_URL
        name = "example.com"
    }

    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val fetchJetpackStatus: FetchJetpackStatus = mock()
    private val jetpackActivationRepository: JetpackActivationRepository = mock()

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = WooPushNotificationsConnectionStepsViewModel(
        selectedSite = selectedSite,
        fetchJetpackStatus = fetchJetpackStatus,
        jetpackActivationRepository = jetpackActivationRepository,
        savedStateHandle = savedStateHandle
    )

    @Test
    fun `given store is already connected to WordPress_com, when screen starts, then connect step is complete`() =
        testBlocking {
            whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true)).thenReturn(
                Result.success(
                    FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                        JetpackStatus(
                            isJetpackInstalled = true,
                            jetpackConnectionStatus = JetpackConnectionStatus.AccountConnected("test@example.com")
                        )
                    )
                )
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
            assertThat(connectStoreStep.state).isEqualTo(WooPushNotificationsConnectionStepsViewModel.StepState.Success)
        }

    @Test
    fun `given store is not connected, when screen starts and connection flow succeeds, then connect step is complete`() =
        testBlocking {
            val status = JetpackConnectionStatus.AccountNotConnected(
                siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
                blogId = 1L
            )
            whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true)).thenReturn(
                Result.success(
                    FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                        JetpackStatus(
                            isJetpackInstalled = true,
                            jetpackConnectionStatus = status
                        )
                    )
                )
            )
            whenever(
                jetpackActivationRepository.connectJetpackAccount(
                    site = site,
                    jetpackConnectionStatus = status,
                    useApplicationPasswords = true
                )
            ).thenReturn(Result.success(Unit))
            whenever(jetpackActivationRepository.fetchJetpackSite(SITE_URL)).thenReturn(Result.success(site))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
            assertThat(connectStoreStep.state).isEqualTo(WooPushNotificationsConnectionStepsViewModel.StepState.Success)
            verify(jetpackActivationRepository).fetchJetpackSite(SITE_URL)
        }

    @Test
    fun `given store is not connected and connection API is unsupported, when screen starts, then open webview with fallback URL`() =
        testBlocking {
            val status = JetpackConnectionStatus.AccountNotConnected(
                siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
                blogId = null
            )
            whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true)).thenReturn(
                Result.success(
                    FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                        JetpackStatus(
                            isJetpackInstalled = true,
                            jetpackConnectionStatus = status
                        )
                    )
                )
            )
            whenever(
                jetpackActivationRepository.fetchJetpackConnectionUrl(
                    site = site,
                    useApplicationPasswords = true
                )
            ).thenReturn(Result.success("https://example.com/connect"))

            val viewModel = createViewModel()
            val event = viewModel.event.runAndCaptureValues {
                advanceUntilIdle()
            }.last()

            assertThat(event).isEqualTo(
                WooPushNotificationsConnectionStepsViewModel.ShowJetpackConnectionWebView(
                    "$SITE_URL/wp-admin/admin.php?page=jetpack"
                )
            )
        }

    @Test
    fun `given store is not connected and connection API is unsupported, when fetched URL is account connection URL, then open fetched URL`() =
        testBlocking {
            val status = JetpackConnectionStatus.AccountNotConnected(
                siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
                blogId = null
            )
            val connectionUrl = "https://jetpack.wordpress.com/jetpack.authorize"
            whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true)).thenReturn(
                Result.success(
                    FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                        JetpackStatus(
                            isJetpackInstalled = true,
                            jetpackConnectionStatus = status
                        )
                    )
                )
            )
            whenever(
                jetpackActivationRepository.fetchJetpackConnectionUrl(
                    site = site,
                    useApplicationPasswords = true
                )
            ).thenReturn(Result.success(connectionUrl))

            val viewModel = createViewModel()
            val event = viewModel.event.runAndCaptureValues {
                advanceUntilIdle()
            }.last()

            assertThat(event).isEqualTo(
                WooPushNotificationsConnectionStepsViewModel.ShowJetpackConnectionWebView(connectionUrl)
            )
        }

    @Test
    fun `given unsupported connection API, when webview connection succeeds, then connect step is complete`() = testBlocking {
        val status = JetpackConnectionStatus.AccountNotConnected(
            siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
            blogId = null
        )
        whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true)).thenReturn(
            Result.success(
                FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                    JetpackStatus(
                        isJetpackInstalled = true,
                        jetpackConnectionStatus = status
                    )
                )
            )
        )
        whenever(
            jetpackActivationRepository.fetchJetpackConnectionUrl(
                site = site,
                useApplicationPasswords = true
            )
        ).thenReturn(Result.success("https://example.com/connect"))
        whenever(jetpackActivationRepository.fetchJetpackSite(SITE_URL)).thenReturn(Result.success(site))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onJetpackConnectionResult(JetpackActivationWebViewViewModel.ConnectionResult.Success)
        advanceUntilIdle()

        val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
        assertThat(connectStoreStep.state).isEqualTo(WooPushNotificationsConnectionStepsViewModel.StepState.Success)
        verify(jetpackActivationRepository).fetchJetpackSite(SITE_URL)
    }

    @Test
    fun `given unsupported connection API, when webview returns forbidden error, then connect step shows permission error`() =
        testBlocking {
            val status = JetpackConnectionStatus.AccountNotConnected(
                siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
                blogId = null
            )
            whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true)).thenReturn(
                Result.success(
                    FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                        JetpackStatus(
                            isJetpackInstalled = true,
                            jetpackConnectionStatus = status
                        )
                    )
                )
            )
            whenever(
                jetpackActivationRepository.fetchJetpackConnectionUrl(
                    site = site,
                    useApplicationPasswords = true
                )
            ).thenReturn(Result.success("https://example.com/connect"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onJetpackConnectionResult(JetpackActivationWebViewViewModel.ConnectionResult.Failure(403))
            advanceUntilIdle()

            val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
            assertThat(connectStoreStep.state).isInstanceOf(StepState.Error::class.java)
            assertThat((connectStoreStep.state as StepState.Error).errorMessage)
                .isEqualTo(
                    UiString.UiStringRes(
                        R.string.woo_push_notifications_connection_steps_error_connection_permission_message
                    )
                )
        }

    @Test
    fun `given unsupported connection API, when webview is canceled, then connect step shows generic error`() = testBlocking {
        val status = JetpackConnectionStatus.AccountNotConnected(
            siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
            blogId = null
        )
        whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true)).thenReturn(
            Result.success(
                FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                    JetpackStatus(
                        isJetpackInstalled = true,
                        jetpackConnectionStatus = status
                    )
                )
            )
        )
        whenever(
            jetpackActivationRepository.fetchJetpackConnectionUrl(
                site = site,
                useApplicationPasswords = true
            )
        ).thenReturn(Result.success("https://example.com/connect"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onJetpackConnectionResult(JetpackActivationWebViewViewModel.ConnectionResult.Cancel)
        advanceUntilIdle()

        val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
        assertThat(connectStoreStep.state).isInstanceOf(StepState.Error::class.java)
        assertThat((connectStoreStep.state as StepState.Error).errorMessage)
            .isEqualTo(UiString.UiStringRes(R.string.woo_push_notifications_connection_steps_generic_error_message))
    }

    @Test
    fun `given connect call fails, when screen starts, then connect step shows permission error`() = testBlocking {
        val status = JetpackConnectionStatus.AccountNotConnected(
            siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
            blogId = 1L
        )
        val connectionError = JetpackStore.JetpackError(
            message = "Connection failed",
            errorCode = 403
        )
        whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true)).thenReturn(
            Result.success(
                FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                    JetpackStatus(
                        isJetpackInstalled = true,
                        jetpackConnectionStatus = status
                    )
                )
            )
        )
        whenever(
            jetpackActivationRepository.connectJetpackAccount(
                site = site,
                jetpackConnectionStatus = status,
                useApplicationPasswords = true
            )
        ).thenReturn(Result.failure(OnChangedException(connectionError)))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
        assertThat(connectStoreStep.state).isInstanceOf(StepState.Error::class.java)
        assertThat((connectStoreStep.state as StepState.Error).errorMessage)
            .isEqualTo(
                UiString.UiStringRes(
                    R.string.woo_push_notifications_connection_steps_error_connection_permission_message
                )
            )
    }

    @Test
    fun `given connection URL fetch fails with forbidden error, when screen starts, then connect step shows permission error`() =
        testBlocking {
            val status = JetpackConnectionStatus.AccountNotConnected(
                siteRegistrationStatus = JetpackSiteRegistrationStatus.UNKNOWN,
                blogId = null
            )
            val connectionError = JetpackStore.JetpackError(
                message = "Connection URL failed",
                errorCode = 403
            )
            whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true)).thenReturn(
                Result.success(
                    FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                        JetpackStatus(
                            isJetpackInstalled = true,
                            jetpackConnectionStatus = status
                        )
                    )
                )
            )
            whenever(
                jetpackActivationRepository.fetchJetpackConnectionUrl(
                    site = site,
                    useApplicationPasswords = true
                )
            ).thenReturn(Result.failure(OnChangedException(connectionError)))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
            assertThat(connectStoreStep.state).isInstanceOf(StepState.Error::class.java)
            assertThat((connectStoreStep.state as StepState.Error).errorMessage)
                .isEqualTo(
                    UiString.UiStringRes(
                        R.string.woo_push_notifications_connection_steps_error_connection_permission_message
                    )
                )
        }

    @Test
    fun `given status fetch fails with forbidden error, when screen starts, then connect step shows generic error`() =
        testBlocking {
            val statusError = JetpackStore.JetpackError(
                message = "Status fetch failed",
                errorCode = 403
            )
            whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true))
                .thenReturn(Result.failure(OnChangedException(statusError)))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
            assertThat(connectStoreStep.state).isInstanceOf(StepState.Error::class.java)
            assertThat((connectStoreStep.state as StepState.Error).errorMessage)
                .isEqualTo(UiString.UiStringRes(R.string.woo_push_notifications_connection_steps_generic_error_message))
        }

    @Test
    fun `given status fetch returns forbidden, when screen starts, then connect step shows permission error`() =
        testBlocking {
            whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true)).thenReturn(
                Result.success(FetchJetpackStatus.JetpackStatusFetchResponse.ConnectionForbidden)
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
            assertThat(connectStoreStep.state).isInstanceOf(StepState.Error::class.java)
            assertThat((connectStoreStep.state as StepState.Error).errorMessage)
                .isEqualTo(
                    UiString.UiStringRes(
                        R.string.woo_push_notifications_connection_steps_error_connection_permission_message
                    )
                )
        }

    @Test
    fun `given connection confirmation fails, when screen starts, then connect step shows generic error`() = testBlocking {
        val status = JetpackConnectionStatus.AccountNotConnected(
            siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
            blogId = 1L
        )
        whenever(fetchJetpackStatus(site = site, useApplicationPasswords = true)).thenReturn(
            Result.success(
                FetchJetpackStatus.JetpackStatusFetchResponse.Success(
                    JetpackStatus(
                        isJetpackInstalled = true,
                        jetpackConnectionStatus = status
                    )
                )
            )
        )
        whenever(
            jetpackActivationRepository.connectJetpackAccount(
                site = site,
                jetpackConnectionStatus = status,
                useApplicationPasswords = true
            )
        ).thenReturn(Result.success(Unit))
        whenever(jetpackActivationRepository.fetchJetpackSite(SITE_URL))
            .thenReturn(Result.failure(IllegalStateException("Site missing")))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
        assertThat(connectStoreStep.state).isInstanceOf(StepState.Error::class.java)
        assertThat((connectStoreStep.state as StepState.Error).errorMessage)
            .isEqualTo(UiString.UiStringRes(R.string.woo_push_notifications_connection_steps_generic_error_message))
    }

    @Test
    fun `given connection step is already complete in saved state, when view model is recreated, then no network call is retriggered`() =
        testBlocking {
            val savedStateHandle = SavedStateHandle(
                mapOf(
                    WooPushNotificationsConnectionStepsViewModel.KEY_CURRENT_STEP to
                        WooPushNotificationsConnectionStepsViewModel.Step(
                            type = WooPushNotificationsConnectionStepsViewModel.StepType.ConnectStore,
                            state = WooPushNotificationsConnectionStepsViewModel.StepState.Success
                        ),
                    WooPushNotificationsConnectionStepsViewModel.KEY_CONNECT_STORE_STAGE to
                        WooPushNotificationsConnectionStepsViewModel.ConnectStoreStage.ConfirmConnection
                )
            )

            createViewModel(savedStateHandle)
            advanceUntilIdle()

            verifyNoInteractions(fetchJetpackStatus, jetpackActivationRepository)
        }

    @Test
    fun `given connection step is ongoing webview stage in saved state, when view model is recreated, then it resumes from webview stage`() =
        testBlocking {
            val savedStateHandle = SavedStateHandle(
                mapOf(
                    WooPushNotificationsConnectionStepsViewModel.KEY_CURRENT_STEP to
                        WooPushNotificationsConnectionStepsViewModel.Step(
                            type = WooPushNotificationsConnectionStepsViewModel.StepType.ConnectStore,
                            state = WooPushNotificationsConnectionStepsViewModel.StepState.Ongoing
                        ),
                    WooPushNotificationsConnectionStepsViewModel.KEY_CONNECT_STORE_STAGE to
                        WooPushNotificationsConnectionStepsViewModel.ConnectStoreStage.WebViewConnection
                )
            )
            whenever(
                jetpackActivationRepository.fetchJetpackConnectionUrl(
                    site = site,
                    useApplicationPasswords = true
                )
            ).thenReturn(Result.success("https://example.com/connect"))

            val firstViewModel = createViewModel(savedStateHandle)
            val firstEvent = firstViewModel.event.runAndCaptureValues {
                advanceUntilIdle()
            }.last()

            assertThat(firstEvent).isEqualTo(
                WooPushNotificationsConnectionStepsViewModel.ShowJetpackConnectionWebView(
                    "$SITE_URL/wp-admin/admin.php?page=jetpack"
                )
            )

            clearInvocations(fetchJetpackStatus, jetpackActivationRepository)

            val secondViewModel = createViewModel(savedStateHandle)
            val secondEvent = secondViewModel.event.runAndCaptureValues {
                advanceUntilIdle()
            }.last()

            assertThat(secondEvent).isEqualTo(
                WooPushNotificationsConnectionStepsViewModel.ShowJetpackConnectionWebView(
                    "$SITE_URL/wp-admin/admin.php?page=jetpack"
                )
            )
            verifyNoInteractions(fetchJetpackStatus)
            verify(jetpackActivationRepository).fetchJetpackConnectionUrl(site = site, useApplicationPasswords = true)
        }

    @Test
    fun `given connection step is ongoing confirmation in saved state, when view model is recreated, then it resumes from confirmation stage`() =
        testBlocking {
            val savedStateHandle = SavedStateHandle(
                mapOf(
                    WooPushNotificationsConnectionStepsViewModel.KEY_CURRENT_STEP to
                        WooPushNotificationsConnectionStepsViewModel.Step(
                            type = WooPushNotificationsConnectionStepsViewModel.StepType.ConnectStore,
                            state = WooPushNotificationsConnectionStepsViewModel.StepState.Ongoing
                        ),
                    WooPushNotificationsConnectionStepsViewModel.KEY_CONNECT_STORE_STAGE to
                        WooPushNotificationsConnectionStepsViewModel.ConnectStoreStage.ConfirmConnection
                )
            )
            whenever(jetpackActivationRepository.fetchJetpackSite(SITE_URL)).thenReturn(Result.success(site))

            val viewModel = createViewModel(savedStateHandle)
            advanceUntilIdle()

            val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
            assertThat(connectStoreStep.state).isEqualTo(WooPushNotificationsConnectionStepsViewModel.StepState.Success)
            clearInvocations(fetchJetpackStatus, jetpackActivationRepository)

            createViewModel(savedStateHandle)
            advanceUntilIdle()

            verifyNoInteractions(fetchJetpackStatus, jetpackActivationRepository)
        }

    @Test
    fun `when close is clicked, then Exit event is triggered`() = testBlocking {
        val viewModel = createViewModel()

        viewModel.onCloseClick()

        val event = viewModel.event.value
        assertThat(event).isEqualTo(Exit)
    }

    @Test
    fun `when contact support is clicked, then NavigateToHelpScreen event is triggered`() = testBlocking {
        val viewModel = createViewModel()

        viewModel.onContactSupportClick()

        val event = viewModel.event.value
        assertThat(event).isInstanceOf(NavigateToHelpScreen::class.java)
        assertThat((event as NavigateToHelpScreen).origin)
            .isEqualTo(HelpOrigin.WOO_PUSH_NOTIFICATIONS_SETUP)
    }
}
