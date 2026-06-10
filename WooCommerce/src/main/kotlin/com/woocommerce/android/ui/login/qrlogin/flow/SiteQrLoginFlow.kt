package com.woocommerce.android.ui.login.qrlogin.flow

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.R
import com.woocommerce.android.network.qrlogin.QrLoginExchangeException
import com.woocommerce.android.network.qrlogin.QrLoginRestClient
import com.woocommerce.android.network.qrlogin.QrLoginScanException
import com.woocommerce.android.network.qrlogin.QrLoginSessionStatus
import com.woocommerce.android.network.qrlogin.QrLoginSessionStatusException
import com.woocommerce.android.ui.login.WPApiSiteRepository.CookieNonceAuthenticationException
import com.woocommerce.android.ui.login.qrlogin.QrLoginAuthenticationException
import com.woocommerce.android.ui.login.qrlogin.QrLoginAuthenticator
import com.woocommerce.android.ui.login.qrlogin.QrLoginPayload
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import java.io.IOException

/**
 * [QrLoginFlow] implementation for the self-hosted (wp-admin) path. Drives:
 *
 *   1. `/qr-login-scan` → returns a session id, the real number, and a 90-second TTL.
 *   2. `/qr-login-session-status` polled until the merchant taps the matching number.
 *   3. `/qr-login-exchange` → returns an Application Password.
 *   4. [QrLoginAuthenticator.completeLogin] → fetches the site, persists the AP, validates the
 *      user, and promotes the site to the selected site.
 *
 * Ends with [FlowCompletion.LoggedIn] on success. The ViewModel translates that into
 * `Dispatch.LoggedIn(localSiteId)` and navigates the activity to the main app.
 *
 * One instance per scan attempt — the flow is single-use. [QrLoginFlowFactory] constructs a
 * fresh flow on every payload hand-off.
 */
