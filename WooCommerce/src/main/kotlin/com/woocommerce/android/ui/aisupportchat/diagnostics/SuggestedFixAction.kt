package com.woocommerce.android.ui.aisupportchat.diagnostics

/**
 * Action the user can take after a failed diagnostics run.
 */
sealed interface SuggestedFixAction {
    data object EnableAnalytics : SuggestedFixAction
}
