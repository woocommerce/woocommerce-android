package com.woocommerce.android.ui.login.qrlogin.flow

import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.delay

/**
 * Loop calling [poll] until the server reaches a terminal state ([PollOutcome.Approved],
 * [PollOutcome.Rejected], [PollOutcome.Expired], [PollOutcome.AlreadyCompleted]) or until a
 * terminal transient error accumulates.
 *
 * The first call fires immediately (no leading delay) — there's no point waiting [POLL_INTERVAL_MS]
 * for the first tick when the server-side state may already have advanced.
 *
 * [shouldContinue] is consulted before every step so the caller can abort by flipping its own
 * flag (typically: "still in WaitingForApproval state"). If it returns false mid-loop, the
 * function returns `null` and the caller drops the result on the floor.
 *
 * Transient errors are tolerated up to [MAX_CONSECUTIVE_POLL_ERRORS]; on the threshold (or
 * immediately, if [PollOutcome.TransientError.terminal] is set) the loop returns the error
 * outcome and the caller surfaces it.
 */
@Suppress("ReturnCount")
internal suspend fun pollUntilTerminal(
    shouldContinue: () -> Boolean,
    poll: suspend () -> PollOutcome,
): PollOutcome? {
    var consecutiveErrors = 0
    var firstTick = true
    while (shouldContinue()) {
        if (firstTick) firstTick = false else delay(POLL_INTERVAL_MS)
        if (!shouldContinue()) return null
        val outcome = poll()
        if (!shouldContinue()) return null
        when (outcome) {
            is PollOutcome.Scanned -> {
                consecutiveErrors = 0
                WooLog.d(WooLog.T.LOGIN, "QR login poll: response=Scanned")
            }
            is PollOutcome.TransientError -> {
                consecutiveErrors++
                WooLog.w(
                    WooLog.T.LOGIN,
                    "QR login poll: failed (consecutive=$consecutiveErrors): ${outcome.cause}"
                )
                if (outcome.terminal || consecutiveErrors >= MAX_CONSECUTIVE_POLL_ERRORS) return outcome
            }
            else -> return outcome
        }
    }
    return null
}

internal const val POLL_INTERVAL_MS = 2_000L
internal const val MAX_CONSECUTIVE_POLL_ERRORS = 4
