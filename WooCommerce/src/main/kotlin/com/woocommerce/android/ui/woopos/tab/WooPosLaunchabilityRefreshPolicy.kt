package com.woocommerce.android.ui.woopos.tab

/**
 * How far a launchability check may go to answer.
 *
 * The WooCommerce system status report is slow, so background callers must not ask for it.
 */
enum class WooPosLaunchabilityRefreshPolicy {
    /** Answer from stored values only. Makes no request. */
    UseCache,

    /** Answer from stored values, then refresh them in the background for the next check. */
    UseCacheAndRefresh,

    /** Fetch and wait for the answer. */
    ForceRefresh,
}
