package com.woocommerce.android.ui.aisupportchat.diagnostics

import androidx.annotation.StringRes
import com.woocommerce.android.R

/**
 * Categories the user can pick from in the AI Support Chat issue picker.
 */
enum class SupportIssueType(@StringRes val displayLabel: Int) {
    LOADING_ORDERS(R.string.ai_support_chat_issue_loading_orders),
    LOADING_PRODUCTS(R.string.ai_support_chat_issue_loading_products),
    LOADING_ANALYTICS(R.string.ai_support_chat_issue_loading_analytics),
    RECEIVING_NOTIFICATIONS(R.string.ai_support_chat_issue_receiving_notifications),
    OTHER(R.string.ai_support_chat_issue_other);

    companion object {
        val selectableEntries = listOf(LOADING_ORDERS, LOADING_PRODUCTS, LOADING_ANALYTICS, OTHER)
    }
}
