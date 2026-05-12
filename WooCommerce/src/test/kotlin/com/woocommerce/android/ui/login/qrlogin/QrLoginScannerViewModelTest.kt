package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.Dispatch
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState
import com.woocommerce.android.ui.login.qrlogin.flow.AuthPhase
import com.woocommerce.android.ui.login.qrlogin.flow.ErrorReason
import com.woocommerce.android.ui.login.qrlogin.flow.FailureStep
import com.woocommerce.android.ui.login.qrlogin.flow.FlowCompletion
import com.woocommerce.android.ui.login.qrlogin.flow.FlowState
import com.woocommerce.android.ui.login.qrlogin.flow.QrLoginFlow
import com.woocommerce.android.ui.login.qrlogin.flow.QrLoginFlowFactory
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.CodeScanningErrorType
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginScannerViewModelTest : BaseUnitTest() {

    private val ticket = QrLoginPayload.Ticket(token = "tok", siteUrl = "https://store.example")
    private val parser: QrLoginPayloadParser = mock()
    private val flowFactory: QrLoginFlowFactory = mock()
    private val accountRepository: AccountRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private val fakeFlow = FakeQrLoginFlow()

    private val viewModel by lazy {
        QrLoginScannerViewModel(
            savedState = SavedStateHandle(),
            parser = parser,
            flowFactory = flowFactory,
            accountRepository = accountRepository,
            analyticsTracker = analyticsTracker,
        )
    }

    @Before
    fun setUp() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(ticket)
        whenever(accountRepository.isUserLoggedIn()).thenReturn(false)
        whenever(accountRepository.logout()).thenReturn(true)
        whenever(flowFactory.create(eq(ticket), any())).thenReturn(fakeFlow)
    }

    // region flow lifecycle

    @Test
    fun `given ticket payload, when scan succeeds, then factory creates a flow and start is invoked`() = testBlocking {
        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        verify(flowFactory).create(eq(ticket), any())
        assertThat(fakeFlow.startCount).isEqualTo(1)
    }

    @Test
    fun `given flow emits WaitingForApproval, when observed, then UiState mirrors it`() = testBlocking {
        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        fakeFlow.emit(
            FlowState.WaitingForApproval(
                sessionId = "sess-1",
                realNumber = "042",
                subtitleLabelRes = R.string.login_qr_match_host_label,
                subtitle = "store.example",
                expiresAtEpochMs = 123L,
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.WaitingForApproval
        assertThat(state.sessionId).isEqualTo("sess-1")
        assertThat(state.realNumber).isEqualTo("042")
        assertThat(state.subtitle).isEqualTo("store.example")
    }

    @Test
    fun `given flow completes with LoggedIn, when observed, then dispatches LoggedIn event`() = testBlocking {
        val events = viewModel.event.captureValues()
        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        fakeFlow.emit(FlowState.Completed(FlowCompletion.LoggedIn(localSiteId = 42)))
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(Dispatch.LoggedIn(localSiteId = 42))
        verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_SUCCESS)
    }

    @Test
    fun `given flow completes with OpenMagicLink, when observed, then dispatches magic-link event`() = testBlocking {
        val events = viewModel.event.captureValues()
        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        fakeFlow.emit(FlowState.Completed(FlowCompletion.OpenMagicLink(url = "https://wordpress.com/magic")))
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(Dispatch.OpenWpComMagicLinkUrl("https://wordpress.com/magic"))
        verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_HANDED_OFF_WP_COM_MAGIC_LINK)
    }

    @Test
    fun `given flow emits Failed, when observed, then UiState is Error with retryable flag`() = testBlocking {
        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        fakeFlow.emit(
            FlowState.Failed(
                reason = ErrorReason.Network,
                retryable = true,
                failedAt = FailureStep.Scan,
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.Network)
        assertThat(state.retryable).isTrue()
    }

    @Test
    fun `given flow emits Authenticating, when observed, then UiState mirrors phase`() = testBlocking {
        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        fakeFlow.emit(FlowState.Authenticating(AuthPhase.Exchange))
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Authenticating
        assertThat(state.phase).isEqualTo(AuthPhase.Exchange)
    }

    @Test
    fun `given flow emits Failed at scan, when observed, then VM tracks LOGIN_QR_SCAN_FAILED with step and reason`() =
        testBlocking {
            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            fakeFlow.emit(
                FlowState.Failed(
                    reason = ErrorReason.Network,
                    retryable = true,
                    failedAt = FailureStep.Scan,
                )
            )
            advanceUntilIdle()

            verify(analyticsTracker).track(
                eq(AnalyticsEvent.LOGIN_QR_SCAN_FAILED),
                any(),
                errorContext = eq(null),
                errorType = eq("Network"),
                errorDescription = eq(null),
            )
        }

    // endregion

    // region user actions delegated to flow

    @Test
    fun `given waiting for approval, when user cancels number match, then flow is cancelled and state returns to Idle`() =
        testBlocking {
            viewModel.onScanResult(successScan())
            advanceUntilIdle()
            fakeFlow.emit(
                FlowState.WaitingForApproval(
                    sessionId = "sess-1",
                    realNumber = "042",
                    subtitleLabelRes = R.string.login_qr_match_host_label,
                    subtitle = "store.example",
                    expiresAtEpochMs = 123L,
                )
            )
            advanceUntilIdle()

            viewModel.onCancelNumberMatch()

            assertThat(viewModel.uiState.value).isEqualTo(UiState.Idle)
            assertThat(fakeFlow.cancelCount).isEqualTo(1)
        }

    @Test
    fun `given error state, when user retries exchange, then flow retry is invoked`() = testBlocking {
        viewModel.onScanResult(successScan())
        advanceUntilIdle()
        fakeFlow.emit(
            FlowState.Failed(
                reason = ErrorReason.Network,
                retryable = true,
                failedAt = FailureStep.Scan,
            )
        )
        advanceUntilIdle()

        viewModel.onRetryExchange()

        assertThat(fakeFlow.retryCount).isEqualTo(1)
    }

    @Test
    fun `given error state, when user starts over, then flow is cancelled and state returns to Idle`() = testBlocking {
        viewModel.onScanResult(successScan())
        advanceUntilIdle()
        fakeFlow.emit(
            FlowState.Failed(
                reason = ErrorReason.Network,
                retryable = true,
                failedAt = FailureStep.Scan,
            )
        )
        advanceUntilIdle()

        viewModel.onStartOver()

        assertThat(viewModel.uiState.value).isEqualTo(UiState.Idle)
        assertThat(fakeFlow.cancelCount).isEqualTo(1)
    }

    // endregion

    // region non-flow payloads

    @Test
    fun `given SiteUrl payload, when scan succeeds, then dispatches RouteToSiteAddressEntry`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.SiteUrl(SITE_URL))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(Dispatch.RouteToSiteAddressEntry(siteUrl = SITE_URL))
        verify(flowFactory, never()).create(any(), any())
    }

    @Test
    fun `given WpComMagicLinkUrl payload, when scan succeeds, then dispatches OpenWpComMagicLinkUrl`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.WpComMagicLinkUrl(WP_COM_URL))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        assertThat(events.last()).isEqualTo(Dispatch.OpenWpComMagicLinkUrl(WP_COM_URL))
    }

    @Test
    fun `given AppLogin Credentials payload, when scan succeeds, then dispatches RouteToAppLoginCredentials`() =
        testBlocking {
            whenever(parser.parse(RAW_SCAN))
                .thenReturn(QrLoginPayload.AppLogin.Credentials(siteUrl = SITE_URL, username = USERNAME))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            assertThat(events.last())
                .isEqualTo(Dispatch.RouteToAppLoginCredentials(siteUrl = SITE_URL, username = USERNAME))
        }

    @Test
    fun `given InstallQrCode payload, when scan succeeds, then state is InstallQrCode error`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.InstallQrCode)

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.InstallQrCode)
    }

    @Test
    fun `given WpComToken payload, when scan succeeds, then factory creates a flow and start is invoked`() =
        testBlocking {
            val wpComPayload = QrLoginPayload.WpComToken(token = "wpc-tok", encrypted = "enc")
            whenever(parser.parse(RAW_SCAN)).thenReturn(wpComPayload)
            whenever(flowFactory.create(eq(wpComPayload), any())).thenReturn(fakeFlow)

            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            verify(flowFactory).create(eq(wpComPayload), any())
            assertThat(fakeFlow.startCount).isEqualTo(1)
        }

    @Test
    fun `given Invalid payload, when scan succeeds, then state is InvalidPayload error`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.Invalid)

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.InvalidPayload)
    }

    @Test
    fun `given camera scan failure, when handled, then state is Scanner error`() = testBlocking {
        viewModel.onScanResult(CodeScannerStatus.Failure(error = "bad", type = CodeScanningErrorType.Other(null)))
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.Scanner)
    }

    // endregion

    // region session replace warning

    @Test
    fun `given user is logged in, when ticket scanned, then state is WarningSessionReplace`() = testBlocking {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(UiState.WarningSessionReplace::class.java)
        verify(flowFactory, never()).create(any(), any())
        verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_WARNING_SHOWN)
    }

    @Test
    fun `given session-replace shown, when user confirms and logout succeeds, then flow is created and started`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            viewModel.onConfirmSessionReplace()
            advanceUntilIdle()

            verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_CONFIRMED)
            verify(flowFactory).create(eq(ticket), any())
            assertThat(fakeFlow.startCount).isEqualTo(1)
        }

    @Test
    fun `given session-replace shown, when user confirms and logout fails, then state is Network error`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(accountRepository.logout()).thenReturn(false)
            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            viewModel.onConfirmSessionReplace()
            advanceUntilIdle()

            val state = viewModel.uiState.value as UiState.Error
            assertThat(state.reason).isEqualTo(ErrorReason.Network)
            verify(flowFactory, never()).create(any(), any())
        }

    @Test
    fun `given session-replace shown, when user cancels, then state returns to Idle`() = testBlocking {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        viewModel.onCancelSessionReplace()

        assertThat(viewModel.uiState.value).isEqualTo(UiState.Idle)
        verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_DISMISSED)
    }

    // endregion

    private fun successScan() = CodeScannerStatus.Success(code = RAW_SCAN, format = BarcodeFormat.FormatQRCode)

    private class FakeQrLoginFlow : QrLoginFlow {
        private val _state = MutableStateFlow<FlowState>(FlowState.Initial)
        override val state: StateFlow<FlowState> = _state.asStateFlow()

        var startCount = 0
            private set
        var cancelCount = 0
            private set
        var retryCount = 0
            private set

        override fun start() {
            startCount++
        }

        override fun cancel() {
            cancelCount++
        }

        override fun retry() {
            retryCount++
        }

        fun emit(state: FlowState) {
            _state.value = state
        }

    }

    private companion object {
        const val RAW_SCAN = "raw"
        const val WP_COM_URL =
            "https://wordpress.com/wp-login.php?action=magic-login&scheme=woocommerce&token=abc"
        const val SITE_URL = "https://store.example.com"
        const val USERNAME = "admin"
    }
}
