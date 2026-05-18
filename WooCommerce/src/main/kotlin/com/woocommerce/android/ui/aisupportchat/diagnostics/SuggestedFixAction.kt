package com.woocommerce.android.ui.aisupportchat.diagnostics

/**
 * Action the user can take after a failed diagnostics run.
 *
 * The diagnostics service currently exposes retry as the only supported action.
 * Targeted actions can be added here when the diagnostics UI supports rendering
 * and handling issue-specific recovery steps.
 */
sealed interface SuggestedFixAction {
    data object RetryDiagnostics : SuggestedFixAction
}
