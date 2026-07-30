package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import javax.inject.Inject

/**
 * The refund flow to use for the selected store.
 *
 * [ServerComputed] means the store is eligible for server-calculated refunds: the preview endpoint
 * (`POST /wc/v3/orders/<order_id>/refunds/preview`) resolves the totals and the refund is created
 * with `compute_totals=true`. [LocalComputed] is the legacy flow where totals are calculated
 * on-device and the classic v3 item refund is sent; it exists only for stores below
 * [WooPosResolveRefundFlow.MIN_WC_VERSION_FOR_SERVER_REFUNDS] and is meant to be deleted once
 * those stores are no longer supported.
 */
sealed interface WooPosRefundFlow {
    data object ServerComputed : WooPosRefundFlow
    data object LocalComputed : WooPosRefundFlow
}

/**
 * Decides which refund flow the selected store should use:
 * - the feature flag must be enabled,
 * - the cached WooCommerce core version, when known, must be at least
 *   [MIN_WC_VERSION_FOR_SERVER_REFUNDS] (unknown versions stay eligible so the preview probe can
 *   settle it),
 * - and the store must not already be known to lack the server endpoints
 *   (see [WooPosServerRefundAvailabilityCache]).
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
        if (!featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_REFUND_V4)) {
            return WooPosRefundFlow.LocalComputed
        }
        if (isWooVersionBelowServerRefundSupport()) {
            return WooPosRefundFlow.LocalComputed
        }
        if (availabilityCache.isAvailable(selectedSite.get().localId().value) == false) {
            return WooPosRefundFlow.LocalComputed
        }
        return WooPosRefundFlow.ServerComputed
    }

    private fun isWooVersionBelowServerRefundSupport(): Boolean {
        val version = getWooCoreVersion() ?: return false
        return version.semverCompareTo(MIN_WC_VERSION_FOR_SERVER_REFUNDS) < 0
    }

    companion object {
        /**
         * The WooCommerce core release that ships the `/wc/v3` refund preview route and the
         * `compute_totals` create support.
         */
        const val MIN_WC_VERSION_FOR_SERVER_REFUNDS = "11.1.0"
    }
}
