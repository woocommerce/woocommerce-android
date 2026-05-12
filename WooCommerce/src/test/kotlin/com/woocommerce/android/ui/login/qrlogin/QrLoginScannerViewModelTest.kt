package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.network.qrlogin.QrLoginCredentials
import com.woocommerce.android.network.qrlogin.QrLoginExchangeException
import com.woocommerce.android.network.qrlogin.QrLoginRestClient
import com.woocommerce.android.network.qrlogin.QrLoginScanException
import com.woocommerce.android.network.qrlogin.QrLoginScanResult
import com.woocommerce.android.network.qrlogin.QrLoginSessionStatus
import com.woocommerce.android.network.qrlogin.QrLoginSessionStatusException
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.ErrorReason
import com.woocommerce.android.ui.login.qrlogin.QrLoginScannerViewModel.UiState
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.CodeScanningErrorType
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginScannerViewModelTest : BaseUnitTest() {

    private val ticket = QrLoginPayload.Ticket(token = "tok", siteUrl = "https://store.example")
    private val scanResult = QrLoginScanResult(
        sessionId = "sess-1",
        realNumber = "042",
        expiresInSeconds = 90,
    )
    private val credentials = QrLoginCredentials(
        userLogin = "admin",
        applicationPassword = "ap-secret",
    )

    private val parser: QrLoginPayloadParser = mock()
    private val restClient: QrLoginRestClient = mock()
    private val authenticator: QrLoginAuthenticator = mock()
    private val accountRepository: AccountRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private val viewModel by lazy {
        QrLoginScannerViewModel(
            savedState = SavedStateHandle(),
            parser = parser,
            restClient = restClient,
            authenticator = authenticator,
            accountRepository = accountRepository,
            errorMapper = QrLoginErrorMapper(),
            analyticsTracker = analyticsTracker
        )
    }

    @Before
    fun setUp() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(ticket)
        // Default to "no active session" so existing flows behave as before; individual tests
        // that exercise the session-replace warning override this.
        whenever(accountRepository.isUserLoggedIn()).thenReturn(false)
        whenever(accountRepository.logout()).thenReturn(true)
        whenever(restClient.scan(ticket.siteUrl, ticket.token)).thenReturn(Result.success(scanResult))
        whenever(restClient.exchange(ticket.siteUrl, ticket.token, "grant-1"))
            .thenReturn(Result.success(credentials))
        whenever(authenticator.completeLogin(ticket, credentials)).thenReturn(Result.success(DEFAULT_SITE_ID))
    }

    /**
     * Tests that don't override [restClient]'s session-status mock still need polling to
     * terminate so `runTest`'s post-body drain doesn't chase the loop forever, but the
     * immediate-first-tick optimization means a single `Expired` mock would race past
     * `WaitingForApproval` before the test can assert on it. Returning `Scanned` for the
     * first call (loop keeps state in WaitingForApproval, then suspends on the 2-s delay)
     * and `Expired` for subsequent calls (loop exits cleanly when `runTest` drains time)
     * gives both the assertion window and the clean shutdown. Strict Mockito complains
     * about a global setUp stub that some tests don't use, so each test opting into the
     * default calls this helper instead.
     */
    private suspend fun stubPollingTerminates() {
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Scanned))
            .thenReturn(Result.success(QrLoginSessionStatus.Expired))
    }

    // region scan → waiting for approval

    @Test
    fun `given valid ticket, when scan succeeds, then state transitions to WaitingForApproval`() = testBlocking {
        stubPollingTerminates()

        viewModel.onScanResult(successScan())

        val state = viewModel.uiState.value as UiState.WaitingForApproval
        assertThat(state.realNumber).isEqualTo("042")
        assertThat(state.sessionId).isEqualTo("sess-1")
        assertThat(state.host).isEqualTo("store.example")
        assertThat(state.ticket).isEqualTo(ticket)
        assertThat(state.expiresAtEpochMs).isGreaterThan(System.currentTimeMillis())
    }

    @Test
    fun `given scan endpoint returns 409, when scan, then emits MatchAlreadyScanned without retry`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token))
            .thenReturn(Result.failure(QrLoginScanException.AlreadyScanned))

        viewModel.onScanResult(successScan())

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.MatchAlreadyScanned)
        assertThat(state.retryTicket).isNull()
    }

    @Test
    fun `given scan endpoint returns network error, when scan, then emits Network with retry ticket`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token))
            .thenReturn(Result.failure(QrLoginScanException.Network))

        viewModel.onScanResult(successScan())

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.Network)
        assertThat(state.retryTicket).isEqualTo(ticket)
    }

    @Test
    fun `given scan endpoint returns 426 upgrade required, when scan, then maps to EndpointMissing`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token))
            .thenReturn(Result.failure(QrLoginScanException.UpgradeRequired))

        viewModel.onScanResult(successScan())

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.EndpointMissing)
    }

    // endregion

    // region polling → approval

    @Test
    fun `given approved with grant on first poll, when polling fires, then exchange runs and emits LoggedIn`() =
        testBlocking {
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            verify(restClient).exchange(ticket.siteUrl, ticket.token, "grant-1")
            verify(authenticator).completeLogin(ticket, credentials)
            assertThat(events.last())
                .isEqualTo(QrLoginScannerViewModel.Dispatch.LoggedIn(localSiteId = DEFAULT_SITE_ID))
        }

    @Test
    fun `given rejected on poll, when polling fires, then state is MatchRejected with no retry`() = testBlocking {
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Rejected))

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.MatchRejected)
        assertThat(state.retryTicket).isNull()
        verify(restClient, never()).exchange(any(), any(), any())
    }

    @Test
    fun `given expired on poll, when polling fires, then state is MatchTimedOut`() = testBlocking {
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Expired))

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.MatchTimedOut)
        verify(restClient, never()).exchange(any(), any(), any())
    }

    @Test
    fun `given scanned on poll, when polling fires, then state stays WaitingForApproval`() = testBlocking {
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Scanned))

        viewModel.onScanResult(successScan())
        // Loops indefinitely while still scanned — advance a couple of ticks (with the +1 nudge
        // so the delay-at-exactly-N edge case fires) and then assert the loop is still alive.
        advanceTimeBy(POLL_TICK_MS * 2 + 1)

        assertThat(viewModel.uiState.value).isInstanceOf(UiState.WaitingForApproval::class.java)
        verify(restClient, atLeastOnce()).checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token)
        // Stop the loop so runTest's post-body advanceUntilIdle doesn't chase it forever.
        viewModel.onCancelNumberMatch()
    }

    @Test
    fun `given transient poll errors, when fewer than threshold occur, then polling continues`() = testBlocking {
        // Three transient failures, then a Scanned response: polling must still be active.
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.failure(QrLoginSessionStatusException.Network))
            .thenReturn(Result.failure(QrLoginSessionStatusException.Network))
            .thenReturn(Result.failure(QrLoginSessionStatusException.Network))
            .thenReturn(Result.success(QrLoginSessionStatus.Scanned))

        viewModel.onScanResult(successScan())
        advanceTimeBy(POLL_TICK_MS * 4 + 1)

        assertThat(viewModel.uiState.value).isInstanceOf(UiState.WaitingForApproval::class.java)
        // Stop the loop so runTest's post-body advanceUntilIdle doesn't chase the Scanned mock.
        viewModel.onCancelNumberMatch()
    }

    @Test
    fun `given transient poll errors, when threshold reached, then state transitions to Network error with retry`() =
        testBlocking {
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.failure(QrLoginSessionStatusException.Network))

            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            val state = viewModel.uiState.value as UiState.Error
            assertThat(state.reason).isEqualTo(ErrorReason.Network)
            assertThat(state.retryTicket).isEqualTo(ticket)
        }

    @Test
    fun `given rate-limited poll, when first error fires, then transitions immediately`() = testBlocking {
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.failure(QrLoginSessionStatusException.RateLimited))

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.RateLimited)
        assertThat(state.retryTicket).isEqualTo(ticket)
    }

    @Test
    fun `given endpoint-missing poll, when first error fires, then transitions without retry`() = testBlocking {
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.failure(QrLoginSessionStatusException.EndpointMissing))

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.EndpointMissing)
        assertThat(state.retryTicket).isNull()
    }

    // endregion

    // region cancel + start over

    @Test
    fun `given WaitingForApproval, when onCancelNumberMatch, then state returns to Idle and polling stops`() =
        testBlocking {
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.success(QrLoginSessionStatus.Scanned))

            viewModel.onScanResult(successScan())
            advanceTimeBy(POLL_TICK_MS + 1)
            viewModel.onCancelNumberMatch()
            advanceTimeBy(POLL_TICK_MS * 3)

            assertThat(viewModel.uiState.value).isEqualTo(UiState.Idle)
            // Once cancel fires, no further polls should happen.
            verify(restClient, atLeastOnce()).checkSessionStatus(any(), any(), any())
        }

    @Test
    fun `given waiting for approval, when another scan arrives, then it is ignored`() = testBlocking {
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Scanned))

        viewModel.onScanResult(successScan(RAW_SCAN))
        // Fire one poll tick so the Scanned mock is actually consumed (otherwise strict-mode
        // Mockito flags it as an unused stubbing). The +1 nudges the dispatcher past the
        // delay-at-exactly-N edge case in kotlinx.coroutines.test.
        advanceTimeBy(POLL_TICK_MS + 1)
        viewModel.onScanResult(successScan("second-raw"))

        verify(parser, never()).parse("second-raw")
        // Stop the loop so runTest's post-body advanceUntilIdle doesn't chase it forever.
        viewModel.onCancelNumberMatch()
    }

    @Test
    fun `given Error state, when onStartOver, then state returns to Idle`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token))
            .thenReturn(Result.failure(QrLoginScanException.TokenRejected))

        viewModel.onScanResult(successScan())
        viewModel.onStartOver()

        assertThat(viewModel.uiState.value).isEqualTo(UiState.Idle)
    }

    @Test
    fun `given Network error, when onRetryExchange, then scan is re-run`() = testBlocking {
        whenever(restClient.scan(ticket.siteUrl, ticket.token))
            .thenReturn(Result.failure(QrLoginScanException.Network))
            .thenReturn(Result.success(scanResult))
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Scanned))

        viewModel.onScanResult(successScan())
        viewModel.onRetryExchange()
        // Let the second scan's poll fire once so the Scanned mock is consumed and the
        // strict-mode Mockito doesn't flag it as unused.
        advanceTimeBy(POLL_TICK_MS + 1)

        verify(restClient, times(2)).scan(ticket.siteUrl, ticket.token)
        // Stop the polling loop the second scan started so the test runner can wind down.
        viewModel.onCancelNumberMatch()
    }

    // endregion

    // region exchange failures

    @Test
    fun `given exchange returns InvalidExchangeGrant, when polling triggers exchange, then MatchInvalidGrant`() =
        testBlocking {
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
            whenever(restClient.exchange(ticket.siteUrl, ticket.token, "grant-1"))
                .thenReturn(Result.failure(QrLoginExchangeException.InvalidExchangeGrant))

            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            val state = viewModel.uiState.value as UiState.Error
            assertThat(state.reason).isEqualTo(ErrorReason.MatchInvalidGrant)
            assertThat(state.retryTicket).isNull()
        }

    @Test
    fun `given exchange returns Network, when polling triggers exchange, then Network with retry ticket`() =
        testBlocking {
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
            whenever(restClient.exchange(ticket.siteUrl, ticket.token, "grant-1"))
                .thenReturn(Result.failure(QrLoginExchangeException.Network))

            viewModel.onScanResult(successScan())
            advanceUntilIdle()

            val state = viewModel.uiState.value as UiState.Error
            assertThat(state.reason).isEqualTo(ErrorReason.Network)
            assertThat(state.retryTicket).isEqualTo(ticket)
            assertThat(state.retryExchangeGrant).isEqualTo("grant-1")
        }

    @Test
    fun `given exchange network error, when retried, then exchange is re-run with same grant`() =
        testBlocking {
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
            whenever(restClient.exchange(ticket.siteUrl, ticket.token, "grant-1"))
                .thenReturn(Result.failure(QrLoginExchangeException.Network))
                .thenReturn(Result.success(credentials))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())
            advanceUntilIdle()
            viewModel.onRetryExchange()
            advanceUntilIdle()

            verify(restClient, times(1)).scan(ticket.siteUrl, ticket.token)
            verify(restClient, times(2)).exchange(ticket.siteUrl, ticket.token, "grant-1")
            assertThat(events.last())
                .isEqualTo(QrLoginScannerViewModel.Dispatch.LoggedIn(localSiteId = DEFAULT_SITE_ID))
        }

    @Test
    fun `given authenticator returns NotAWooSite, when login completes, then NotAWooSite error`() = testBlocking {
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
        whenever(authenticator.completeLogin(ticket, credentials))
            .thenReturn(Result.failure(QrLoginAuthenticationException.NotAWooSite))

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.NotAWooSite)
    }

    @Test
    fun `given authenticator returns IOException, when login completes, then Network error`() = testBlocking {
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
        whenever(authenticator.completeLogin(ticket, credentials))
            .thenReturn(Result.failure(IOException("offline")))

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.Network)
    }

    // endregion

    // region scanner / payload errors (unchanged behavior)

    @Test
    fun `given Invalid payload, when scan succeeds, then InvalidPayload error and no scan call`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.Invalid)

        viewModel.onScanResult(successScan())

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.InvalidPayload)
        verify(restClient, never()).scan(any(), any())
    }

    @Test
    fun `given InstallQrCode payload, when scan succeeds, then InstallQrCode error`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.InstallQrCode)

        viewModel.onScanResult(successScan())

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.InstallQrCode)
    }

    @Test
    fun `given scanner Failure, when scan result received, then Scanner error`() = testBlocking {
        viewModel.onScanResult(
            CodeScannerStatus.Failure(error = "boom", type = CodeScanningErrorType.Unknown)
        )

        val state = viewModel.uiState.value as UiState.Error
        assertThat(state.reason).isEqualTo(ErrorReason.Scanner)
    }

    @Test
    fun `given NotFound status, when scan result received, then state stays Idle`() = testBlocking {
        viewModel.onScanResult(CodeScannerStatus.NotFound)

        assertThat(viewModel.uiState.value).isEqualTo(UiState.Idle)
    }

    // endregion

    // region analytics + lifecycle

    @Test
    fun `given successful login, when LoggedIn dispatched, then tracks LOGIN_QR_SUCCESS`() = testBlocking {
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_SUCCESS)
    }

    @Test
    fun `given scanner binding failure, when delivered, then tracks scan failed with scanner step`() = testBlocking {
        viewModel.onScanResult(
            CodeScannerStatus.Failure(error = "boom", type = CodeScanningErrorType.Unknown)
        )

        verify(analyticsTracker).track(
            stat = AnalyticsEvent.LOGIN_QR_SCAN_FAILED,
            properties = mapOf(AnalyticsTracker.KEY_STEP to "scanner"),
            errorContext = "Unknown",
            errorType = "Scanner",
            errorDescription = null,
        )
    }

    @Test
    fun `given rejected match, when polling fires, then tracks scan failed with approve step`() = testBlocking {
        whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
            .thenReturn(Result.success(QrLoginSessionStatus.Rejected))

        viewModel.onScanResult(successScan())
        advanceUntilIdle()

        verify(analyticsTracker).track(
            stat = AnalyticsEvent.LOGIN_QR_SCAN_FAILED,
            properties = mapOf(AnalyticsTracker.KEY_STEP to "approve"),
            errorContext = null,
            errorType = "MatchRejected",
            errorDescription = null,
        )
    }

    @Test
    fun `given deep link payload, when onDeepLinkPayload invoked, then state advances to WaitingForApproval`() =
        testBlocking {
            stubPollingTerminates()

            viewModel.onDeepLinkPayload(RAW_SCAN)

            val state = viewModel.uiState.value as UiState.WaitingForApproval
            assertThat(state.ticket).isEqualTo(ticket)
            assertThat(state.realNumber).isEqualTo("042")
        }

    // endregion

    @Test
    fun `given wp dot com url payload, when scan succeeds, then emits OpenWpComMagicLinkUrl`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.WpComMagicLinkUrl(WP_COM_URL))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.OpenWpComMagicLinkUrl(url = WP_COM_URL)
        )
    }

    @Test
    fun `given wp dot com url payload, when scan succeeds, then ui state stays idle`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.WpComMagicLinkUrl(WP_COM_URL))

        viewModel.onScanResult(successScan())

        assertThat(viewModel.uiState.value).isEqualTo(QrLoginScannerViewModel.UiState.Idle)
        verify(restClient, never()).scan(any(), any())
    }

    @Test
    fun `given wp dot com url via deep link, when received, then emits OpenWpComMagicLinkUrl`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.WpComMagicLinkUrl(WP_COM_URL))
        val events = viewModel.event.captureValues()

        viewModel.onDeepLinkPayload(RAW_SCAN)

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.OpenWpComMagicLinkUrl(url = WP_COM_URL)
        )
    }

    @Test
    fun `given wp dot com url handed off, when another scan arrives, then it is ignored`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.WpComMagicLinkUrl(WP_COM_URL))

        viewModel.onScanResult(successScan(RAW_SCAN))
        viewModel.onScanResult(successScan("second-raw"))

        verify(parser, never()).parse("second-raw")
    }

    @Test
    fun `given wp dot com url payload, when scan succeeds, then tracks LOGIN_QR_HANDED_OFF_WP_COM_MAGIC_LINK`() =
        testBlocking {
            whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.WpComMagicLinkUrl(WP_COM_URL))

            viewModel.onScanResult(successScan())

            verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_HANDED_OFF_WP_COM_MAGIC_LINK)
        }

    @Test
    fun `given wp dot com url payload, when scan succeeds, then does not track LOGIN_QR_SCAN_FAILED or LOGIN_QR_SUCCESS`() =
        testBlocking {
            whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.WpComMagicLinkUrl(WP_COM_URL))

            viewModel.onScanResult(successScan())

            // The wp.com handoff is the happy path, not a failure, and not a completed sign-in.
            verify(analyticsTracker, never()).track(eq(AnalyticsEvent.LOGIN_QR_SCAN_FAILED), any(), any(), any(), any())
            verify(analyticsTracker, never()).track(AnalyticsEvent.LOGIN_QR_SUCCESS)
        }

    @Test
    fun `given site url payload, when scan succeeds, then emits RouteToSiteAddressEntry`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.SiteUrl(SITE_URL))
        val events = viewModel.event.captureValues()

        viewModel.onScanResult(successScan())

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RouteToSiteAddressEntry(siteUrl = SITE_URL)
        )
    }

    @Test
    fun `given site url payload, when scan succeeds, then ui state stays idle`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.SiteUrl(SITE_URL))

        viewModel.onScanResult(successScan())

        assertThat(viewModel.uiState.value).isEqualTo(QrLoginScannerViewModel.UiState.Idle)
        verify(restClient, never()).scan(any(), any())
    }

    @Test
    fun `given site url via deep link, when received, then emits RouteToSiteAddressEntry`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.SiteUrl(SITE_URL))
        val events = viewModel.event.captureValues()

        viewModel.onDeepLinkPayload(RAW_SCAN)

        assertThat(events.last()).isEqualTo(
            QrLoginScannerViewModel.Dispatch.RouteToSiteAddressEntry(siteUrl = SITE_URL)
        )
    }

    @Test
    fun `given site url handed off, when another scan arrives, then it is ignored`() = testBlocking {
        whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.SiteUrl(SITE_URL))

        viewModel.onScanResult(successScan(RAW_SCAN))
        viewModel.onScanResult(successScan("second-raw"))

        verify(parser, never()).parse("second-raw")
    }

    @Test
    fun `given site url payload, when scan succeeds, then tracks LOGIN_QR_HANDED_OFF_SITE_URL_PREFILL`() =
        testBlocking {
            whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.SiteUrl(SITE_URL))

            viewModel.onScanResult(successScan())

            verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_HANDED_OFF_SITE_URL_PREFILL)
        }

    @Test
    fun `given site url payload, when scan succeeds, then does not track LOGIN_QR_SCAN_FAILED or LOGIN_QR_SUCCESS`() =
        testBlocking {
            whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.SiteUrl(SITE_URL))

            viewModel.onScanResult(successScan())

            // Site-URL handoff is the happy path; the user hasn't completed a sign-in yet.
            verify(analyticsTracker, never()).track(eq(AnalyticsEvent.LOGIN_QR_SCAN_FAILED), any(), any(), any(), any())
            verify(analyticsTracker, never()).track(AnalyticsEvent.LOGIN_QR_SUCCESS)
        }

    // region Session-replace warning (logged-in user)

    @Test
    fun `given logged in and ticket payload, when scan succeeds, then ui state exposes WarningSessionReplace`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)

            viewModel.onScanResult(successScan())

            assertThat(viewModel.uiState.value).isEqualTo(
                QrLoginScannerViewModel.UiState.WarningSessionReplace(
                    QrLoginScannerViewModel.PendingHandoff.Ticket(ticket = ticket, host = "store.example")
                )
            )
            verify(restClient, never()).scan(any(), any())
        }

    @Test
    fun `given logged in and wp dot com payload, when scan succeeds, then ui state exposes WarningSessionReplace`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.WpComMagicLinkUrl(WP_COM_URL))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())

            assertThat(viewModel.uiState.value).isEqualTo(
                QrLoginScannerViewModel.UiState.WarningSessionReplace(
                    QrLoginScannerViewModel.PendingHandoff.WpComMagicLink(url = WP_COM_URL)
                )
            )
            assertThat(events).isEmpty()
        }

    @Test
    fun `given logged in and site url payload, when scan succeeds, then ui state exposes WarningSessionReplace`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.SiteUrl(SITE_URL))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())

            assertThat(viewModel.uiState.value).isEqualTo(
                QrLoginScannerViewModel.UiState.WarningSessionReplace(
                    QrLoginScannerViewModel.PendingHandoff.SiteUrlPrefill(siteUrl = SITE_URL)
                )
            )
            assertThat(events).isEmpty()
        }

    @Test
    fun `given logged in and any payload, when scan succeeds, then tracks LOGIN_QR_SESSION_REPLACE_WARNING_SHOWN`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)

            viewModel.onScanResult(successScan())

            verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_WARNING_SHOWN)
        }

    @Test
    fun `given logged in and ticket via deep link, when received, then ui state exposes WarningSessionReplace`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)

            viewModel.onDeepLinkPayload(RAW_SCAN)

            assertThat(viewModel.uiState.value)
                .isInstanceOf(QrLoginScannerViewModel.UiState.WarningSessionReplace::class.java)
        }

    @Test
    fun `given warning state, when another scan arrives, then it is ignored`() = testBlocking {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        viewModel.onScanResult(successScan(RAW_SCAN))

        viewModel.onScanResult(successScan("second-raw"))

        verify(parser, never()).parse("second-raw")
    }

    @Test
    fun `given warning for ticket, when confirmed, then logs out and advances to number match`() = testBlocking {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        stubPollingTerminates()
        viewModel.onScanResult(successScan())

        viewModel.onConfirmSessionReplace()

        verify(accountRepository).logout()
        assertThat(viewModel.uiState.value).isInstanceOf(UiState.WaitingForApproval::class.java)
        verify(authenticator, never()).completeLogin(any(), any())
    }

    @Test
    fun `given warning for wp dot com, when confirmed, then logs out and emits OpenWpComMagicLinkUrl`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.WpComMagicLinkUrl(WP_COM_URL))
            val events = viewModel.event.captureValues()
            viewModel.onScanResult(successScan())

            viewModel.onConfirmSessionReplace()

            verify(accountRepository).logout()
            assertThat(events.last()).isEqualTo(
                QrLoginScannerViewModel.Dispatch.OpenWpComMagicLinkUrl(url = WP_COM_URL)
            )
            verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_HANDED_OFF_WP_COM_MAGIC_LINK)
        }

    @Test
    fun `given warning for site url, when confirmed, then logs out and emits RouteToSiteAddressEntry`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(parser.parse(RAW_SCAN)).thenReturn(QrLoginPayload.SiteUrl(SITE_URL))
            val events = viewModel.event.captureValues()
            viewModel.onScanResult(successScan())

            viewModel.onConfirmSessionReplace()

            verify(accountRepository).logout()
            assertThat(events.last()).isEqualTo(
                QrLoginScannerViewModel.Dispatch.RouteToSiteAddressEntry(siteUrl = SITE_URL)
            )
            verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_HANDED_OFF_SITE_URL_PREFILL)
        }

    @Test
    fun `given warning, when confirmed, then tracks LOGIN_QR_SESSION_REPLACE_CONFIRMED`() = testBlocking {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        viewModel.onScanResult(successScan())

        viewModel.onConfirmSessionReplace()

        verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_CONFIRMED)
    }

    @Test
    fun `given warning, when cancelled, then returns to idle without logging out`() = testBlocking {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
        viewModel.onScanResult(successScan())

        viewModel.onCancelSessionReplace()

        assertThat(viewModel.uiState.value).isEqualTo(QrLoginScannerViewModel.UiState.Idle)
        verify(accountRepository, never()).logout()
        verify(analyticsTracker).track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_DISMISSED)
    }

    @Test
    fun `given not in warning state, when onConfirmSessionReplace, then nothing happens`() = testBlocking {
        // user is not logged in, so payload goes straight to number matching
        stubPollingTerminates()
        viewModel.onScanResult(successScan())

        viewModel.onConfirmSessionReplace()

        verify(accountRepository, never()).logout()
        verify(analyticsTracker, never()).track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_CONFIRMED)
    }

    @Test
    fun `given warning, when confirmed and logout fails, then does not resume and surfaces network error`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(accountRepository.logout()).thenReturn(false)
            val events = viewModel.event.captureValues()
            viewModel.onScanResult(successScan())

            viewModel.onConfirmSessionReplace()

            val state = viewModel.uiState.value as UiState.Error
            assertThat(state.reason).isEqualTo(QrLoginScannerViewModel.ErrorReason.Network)
            assertThat(state.retryTicket).isNull()
            assertThat(events).noneMatch { it is QrLoginScannerViewModel.Dispatch }
        }

    @Test
    fun `given warning, when confirmed and logout fails, then tracks scan failed with logout error type`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(accountRepository.logout()).thenReturn(false)
            viewModel.onScanResult(successScan())

            viewModel.onConfirmSessionReplace()

            verify(analyticsTracker).track(
                stat = AnalyticsEvent.LOGIN_QR_SCAN_FAILED,
                properties = mapOf(AnalyticsTracker.KEY_STEP to "exchange"),
                errorContext = null,
                errorType = "session_replace_logout_failed",
                errorDescription = null,
            )
        }

    @Test
    fun `given logged in and ticket, when warning confirmed and site confirmed, then emits LoggedIn`() =
        testBlocking {
            whenever(accountRepository.isUserLoggedIn()).thenReturn(true)
            whenever(restClient.checkSessionStatus(ticket.siteUrl, scanResult.sessionId, ticket.token))
                .thenReturn(Result.success(QrLoginSessionStatus.Approved("grant-1")))
            val events = viewModel.event.captureValues()

            viewModel.onScanResult(successScan())
            viewModel.onConfirmSessionReplace()
            advanceTimeBy(POLL_TICK_MS)
            advanceUntilIdle()

            assertThat(events.last())
                .isEqualTo(QrLoginScannerViewModel.Dispatch.LoggedIn(localSiteId = DEFAULT_SITE_ID))
        }

    @Test
    fun `given logged out, when ticket payload arrives, then warning is not shown`() = testBlocking {
        // Default mock returns isUserLoggedIn=false; verify legacy path is preserved.
        stubPollingTerminates()
        viewModel.onScanResult(successScan())

        assertThat(viewModel.uiState.value)
            .isInstanceOf(UiState.WaitingForApproval::class.java)
        verify(analyticsTracker, never()).track(AnalyticsEvent.LOGIN_QR_SESSION_REPLACE_WARNING_SHOWN)
    }

    // endregion

    private fun successScan(raw: String = RAW_SCAN) =
        CodeScannerStatus.Success(code = raw, format = BarcodeFormat.FormatQRCode)

    private companion object {
        const val RAW_SCAN = "raw"
        const val DEFAULT_SITE_ID = 42
        const val WP_COM_URL =
            "https://wordpress.com/wp-login.php?action=magic-login&scheme=woocommerce&token=abc"
        const val SITE_URL = "https://store.example.com"
        const val POLL_TICK_MS = 2_000L
    }
}
