package com.woocommerce.android.ui.payments.refunds

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.WooLog
import javax.inject.Inject

/**
 * The refund flow to use for the selected store.
 *
 * [ServerComputed] means the store can resolve totals through the preview endpoint
 * (`POST /wc/v3/orders/<order_id>/refunds/preview`) and create the refund with
 * `compute_totals=true`. [LocalComputed] is the legacy flow: totals are calculated on-device and
 * the classic v3 item refund is sent. It goes away once every supported store ships the server
 * endpoints.
 */
sealed interface RefundCalculationFlow {
    /**
     * [wooVersion] is the version this decision was made against. Callers pass it back to
     * [ServerRefundAvailabilityCache] so a probe result is never reused on another version.
     */
    data class ServerComputed(val wooVersion: String) : RefundCalculationFlow
    data object LocalComputed : RefundCalculationFlow
}

/**
 * Decides which refund flow the selected store uses. Server calculation needs a known WooCommerce
 * version of at least [MIN_WC_VERSION_FOR_SERVER_REFUNDS] and no earlier preview probe that found
 * the endpoints missing (see [ServerRefundAvailabilityCache]). Anything else falls back to
 * [LocalComputed].
 *
 * [RefundCalculationFlow.ServerComputed] means eligible, not confirmed. Only a successful preview sets
 * the availability cache to `true`, and only that permits the computed create (see
 * `WooPosRefundViewModel.buildSubmissionRequest`).
 */
class ResolveRefundFlow @Inject constructor(
    private val selectedSite: SelectedSite,
    private val availabilityCache: ServerRefundAvailabilityCache,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
) {
    operator fun invoke(): RefundCalculationFlow {
        val wooVersion = getWooCoreVersion()
        val eligibleVersion = when {
            // An unknown version means the plugin info was never fetched. Fail closed.
            wooVersion == null -> fallBackToLocal("WooCommerce version unknown")
            wooVersion.semverCompareTo(MIN_WC_VERSION_FOR_SERVER_REFUNDS) < 0 ->
                fallBackToLocal("WooCommerce $wooVersion is below $MIN_WC_VERSION_FOR_SERVER_REFUNDS")
            availabilityCache.isAvailable(selectedSite.get().localId().value, wooVersion) == false ->
                fallBackToLocal("a preview probe found no server refund support on WooCommerce $wooVersion")
            else -> wooVersion
        }

        return eligibleVersion?.let {
            WooLog.i(WooLog.T.ORDERS, "Refund: server calculation eligible on WooCommerce $it")
            RefundCalculationFlow.ServerComputed(it)
        } ?: RefundCalculationFlow.LocalComputed
    }

    /** Logs the reason, then reports "no eligible version" so a fallback is never silent. */
    private fun fallBackToLocal(reason: String): String? {
        WooLog.i(WooLog.T.ORDERS, "Refund: using local calculation, $reason")
        return null
    }

    companion object {
        /**
         * The WooCommerce release that shipped both the `/wc/v3` refund preview route
         * (woocommerce/woocommerce#67042) and the `compute_totals` create (#67043). A successful
         * preview already proves both, so this is a second gate: it answers before any preview has
         * run.
         */
        const val MIN_WC_VERSION_FOR_SERVER_REFUNDS = "11.1.0"
    }
}
