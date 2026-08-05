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

    private fun setup() {
        val savedStateHandle = WPComLogin2FAFragmentArgs(
            jetpackStatus = JETPACK_STATUS,
            emailOrUsername = EMAIL,
            password = PASSWORD,
            userId = "",
            webauthnNonce = "",
            supportedAuthTypes = emptyArray()
        ).toSavedStateHandle()

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
        val JETPACK_STATUS = JetpackStatus(
            isJetpackInstalled = true,
            jetpackConnectionStatus = JetpackConnectionStatus.AccountNotConnected(
                siteRegistrationStatus = JetpackSiteRegistrationStatus.REGISTERED,
                blogId = 1
            )
        )
    }
}
