package com.woocommerce.android.ui.pushnotifications.connection

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.StepState
import com.woocommerce.android.ui.pushnotifications.connection.WooPushNotificationsConnectionStepsViewModel.StepType
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndGetValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPushNotificationsConnectionStepsViewModelTest : BaseUnitTest() {
    private val site = SiteModel().apply {
        name = "coffeebeans.com"
        url = "https://coffeebeans.com"
    }

    private lateinit var viewModel: WooPushNotificationsConnectionStepsViewModel

    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val pushNotificationRepository: PushNotificationRepository = mock()
    private val jetpackActivationRepository: JetpackActivationRepository = mock()
    private val stringUtils: StringUtils = mock()

    private suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        whenever(jetpackActivationRepository.registerSite(any(), any()))
            .thenReturn(Result.success(1L))
        whenever(stringUtils.getSiteDomainAndPath(site))
            .thenReturn(site.name)
        prepareMocks()
        viewModel = WooPushNotificationsConnectionStepsViewModel(
            selectedSite = selectedSite,
            appPrefsWrapper = appPrefsWrapper,
            pushNotificationRepository = pushNotificationRepository,
            jetpackActivationRepository = jetpackActivationRepository,
            stringUtils = stringUtils,
            savedStateHandle = SavedStateHandle()
        )
    }

    @Test
    fun `when initialized, then steps are shown in expected order`() {
        testBlocking {
            setup()

            val state = viewModel.viewState.getOrAwaitValue()

            assertThat(state.steps).hasSize(3)
            assertThat(state.steps[0].type).isEqualTo(StepType.CheckPluginCompatibility)
            assertThat(state.steps[1].type).isEqualTo(StepType.ConnectStore)
            assertThat(state.steps[2].type).isEqualTo(StepType.EnablePushNotifications)
        }
    }

    @Test
    fun `when initialized, then isDone is false`() {
        testBlocking {
            setup()

            val state = viewModel.viewState.getOrAwaitValue()

            assertThat(state.isDone).isFalse()
        }
    }

    @Test
    fun `when initialized, then site address is set`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
            whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any()))
                .thenReturn(Result.success(Unit))
        }

        val state = viewModel.viewState.getOrAwaitValue()

        assertThat(state.siteAddress).isEqualTo("coffeebeans.com")
    }

    @Test
    fun `when close is clicked, then Exit event is triggered`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
            whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any()))
                .thenReturn(Result.success(Unit))
        }

        viewModel.onCloseClick()

        val event = viewModel.event.value
        assertThat(event).isEqualTo(Exit)
    }

    @Test
    fun `when contact support is clicked, then NavigateToHelpScreen event is triggered`() = testBlocking {
        setup()

        viewModel.onContactSupportClick()

        val event = viewModel.event.value
        assertThat(event).isInstanceOf(NavigateToHelpScreen::class.java)
        assertThat((event as NavigateToHelpScreen).origin)
            .isEqualTo(HelpOrigin.WOO_PUSH_NOTIFICATIONS_SETUP)
    }

    @Test
    fun `given register site returns forbidden, when ConnectStore runs, then permission error is shown`() =
        testBlocking {
            setup {
                whenever(jetpackActivationRepository.registerSite(any(), any()))
                    .thenReturn(
                        Result.failure(
                            OnChangedException(
                                JetpackStore.JetpackError(
                                    message = "Forbidden",
                                    errorCode = 403
                                )
                            )
                        )
                    )
            }

            val state = viewModel.viewState.runAndGetValue {
                advanceUntilIdle()
            }

            assertThat(state.steps[1].type).isEqualTo(StepType.ConnectStore)
            assertThat(state.steps[1].state)
                .isEqualTo(
                    StepState.Error(
                        R.string.woo_push_notifications_connection_steps_error_connection_permission_message
                    )
                )
        }

    @Test
    fun `given register site fails, when ConnectStore runs, then generic error is shown`() = testBlocking {
        setup {
            whenever(jetpackActivationRepository.registerSite(any(), any()))
                .thenReturn(Result.failure(Exception("registration failed")))
        }

        val state = viewModel.viewState.runAndGetValue {
            advanceUntilIdle()
        }

        assertThat(state.steps[1].type).isEqualTo(StepType.ConnectStore)
        assertThat(state.steps[1].state)
            .isEqualTo(StepState.Error(R.string.woo_push_notifications_connection_steps_generic_error))
    }

    @Test
    fun `when connect store succeeds, then flow advances to enable push notifications step`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("")
        }

        val state = viewModel.viewState.runAndGetValue {
            advanceUntilIdle()
        }

        assertThat(state.steps[0].type).isEqualTo(StepType.CheckPluginCompatibility)
        assertThat(state.steps[0].state).isEqualTo(StepState.Success)
        assertThat(state.steps[1].type).isEqualTo(StepType.ConnectStore)
        assertThat(state.steps[1].state).isEqualTo(StepState.Success)
        assertThat(state.steps[2].type).isEqualTo(StepType.EnablePushNotifications)
        assertThat(state.steps[2].state).isInstanceOf(StepState.Error::class.java)
    }

    @Test
    fun `given push registration fails, when EnablePushNotifications runs, then step is Error`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
            whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any()))
                .thenReturn(Result.failure(Exception("registration failed")))
        }

        val state = viewModel.viewState.runAndGetValue {
            advanceUntilIdle()
        }

        assertThat(state.steps[2].type).isEqualTo(StepType.EnablePushNotifications)
        assertThat(state.steps[2].state).isInstanceOf(StepState.Error::class.java)
    }

    @Test
    fun `given empty FCM token, when EnablePushNotifications runs, then step is Error`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("")
        }

        val state = viewModel.viewState.runAndGetValue {
            advanceUntilIdle()
        }

        assertThat(state.steps[2].type).isEqualTo(StepType.EnablePushNotifications)
        assertThat(state.steps[2].state).isInstanceOf(StepState.Error::class.java)
    }

    @Test
    fun `given push registration fails then succeeds on retry, when retry clicked, then isDone is true`() =
        testBlocking {
            setup {
                whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
                whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any()))
                    .thenReturn(Result.failure(Exception("registration failed")))
            }

            val errorState = viewModel.viewState.runAndGetValue {
                advanceUntilIdle()
            }
            assertThat(errorState.steps[2].state).isInstanceOf(StepState.Error::class.java)

            whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any()))
                .thenReturn(Result.success(Unit))
            viewModel.onRetryClick()

            val successState = viewModel.viewState.getOrAwaitValue()
            assertThat(successState.isDone).isTrue()
        }

    @Test
    fun `when all steps succeed, then CheckPluginCompatibility and ConnectStore show as Success`() = testBlocking {
        setup {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("test-token")
            whenever(pushNotificationRepository.registerPushTokenInWooCoreSystem(any(), any()))
                .thenReturn(Result.success(Unit))
        }

        val state = viewModel.viewState.runAndGetValue {
            advanceUntilIdle()
        }

        assertThat(state.steps[0].type).isEqualTo(StepType.CheckPluginCompatibility)
        assertThat(state.steps[0].state).isEqualTo(StepState.Success)
        assertThat(state.steps[1].type).isEqualTo(StepType.ConnectStore)
        assertThat(state.steps[1].state).isEqualTo(StepState.Success)
    }
}
