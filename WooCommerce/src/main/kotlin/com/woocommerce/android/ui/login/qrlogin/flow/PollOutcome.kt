package com.woocommerce.android.ui.login.qrlogin.flow

/**
 * Protocol-agnostic outcome of a single session-status poll, used by [pollUntilTerminal].
 *
 * Each [QrLoginFlow] adapts its transport-level response shape into this small sum type so the
 * polling loop can stay flow-agnostic.
 */
internal sealed interface PollOutcome {
    /** Server says the QR was scanned but the merchant hasn't tapped yet. Keep polling. */
    data object Scanned : PollOutcome

    /** Approved — caller should proceed to /exchange with [grant]. Terminal for the loop. */
    data class Approved(val grant: String) : PollOutcome

    /** Merchant rejected the sign-in. Terminal. */
    data object Rejected : PollOutcome

    /** 90-second approval window elapsed. Terminal. */
    data object Expired : PollOutcome

    /** wp.com only: a previous /exchange already consumed the session. Terminal. */
    data object AlreadyCompleted : PollOutcome

    /**
     * Mapped from a poll exception. The loop increments its consecutive-error counter; if
     * [terminal] is true (e.g. rate-limit or endpoint-missing) it stops immediately, otherwise
     * it keeps polling up to the consecutive-error threshold.
     */
    data class TransientError(
        val reason: ErrorReason,
        val terminal: Boolean,
        val cause: Throwable,
    ) : PollOutcome
}
