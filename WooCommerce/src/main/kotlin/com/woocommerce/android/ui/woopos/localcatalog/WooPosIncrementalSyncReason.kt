package com.woocommerce.android.ui.woopos.localcatalog

/**
 * Enum representing the reasons for triggering an incremental sync of the local catalog.
 *
 * This provides type-safe reasons and better documentation compared to using string literals.
 * The reason is used for logging to help with debugging and monitoring sync behavior.
 */
enum class WooPosIncrementalSyncReason(val description: String) {
    AFTER_SUCCESSFUL_PAYMENT("after successful payment"),
}
