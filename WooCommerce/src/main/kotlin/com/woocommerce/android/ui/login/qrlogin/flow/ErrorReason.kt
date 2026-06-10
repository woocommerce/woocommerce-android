package com.woocommerce.android.ui.login.qrlogin.flow

/**
 * QR login failure reasons surfaced to the user.
 *
 * Flow producers:
 *
 *  - **Shared** — both site and wp.com flows can produce these.
 *  - **Site-only** ([NotAWooSite], [UserNotEligible], [SiteAuthFailure]) — produced only during
 *    the post-exchange site fetch + Application-Password save + eligibility check. The wp.com
 *    flow ends at "open magic link" and never inspects a specific site, so it cannot produce
 *    these.
 *
 * Notable mappings:
 *
 *  - [MatchAlreadyCompleted]: wp.com 500 + `already_consumed` — the exchange grant was already
 *    used (likely by a previous /exchange call). The site protocol folds this case into
 *    [MatchInvalidGrant] (412 + `invalid_exchange_grant`) and cannot distinguish it, so only the
 *    wp.com flow produces [MatchAlreadyCompleted].
 *
 * Errors NOT modelled:
 *
 *  - `no_number_matching` (wp.com 400): unreachable because the client always sends
 *    `supports_number_matching=true`. Folded into [Unknown] with a diagnostic log.
 */
sealed interface ErrorReason {
    data object Network : ErrorReason
    data object RateLimited : ErrorReason
    data object ServerError : ErrorReason
    data object Unknown : ErrorReason
    data object EndpointMissing : ErrorReason
    data object TokenRejected : ErrorReason
    data object MatchTimedOut : ErrorReason
    data object MatchRejected : ErrorReason
    data object MatchAlreadyScanned : ErrorReason
    data object MatchInvalidGrant : ErrorReason
    data object MatchAlreadyCompleted : ErrorReason
    data object InvalidPayload : ErrorReason
    data object Scanner : ErrorReason
    data object InstallQrCode : ErrorReason

    // Site-only — produced only by SiteQrLoginFlow during the post-exchange validation step.
    data object NotAWooSite : ErrorReason
    data object UserNotEligible : ErrorReason
    data object SiteAuthFailure : ErrorReason
}

internal fun ErrorReason.isRetryable(): Boolean = when (this) {
    ErrorReason.Network,
    ErrorReason.ServerError,
    ErrorReason.RateLimited -> true
    else -> false
}
