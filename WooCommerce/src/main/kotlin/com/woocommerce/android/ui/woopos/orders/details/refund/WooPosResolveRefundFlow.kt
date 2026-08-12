package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.WooLog
import javax.inject.Inject

/**
 * The refund flow to use for the selected store.
 *
 * [ServerComputed] means the store is eligible for server-calculated refunds: the preview endpoint
 * (`POST /wc/v3/orders/<order_id>/refunds/preview`) resolves the totals and the refund is created
 * with `compute_totals=true`. [LocalComputed] is the legacy flow where totals are calculated
 * on-device and the classic v3 item refund is sent. It is the flow for stores below
 * [WooPosResolveRefundFlow.MIN_WC_VERSION_FOR_SERVER_REFUNDS], and also for a disabled feature
 * flag, an unknown store version, or a store already probed as lacking the server endpoints. It is
 * meant to be deleted once every supported store ships the server endpoints.
 */
sealed interface WooPosRefundFlow {
    /**
     * [wooVersion] is the store's WooCommerce version this decision was made against. Callers pass
     * it back when reading or writing [WooPosServerRefundAvailabilityCache], so a probe result is
     * never applied to a store running a different version than the one probed.
     */
    data class ServerComputed(val wooVersion: String) : WooPosRefundFlow
    data object LocalComputed : WooPosRefundFlow
}

/**
 * Decides which refund flow the selected store should use:
 * - the feature flag must be enabled,
 * - the cached WooCommerce core version must be known and at least
 *   [MIN_WC_VERSION_FOR_SERVER_REFUNDS]; an unknown version fails closed to [LocalComputed],
 * - and the store must not already be known to lack the server endpoints
 *   (see [WooPosServerRefundAvailabilityCache]).
 *
 * The version requirement is authoritative for the create capability and cannot be bypassed by a
 * successful preview: the preview route and `compute_totals` create support ship in separate
 * WooCommerce core changes, so a preview succeeding only proves the preview route exists. A store
 * with the preview but without `compute_totals` (partial backport, or the changes splitting across
 * releases) would silently drop the parameter and create a zero-amount refund while restocking
 * items. Only a version known to contain both changes unlocks the server flow.
 *
 * [WooPosRefundFlow.ServerComputed] is eligibility, not proof: the availability cache is only set
 * `true` by a successful preview, and only that value permits sending the computed create (see
 * [WooPosRefundViewModel.buildSubmissionRequest]).
 */
class WooPosResolveRefundFlow @Inject constructor(
    private val selectedSite: SelectedSite,
    private val availabilityCache: WooPosServerRefundAvailabilityCache,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
    private val featureFlagRepository: FeatureFlagRepository,
) {
    operator fun invoke(): WooPosRefundFlow {
        val wooVersion = getWooCoreVersion()
        val eligibleVersion = when {
            !featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_SERVER_REFUNDS) ->
                fallBackToLocal("feature flag disabled")
            // Unknown version fails closed: eligibility must never rest on the preview probe alone,
            // because the preview route does not prove `compute_totals` create support.
            wooVersion == null -> fallBackToLocal("WooCommerce version unknown")
            wooVersion.semverCompareTo(MIN_WC_VERSION_FOR_SERVER_REFUNDS) < 0 ->
                fallBackToLocal("WooCommerce $wooVersion is below $MIN_WC_VERSION_FOR_SERVER_REFUNDS")
            availabilityCache.isAvailable(selectedSite.get().localId().value, wooVersion) == false ->
                fallBackToLocal("a preview probe found no server refund support on WooCommerce $wooVersion")
            else -> wooVersion
        }

        return eligibleVersion?.let {
            WooLog.i(WooLog.T.POS, "WooPosRefund: server calculation eligible on WooCommerce $it")
            WooPosRefundFlow.ServerComputed(it)
        } ?: WooPosRefundFlow.LocalComputed
    }

    /**
     * Logs why the store falls back, then reports "no eligible version" to the caller. Without this
     * a store on local calculation leaves no trace of the reason, which makes "why are this store's
     * totals local?" reports hard to answer — especially since an unknown version fails closed.
     */
    private fun fallBackToLocal(reason: String): String? {
        WooLog.i(WooLog.T.POS, "WooPosRefund: using local calculation, $reason")
        return null
    }

    companion object {
        /**
         * The earliest WooCommerce core release guaranteed to contain BOTH the `/wc/v3` refund
         * preview route (woocommerce/woocommerce#67042) and the `compute_totals` create support
         * (woocommerce/woocommerce#67043). If the two land in different releases, this constant
         * must point at the release containing the latter.
         */
        const val MIN_WC_VERSION_FOR_SERVER_REFUNDS = "11.1.0"
    }
}
