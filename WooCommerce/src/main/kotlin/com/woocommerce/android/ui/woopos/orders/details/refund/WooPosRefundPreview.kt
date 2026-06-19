package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.refunds.RefundV4LineItem
import org.wordpress.android.fluxc.model.refunds.WCRefundPreview
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WCRefundStore
import javax.inject.Inject

/**
 * Fetches a server-calculated refund preview for the selected items, transparently falling back to
 * the v3 + local-calculation flow when the store does not expose the v4 endpoints.
 *
 * Detection strategy (WOOMOB-2693): attempt the v4 preview call; if it fails with a `404
 * rest_no_route` ([WooErrorType.API_NOT_FOUND]), the route is not registered, so mark v4
 * unavailable for the site (cached for the session) and fall back. A successful call marks v4
 * available so subsequent refunds skip the probe.
 */
class WooPosRefundPreview @Inject constructor(
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite,
    private val availabilityCache: WooPosV4RefundAvailabilityCache,
) {
    suspend operator fun invoke(
        orderId: Long,
        lineItems: List<RefundV4LineItem>,
    ): Result {
        val site = selectedSite.get()

        if (availabilityCache.isV4Available(site.siteId) == false) {
            return Result.FallbackToLocal
        }

        if (lineItems.isEmpty()) {
            // An empty request is rejected by the server with rest_invalid_param (400), not the
            // 404 we use for detection — so never probe with nothing selected.
            return Result.FallbackToLocal
        }

        val response = refundStore.previewRefund(site, orderId, lineItems)
        return when {
            response.isError -> {
                if (response.error.type == WooErrorType.API_NOT_FOUND) {
                    WooLog.i(WooLog.T.POS, "WooPosRefund: v4 preview not available; falling back to v3")
                    availabilityCache.markV4Unavailable(site.siteId)
                    Result.FallbackToLocal
                } else {
                    WooLog.e(
                        WooLog.T.POS,
                        "WooPosRefund: v4 preview failed orderId=$orderId, type=${response.error.type}, " +
                            "message=${response.error.message}"
                    )
                    Result.Error
                }
            }

            response.model != null -> {
                availabilityCache.markV4Available(site.siteId)
                Result.ServerCalculated(response.model!!)
            }

            else -> Result.Error
        }
    }

    sealed interface Result {
        data class ServerCalculated(val preview: WCRefundPreview) : Result
        data object FallbackToLocal : Result
        data object Error : Result
    }
}