internal class SiteQrLoginFlow(
    private val ticket: QrLoginPayload.Ticket,
    private val scope: CoroutineScope,
    private val restClient: QrLoginRestClient,
    private val authenticator: QrLoginAuthenticator,
) : QrLoginFlow {

    private val _state = MutableStateFlow<FlowState>(FlowState.Initial)
    override val state: StateFlow<FlowState> = _state.asStateFlow()

    private var activeJob: Job? = null
    private var retainedGrant: String? = null
    // Holds the WaitingForApproval snapshot from the most recent successful scan so a retry from
    // a Poll-step failure can resume polling the existing session instead of re-issuing the scan.
    // Set the moment we transition into WaitingForApproval; consulted only from retry()'s Poll branch.
    private var retainedWaitingForApproval: FlowState.WaitingForApproval? = null

    override fun start() {
        if (_state.value !is FlowState.Initial) return
        startScan()
    }

    override fun cancel() {
        activeJob?.cancel()
        activeJob = null
        retainedGrant = null
        retainedWaitingForApproval = null
        _state.value = FlowState.Initial
    }

    override fun retry() {
        val failed = _state.value as? FlowState.Failed ?: return
        if (!failed.retryable) return
        when (failed.failedAt) {
            FailureStep.Exchange -> retainedGrant?.let(::startExchange) ?: startScan()
            FailureStep.Poll -> retainedWaitingForApproval?.let(::resumePolling) ?: startScan()
            else -> startScan()
        }
    }

    private fun resumePolling(waiting: FlowState.WaitingForApproval) {
        _state.value = waiting
        activeJob = scope.launch { pollUntilApprovedOrTerminal(sessionId = waiting.sessionId) }
    }

    private fun startScan() {
        retainedGrant = null
        retainedWaitingForApproval = null
        _state.value = FlowState.Authenticating(AuthPhase.Scan)
        activeJob = scope.launch {
            restClient.scan(ticket.siteUrl, ticket.token).fold(
                onSuccess = { scan ->
                    val expiresAt = System.currentTimeMillis() + scan.expiresInSeconds * MILLIS_PER_SECOND
                    _state.value = FlowState.WaitingForApproval(
                        sessionId = scan.sessionId,
                        realNumber = scan.realNumber,
                        subtitleLabelRes = R.string.login_qr_match_host_label,
                        subtitle = ticket.siteUrl.toDisplayHost(),
                        expiresAtEpochMs = expiresAt,
                    ).also { retainedWaitingForApproval = it }
                    pollUntilApprovedOrTerminal(sessionId = scan.sessionId)
                },
                onFailure = { failure ->
                    failWith(step = FailureStep.Scan, reason = failure.toScanReason())
                }
            )
        }
    }

    private suspend fun pollUntilApprovedOrTerminal(sessionId: String) {
        WooLog.d(WooLog.T.LOGIN, "QR login site poll: starting")
        val outcome = pollUntilTerminal(
            shouldContinue = { _state.value is FlowState.WaitingForApproval },
            poll = {
                restClient.checkSessionStatus(ticket.siteUrl, sessionId, ticket.token).fold(
                    onSuccess = { it.toPollOutcome() },
                    onFailure = { it.toPollErrorOutcome() }
                )
            }
        ) ?: return // shouldContinue went false (cancel/retry mid-loop) — drop outcome.

        when (outcome) {
            is PollOutcome.Approved -> startExchange(outcome.grant)
            PollOutcome.Rejected -> failApproveTerminal(ErrorReason.MatchRejected)
            PollOutcome.Expired -> failApproveTerminal(ErrorReason.MatchTimedOut)
            PollOutcome.AlreadyCompleted -> failApproveTerminal(ErrorReason.MatchAlreadyCompleted)
            is PollOutcome.TransientError -> failWith(step = FailureStep.Poll, reason = outcome.reason)
            PollOutcome.Scanned -> Unit // pollUntilTerminal never returns Scanned at terminal time
        }
    }

    private fun startExchange(exchangeGrant: String) {
        retainedGrant = exchangeGrant
        _state.value = FlowState.Authenticating(AuthPhase.Exchange)
        activeJob = scope.launch {
            restClient.exchange(ticket.siteUrl, ticket.token, exchangeGrant).fold(
                onSuccess = { credentials ->
                    _state.value = FlowState.Authenticating(AuthPhase.Complete)
                    completeLogin(credentials)
                },
                onFailure = { failure ->
                    val httpCode = (failure as? QrLoginExchangeException.HttpError)?.code
                    failWith(
                        step = FailureStep.Exchange,
                        reason = failure.toExchangeReason(),
                        extras = httpCode?.let { mapOf(KEY_ERROR_CODE to it) }.orEmpty(),
                    )
                }
            )
        }
    }

    private suspend fun completeLogin(
        credentials: com.woocommerce.android.network.qrlogin.QrLoginCredentials,
    ) {
        authenticator.completeLogin(ticket, credentials).fold(
            onSuccess = { localSiteId ->
                retainedGrant = null
                _state.value = FlowState.Completed(FlowCompletion.LoggedIn(localSiteId = localSiteId))
            },
            onFailure = { failure ->
                failWith(step = FailureStep.Auth, reason = failure.toAuthReason(), retryable = false)
            }
        )
    }

    private fun failApproveTerminal(reason: ErrorReason) {
        retainedGrant = null
        _state.value = FlowState.Failed(
            reason = reason,
            retryable = false,
            failedAt = FailureStep.Approve,
        )
    }

    private fun failWith(
        step: FailureStep,
        reason: ErrorReason,
        extras: Map<String, Any> = emptyMap(),
        retryable: Boolean = reason.isRetryable(),
    ) {
        if (!retryable) retainedGrant = null
        _state.value = FlowState.Failed(
            reason = reason,
            retryable = retryable,
            failedAt = step,
            extras = extras,
        )
    }

    private fun QrLoginSessionStatus.toPollOutcome(): PollOutcome = when (this) {
        QrLoginSessionStatus.Scanned -> PollOutcome.Scanned
        is QrLoginSessionStatus.Approved -> PollOutcome.Approved(grant)
        QrLoginSessionStatus.Rejected -> PollOutcome.Rejected
        QrLoginSessionStatus.Expired -> PollOutcome.Expired
    }

    private fun Throwable.toPollErrorOutcome(): PollOutcome = PollOutcome.TransientError(
        reason = toPollReason(),
        terminal = this is QrLoginSessionStatusException.RateLimited ||
            this is QrLoginSessionStatusException.EndpointMissing,
        cause = this,
    )

    private fun Throwable.toScanReason(): ErrorReason = when (this) {
        QrLoginScanException.TokenRejected -> ErrorReason.TokenRejected
        QrLoginScanException.AlreadyScanned -> ErrorReason.MatchAlreadyScanned
        QrLoginScanException.EndpointMissing -> ErrorReason.EndpointMissing
        QrLoginScanException.RateLimited -> ErrorReason.RateLimited
        QrLoginScanException.UpgradeRequired -> ErrorReason.EndpointMissing
        QrLoginScanException.Network -> ErrorReason.Network
        QrLoginScanException.MalformedRequest,
        QrLoginScanException.MalformedResponse -> ErrorReason.ServerError
        is QrLoginScanException.HttpError -> ErrorReason.ServerError
        is QrLoginScanException.Unknown -> ErrorReason.Unknown
        is IOException -> ErrorReason.Network
        else -> ErrorReason.Unknown
    }

    private fun Throwable.toPollReason(): ErrorReason = when (this) {
        QrLoginSessionStatusException.EndpointMissing -> ErrorReason.EndpointMissing
        QrLoginSessionStatusException.RateLimited -> ErrorReason.RateLimited
        QrLoginSessionStatusException.Network -> ErrorReason.Network
        QrLoginSessionStatusException.MalformedResponse -> ErrorReason.ServerError
        is QrLoginSessionStatusException.HttpError -> ErrorReason.ServerError
        is QrLoginSessionStatusException.Unknown -> ErrorReason.Unknown
        else -> {
            WooLog.w(WooLog.T.LOGIN, "QR login site poll: unmapped failure type ${javaClass.simpleName}")
            ErrorReason.Unknown
        }
    }

    private fun Throwable.toExchangeReason(): ErrorReason = when (this) {
        QrLoginExchangeException.TokenRejected -> ErrorReason.TokenRejected
        QrLoginExchangeException.EndpointMissing -> ErrorReason.EndpointMissing
        QrLoginExchangeException.RateLimited -> ErrorReason.RateLimited
        QrLoginExchangeException.Network -> ErrorReason.Network
        QrLoginExchangeException.MalformedResponse -> ErrorReason.ServerError
        QrLoginExchangeException.NotApproved -> ErrorReason.MatchTimedOut
        QrLoginExchangeException.InvalidExchangeGrant -> ErrorReason.MatchInvalidGrant
        is QrLoginExchangeException.HttpError -> ErrorReason.ServerError
        is QrLoginExchangeException.Unknown -> ErrorReason.Unknown
        else -> ErrorReason.Unknown
    }

    private fun Throwable.toAuthReason(): ErrorReason = when (this) {
        QrLoginAuthenticationException.NotAWooSite -> ErrorReason.NotAWooSite
        is QrLoginAuthenticationException.UserNotEligible -> ErrorReason.UserNotEligible
        is CookieNonceAuthenticationException -> ErrorReason.SiteAuthFailure
        is OnChangedException -> (this.error as? SiteError)?.type.toSiteErrorReason()
        // DNS, socket, SSL handshake, and read failures during post-exchange site discovery + AP
        // save bubble up as raw IOException without QrLoginExchangeException wrapping.
        is IOException -> ErrorReason.Network
        is CancellationException -> throw this
        else -> {
            WooLog.w(WooLog.T.LOGIN, "QR login site auth: unmapped failure type ${javaClass.simpleName}")
            ErrorReason.Unknown
        }
    }

    private fun SiteErrorType?.toSiteErrorReason(): ErrorReason = when (this) {
        SiteErrorType.UNAUTHORIZED,
        SiteErrorType.NOT_AUTHENTICATED -> ErrorReason.SiteAuthFailure
        SiteErrorType.WORDPRESS_COM_CONNECTIVITY_ERROR -> ErrorReason.Network
        else -> ErrorReason.Unknown
    }

    /**
     * Punycode-form the host so homograph attacks (`my-stōre.example` →
     * `xn--my-stre-1za.example`) surface in the confirmation UI. Non-default ports are kept.
     */
    private fun String.toDisplayHost(): String {
        val parsed = this.toHttpUrlOrNull() ?: return this
        val defaultPort = if (parsed.scheme == "https") HTTPS_DEFAULT_PORT else HTTP_DEFAULT_PORT
        return if (parsed.port == defaultPort) parsed.host else "${parsed.host}:${parsed.port}"
    }

    private companion object {
        const val HTTPS_DEFAULT_PORT = 443
        const val HTTP_DEFAULT_PORT = 80
        const val MILLIS_PER_SECOND = 1_000L
        const val KEY_ERROR_CODE = "error_code"
    }
}
