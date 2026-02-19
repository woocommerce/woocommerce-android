package com.woocommerce.android.ui.pushnotifications.connection

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.UiString
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.StepState
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.getOrAwaitValue
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
        private val EXPECTED_DEFAULT_CONNECTION_STATUS = JetpackConnectionStatus.AccountNotConnected(
            siteRegistrationStatus = JetpackSiteRegistrationStatus.NOT_REGISTERED,
            blogId = null
        )
    }

    private val site = SiteModel().apply {
        url = SITE_URL
        name = "example.com"
    }

    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }

    private val jetpackActivationRepository: JetpackActivationRepository = mock()
    private val stringUtils: StringUtils = mock {
        on { getSiteDomainAndPath(site) } doReturn "example.com"
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ) = WooPushNotificationsConnectionStepsViewModel(
        selectedSite = selectedSite,
        jetpackActivationRepository = jetpackActivationRepository,
        stringUtils = stringUtils,
        savedStateHandle = savedStateHandle
    )

    @Test
    fun `when connect and confirm succeed, then connect step is complete`() =
        testBlocking {
            whenever(
                jetpackActivationRepository.connectJetpackAccount(
                    site = site,
                    jetpackConnectionStatus = EXPECTED_DEFAULT_CONNECTION_STATUS,
                    useApplicationPasswords = true
                )
            ).thenReturn(Result.success(Unit))
            whenever(jetpackActivationRepository.fetchJetpackSite(SITE_URL)).thenReturn(Result.success(site))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
            assertThat(connectStoreStep.state).isEqualTo(StepState.Success)
            verify(jetpackActivationRepository).connectJetpackAccount(
                site = site,
                jetpackConnectionStatus = EXPECTED_DEFAULT_CONNECTION_STATUS,
                useApplicationPasswords = true
            )
            verify(jetpackActivationRepository).fetchJetpackSite(SITE_URL)
        }

    @Test
    fun `given connect account fails with forbidden, when screen starts, then permission error is shown`() =
        testBlocking {
            whenever(
                jetpackActivationRepository.connectJetpackAccount(
                    site = site,
                    jetpackConnectionStatus = EXPECTED_DEFAULT_CONNECTION_STATUS,
                    useApplicationPasswords = true
                )
            ).thenReturn(Result.failure(forbiddenError("Connection failed")))

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertErrorMessage(
                viewModel = viewModel,
                messageRes = R.string.woo_push_notifications_connection_steps_error_connection_permission_message
            )
        }

    @Test
    fun `given connect account fails with generic error, when screen starts, then generic error is shown`() =
        testBlocking {
            whenever(
                jetpackActivationRepository.connectJetpackAccount(
                    site = site,
                    jetpackConnectionStatus = EXPECTED_DEFAULT_CONNECTION_STATUS,
                    useApplicationPasswords = true
                )
            ).thenReturn(Result.failure(IllegalStateException("Connection failed")))

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertErrorMessage(
                viewModel = viewModel,
                messageRes = R.string.woo_push_notifications_connection_steps_generic_error_message
            )
        }

    @Test
    fun `given fetch jetpack site fails during connection confirmation, when screen starts, then generic error is shown`() =
        testBlocking {
            whenever(
                jetpackActivationRepository.connectJetpackAccount(
                    site = site,
                    jetpackConnectionStatus = EXPECTED_DEFAULT_CONNECTION_STATUS,
                    useApplicationPasswords = true
                )
            ).thenReturn(Result.success(Unit))
            whenever(jetpackActivationRepository.fetchJetpackSite(SITE_URL))
                .thenReturn(Result.failure(IllegalStateException("Site missing")))

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertErrorMessage(
                viewModel = viewModel,
                messageRes = R.string.woo_push_notifications_connection_steps_generic_error_message
            )
        }

    @Test
    fun `given connection step is already complete in saved state, when recreated, then no calls are retriggered`() =
        testBlocking {
            val savedStateHandle = SavedStateHandle(
                mapOf(
                    WooPushNotificationsConnectionStepsViewModel.KEY_CURRENT_STEP to
                        WooPushNotificationsConnectionStepsViewModel.Step(
                            type = WooPushNotificationsConnectionStepsViewModel.StepType.ConnectStore,
                            state = StepState.Success
                        ),
                    WooPushNotificationsConnectionStepsViewModel.KEY_CONNECT_STORE_STAGE to
                        WooPushNotificationsConnectionStepsViewModel.ConnectStoreStage.ConfirmConnection
                )
            )

            createViewModel(savedStateHandle)
            advanceUntilIdle()

            verifyNoInteractions(jetpackActivationRepository)
        }

    @Test
    fun `given connection confirmation stage in saved state, when recreated after success, then no network call is retriggered`() =
        testBlocking {
            val savedStateHandle = SavedStateHandle(
                mapOf(
                    WooPushNotificationsConnectionStepsViewModel.KEY_CURRENT_STEP to
                        WooPushNotificationsConnectionStepsViewModel.Step(
                            type = WooPushNotificationsConnectionStepsViewModel.StepType.ConnectStore,
                            state = StepState.Ongoing
                        ),
                    WooPushNotificationsConnectionStepsViewModel.KEY_CONNECT_STORE_STAGE to
                        WooPushNotificationsConnectionStepsViewModel.ConnectStoreStage.ConfirmConnection
                )
            )
            whenever(jetpackActivationRepository.fetchJetpackSite(SITE_URL)).thenReturn(Result.success(site))

            val firstViewModel = createViewModel(savedStateHandle)
            advanceUntilIdle()
            val connectStoreStep = firstViewModel.viewState.getOrAwaitValue().steps.first()
            assertThat(connectStoreStep.state).isEqualTo(StepState.Success)

            clearInvocations(jetpackActivationRepository)

            createViewModel(savedStateHandle)
            advanceUntilIdle()

            verifyNoInteractions(jetpackActivationRepository)
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

    private fun forbiddenError(message: String): OnChangedException {
        val jetpackError = JetpackStore.JetpackError(
            message = message,
            errorCode = 403
        )
        return OnChangedException(jetpackError)
    }

    private fun assertErrorMessage(
        viewModel: WooPushNotificationsConnectionStepsViewModel,
        messageRes: Int
    ) {
        val connectStoreStep = viewModel.viewState.getOrAwaitValue().steps.first()
        assertThat(connectStoreStep.state).isInstanceOf(StepState.Error::class.java)
        assertThat((connectStoreStep.state as StepState.Error).errorMessage)
            .isEqualTo(UiString.UiStringRes(messageRes))
    }
}
