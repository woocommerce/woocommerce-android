package com.woocommerce.android.ui.login.qrlogin.flow

import com.woocommerce.android.R
import com.woocommerce.android.network.qrlogin.WpComQrLoginExchangeException
import com.woocommerce.android.network.qrlogin.WpComQrLoginRestClient
import com.woocommerce.android.network.qrlogin.WpComQrLoginScanException
import com.woocommerce.android.network.qrlogin.WpComQrLoginSessionStatus
import com.woocommerce.android.network.qrlogin.WpComQrLoginSessionStatusException
import com.woocommerce.android.ui.login.qrlogin.QrLoginPayload
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * [QrLoginFlow] implementation for the wp.com path. Drives:
 *
 *   1. `POST /wpcom/v2/auth/qr-code-app/scan` → returns session id, real number, the user's
 *      wp.com email, and a 90-second TTL.
 *   2. `GET /wpcom/v2/auth/qr-code-app/session-status` polled until the user approves on
 *      wordpress.com (or rejects / lets it expire).
 *   3. `POST /wpcom/v2/auth/qr-code-app/exchange` → returns a single-use magic-link URL.
 *
 * Ends with [FlowCompletion.OpenMagicLink] on success. The ViewModel translates that into
 * `Dispatch.OpenWpComMagicLinkUrl`, the Fragment opens it in a Custom Tab, and wp.com 3xx-redirects
 * to `woocommerce://magic-login` which `MagicLinkInterceptActivity` finishes.
 *
 * No site-fetch / Application-Password persistence — that's the wp-admin variant
 * ([SiteQrLoginFlow]). One instance per scan attempt.
 */
