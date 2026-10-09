package com.woocommerce.android.ui.login.wpcom

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackConnectionStatus
import com.woocommerce.android.model.JetpackSiteRegistrationStatus
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.notifications.push.RegisterDevice
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.ui.login.WPComLoginRepository.SMSRequestResult
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WPComLogin2FAViewModelTest : BaseUnitTest() {
    private val wpComLoginRepository: WPComLoginRepository = mock()
    private val accountRepository: AccountRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val selectedSite: SelectedSite = mock()
    private val jetpackActivationRepository: JetpackActivationRepository = mock()
    private val registerDevice: RegisterDevice = mock()

    private lateinit var viewModel: WPComLogin2FAViewModel

    @Test
    fun `given authenticator and SMS, when initialized, then show authenticator instructions`() = testBlocking {
        setup(supportedAuthTypes = arrayOf("backup", "authenticator", "sms"))

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState.instructions).isEqualTo(R.string.enter_verification_code_authenticator)
        assertThat(viewState.isSmsSupported).isTrue()
    }

    @Test
    fun `given SMS is supported, when initialized, then show SMS instructions`() = testBlocking {
        setup(supportedAuthTypes = arrayOf("backup", "sms"))

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState.instructions).isEqualTo(R.string.enter_verification_code_sms_generic)
    }

    @Test
    fun `given email is supported, when initialized, then show email instructions and hide SMS`() = testBlocking {
        setup(supportedAuthTypes = arrayOf("email"))

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState.instructions).isEqualTo(R.string.enter_verification_code_email)
        assertThat(viewState.isSmsSupported).isFalse()
    }

    @Test
    fun `given no supported methods, when initialized, then show generic instructions and SMS`() = testBlocking {
        setup()

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState.instructions).isEqualTo(R.string.enter_verification_code)
        assertThat(viewState.isSmsSupported).isTrue()
    }

    @Test
    fun `given webauthn and backup, when initialized, then show security key mode and backup option`() = testBlocking {
        setup(supportedAuthTypes = arrayOf("webauthn", "backup"))

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState.isSecurityKeyMode).isTrue()
        assertThat(viewState.showBackupCodeButton).isTrue()
        assertThat(viewState.instructions).isEqualTo(R.string.notification_security_key_needed)
    }

    @Test
    fun `given webauthn without backup, when initialized, then hide backup option`() = testBlocking {
        setup(supportedAuthTypes = arrayOf("webauthn"))

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState.isSecurityKeyMode).isTrue()
        assertThat(viewState.showBackupCodeButton).isFalse()
    }

    @Test
    fun `given webauthn and backup, when entering backup mode, then require a backup code`() = testBlocking {
        setup(supportedAuthTypes = arrayOf("webauthn", "backup"))
        val states = viewModel.viewState.captureValues()

        viewModel.onBackupCodeClick()

        val state = states.last()
        assertThat(state.isSecurityKeyMode).isFalse()
        assertThat(state.instructions).isEqualTo(R.string.enter_verification_code_backup)
        assertThat(state.showBackupCodeButton).isFalse()

        viewModel.onContinueClick()
        runCurrent()

        verify(wpComLoginRepository, never()).submitTwoStepCode(EMAIL, PASSWORD, "")
    }

    @Test
    fun `given webauthn with code methods, when initialized, then show matching code instructions`() = testBlocking {
        val methods = listOf(
            "authenticator" to R.string.enter_verification_code_authenticator,
            "sms" to R.string.enter_verification_code_sms_generic,
            "email" to R.string.enter_verification_code_email
        )

        methods.forEach { (method, instructions) ->
            setup(supportedAuthTypes = arrayOf("webauthn", method))

            val viewState = viewModel.viewState.captureValues().last()

            assertThat(viewState.isSecurityKeyMode).isFalse()
            assertThat(viewState.instructions).`as`(method).isEqualTo(instructions)
            assertThat(viewState.isSecurityKeySupported).isTrue()
        }
    }

    @Test
    fun `given restored backup mode and OTP, when initialized, then restore entered code`() = testBlocking {
        setup(
            supportedAuthTypes = arrayOf("webauthn", "backup"),
            restoredState = mapOf("is-backup-code-requested" to true, "otp" to "12345678")
        )

        val viewState = viewModel.viewState.captureValues().last()

        assertThat(viewState.isSecurityKeyMode).isFalse()
        assertThat(viewState.otp).isEqualTo("12345678")
    }

    @Test
    fun `given replacement nonce, when passkey is cancelled and retried, then use the replacement`() = testBlocking {
        whenever(wpComLoginRepository.startSecurityKeyChallenge(USER_ID, "n0")).thenReturn(
            Result.success(WPComLoginRepository.SecurityKeyChallengeData(USER_ID, "n1", "challenge-1"))
        )
        whenever(wpComLoginRepository.startSecurityKeyChallenge(USER_ID, "n1")).thenReturn(
            Result.success(WPComLoginRepository.SecurityKeyChallengeData(USER_ID, "n2", "challenge-2"))
        )
        setup(
            supportedAuthTypes = arrayOf("webauthn"),
            webauthnNonce = "n0",
            userId = USER_ID
        )

        viewModel.onSecurityKeyClick()
        runCurrent()

        viewModel.onPasskeyError()
        viewModel.onSecurityKeyClick()
        runCurrent()

        verify(wpComLoginRepository).startSecurityKeyChallenge(USER_ID, "n0")
        verify(wpComLoginRepository).startSecurityKeyChallenge(USER_ID, "n1")
    }

    @Test
    fun `when SMS request is in progress, then only SMS action shows loading`() = testBlocking {
        val requestResult = CompletableDeferred<Result<SMSRequestResult>>()
        whenever(wpComLoginRepository.requestTwoStepSMS(EMAIL, PASSWORD)).doSuspendableAnswer {
            requestResult.await()
        }
        setup()
        val states = viewModel.viewState.captureValues()

        viewModel.onSmsButtonClick()
        runCurrent()
        viewModel.onSmsButtonClick()
        runCurrent()

        assertThat(states.last().isRequestingSms).isTrue()
        assertThat(states.last().loadingMessage).isNull()
        assertThat(states.last().enableSubmit).isFalse()
        verify(wpComLoginRepository).requestTwoStepSMS(EMAIL, PASSWORD)

        requestResult.complete(Result.success(SMSRequestResult.SMSRequested))
        runCurrent()

        assertThat(states.last().isRequestingSms).isFalse()
    }

    @Test
    fun `when SMS request succeeds, then show sent state and success message`() = testBlocking {
        whenever(wpComLoginRepository.requestTwoStepSMS(EMAIL, PASSWORD))
            .thenReturn(Result.success(SMSRequestResult.SMSRequested))
        setup()
        val states = viewModel.viewState.captureValues()
        val events = viewModel.event.captureValues()

        viewModel.onSmsButtonClick()

        assertThat(states.last().hasRequestedSms).isTrue()
        assertThat(states.last().isRequestingSms).isFalse()
        assertThat(states.last().instructions).isEqualTo(R.string.enter_verification_code_sms_generic)
        assertThat(events.last()).isEqualTo(ShowSnackbar(R.string.requesting_sms_otp_success))
    }

    @Test
    fun `when SMS request fails, then show failure message`() = testBlocking {
        whenever(wpComLoginRepository.requestTwoStepSMS(EMAIL, PASSWORD)).thenReturn(
            Result.failure(IllegalStateException())
        )
        setup()
        val states = viewModel.viewState.captureValues()
        val events = viewModel.event.captureValues()

        viewModel.onSmsButtonClick()

        assertThat(states.last().hasRequestedSms).isFalse()
        assertThat(states.last().isRequestingSms).isFalse()
        assertThat(events.last()).isEqualTo(ShowSnackbar(R.string.requesting_sms_otp_failure))
    }

    private fun setup(
        supportedAuthTypes: Array<String> = emptyArray(),
        restoredState: Map<String, Any> = emptyMap(),
        webauthnNonce: String = "",
        userId: String = ""
    ) {
        val savedStateHandle = WPComLogin2FAFragmentArgs(
            jetpackStatus = JETPACK_STATUS,
            emailOrUsername = EMAIL,
            password = PASSWORD,
            userId = userId,
            webauthnNonce = webauthnNonce,
            supportedAuthTypes = supportedAuthTypes
        ).toSavedStateHandle()
        restoredState.forEach { (key, value) -> savedStateHandle[key] = value }

        viewModel = WPComLogin2FAViewModel(
            savedStateHandle = savedStateHandle,
            selectedSite = selectedSite,
            jetpackAccountRepository = jetpackActivationRepository,
            wpComLoginRepository = wpComLoginRepository,
            accountRepository = accountRepository,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            registerDevice = registerDevice
        )
    }

    private companion object {
        const val EMAIL = "user@example.com"
        const val PASSWORD = "password123"
        const val USER_ID = "user-1"
        val JETPACK_STATUS = JetpackStatus(
            isJetpackInstalled = true,
            jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
                blogId = 1
            )
        )
    }
}
