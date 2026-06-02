package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
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
import org.mockito.kotlin.inOrder
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
    private val unifiedLoginTracker: UnifiedLoginTracker = mock()

    private val fakeFlow = FakeQrLoginFlow()

    private val viewModel by lazy {
        QrLoginScannerViewModel(
            savedState = SavedStateHandle(),
            parser = parser,
            flowFactory = flowFactory,
            accountRepository = accountRepository,
            analyticsTracker = analyticsTracker,
            unifiedLoginTracker = unifiedLoginTracker,
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
    fun `given flow emits WaitingForApproval, when observed, then UiState mirrors it and step is tracked`() =
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

            val state = viewModel.uiState.value as UiState.WaitingForApproval
            assertThat(state.sessionId).isEqualTo("sess-1")
            assertThat(state.realNumber).isEqualTo("042")
            assertThat(state.subtitle).isEqualTo("store.example")
            verify(unifiedLoginTracker).track(Flow.LOGIN_QR, Step.QR_NUMBER_MATCH)
        }

    @Test
    fun `given flow completes with LoggedIn, when observed, then dispatches LoggedIn event and no QR success event is fired`() =
        testBlocking {
            val events = viewModel.event.captureValues()
            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            fakeFlow.emit(FlowState.Completed(FlowCompletion.LoggedIn(localSiteId = 42)))
            advanceUntilIdle()

            assertThat(events.last()).isEqualTo(Dispatch.LoggedIn(localSiteId = 42))
            // SitePickerViewModel fires UNIFIED_LOGIN_STEP(SUCCESS) for every flow — the QR VM
            // must not emit its own success event on completion.
            verify(unifiedLoginTracker, never()).track(any(), eq(Step.SUCCESS))
        }

    @Test
    fun `given flow completes with OpenMagicLink, when observed, then dispatches magic-link event and joins magic-link funnel`() =
        testBlocking {
            val events = viewModel.event.captureValues()
            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            fakeFlow.emit(FlowState.Completed(FlowCompletion.OpenMagicLink(url = "https://wordpress.com/magic")))
            advanceUntilIdle()

            assertThat(events.last()).isEqualTo(Dispatch.OpenWpComMagicLinkUrl("https://wordpress.com/magic"))
            verify(unifiedLoginTracker).track(Flow.LOGIN_MAGIC_LINK, Step.MAGIC_LINK_REQUESTED)
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
    fun `given flow emits Authenticating, when observed, then UiState mirrors phase and step is tracked once`() =
        testBlocking {
            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            fakeFlow.emit(FlowState.Authenticating(AuthPhase.Exchange))
            advanceUntilIdle()

            val state = viewModel.uiState.value as UiState.Authenticating
            assertThat(state.phase).isEqualTo(AuthPhase.Exchange)
            verify(unifiedLoginTracker).track(Flow.LOGIN_QR, Step.QR_AUTHENTICATING)
        }

    @Test
    fun `given consecutive Authenticating phases, when observed, then QR_AUTHENTICATING is emitted only on first transition`() =
        testBlocking {
            // After the first emission the tracker reports the step it just received — return
            // QR_AUTHENTICATING so the VM's dedup gate sees the duplicate and skips the second emit.
            whenever(unifiedLoginTracker.currentStep).thenReturn(null, Step.QR_AUTHENTICATING, Step.QR_AUTHENTICATING)
            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            fakeFlow.emit(FlowState.Authenticating(AuthPhase.Scan))
            advanceUntilIdle()
            fakeFlow.emit(FlowState.Authenticating(AuthPhase.Exchange))
            advanceUntilIdle()
            fakeFlow.emit(FlowState.Authenticating(AuthPhase.Complete))
            advanceUntilIdle()

            verify(unifiedLoginTracker, org.mockito.kotlin.times(1))
                .track(Flow.LOGIN_QR, Step.QR_AUTHENTICATING)
        }

    @Test
    fun `given flow emits Failed at scan, when observed, then VM tracks unified failure with reason and phase`() =
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

            inOrder(unifiedLoginTracker).apply {
                verify(unifiedLoginTracker).track(Flow.LOGIN_QR, Step.QR_ERROR)
                verify(unifiedLoginTracker).trackFailure("Network:Scan")
            }
        }

    @Test
    fun `given camera scan failure, when handled, then VM tracks unified failure with Scanner reason`() = testBlocking {
        viewModel.onScanResult(CodeScannerStatus.Failure(error = "bad", type = CodeScanningErrorType.Other(null)))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            UiState.Error(
                reason = ErrorReason.Scanner,
                retryable = false,
            )
        )
        verify(unifiedLoginTracker).track(Flow.LOGIN_QR, Step.QR_ERROR)
        verify(unifiedLoginTracker).trackFailure("Scanner")
    }

    // endregion

    // region user actions delegated to flow

    @Test
    fun `given waiting for approval, when user cancels number match, then flow is cancelled and click is tracked`() =
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
            verify(unifiedLoginTracker).trackClick(Click.QR_CANCEL_NUMBER_MATCH)
        }

    @Test
    fun `given error state, when user retries exchange, then flow retry is invoked and click is tracked`() =
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

            viewModel.onRetryExchange()

            assertThat(fakeFlow.retryCount).isEqualTo(1)
            verify(unifiedLoginTracker).trackClick(Click.QR_RETRY)
        }

    @Test
    fun `given error state, when user starts over, then flow is cancelled and click is tracked`() = testBlocking {
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
        verify(unifiedLoginTracker).trackClick(Click.QR_START_OVER)
    }

    // endregion

    // region non-flow payloads

    @Test
    fun `given SiteUrl payload, when scan succeeds, then dispatches RouteToSiteAddressEntry with no QR event`() =
        testBlocking {
            whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.SiteUrl(SITE_URL))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            assertThat(events.last()).isEqualTo(Dispatch.RouteToSiteAddressEntry(siteUrl = SITE_URL))
            verify(flowFactory, never()).create(any(), any())
        }

    @Test
    fun `given WpComMagicLinkUrl payload, when scan succeeds, then dispatches OpenWpComMagicLinkUrl and joins magic-link funnel`() =
        testBlocking {
            whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.WpComMagicLinkUrl(WP_COM_URL))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            assertThat(events.last()).isEqualTo(Dispatch.OpenWpComMagicLinkUrl(WP_COM_URL))
            verify(unifiedLoginTracker).track(Flow.LOGIN_MAGIC_LINK, Step.MAGIC_LINK_REQUESTED)
        }

    @Test
    fun `given AppLogin Credentials payload, when scan succeeds, then dispatches and tracks legacy app-login success`() =
        testBlocking {
            whenever(parser.parse(RAW_SCAN))
                .thenReturn(QrLoginPayload.AppLogin.Credentials(siteUrl = SITE_URL, username = USERNAME))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            assertThat(events.last())
                .isEqualTo(Dispatch.RouteToAppLoginCredentials(siteUrl = SITE_URL, username = USERNAME))
            verify(analyticsTracker).track(
                AnalyticsEvent.LOGIN_APP_LOGIN_LINK_SUCCESS,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NO_WP_COM,
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_APP_LOGIN_SOURCE_QR,
                )
            )
        }

    @Test
    fun `given AppLogin WpComEmail payload, when scan succeeds, then dispatches and tracks legacy app-login success`() =
        testBlocking {
            whenever(parser.parse(RAW_SCAN))
                .thenReturn(QrLoginPayload.AppLogin.WpComEmail(siteUrl = SITE_URL, wpComEmail = "user@example.com"))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            assertThat(events.last())
                .isEqualTo(Dispatch.RouteToAppLoginWpComEmail(siteUrl = SITE_URL, wpComEmail = "user@example.com"))
            verify(analyticsTracker).track(
                AnalyticsEvent.LOGIN_APP_LOGIN_LINK_SUCCESS,
                mapOf(
                    AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_WP_COM,
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_APP_LOGIN_SOURCE_QR,
                )
            )
        }

    @Test
    fun `given InstallQrCode payload, when scan succeeds, then state is InstallQrCode error`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.InstallQrCode)

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.InstallQrCode)
        verify(unifiedLoginTracker).trackFailure("InstallQrCode")
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
        verify(unifiedLoginTracker).trackFailure("InvalidPayload")
    }

    // endregion

    // region session replace warning

    @Test
    fun `given user is logged in, when ticket scanned, then state is WarningSessionReplace and step is tracked`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)

            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isInstanceOf(UiState.WarningSessionReplace::class.java)
            verify(flowFactory, never()).create(any(), any())
            verify(unifiedLoginTracker).track(Flow.LOGIN_QR, Step.QR_SESSION_REPLACE_WARNING)
        }

    @Test
    fun `given session-replace shown, when user confirms and logout succeeds, then flow is created and click is tracked`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            viewModel.onConfirmSessionReplace()
            advanceUntilIdle()

            verify(unifiedLoginTracker).trackClick(Click.SUBMIT)
            verify(flowFactory).create(eq(ticket), any())
            assertThat(fakeFlow.startCount).isEqualTo(1)
        }

    @Test
    fun `given session-replace shown, when user confirms and logout fails, then state is Network error and failure tracked`() =
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
            verify(unifiedLoginTracker).trackFailure("Network:session_replace_logout_failed")
        }

    @Test
    fun `given session-replace shown, when user cancels, then state returns to Idle and dismiss tracked`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            viewModel.onCancelSessionReplace()

            assertThat(viewModel.uiState.value).isEqualTo(UiState.Idle)
            verify(unifiedLoginTracker).trackClick(Click.DISMISS)
        }

    // endregion

    // region resume recovery

    @Test
    fun `given OpenMagicLink dispatched, when screen resumes, then state returns to Idle and flow is cancelled`() =
        testBlocking {
            viewModel.onScanResult(successScan())
            advanceUntilIdle()
            fakeFlow.emit(FlowState.Authenticating(AuthPhase.Exchange))
            advanceUntilIdle()
            fakeFlow.emit(FlowState.Completed(FlowCompletion.OpenMagicLink(url = WP_COM_URL)))
            advanceUntilIdle()
            assertThat(viewModel.uiState.value).isEqualTo(UiState.Authenticating(AuthPhase.Exchange))
            val cancelCountBefore = fakeFlow.cancelCount

            viewModel.onScreenResumed()

            assertThat(viewModel.uiState.value).isEqualTo(UiState.Idle)
            assertThat(fakeFlow.cancelCount).isEqualTo(cancelCountBefore + 1)
        }

    @Test
    fun `given WpComMagicLink handoff dispatched, when screen resumes and user scans again, then handoff fires again`() =
        testBlocking {
            whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.WpComMagicLinkUrl(WP_COM_URL))
            val events = viewModel.event.captureValues()
            viewModel.onScanResult(successScan())
            advanceUntilIdle()
            val eventCountAfterFirstHandoff = events.size

            viewModel.onScreenResumed()
            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            assertThat(events.size).isGreaterThan(eventCountAfterFirstHandoff)
            assertThat(events.last()).isEqualTo(Dispatch.OpenWpComMagicLinkUrl(WP_COM_URL))
        }

    @Test
    fun `given no terminal event dispatched, when screen resumes, then state is unchanged`() = testBlocking {
        viewModel.onScanResult(successScan())
        advanceUntilIdle()
        fakeFlow.emit(
            FlowState.WaitingForApproval(
                sessionId = "sess",
                realNumber = "247",
                subtitleLabelRes = R.string.login_qr_match_account_label,
                subtitle = "user@example.com",
                expiresAtEpochMs = 0L,
            )
        )
        advanceUntilIdle()
        val stateBefore = viewModel.uiState.value

        viewModel.onScreenResumed()

        assertThat(viewModel.uiState.value).isEqualTo(stateBefore)
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
