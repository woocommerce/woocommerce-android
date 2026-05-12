package com.woocommerce.android.ui.login.qrlogin.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A single run of the QR login protocol: scan → wait-for-approval → exchange → finish.
 *
 * Two implementations exist:
 *
 *  - [SiteQrLoginFlow] talks to the merchant's wp-admin and ends with an Application Password +
 *    a logged-in site ([FlowCompletion.LoggedIn]).
 *  - [WpComQrLoginFlow] talks to public-api.wordpress.com and ends with a single-use magic-link
 *    URL ([FlowCompletion.OpenMagicLink]) that the Fragment opens in a Custom Tab; the existing
 *    `MagicLinkInterceptActivity` then completes the OAuth sign-in.
 *
 * The state machine shape is identical for both: only the transport, the completion shape, and a
 * few flow-specific error codes differ. The ViewModel selects an implementation via
 * [QrLoginFlowFactory], forwards [state] into its `UiState`, and forwards [analyticsEvents] into
 * the analytics tracker.
 */
interface QrLoginFlow {
    val state: StateFlow<FlowState>
    val analyticsEvents: Flow<FlowAnalyticsEvent>

    /** Kick off the flow. Safe to call once; subsequent calls are ignored. */
    fun start()

    /** Cancel any in-flight network work and any active polling loop. */
    fun cancel()

    /**
     * Re-run the most recent retryable step (scan or exchange) using the retained input. No-op if
     * the flow's current state is not a retryable [FlowState.Failed].
     */
    fun retry()
}

sealed interface FlowState {
    data object Initial : FlowState
    data class Authenticating(val phase: AuthPhase) : FlowState
    data class WaitingForApproval(
        val sessionId: String,
        val realNumber: String,
        val subtitle: String,
        val expiresAtEpochMs: Long,
    ) : FlowState
    data class Failed(val reason: ErrorReason, val retryable: Boolean) : FlowState
    data class Completed(val completion: FlowCompletion) : FlowState
}

enum class AuthPhase {
    Scan,
    Exchange,
    Complete,
}

/** Analytics-facing breakdown of where in the flow a failure occurred. */
enum class FailureStep {
    Scan,
    Poll,
    Approve,
    Exchange,
    Auth,
}

/**
 * Terminal success shape of a flow. The ViewModel translates each variant into the matching
 * `Dispatch` event for its `triggerEvent` listener.
 */
sealed interface FlowCompletion {
    data class LoggedIn(val localSiteId: Int) : FlowCompletion
    data class OpenMagicLink(val url: String) : FlowCompletion
}

sealed interface FlowAnalyticsEvent {
    data class Failure(
        val step: FailureStep,
        val errorContext: String?,
        val reason: ErrorReason,
        val extras: Map<String, Any> = emptyMap(),
    ) : FlowAnalyticsEvent

    data object Success : FlowAnalyticsEvent
}
