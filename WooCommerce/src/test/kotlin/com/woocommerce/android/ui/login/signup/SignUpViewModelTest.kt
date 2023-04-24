package com.woocommerce.android.ui.login.signup

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.login.signup.SignUpFragment.NextStep
import com.woocommerce.android.ui.login.signup.SignUpRepository.AccountCreationError
import com.woocommerce.android.ui.login.signup.SignUpRepository.AccountCreationSuccess
import com.woocommerce.android.ui.login.signup.SignUpRepository.SignUpError
import com.woocommerce.android.ui.login.signup.SignUpViewModel.ErrorType.UNKNOWN
import com.woocommerce.android.ui.login.signup.SignUpViewModel.OnAccountCreated
import com.woocommerce.android.ui.login.signup.SignUpViewModel.OnLoginWithEmail
import com.woocommerce.android.ui.login.signup.SignUpViewModel.SignUpErrorUi
import com.woocommerce.android.ui.login.signup.SignUpViewModel.SignUpState
import com.woocommerce.android.ui.login.signup.SignUpViewModel.SignUpStepType.EMAIL
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class SignUpViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    private companion object {
        private const val VALID_EMAIL = "validEmail@gmail.com"
        private const val VALID_PASSWORD = "sdfgbds23453t"
        private const val VALID_EMAIL_WITH_WHITE_SPACES = "validEmail@gmail.com "
        private val ACCOUNT_CREATION_EMAIL_EXIST_ERROR = AccountCreationError(SignUpError.EMAIL_EXIST)
        private val ACCOUNT_CREATION_ERROR_PASSWORD_INVALID = AccountCreationError(SignUpError.PASSWORD_INVALID)
        private val ACCOUNT_CREATION_ERROR_UNKNOWN = AccountCreationError(SignUpError.UNKNOWN_ERROR)
    }

    private val savedStateHandle: SavedStateHandle = mock()
    private val signUpRepository: SignUpRepository = mock()
    private val networkStatus: NetworkStatus = mock()
    private val appPrefs: AppPrefsWrapper = mock()
    private val signUpCredentialsValidator: SignUpCredentialsValidator = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private val viewModel = SignUpViewModel(
        savedStateHandle,
        signUpRepository,
        networkStatus,
        appPrefs,
        signUpCredentialsValidator,
        analyticsTrackerWrapper
    )

    @Before
    fun setup() {
        whenever(networkStatus.isConnected()).thenReturn(true)
    }

    @Test
    fun `when viewmodel is initiallised, then shows enter email screen`() {
        testBlocking {
            assertThat(viewModel.viewState.value).isEqualTo(SignUpState(stepType = EMAIL))
        }
    }

    @Test
    fun `when email with white spaces is entered, then email is trimmed`() {
        testBlocking {
            // When
            viewModel.onEmailInputChanged(VALID_EMAIL_WITH_WHITE_SPACES)

            assertThat(viewModel.viewState.value).isEqualTo(
                viewModel.viewState.value?.copy(
                    email = VALID_EMAIL,
                    error = null
                )
            )
        }
    }

    @Test
    fun `given a valid email, when continue is clicked, then trigger account creation to verify if email exist`() {
        testBlocking {
            // Given
            viewModel.onEmailInputChanged(VALID_EMAIL)
            whenever(signUpCredentialsValidator.isEmailValid(VALID_EMAIL)).thenReturn(true)

            // When
            viewModel.onEmailContinueClicked()
            advanceUntilIdle()

            // Then
            verify(signUpRepository).createAccount(VALID_EMAIL, "")
        }
    }

    @Test
    fun `given email is not an existing wordpress account, when continue is clicked, then navigate to password step`() {
        testBlocking {
            // Given
            viewModel.onEmailInputChanged(VALID_EMAIL)
            whenever(signUpCredentialsValidator.isEmailValid(VALID_EMAIL)).thenReturn(true)
            whenever(signUpRepository.createAccount(VALID_EMAIL, ""))
                .thenReturn(ACCOUNT_CREATION_ERROR_PASSWORD_INVALID)

            // When
            viewModel.onEmailContinueClicked()
            advanceUntilIdle()

            // Then
            assertThat(viewModel.viewState.value).isEqualTo(
                SignUpState(
                    stepType = SignUpViewModel.SignUpStepType.PASSWORD,
                    email = VALID_EMAIL,
                    password = "",
                    isLoading = false,
                    error = null,
                )
            )
        }
    }

    @Test
    fun `given email already exist, when continue is clicked, then navigate to log in with wordpress email screen`() {
        testBlocking {
            // Given
            viewModel.onEmailInputChanged(VALID_EMAIL)
            whenever(signUpCredentialsValidator.isEmailValid(VALID_EMAIL)).thenReturn(true)
            whenever(signUpRepository.createAccount(VALID_EMAIL, ""))
                .thenReturn(ACCOUNT_CREATION_EMAIL_EXIST_ERROR)

            // When
            viewModel.onEmailContinueClicked()
            advanceUntilIdle()

            // Then
            assertThat(viewModel.event.value).isEqualTo(OnLoginWithEmail(VALID_EMAIL))
            assertThat(viewModel.viewState.value).isEqualTo(
                SignUpState(
                    stepType = SignUpViewModel.SignUpStepType.EMAIL,
                    email = VALID_EMAIL,
                    password = "",
                    isLoading = false,
                    error = null,
                )
            )
        }
    }

    @Test
    fun `given a valid email and password, when continue is clicked, then trigger account creation and track`() {
        testBlocking {
            // Given
            viewModel.onEmailInputChanged(VALID_EMAIL)
            viewModel.onPasswordInputChanged(VALID_PASSWORD)
            whenever(signUpCredentialsValidator.validatePassword(VALID_PASSWORD)).thenReturn(null)

            // When
            viewModel.onPasswordContinueClicked()
            advanceUntilIdle()

            // Then
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.SIGNUP_SUBMITTED)
            verify(signUpRepository).createAccount(VALID_EMAIL, VALID_PASSWORD)
        }
    }

    @Test
    fun `given valid credentials, when failing to create account, then display error and track error`() {
        testBlocking {
            // Given
            whenever(signUpCredentialsValidator.isEmailValid(VALID_EMAIL)).thenReturn(true)
            whenever(signUpCredentialsValidator.validatePassword(VALID_PASSWORD)).thenReturn(null)
            whenever(signUpRepository.createAccount(VALID_EMAIL, VALID_PASSWORD))
                .thenReturn(ACCOUNT_CREATION_ERROR_UNKNOWN)

            // When
            viewModel.onEmailInputChanged(VALID_EMAIL)
            viewModel.onEmailContinueClicked()
            viewModel.onPasswordInputChanged(VALID_PASSWORD)
            viewModel.onPasswordContinueClicked()
            advanceUntilIdle()

            // Then
            assertThat(viewModel.viewState.value).isEqualTo(
                SignUpState(
                    stepType = SignUpViewModel.SignUpStepType.PASSWORD,
                    email = VALID_EMAIL,
                    password = VALID_PASSWORD,
                    isLoading = false,
                    error = SignUpErrorUi(
                        type = UNKNOWN,
                        stringId = string.signup_api_generic_error
                    )
                )
            )
            verify(analyticsTrackerWrapper).track(
                stat = AnalyticsEvent.SIGNUP_ERROR,
                properties = mapOf(AnalyticsTracker.KEY_ERROR_TYPE to ACCOUNT_CREATION_ERROR_UNKNOWN.error.name)
            )
        }
    }

    @Test
    fun `given valid credentials, when account created, then trigger account created event and track success`() {
        testBlocking {
            // Given
            whenever(signUpCredentialsValidator.isEmailValid(VALID_EMAIL)).thenReturn(true)
            whenever(signUpCredentialsValidator.validatePassword(VALID_PASSWORD)).thenReturn(null)
            whenever(signUpRepository.createAccount(VALID_EMAIL, VALID_PASSWORD)).thenReturn(AccountCreationSuccess)
            viewModel.nextStep = NextStep.STORE_CREATION

            // When
            viewModel.onEmailInputChanged(VALID_EMAIL)
            viewModel.onEmailContinueClicked()
            viewModel.onPasswordInputChanged(VALID_PASSWORD)
            viewModel.onPasswordContinueClicked()
            advanceUntilIdle()

            // Then
            assertThat(viewModel.event.value).isEqualTo(OnAccountCreated)
            verify(analyticsTrackerWrapper).track(stat = AnalyticsEvent.SIGNUP_SUCCESS)
            verify(appPrefs).markAsNewSignUp(true)
        }
    }
}
