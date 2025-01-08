package com.woocommerce.android.ui.login.jetpack.wpcom

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.ui.login.jetpack.wpcom.JetpackActivationWPComEmailViewModel.ShowMagicLinkScreen
import com.woocommerce.android.ui.login.jetpack.wpcom.JetpackActivationWPComEmailViewModel.ShowPasswordScreen
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.AccountStore.AuthOptionsError
import org.wordpress.android.fluxc.store.AccountStore.AuthOptionsErrorType
import org.wordpress.android.login.AuthOptions
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class JetpackActivationWPComEmailViewModelTest : BaseUnitTest() {
    companion object {
        const val WPCOM_EMAIL = "wpcomUser@gmail.com"
        const val UNKNOWN_EMAIL = "newUser@example.com"
        const val UNKNOWN_USERNAME = "newUser"
        val JETPACK_STATUS = JetpackStatus(
            isJetpackInstalled = true,
            isJetpackConnected = false,
            wpComEmail = ""
        )
    }

    private val saveStateHandle = JetpackActivationWPComEmailFragmentArgs(
        jetpackStatus = JETPACK_STATUS,
    ).toSavedStateHandle()
    private val wpComLoginRepository: WPComLoginRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val stringUtils: StringUtils = mock()
    private lateinit var viewModel: JetpackActivationWPComEmailViewModel

    fun setup(wpComEmail: String) {
        saveStateHandle["email"] = wpComEmail
        viewModel = JetpackActivationWPComEmailViewModel(
            savedStateHandle = saveStateHandle,
            wpComLoginRepository = wpComLoginRepository,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            stringUtils = stringUtils
        )
    }

    @Test
    fun `when close is clicked, then trigger Exit event and clear access token`() = testBlocking {
        setup(WPCOM_EMAIL)
        val event = viewModel.event.runAndCaptureValues {
            viewModel.onCloseClick()
        }.last()

        assertTrue(event is MultiLiveEvent.Event.Exit)
        verify(wpComLoginRepository).clearAccessToken()
    }

    @Test
    fun `given email is not WPcom, when onContinueClick clicked, then trigger ShowMagicLinkScreen with new wpcom account true`() =
        testBlocking {
            setup(UNKNOWN_EMAIL)
            whenever(stringUtils.isValidEmail(UNKNOWN_EMAIL)).thenReturn(true)
            whenever(wpComLoginRepository.fetchAuthOptions(UNKNOWN_EMAIL))
                .thenReturn(
                    Result.failure(OnChangedException(AuthOptionsError(AuthOptionsErrorType.UNKNOWN_USER, "")))
                )

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onContinueClick()
            }.last()

            assertThat(event).isEqualTo(
                ShowMagicLinkScreen(
                    UNKNOWN_EMAIL,
                    JETPACK_STATUS,
                    isNewWpComAccount = true
                )
            )
        }

    @Test
    fun `given username is not WPcom, when onContinueClick clicked, then show user name not wpcom error`() =
        testBlocking {
            setup(UNKNOWN_USERNAME)
            whenever(stringUtils.isValidEmail(UNKNOWN_USERNAME)).thenReturn(false)
            whenever(wpComLoginRepository.fetchAuthOptions(UNKNOWN_USERNAME)).thenReturn(
                Result.failure(
                    OnChangedException(
                        AuthOptionsError(AuthOptionsErrorType.UNKNOWN_USER, "")
                    )
                )
            )

            val state = viewModel.viewState.runAndCaptureValues {
                viewModel.onContinueClick()
            }.last()

            assertThat(state.errorMessage).isEqualTo(R.string.username_not_registered_wpcom)
        }

    @Test
    fun `given email not allowed, when onContinueClick clicked, then show user name not wpcom error`() =
        testBlocking {
            setup(UNKNOWN_USERNAME)
            whenever(wpComLoginRepository.fetchAuthOptions(UNKNOWN_USERNAME)).thenReturn(
                Result.failure(
                    OnChangedException(
                        AuthOptionsError(AuthOptionsErrorType.EMAIL_LOGIN_NOT_ALLOWED, "")
                    )
                )
            )

            val state = viewModel.viewState.runAndCaptureValues {
                viewModel.onContinueClick()
            }.last()

            assertThat(state.errorMessage).isEqualTo(R.string.error_user_username_instead_of_email)
        }

    @Test
    fun `given email is WPcom user, when onContinueClick clicked, then trigger ShowPasswordScreen`() =
        testBlocking {
            setup(WPCOM_EMAIL)
            whenever(wpComLoginRepository.fetchAuthOptions(WPCOM_EMAIL)).thenReturn(
                Result.success(
                    AuthOptions(
                        isPasswordless = false,
                        isEmailVerified = true
                    )
                )
            )

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onContinueClick()
            }.last()

            assertThat(event).isEqualTo(
                ShowPasswordScreen(
                    WPCOM_EMAIL,
                    JETPACK_STATUS
                )
            )
        }

    @Test
    fun `given email is WPcom passwordless user, when onContinueClick clicked, then trigger ShowPasswordScreen`() =
        testBlocking {
            setup(WPCOM_EMAIL)
            whenever(wpComLoginRepository.fetchAuthOptions(WPCOM_EMAIL)).thenReturn(
                Result.success(
                    AuthOptions(
                        isPasswordless = true,
                        isEmailVerified = true
                    )
                )
            )

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onContinueClick()
            }.last()

            assertThat(event).isEqualTo(
                ShowMagicLinkScreen(
                    WPCOM_EMAIL,
                    JETPACK_STATUS,
                    isNewWpComAccount = false
                )
            )
        }
}