internal class WpComQrLoginFlow(
    private val payload: QrLoginPayload.WpComToken,
    private val scope: CoroutineScope,
    private val restClient: WpComQrLoginRestClient,
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
            restClient.scan(token = payload.token, encrypted = payload.encrypted).fold(
                onSuccess = { scan ->
                    val expiresAt = System.currentTimeMillis() + scan.expiresInSeconds * MILLIS_PER_SECOND
                    _state.value = FlowState.WaitingForApproval(
                        sessionId = scan.sessionId,
                        realNumber = scan.realNumber,
                        subtitleLabelRes = R.string.login_qr_match_account_label,
                        subtitle = scan.userEmail,
                        expiresAtEpochMs = expiresAt,
                    ).also { retainedWaitingForApproval = it }
                    pollUntilApprovedOrTerminal(sessionId = scan.sessionId, token = payload.token)
                },
                onFailure = { failure ->
                    failWith(step = FailureStep.Scan, reason = failure.toScanReason())
                }
            )
        }
    }

    private suspend fun pollUntilApprovedOrTerminal(sessionId: String, token: String) {
        WooLog.d(WooLog.T.LOGIN, "QR login wp.com poll: starting")
        val outcome = pollUntilTerminal(
            shouldContinue = { _state.value is FlowState.WaitingForApproval },
            poll = {
                restClient.checkSessionStatus(sessionId, token).fold(
                    onSuccess = { it.toPollOutcome() },
                    onFailure = { it.toPollErrorOutcome() }
                )
            }
        ) ?: return

        when (outcome) {
            is PollOutcome.Approved -> startExchange(outcome.grant)
            PollOutcome.Rejected -> failApproveTerminal(ErrorReason.MatchRejected)
            PollOutcome.Expired -> failApproveTerminal(ErrorReason.MatchTimedOut)
            PollOutcome.AlreadyCompleted -> failApproveTerminal(ErrorReason.MatchAlreadyCompleted)
            is PollOutcome.TransientError -> failWith(step = FailureStep.Poll, reason = outcome.reason)
            PollOutcome.Scanned -> Unit
        }
    }

    private fun startExchange(exchangeGrant: String) {
        retainedGrant = exchangeGrant
        _state.value = FlowState.Authenticating(AuthPhase.Exchange)
        activeJob = scope.launch {
            restClient.exchange(
                token = payload.token,
                encrypted = payload.encrypted,
                exchangeGrant = exchangeGrant,
            ).fold(
                onSuccess = { result ->
                    retainedGrant = null
                    _state.value = FlowState.Completed(FlowCompletion.OpenMagicLink(url = result.magicLinkUrl))
                },
                onFailure = { failure ->
                    val httpCode = (failure as? WpComQrLoginExchangeException.HttpError)?.code
                    failWith(
                        step = FailureStep.Exchange,
                        reason = failure.toExchangeReason(),
                        extras = httpCode?.let { mapOf(KEY_ERROR_CODE to it) }.orEmpty(),
                    )
                }
            )
        }
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

    private fun WpComQrLoginSessionStatus.toPollOutcome(): PollOutcome = when (this) {
        WpComQrLoginSessionStatus.Scanned -> PollOutcome.Scanned
        is WpComQrLoginSessionStatus.Approved -> PollOutcome.Approved(grant)
        WpComQrLoginSessionStatus.Rejected -> PollOutcome.Rejected
        WpComQrLoginSessionStatus.Expired -> PollOutcome.Expired
        WpComQrLoginSessionStatus.Consumed -> PollOutcome.AlreadyCompleted
    }

    private fun Throwable.toPollErrorOutcome(): PollOutcome = PollOutcome.TransientError(
        reason = toPollReason(),
        terminal = isTerminalPollFailure(),
        cause = this,
    )

    private fun Throwable.isTerminalPollFailure(): Boolean = when (this) {
        WpComQrLoginSessionStatusException.RateLimited,
        // 403 — token-hash mismatch. Per spec: treat as compromised, abort, require fresh scan.
        WpComQrLoginSessionStatusException.TokenHashMismatch -> true
        else -> false
    }

    private fun Throwable.toScanReason(): ErrorReason = when (this) {
        WpComQrLoginScanException.RestForbidden -> ErrorReason.TokenRejected
        WpComQrLoginScanException.SessionNotFound -> ErrorReason.TokenRejected
        WpComQrLoginScanException.AlreadyScanned -> ErrorReason.MatchAlreadyScanned
        WpComQrLoginScanException.RateLimited -> ErrorReason.RateLimited
        WpComQrLoginScanException.Network -> ErrorReason.Network
        WpComQrLoginScanException.MalformedResponse -> ErrorReason.ServerError
        is WpComQrLoginScanException.HttpError -> ErrorReason.ServerError
        is WpComQrLoginScanException.Unknown -> ErrorReason.Unknown
        // Unreachable as long as supports_number_matching=true is always sent — log if it happens.
        WpComQrLoginScanException.NoNumberMatching -> {
            WooLog.w(WooLog.T.LOGIN, "QR login wp.com scan: unexpected no_number_matching from server")
            ErrorReason.Unknown
        }
        is IOException -> ErrorReason.Network
        else -> ErrorReason.Unknown
    }

    private fun Throwable.toPollReason(): ErrorReason = when (this) {
        WpComQrLoginSessionStatusException.RateLimited -> ErrorReason.RateLimited
        WpComQrLoginSessionStatusException.Network -> ErrorReason.Network
        WpComQrLoginSessionStatusException.MalformedResponse -> ErrorReason.ServerError
        WpComQrLoginSessionStatusException.TokenHashMismatch -> {
            WooLog.w(WooLog.T.LOGIN, "QR login wp.com poll: token_hash did not match session — aborting")
            ErrorReason.TokenRejected
        }
        is WpComQrLoginSessionStatusException.HttpError -> ErrorReason.ServerError
        is WpComQrLoginSessionStatusException.Unknown -> ErrorReason.Unknown
        else -> {
            WooLog.w(WooLog.T.LOGIN, "QR login wp.com poll: unmapped failure type ${javaClass.simpleName}")
            ErrorReason.Unknown
        }
    }

    private fun Throwable.toExchangeReason(): ErrorReason = when (this) {
        WpComQrLoginExchangeException.NotApproved -> ErrorReason.MatchTimedOut
        WpComQrLoginExchangeException.InvalidExchangeGrant -> ErrorReason.MatchInvalidGrant
        WpComQrLoginExchangeException.AlreadyConsumed -> ErrorReason.MatchAlreadyCompleted
        WpComQrLoginExchangeException.SessionNotFound -> ErrorReason.MatchInvalidGrant
        WpComQrLoginExchangeException.RateLimited -> ErrorReason.RateLimited
        WpComQrLoginExchangeException.Network -> ErrorReason.Network
        WpComQrLoginExchangeException.MalformedResponse -> ErrorReason.ServerError
        is WpComQrLoginExchangeException.HttpError -> ErrorReason.ServerError
        is WpComQrLoginExchangeException.Unknown -> ErrorReason.Unknown
        else -> ErrorReason.Unknown
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val KEY_ERROR_CODE = "error_code"
    }
}
