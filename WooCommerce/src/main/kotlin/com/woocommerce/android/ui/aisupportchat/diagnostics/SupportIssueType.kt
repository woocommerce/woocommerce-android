package com.woocommerce.android.ui.aisupportchat.diagnostics

import androidx.annotation.StringRes
import com.woocommerce.android.R

/**
 * Categories the user can pick from in the AI Support Chat issue picker.
 */
enum class SupportIssueType(
    @StringRes val displayLabel: Int,
    val initialMessage: String
) {
    LOADING_ORDERS(
        displayLabel = R.string.ai_support_chat_issue_loading_orders,
        initialMessage = "I can't see my orders"
    ),
    LOADING_PRODUCTS(
        displayLabel = R.string.ai_support_chat_issue_loading_products,
        initialMessage = "I can't see my products"
    ),
    LOADING_ANALYTICS(
        displayLabel = R.string.ai_support_chat_issue_loading_analytics,
        initialMessage = "My analytics aren't loading"
    ),
    RECEIVING_NOTIFICATIONS(
        displayLabel = R.string.ai_support_chat_issue_receiving_notifications,
        initialMessage = "I'm not receiving notifications"
    ),
    OTHER(
        displayLabel = R.string.ai_support_chat_issue_other,
        initialMessage = "Something else"
    )
}
